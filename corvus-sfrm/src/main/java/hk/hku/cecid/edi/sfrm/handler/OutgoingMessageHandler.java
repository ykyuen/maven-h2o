package hk.hku.cecid.edi.sfrm.handler;

import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import hk.hku.cecid.edi.sfrm.pkg.SFRMConstant;
import hk.hku.cecid.edi.sfrm.pkg.SFRMMessage;
import hk.hku.cecid.edi.sfrm.pkg.SFRMMessageException;

import hk.hku.cecid.edi.sfrm.dao.SFRMPartnershipDVO;
import hk.hku.cecid.edi.sfrm.dao.SFRMMessageDVO;

import hk.hku.cecid.edi.sfrm.spa.SFRMLog;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;

import hk.hku.cecid.piazza.commons.net.FastHttpConnector;
import hk.hku.cecid.piazza.commons.net.ConnectionException;
import hk.hku.cecid.piazza.commons.security.KeyStoreManager;
import hk.hku.cecid.piazza.commons.security.SMimeException;
import hk.hku.cecid.piazza.commons.security.TrustedHostnameVerifier;

/**
 * The outgoing message handler is a singleton classes
 * that provides service for processing outgoing SFRM 
 * message.<br><br>
 * 
 * Creation Date: 5/12/2006
 * 
 * @author Twinsen Tsang
 * @version 1.0.0
 * @since	1.0.3
 */
public class OutgoingMessageHandler {
	
	static{
		System.setProperty("sun.net.client.defaultConnectTimeout", "60000");
		System.setProperty("sun.net.client.defaultReadTimeout"	 , "60000");
	}
		
	/**
	 * Singleton Handler.
	 */
	private static OutgoingMessageHandler omh = new OutgoingMessageHandler();
	
	/**
	 * @return an instnace of OutgoingMessageHandler.
	 */
	public static OutgoingMessageHandler getInstance(){
		return omh;
	}
	
	/**
	 * Pack the SMIME (secure MIME) message to become 
	 * secured SFRM Message.
	 * <br><br>
	 * 
	 * Currently, the packing mechanisms support: <br>
	 * <ol>
	 * 	<li> Digitial Signing using MD5 or SHA-1 </li>
	 *  <li> Encryption using RC2_CBC or DES_EDE3_CBC </li>
	 * </ol>
	 *   
	 * @param message
	 * 			The outgoing SFRM Message.
	 * @param msgDVO
	 * 			The message record associated to this SFRM message. 
	 * @param pDVO
	 * 			
	 * @return
	 * 			The secured SFRM message.
	 * @throws UnrecoverableKeyException 
	 * @throws NoSuchAlgorithmException 
	 * @throws SFRMMessageException 
	 * 
	 * @throws SFRMMessageException 			
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableKeyException
	 * @throws SMimeException
	 * 
	 * @since	
	 * 			1.0.3 
	 */
	protected SFRMMessage packOutgoingMessage(
			SFRMMessage message, SFRMMessageDVO	msgDVO, SFRMPartnershipDVO pDVO) 
		throws SFRMMessageException, NoSuchAlgorithmException, UnrecoverableKeyException {
	
		// No need to sign and encrypt, return immediately.
		if (!msgDVO.getIsSigned() && !msgDVO.getIsEncrypted())
			return message;
		
		// Create SMIME Header.
		KeyStoreManager keyman = SFRMProcessor.getKeyStoreManager();
		
		String logInfo = " msg id: " + message.getMessageID()
						+" and sgt no: " + message.getSegmentNo();
		
		// Setup up signing using MD5 or SHA1		
		if (msgDVO.getIsSigned() && pDVO.getSignAlgorithm() != null){
			SFRMProcessor.core.log.info(SFRMLog.OMH_CALLER + SFRMLog.SIGNING_SGT + logInfo);  
			message.sign(keyman.getX509Certificate(), keyman.getPrivateKey(), pDVO.getSignAlgorithm());
		}
		
		// Setup up encrypting using RC2, DES
		if (msgDVO.getIsEncrypted() && pDVO.getEncryptAlgorithm() != null) {
			SFRMProcessor.core.log.info(SFRMLog.OMH_CALLER + SFRMLog.ENCRYPT_SGT + logInfo); 
			message.encrypt(pDVO.getEncryptX509Certificate(), pDVO.getEncryptAlgorithm());
		}
						
		return message;
	}
	
	/**
	 * @param message
	 * @param pDVO
	 * @param msgDVO
	 * 
	 * @throws Exception
	 */
	public void processOutgoingMessage(SFRMMessage message, SFRMPartnershipDVO pDVO, SFRMMessageDVO	msgDVO) 
		throws Exception {
		
		if (message == null)
			throw new NullPointerException("Missing SFRM Message.");
		if (pDVO == null)
			throw new NullPointerException("Missing Partnership Record.");
		if (msgDVO == null)
			throw new NullPointerException("Missing Message Record.");		
		// Pack the SFRM Message
		// TODO: All segment should use this method to pack, re-design interface.
		if (message.getSegmentType().equals(SFRMConstant.MSGT_META))
			message = this.packOutgoingMessage(message, msgDVO, pDVO);
		
		// Create the HTTP Connection.
		// TODO: Use connection pool.
		FastHttpConnector httpConn = new FastHttpConnector
			(msgDVO.getPartnerEndpoint());
		
		// Add SSL Verification if switched on.
		if (pDVO.isHostnameVerified())
			httpConn.setHostnameVerifier(new TrustedHostnameVerifier());
		
		// Log sending information.
		SFRMProcessor.core.log.info(
			  SFRMLog.OMH_CALLER
		   +  SFRMLog.SEND_SGT
		   +" To " + msgDVO.getPartnerEndpoint()
		   +" with msg info"
		   +  message);
		
		try {
			httpConn.send(message.getContentStream(), message.getHeaders());
			
			int responseCode = httpConn.getResponseCode();
			
			SFRMProcessor.core.log.debug("Response code for handshaking: " + responseCode);

			if (responseCode != 200)
				throw new ConnectionException("Invalid Response Code.");
 
		} catch (ConnectionException ce) {
			throw ce;
		} 
	}
	
}
