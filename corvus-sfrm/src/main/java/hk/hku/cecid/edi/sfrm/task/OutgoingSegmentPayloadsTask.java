/**
 * Contains the set of active module collector for 
 * handling packaging, segmenting of payloads.
 */
package hk.hku.cecid.edi.sfrm.task;

import java.sql.Timestamp;

import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.activation.DataSource;
import javax.mail.MessagingException;

import hk.hku.cecid.edi.sfrm.spa.SFRMLog;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;

import hk.hku.cecid.edi.sfrm.com.PackagedPayloads;

import hk.hku.cecid.edi.sfrm.handler.OutgoingMessageHandler;
import hk.hku.cecid.edi.sfrm.handler.SFRMMessageSegmentHandler;

import hk.hku.cecid.edi.sfrm.dao.SFRMPartnershipDVO;
import hk.hku.cecid.edi.sfrm.dao.SFRMMessageDVO;
import hk.hku.cecid.edi.sfrm.dao.SFRMMessageSegmentDVO;

import hk.hku.cecid.edi.sfrm.pkg.SFRMConstant;
import hk.hku.cecid.edi.sfrm.pkg.SFRMMessage;
import hk.hku.cecid.edi.sfrm.pkg.SFRMMessageClassifier;
import hk.hku.cecid.edi.sfrm.pkg.SFRMMessageException;

import hk.hku.cecid.piazza.commons.activation.FileRegionDataSource;
import hk.hku.cecid.piazza.commons.activation.EmptyDataSource;
import hk.hku.cecid.piazza.commons.module.ActiveTaskAdaptor;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.security.KeyStoreManager;
import hk.hku.cecid.piazza.commons.security.SMimeException;

/**
 * 
 * 
 * 
 * Creation Date: 9/10/2006
 * 
 * @author Twinsen Tsang
 * @version 1.0.0
 * @since	1.0.0
 */
public class OutgoingSegmentPayloadsTask extends ActiveTaskAdaptor {

	/**
	 * The packaged payload.
	 */
	private final PackagedPayloads 		payload;
	
	/**
	 * The sending payload of this tasks.
	 */
	private final SFRMMessageSegmentDVO	segDVO;
	
	/**
	 * The partnership record for the sending message.
	 */
	private final SFRMPartnershipDVO 	pDVO;
	
	/**
	 * The message record for the sending message.
	 */
	private SFRMMessageDVO			msgDVO;
				
	/**
	 * The message classifier for this message.
	 * 
	 * @see hk.hku.cecid.edi.sfrm.pkg.SFRMMessageClassifier
	 */
	private SFRMMessageClassifier 	msgClassifier;

	/**
	 * The current retried time of sending.
	 */
	private int currentRetried;
	
	/**
	 * The flag of message retry flag.
	 */
	private boolean retryEnabled;
	
	/**
	 * The constant field for the suffix of the log. (each thread has different value).
	 */
	private final String SGT_LOG_SUFFIX; 

	/** 
	 * Explicit Constructor.<br><br>
	 * 
	 * @param sgtDVO	
	 * 			The payload need to be send out.
	 * @param pDVO
	 * 			The partnership record associated to this segment.
	 * @param msgDVO 
	 * 			The message record associated to this segment.
	 * @param payload 
	 * 			The packaged payloads
	 * @since	
	 * 			1.0.0 
	 * @throws NullPointerException
	 * 			If the message, partnership and segment is null.
	 */
	public 
	OutgoingSegmentPayloadsTask(
		SFRMMessageSegmentDVO 	sgtDVO,
		SFRMPartnershipDVO 		pDVO,
		SFRMMessageDVO 			msgDVO,
		PackagedPayloads 		payload)
	{				
		// Error reporting.
		if (pDVO == null)
			throw new NullPointerException("Outgoing Segment Payloads Task: Missing partnership object");				
		if (sgtDVO == null)
			throw new NullPointerException("Outgoing Segment Payloads Task: Missing segment object");		
		if (msgDVO == null)
			throw new NullPointerException("Outgoing Segment Payloads Task: Missing message object");		
		if (payload == null && 
			sgtDVO.getSegmentType().equals(SFRMConstant.MSGT_PAYLOAD)){
			throw new NullPointerException("Outgoing Segment Payloads Task: Missing payload object for msg id: " 
										  + msgDVO.getMessageId());
		}		
		this.pDVO 	 = pDVO;
		this.msgDVO	 = msgDVO;
		this.segDVO	 = sgtDVO;
		this.payload = payload; 
		this.retryEnabled	= (this.pDVO.getRetryMax() > 0);
		this.currentRetried	= this.segDVO.getRetried();
		
		this.SGT_LOG_SUFFIX	= 
			" msg id: " 
		   +  this.segDVO.getMessageId() 
		   +" and sgt no: " 
		   +  this.segDVO.getSegmentNo() 
		   +" and sgt type: " 
		   +  this.segDVO.getSegmentType();		
	}
		
	/**
	 * Create a SFRM Message for sending.<br><br>
	 * 
	 * The SFRM Message maybe sign, encrypt according 
	 * to the partnership configuration of this message.
	 * 
	 * This is the first steps of this active task.<br><br>
	 * 
	 * @return 
	 * 			A new SFRM message for sending.
	 * @throws SFRMMessageException 
	 * @since	
	 * 			1.0.0 
	 * @throws DAOException 			
	 * 			if database I/O Errors.
	 * @throws SFRMMessageException
	 * @throws UnrecoverableKeyException 
	 * @throws NoSuchAlgorithmException 
	 * @throws NoSuchAlgorithmException
	 * 			if the signing and encryption can't be found.
	 * @throws UnrecoverableKeyException 
	 * @throws SMimeException
	 * @throws MessagingException 
	 */
	private SFRMMessage createSFRMMessage() 
		throws SFRMMessageException, NoSuchAlgorithmException, UnrecoverableKeyException {					
		
		// Check if it is meta message.
		boolean isMeta = this.segDVO.getSegmentNo() == 0 &&
		 				 this.segDVO.getSegmentType().
		 				 equalsIgnoreCase(SFRMConstant.MSGT_META);
		boolean isPayload = this.segDVO.getSegmentType().
		 					equalsIgnoreCase(SFRMConstant.MSGT_PAYLOAD);
		boolean isReceipt = this.segDVO.getSegmentType().
		 					equalsIgnoreCase(SFRMConstant.MSGT_RECEIPT) ||
		 					this.segDVO.getSegmentType().
		 					equalsIgnoreCase(SFRMConstant.MSGT_RECOVERY) ||
		 					this.segDVO.getSegmentType().
		 					equalsIgnoreCase(SFRMConstant.MSGT_LAST_RECEIPT);
		boolean isLastReceipt = this.segDVO.getSegmentType().
								equalsIgnoreCase(SFRMConstant.MSGT_LAST_RECEIPT);
		
		// Create return message.
		SFRMMessage message = new SFRMMessage();		
		// Construct the message header. Fill in general information
		message.setMessageID	(this.segDVO.getMessageId());
		message.setPartnershipId(this.msgDVO.getPartnershipId());
		message.setSegmentNo	(this.segDVO.getSegmentNo());		
		message.setSegmentType	(this.segDVO.getSegmentType());
		
		if (isLastReceipt)
			message.setSegmentType(SFRMConstant.MSGT_RECEIPT);
		if (isReceipt)
			message.setIsLastReceipt(isLastReceipt);
		
		DataSource cacheSource = new EmptyDataSource();
		if (isPayload || isMeta){			
			// Calculate the segment size.
			long size = this.segDVO.getSegmentEnd() - 
						this.segDVO.getSegmentStart();					
			if (isMeta){
				message.setTotalSize(this.msgDVO.getTotalSize());
				message.setTotalSegment(this.msgDVO.getTotalSegment());
			}
			message.setSegmentOffset(this.segDVO.getSegmentStart());	
			message.setSegmentLength(size);
			if (isPayload){		
				cacheSource = new FileRegionDataSource(payload.getRoot(),
						this.segDVO.getSegmentStart(), size);
			}			
		}			
		
		String contentType = payload == null ? "application/octet-stream"
				: payload.getContentType();		
		message.setContent(cacheSource, contentType);
		
		// Create SMIME Header.
		KeyStoreManager keyman = SFRMProcessor.getKeyStoreManager();
		
		// Generate checksum value using MD5 Hash algorithm.		
		if (isPayload)
			message.setMicValue(message.digest());
		else
			message.setMicValue("");
				
		// Setup up signing using MD5 or SHA1		
		if (this.pDVO.isOutboundSignRequested() && this.pDVO.getSignAlgorithm() != null){
			SFRMProcessor.core.log.info(SFRMLog.OSPT_CALLER + SFRMLog.SIGNING_SGT + SGT_LOG_SUFFIX); 
			message.sign(keyman.getX509Certificate(), keyman.getPrivateKey(), this.pDVO.getSignAlgorithm());
		}
		
		// Setup up encrypting using RC2, DES
		if (this.pDVO.isOutboundEncryptRequested() && pDVO.getEncryptAlgorithm() != null){
			SFRMProcessor.core.log.info(SFRMLog.OSPT_CALLER + SFRMLog.ENCRYPT_SGT + SGT_LOG_SUFFIX);
			message.encrypt(pDVO.getEncryptX509Certificate(), pDVO.getEncryptAlgorithm());
		}
		
		// Before we return, we need to set up the classifier
		this.msgClassifier = message.getClassifier();		
		return message;		
	}
			
	/**
	 * Send a SFRM Message using Fast HTTP Connector.<br><br>
	 * 
	 * This is step 2 of this active task.<br><br> 
	 * 
	 * @param message
	 * 			The sfrm message to be sent.
	 * @since	
	 * 			1.0.3
	 */
	private void 
	sendSFRMMessage(SFRMMessage message) throws	Exception 
	{						
		OutgoingMessageHandler.getInstance().processOutgoingMessage(
				message, this.pDVO, this.msgDVO);
		
		// Update the payload status to processed if the segment is receipt.
		this.cascadeUpdate(SFRMConstant.MSGS_PROCESSED);			
		this.segDVO.setStatus(SFRMConstant.MSGS_DELIVERED);
		this.segDVO.setProceedTimestamp(new Timestamp(System.currentTimeMillis()));
		SFRMProcessor.getMessageSegmentHandler().getDAOInstance().persist(this.segDVO);
		this.retryEnabled = false;
	}
			
	/**
	 * Execute the active task.
	 * 
	 * @since	1.0.0
	 * 
	 * @see hk.hku.cecid.piazza.commons.module.ActiveTask#execute()
	 */
	public void 
	execute() throws Exception 
	{			
		// Log information.
		SFRMProcessor.core.log.info(SFRMLog.OSPT_CALLER + SFRMLog.OUTG_TASK + SGT_LOG_SUFFIX);
		// --------------------------------------------------------------------------- 
		// Step 0: Check whether it has exceed the retries times.		
		// ---------------------------------------------------------------------------
		if (this.currentRetried > this.getMaxRetries())
			throw new SFRMMessageException(
				  SFRMLog.OSPT_CALLER 
			   +" this sending segment has exceeding retries time: "
			   +  this.currentRetried
			   +  SGT_LOG_SUFFIX);
		
		// --------------------------------------------------------------------------- 
		// Step 1: Check whether the message has been failed.		
		// ---------------------------------------------------------------------------
		SFRMMessageDVO msgDVO = SFRMProcessor.getMessageHandler()
		.retrieveMessage(
			this.segDVO.getMessageId(),
			this.msgDVO.getMessageBox());
	
		if (msgDVO.getStatus().equalsIgnoreCase(SFRMConstant.MSGS_DELIVERY_FAILURE)){
			SFRMProcessor.core.log.info(
				  SFRMLog.OSPT_CALLER 
			   +" Failed msg with msg id:" 
			   +  msgDVO.getMessageId()
			   +" and sgt no: " 
			   +  this.segDVO.getSegmentNo());
			return;
		}					
		// ---------------------------------------------------------------------------
		// Step 2: Update the segment status to PR and timestamp
		// ---------------------------------------------------------------------------		
		this.segDVO.setStatus(SFRMConstant.MSGS_PROCESSING);
		SFRMProcessor.getMessageSegmentHandler().getDAOInstance().persist(this.segDVO);
		
		// ---------------------------------------------------------------------------
		// Step 3: create the sfrm message for sending and cache it for retry
		// ---------------------------------------------------------------------------
		// FIXME: use back cache / create each times ?
		SFRMMessage sendMessage = this.createSFRMMessage();		
		
		// ---------------------------------------------------------------------------
		// Step 4: send the message.
		// ---------------------------------------------------------------------------							
		this.sendSFRMMessage(sendMessage);		
	}
	
	/**
	 * Update the status of payload / meta segment associated to the 
	 * current message if the current segment is receipt. 
	 * Otherwise, nothing done.<br><br>
	 * 
	 * This method should only be used when the segment is receipt.<br><br>
	 * 
	 * If the segment is last receipt, notify the global message lock 
	 * so that any incoming payload task can set the status to PS.
	 * 
 	 * @param newStatus
	 * 			The new status of the payload / meta associated to the 
	 * 			reciept.
	 * 
	 * @since 1.0.2
	 */
	protected void 
	cascadeUpdate(String newStatus) throws DAOException 
	{
		// If the message is receipt, set the payload segment to processed also.
		if (this.msgClassifier != null && this.msgClassifier.isReceipt())
		{
			// Local variable declaration.
			String mID = this.segDVO.getMessageId();
			
			SFRMMessageSegmentHandler msHandle = SFRMProcessor.getMessageSegmentHandler();
			
			SFRMMessageSegmentDVO segDVO = msHandle.retrieveMessageSegment(
				this.segDVO.getMessageId(),
				SFRMConstant.MSGBOX_IN,
				this.segDVO.getSegmentNo(),
				SFRMConstant.MSGT_PAYLOAD);
			
			if (segDVO == null)
			{
				SFRMProcessor.core.log.fatal(
					SFRMLog.OSPT_CALLER 
				  + "Missing associated segment record with receipt: " 
				  +  this.segDVO.toString());	
				throw new NullPointerException("Missing associated payload DB record.");
			}
			
			Timestamp currentTime = new Timestamp(System.currentTimeMillis());
			
			/*
			 * If the receipt has been sent successfully or delivery failure, 
			 * the payload segment associated to it is considered to be done.   
			 */
			segDVO.setStatus(newStatus);			
			segDVO.setCompletedTimestamp(currentTime);
			msHandle.getDAOInstance().persist(segDVO);
			
			// Refactor
			if (this.msgDVO.getStatus().equalsIgnoreCase(SFRMConstant.MSGS_DELIVERY_FAILURE)|| 
				msHandle.retrieveMessageSegmentCount(mID,
					SFRMConstant.MSGBOX_IN, SFRMConstant.MSGT_PAYLOAD,
					SFRMConstant.MSGS_PROCESSED) == this.msgDVO.getTotalSegment()) 
			{	
				SFRMProcessor.core.log.info(SFRMLog.OSPT_CALLER + SFRMLog.NOTIFY_REPT + SGT_LOG_SUFFIX);
				Object msgLock = SFRMProcessor.getLock(this.segDVO.getMessageId());
				if (msgLock != null)
					synchronized(msgLock){ msgLock.notifyAll();	}
			}					
		}			
	}
	
	/**
	 * Set the retries of active task.<br><br>
	 * 
	 * The parameter <code>retried</code> is useless here
	 * as we use the field "retried" in the database segment  
	 * table for reference.
	 * 
	 * @since	
	 * 			1.0.0
	 * 
	 * @param retried The number of times that has been tried.
	 */
	public void 
	setRetried(int retried) 
	{
		this.currentRetried++;
		try{
			this.segDVO.setRetried(this.currentRetried);
			SFRMProcessor.getMessageSegmentHandler().getDAOInstance()
				.persist(this.segDVO);
		}
		catch(DAOException daoe){
			SFRMProcessor.core.log.error("Error in database", daoe);
		}
	}
	
	/**
	 * @since	
	 * 			1.0.0
	 * 
	 * @return return the max retries allowed for this active task.
	 * 
	 */
	public int 
	getMaxRetries() 
	{
		return this.pDVO.getRetryMax();
	}

	/**
	 * @since	
	 * 			1.0.0
	 * 
	 * @return return the interval between each sending retry.
	 */
	public long 
	getRetryInterval() 
	{
		return this.pDVO.getRetryInterval();
	}
	
	/**
	 * @since	
	 * 			1.0.0
	 * 
	 * @return return true if this task can be retried.
	 */
	public boolean 
	isRetryEnabled() 
	{
		return this.retryEnabled;
	}

	/**
	 * The method is invoked upon the task fails to send.<br><br>
	 * 
	 * The message segment and message will treat as FAIL. 
	 * with status DF (Delivery Failure).<br><br>
	 * 
	 * Also, if the outgoing segment is a RECEIPT,
	 * then the PAYLOAD segment corresponding to this 
	 * RECEIPT is also treated as FAIL.
	 * 
	 * @param e
	 * 			The failure cause.
	 * @since	
	 * 			1.0.0  
	 */
	public void 
	onFailure(Throwable e) 
	{
		SFRMProcessor.core.log.error(
			"Error in Outgoing Segmented Payload Task", e);
		// Unrecoverable exception
		if (!this.retryEnabled ||
			 this.currentRetried >= this.getMaxRetries()){
			try {
				// ---------------------------------------------------------------
				// Step 0: Update the sfrm message record to be fail
				// ---------------------------------------------------------------				
				this.msgDVO.setStatus(SFRMConstant.MSGS_DELIVERY_FAILURE);
				this.msgDVO.setStatusDescription(
					"Segment: " + this.segDVO.getSegmentNo() + " has error: " + e.toString());
				this.msgDVO.setCompletedTimestamp(new Timestamp(System.currentTimeMillis()));
				SFRMProcessor.getMessageHandler().updateMessage(this.msgDVO);				
				// ---------------------------------------------------------------
				// Step 1: If the message is receipt, set the payload segment
				// to fails also.				
				this.cascadeUpdate(SFRMConstant.MSGS_DELIVERY_FAILURE);
				// ---------------------------------------------------------------
				// Step 2: Update the sfrm segment record to fail
				// ---------------------------------------------------------------
				this.segDVO.setStatus(SFRMConstant.MSGS_DELIVERY_FAILURE);
				this.segDVO.setCompletedTimestamp(new Timestamp(System.currentTimeMillis()));
				SFRMProcessor.getMessageSegmentHandler().getDAOInstance().persist(this.segDVO);
				// ---------------------------------------------------------------
				// Step 3: clear all the cache.
				SFRMProcessor.getMessageHandler().clearCache(this.msgDVO);
				SFRMProcessor.getPartnershipHandler().clearCache(
					this.msgDVO.getPartnershipId(), 
					this.msgDVO.getMessageId());
				
				this.retryEnabled = false;
			} catch (Exception ex) {
				SFRMProcessor.core.log.fatal(
					"Unable to mark failure to outgoing SFRM message: "
				   + this.msgDVO.getMessageId(), ex);
			}		
		}
		else{
			SFRMProcessor.core.log.error("Unknown Error", e);
		}
	}
}
