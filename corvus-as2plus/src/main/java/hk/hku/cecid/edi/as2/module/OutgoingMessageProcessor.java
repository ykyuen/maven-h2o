package hk.hku.cecid.edi.as2.module;

import java.io.InputStream;
import java.util.Properties;

import javax.activation.DataSource;
import hk.hku.cecid.edi.as2.dao.AS2DAOHandler;
import hk.hku.cecid.edi.as2.dao.MessageDVO;
import hk.hku.cecid.edi.as2.dao.PartnershipDVO;
import hk.hku.cecid.edi.as2.dao.RepositoryDVO;
import hk.hku.cecid.edi.as2.pkg.AS2Header;
import hk.hku.cecid.edi.as2.pkg.AS2Message;
import hk.hku.cecid.edi.as2.pkg.DispositionNotificationOption;
import hk.hku.cecid.piazza.commons.activation.InputStreamDataSource;
import hk.hku.cecid.piazza.commons.module.SystemComponent;
import hk.hku.cecid.piazza.commons.security.KeyStoreManager;
import hk.hku.cecid.piazza.commons.security.SMimeException;
import hk.hku.cecid.piazza.commons.security.SMimeMessage;

public class OutgoingMessageProcessor extends SystemComponent{
	
	private Properties payloadTypes;
	
	@Override
	protected void init() throws Exception {
		super.init();

		String[] types = getProperties().getProperties("/as2/content_type/type");
		Properties props = new Properties();
		for(String type :types){
			String[] tokens = type.split(";");
			props.setProperty(tokens[0], tokens[1]);
		}
		payloadTypes = props;
	}
	
	public AS2Message storeOutgoingMessage(
			String messageID, String fromPartyID, String toPartyID, String type,
			PartnershipDVO partnership, DataSource attachementSource) throws Exception{
		
		AS2Message as2Message;
		try {
			as2Message = new AS2Message();

			// Attach Header Value
			as2Message.setMessageID(messageID);
			as2Message.setFromPartyID(fromPartyID);
			as2Message.setToPartyID(toPartyID);
			as2Message.setHeader(AS2Header.SUBJECT, partnership.getSubject());

			// Set Content to Message
			as2Message.setContent(attachementSource, 
						getPayloadContentType(attachementSource.getContentType()));
			if(attachementSource.getName() != null){
				as2Message.getBodyPart().addHeader("Content-Disposition", "attachment; filename=" + attachementSource.getName());
			}
			
	        String micAlg = null;
	        if (partnership.isReceiptRequired()) {
	            String returnUrl = null;
	            if (!partnership.isSyncReply()) {
	                returnUrl = partnership.getReceiptAddress();
	            }
	            if (partnership.isReceiptSignRequired()) {
	                micAlg = partnership.getMicAlgorithm();
	            }
	            as2Message.requestReceipt(returnUrl, micAlg);
	        }
	       
	        KeyStoreManager keyman =(KeyStoreManager) getComponent("keystore-manager");
	        SMimeMessage smime = new SMimeMessage(as2Message.getBodyPart(), keyman.getX509Certificate(), keyman.getPrivateKey());
	        smime.setContentTransferEncoding(SMimeMessage.CONTENT_TRANSFER_ENC_BINARY);
	        
	        String mic = calculateMIC(smime, partnership);
	        
	        if (partnership.isOutboundCompressRequired()) {
	            getLogger().info("Compressing outbound "+as2Message);
	            smime = smime.compress();
	            if (partnership.isOutboundSignRequired()) {
	                mic = calculateMIC(smime, partnership);
	            }
	        }
	        if (partnership.isOutboundSignRequired()) {
	            getLogger().info("Signing outbound "+as2Message);
	            String alg = partnership.getSignAlgorithm();
	            if (alg != null && alg.equalsIgnoreCase(PartnershipDVO.ALG_SIGN_MD5)) {
	                smime.setDigestAlgorithm(SMimeMessage.DIGEST_ALG_MD5);
	            }
	            else {
	                smime.setDigestAlgorithm(SMimeMessage.DIGEST_ALG_SHA1);
	            }
	            smime = smime.sign();
	        }
	        if (partnership.isOutboundEncryptRequired()) {
	            getLogger().info("Encrypting outbound "+as2Message);
	            String alg = partnership.getEncryptAlgorithm();
	            if (alg != null && alg.equalsIgnoreCase(PartnershipDVO.ALG_ENCRYPT_RC2)) {
	                smime.setEncryptAlgorithm(SMimeMessage.ENCRYPT_ALG_RC2_CBC);
	            }
	            else {
	                smime.setEncryptAlgorithm(SMimeMessage.ENCRYPT_ALG_DES_EDE3_CBC);
	            }
	            smime = smime.encrypt(partnership.getEncryptX509Certificate());
	        }
	        as2Message.setBodyPart(smime.getBodyPart());
	        
	        // Persist Message to Database
	        getLogger().info("Persisting outbound "+as2Message);
	        AS2DAOHandler daoHandler = new AS2DAOHandler(getDAOFactory());
	        RepositoryDVO repositoryDVO = daoHandler.createRepositoryDVO(as2Message, false);
	        MessageDVO messageDVO = daoHandler.createMessageDVO(as2Message, false); 
	        messageDVO.setStatus(MessageDVO.STATUS_PENDING);
	        messageDVO.setMicValue(mic);
	        
	        /* Capture the outgoing message */
	        daoHandler.createMessageStore().storeMessage(messageDVO, repositoryDVO);
	        getLogger().debug("AS2 Message is stored on database. ["+as2Message.getMessageID()+"]");
	        
	        return as2Message;
	        
		} catch (Exception e) {
			throw new Exception("OutgoingPayloadProcessor error", e);
		}
		
	}
	
	public AS2Message storeOutgoingMessage(String fromPartyID, String toPartyID, String type,
			PartnershipDVO partnership, DataSource attachementSource) throws Exception{
		return storeOutgoingMessage(AS2Message.generateID(), fromPartyID, toPartyID, type, partnership, attachementSource);
	}
	
	public AS2Message storeOutgoingMessage(
			String fromPartyID, String toPartyID, String type,
			PartnershipDVO partnership, InputStream messageAttachement) throws Exception{
		InputStreamDataSource insDS = new InputStreamDataSource(messageAttachement, type, "payload");
		return storeOutgoingMessage(AS2Message.generateID(),fromPartyID, toPartyID, type, partnership, insDS);
	}
	
	public AS2Message storeOutgoingMessage(String messageID,
			String fromPartyID, String toPartyID, String type,
			PartnershipDVO partnership, InputStream messageAttachement) throws Exception{
		InputStreamDataSource insDS = new InputStreamDataSource(messageAttachement, type, "payload");
		return storeOutgoingMessage(messageID, fromPartyID, toPartyID, type, partnership, insDS);
	}
	
    public String calculateMIC(SMimeMessage smime, PartnershipDVO partnership) throws SMimeException {
        String mic = null;
        if (partnership.isReceiptSignRequired()) {
            boolean isSMime = partnership.isOutboundCompressRequired() ||
                              partnership.isOutboundSignRequired() ||
                              partnership.isOutboundEncryptRequired();
            
            String micAlg = partnership.getMicAlgorithm();
            if (micAlg !=null && micAlg.equalsIgnoreCase(PartnershipDVO.ALG_MIC_MD5)) {
                mic = smime.digest(SMimeMessage.DIGEST_ALG_MD5, isSMime);
                micAlg = DispositionNotificationOption.SIGNED_RECEIPT_MICALG_MD5;
            }
            else {
                mic = smime.digest(SMimeMessage.DIGEST_ALG_SHA1, isSMime);
                micAlg = DispositionNotificationOption.SIGNED_RECEIPT_MICALG_SHA1;
            }
            mic =  mic + ", " + micAlg;
        }
        return mic;
    }

    //Return the content-type mapping that defined on OutgoingMessageProcessor Component Parameters
    private String getPayloadContentType(String type){
    	if(type == null){
    		return "application/octet-stream";
    	}else{
    		 String t = payloadTypes.getProperty(type);
             if (t == null) {
                 return"application/octet-stream";
             }
             else {
                 return t.toString();
             }
    	}
    }
    
}