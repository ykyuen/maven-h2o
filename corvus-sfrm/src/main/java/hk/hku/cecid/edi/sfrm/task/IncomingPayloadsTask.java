/**
 * Contains the set of active module collector for 
 * handling packaging, segmenting, sending, joining and unpacking of payloads.
 */
package hk.hku.cecid.edi.sfrm.task;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;

import hk.hku.cecid.edi.sfrm.com.FoldersPayload;
import hk.hku.cecid.edi.sfrm.com.PackagedPayloads;
import hk.hku.cecid.edi.sfrm.dao.SFRMMessageDVO;
import hk.hku.cecid.edi.sfrm.dao.SFRMPartnershipDVO;
import hk.hku.cecid.edi.sfrm.pkg.SFRMConstant;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.edi.sfrm.spa.SFRMLog;
import hk.hku.cecid.edi.sfrm.handler.SFRMMessageHandler;
import hk.hku.cecid.edi.sfrm.handler.SFRMMessageSegmentHandler;

import hk.hku.cecid.piazza.commons.module.ActiveTaskAdaptor;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.io.FileSystem;

/**
 * The incoming payloads task accepts one package payloads 
 * and do unpackaging (unzip) into the incoming payload
 * repository.<br><br>
 * 
 * <strong> What the task does </strong>
 * <ul>
 * 	<li> Set the status of the message associated to this payloads to "UK".</li>
 * 	<li> Use the Archiver to unpack the packaged payloads back to a folder structure.</li>
 *  <li> Set the status of the message associated to this payloads to "PS".</li>
 *  <li> Remove the packaged payload once unpackaging complete.</li>
 * </ul>
 * 
 * Creation Date: 16/10/2006
 * 
 * @author Twinsen Tsang
 * @version 1.0.2
 * @since	1.0.0
 */
public class IncomingPayloadsTask extends ActiveTaskAdaptor {

	/**
	 * The archived payload of this task.
	 */
	private final PackagedPayloads payloads;
	
	/**
	 * The SFRM partnership DVO for this task.
	 */
	private final SFRMPartnershipDVO pDVO;
	
	/**
	 * The SFRM message DVO for this task.
	 */
	private SFRMMessageDVO msgDVO;
	
	/**
	 * 
	 */
	private boolean retryEnabled;
	
	/**
	 * 
	 */
	private int currentRetried;
		
	/**
	 * 
	 */
	private FoldersPayload dir;
			
	/** 
	 * Explicit Constructor.
	 * 
	 * @param payload
	 * 			The package payloads for unzipping.
	 * @throws NullPointerException
	 * 			if the payloads is missing. 			
	 * @throws IOException
	 * 			if the payloads can not set to processing.
	 * @throws DAOException
	 * 			if the message record or partnership record can not be retrieve.
	 */
	public 
	IncomingPayloadsTask(PackagedPayloads payload) throws IOException, DAOException
	{
		if (payload == null)
			throw new NullPointerException("Incoming Payload Task: Missing payload.");			
		this.payloads = payload;
		
		// TODO: When there is folder with the same name, then the payload can not set to processing.
		// The possibility of this is very low as long as the message id is assumed to be unique.
		this.payloads.setToProcessing();
		
		// Return cached result or extract from DB. The message is Threadlocal 
		// object and is appropriate to put in constructor of ActiveTask.
		this.msgDVO   = SFRMProcessor.getMessageHandler().retrieveMessage(
			payloads.getRefMessageId(), 
			SFRMConstant.MSGBOX_IN);
		
		if (this.msgDVO == null)
			throw new NullPointerException(
				"Incoming Payload Task: Missing msg for payload:" 
			  + this.payloads);
		
		// Return cached result or extract from DB.
		// TODO: Can pull-up the partnership extraction to collector level when the task list is sorted by partnership.		
		this.pDVO	  = SFRMProcessor.getPartnershipHandler().retreivePartnership(
			payloads.getPartnershipId(), msgDVO.getMessageId());
		
		if (this.pDVO == null)
			throw new NullPointerException(
				"Incoming Payload Task: Missing partnership for payload: "
			  + this.payloads);
		
		this.retryEnabled 	= this.pDVO.getRetryMax() > 0;
		this.currentRetried = 0;
	}
		
	/**
	 * Execute the active task. 
	 * 
	 * @see hk.hku.cecid.piazza.commons.module.ActiveTask#execute() 
	 */
	public void execute() throws Exception 
	{				
		// Log debug information.
		SFRMProcessor.core.log.info(
			  SFRMLog.IPT_CALLER	
		   +  SFRMLog.UNPACK_MSG 
		   +" msg id: " 
		   +  payloads.getRefMessageId());
		// ------------------------------------------------------------------------		
		// Step 0: Update the message status to "UK" (unpackaging).
		// ------------------------------------------------------------------------
		SFRMMessageHandler mHandle = SFRMProcessor.getMessageHandler(); 	
		SFRMMessageSegmentHandler msHandle = SFRMProcessor.getMessageSegmentHandler();
		
		this.msgDVO.setStatus		(SFRMConstant.MSGS_UNPACKAGING);
		this.msgDVO.setProceedTimestamp	(new Timestamp(System.currentTimeMillis()));
		this.msgDVO.setStatusDescription(SFRMConstant.MSGSDESC_UNPACKAGING);
		mHandle.updateMessage(msgDVO);		
		// ------------------------------------------------------------------------				
		// Step 1: Extract the archive.
		// ------------------------------------------------------------------------
		this.dir = this.payloads.unpack();
			
		// ------------------------------------------------------------------------		
		// Step 2: Size and file validation.
		// ------------------------------------------------------------------------
		long EPSize = this.payloads.estimatePackedSize(dir);
		long APSize = msgDVO.getTotalSize();
		if (APSize != EPSize){
			throw new IOException(
			     "Incoming Payload Task"
			   +" Message with ref message id: "
			   +  payloads.getRefMessageId()
			   +" has different size after unpackaging."
			   +" Expected size: "
			   +  EPSize 
			   +" Actual size: "
			   +  APSize);					
		}
		
		// ------------------------------------------------------------------------		
		// Step 3: Wait for all receipt to be done before setting the processed.
		// ------------------------------------------------------------------------
		String mID = this.msgDVO.getMessageId();

		do{
			// Update the message dvo.
			this.msgDVO = SFRMProcessor.getMessageHandler().retrieveMessage(
				payloads.getRefMessageId(),
				SFRMConstant.MSGBOX_IN);			
			// Message status is DF already. kill the task.
			if (this.msgDVO.getStatus().equalsIgnoreCase(SFRMConstant.MSGS_DELIVERY_FAILURE))
				break;
						
			// Query how many payloads has been done.
			int numOfProcessedPayload = msHandle.retrieveMessageSegmentCount(
				mID, 
				SFRMConstant.MSGBOX_IN, 
				SFRMConstant.MSGT_PAYLOAD, 
				SFRMConstant.MSGS_PROCESSED);
			
			// If all payload is processed. set the message to processed		
			if (numOfProcessedPayload == this.msgDVO.getTotalSegment()){
				msgDVO.setStatus			(SFRMConstant.MSGS_PROCESSED);
				msgDVO.setStatusDescription	(SFRMConstant.MSGSDESC_PROCESSED);
				msgDVO.setCompletedTimestamp(new Timestamp(System.currentTimeMillis()));
				mHandle.updateMessage(msgDVO);
				break;
			}
			else{
				SFRMProcessor.core.log.info(SFRMLog.IPT_CALLER + SFRMLog.WAIT_REPT + " msg id: " + mID);
				// Need to wait for last receipt notify.
				Object obj = SFRMProcessor.createLock(mID);
				synchronized(obj){ obj.wait(); }
				// Being notify by last receipt. Remove lock.
				SFRMProcessor.removeLock(mID);				
			}
		}while(true);									
		
		// ------------------------------------------------------------------------
		// Step 5: Set the payload's status to processed.				
		// ------------------------------------------------------------------------
		if (!this.payloads.setToProcessed()){
			SFRMProcessor.core.log.warn(
				SFRMLog.IPT_CALLER 
			  + "Unable to set the payload to processed: " 
			  + this.payloads);
		}
		
		// ------------------------------------------------------------------------		
		// Step 5: Delete the archive payload and rename the dir payload 
		// ------------------------------------------------------------------------
		this.payloads.clearPayloadCache();
		
		if (!dir.setToPending()){			
			// TODO: Use the payload repository system.						
			File f = new File(SFRMProcessor.getIncomingPayloadRepository()
				.getRepositoryPath(),
				dir.getOriginalRootname());			
			
			if (f.exists()){
				SFRMProcessor.core.log.warn(
					SFRMLog.IPT_CALLER
				   + "Deleting the payload folders with the same name: "
				   +  dir.getOriginalRootname());
				new FileSystem(f).purge();
			}
			dir.setToPending();
		}		
		// Task Finish. 
		this.retryEnabled = false;
	}
	
	/**
	 * 
	 */
	public void setRetried(int retried) 
	{
		this.currentRetried = retried;
	}		

	/**
	 * 
	 */
	public boolean isRetryEnabled() 
	{
		return this.retryEnabled;
	}	
	
	/**
	 * 
	 */
	public int getMaxRetries() 
	{
		return this.pDVO.getRetryMax();
	}

	/**
	 * 
	 */
	public long getRetryInterval() 
	{
		return this.pDVO.getRetryInterval();
	}

	/**
	 * Invoke when failure.
	 */
	public void 
	onFailure(Throwable e)
	{
		SFRMProcessor.core.log.error(SFRMLog.IPT_CALLER + "Error", e);
		// Unrecoverable exception
		if ( e instanceof IOException	|| e instanceof DAOException ||
			!this.retryEnabled 			|| this.currentRetried >= this.getMaxRetries()){
			try {											
				// ---------------------------------------------------------------
				// Step 0: Update the sfrm message record to be fail
				// ---------------------------------------------------------------
				this.msgDVO.setStatus(SFRMConstant.MSGS_DELIVERY_FAILURE);
				this.msgDVO.setStatusDescription(e.toString());
				this.msgDVO.setCompletedTimestamp(new Timestamp(System.currentTimeMillis()));
				SFRMProcessor.getMessageHandler().updateMessage(this.msgDVO);		
				// ---------------------------------------------------------------
				// Step 1: Clear the folders.
				// ---------------------------------------------------------------
				if (dir != null)
					dir.clearPayloadCache();
				// ---------------------------------------------------------------
				// Step 2: Clear the cache.
				// ---------------------------------------------------------------
				SFRMProcessor.getMessageHandler().clearCache(msgDVO);
				SFRMProcessor.getPartnershipHandler().clearCache(
					msgDVO.getPartnershipId(), 
					msgDVO.getMessageId());						
				this.retryEnabled = false;
				
			} catch (Exception ex) {
				SFRMProcessor.core.log.fatal(
					SFRMLog.IPT_CALLER 
				  + "Unable to mark failure for msg: " 
				  + this.msgDVO.getMessageId(), ex); 
			}		
		}
		else{
			SFRMProcessor.core.log.error(SFRMLog.IPT_CALLER + "Unknown Error", e);
		}
	}
}
