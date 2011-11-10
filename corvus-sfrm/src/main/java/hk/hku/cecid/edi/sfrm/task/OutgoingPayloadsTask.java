/**
 * Contains the set of active module collector for 
 * handling packaging, segmenting, sending, joining and unpacking of payloads.
 */
package hk.hku.cecid.edi.sfrm.task;

import java.io.IOException;
import java.sql.Timestamp;
import java.net.MalformedURLException;

import hk.hku.cecid.edi.sfrm.com.FoldersPayload;
import hk.hku.cecid.edi.sfrm.com.PackagedPayloads;
import hk.hku.cecid.edi.sfrm.com.PayloadsState;

import hk.hku.cecid.edi.sfrm.dao.SFRMMessageDAO;
import hk.hku.cecid.edi.sfrm.dao.SFRMMessageDVO;
import hk.hku.cecid.edi.sfrm.dao.SFRMPartnershipDVO;

import hk.hku.cecid.edi.sfrm.handler.SFRMMessageHandler;
import hk.hku.cecid.edi.sfrm.handler.SFRMMessageFactory;
import hk.hku.cecid.edi.sfrm.handler.OutgoingMessageHandler;

import hk.hku.cecid.edi.sfrm.pkg.SFRMConstant;

import hk.hku.cecid.edi.sfrm.spa.SFRMException;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.edi.sfrm.spa.SFRMProperties;
import hk.hku.cecid.edi.sfrm.spa.SFRMLog;

import hk.hku.cecid.piazza.commons.net.InternalServerErrorException;
import hk.hku.cecid.piazza.commons.module.ActiveTaskAdaptor;
import hk.hku.cecid.piazza.commons.util.StopWatch;

/**
 * The outgoing payloads tasks collects all files and directories,
 * packing them as a archive file which is used generating segment and 
 * folders preservation.<br><br>
 * 
 * <strong> What the task does </strong>
 * <ul>
 * 	<li> Validate if there is a partnership specified by the root payload 
 * 		 folders.</li>
 * 	<li> Create a message db record according to the partnership found in 
 * 		 bullet point one. </li>
 * 	<li> Send a Handshaking request to receiver confirming the receiver 
 * 		 is capable to receive this message.</li> 
 * 	<li> Package the payloads by using designated compresser. It store the 
 * 		 packaged archive at the packaged repository </li>  
 * </ul>		 
 * 
 * <strong> What the task does if failed once</strong>
 * <ul>
 * 	<li> It rollback the state change for the payload folders. </li>
 * 	<li> Delete the newly created message db record if it exists. </li>
 * 	<li> Delete the packaged payloads at the packaged repository. </li>
 * </ul>	
 *   
 * @author Twinsen Tsang
 * @version 1.0.3
 * @since	1.0.0
 */
public class OutgoingPayloadsTask extends ActiveTaskAdaptor {
			
	/**
	 * The root directory of the payload set.
	 */
	private FoldersPayload payloadDir;
	
	/**
	 * The partnership DVO associated to this payloads.
	 */
	private SFRMPartnershipDVO pDVO; 
	
	/**
	 * The message DVO associated to this payloads.
	 */
	private SFRMMessageDVO msgDVO;
	
	/**
	 * The current retried time of sending.
	 */
	private int currentRetried = 0;
	
	/**
	 * The flag of message retry flag. 
	 */
	private boolean retryEnabled = false;
	
	/**
	 * The flag of message creation flag.
	 */
	private boolean msgCreated = false;
					
	/**
	 * Explicit Constructor.<br><br>
	 * 
	 * @param dir
	 * 			The root directory of the payload set.
	 * @param pDVO 
	 * 			The partnership for the folder.
	 * @throws IOException 	
	 * 			if the <code>dir</code> is not a directory.
	 * @throws NullPointerException
	 * 			if the <code>dir</code> is null.
	 */
	public 
	OutgoingPayloadsTask(
			FoldersPayload 		dir, 
			SFRMPartnershipDVO 	pDVO) throws 
			IOException
	{
		if (dir == null)
			throw new NullPointerException("Outgoing Payloads Task: Missing payload.");
		if (pDVO == null)
			throw new NullPointerException("Outgoing Payloads Task: Missing partnership.");
		
		// Rename the root by adding prefix ## so that we tell the 
		// system that this directory is processing.
		this.payloadDir 	= dir;
		this.pDVO			= pDVO;
		this.retryEnabled	= pDVO.getRetryMax() > 0;		
		
		// TODO: When there is folder with the same name, then the payload can not set to processing.
		// The possibility of this is very low as long as the message id is assumed to be unique.
		this.payloadDir.setToProcessing();		 
	}
						
	/**
	 * Execute the active task.
	 * 
	 * @see hk.hku.cecid.piazza.commons.module.ActiveTask#execute()
	 */
	public void 
	execute() throws Exception 
	{		
		String pID = this.payloadDir.getPartnershipId();
		String mID = this.payloadDir.getMessageId();
		String logInfo = " msg id: " + mID;
						
		// ------------------------------------------------------------------------
		// Step 1: Create message record.
		// ------------------------------------------------------------------------
		// DAO Extraction and DVO Creation.
		// 
		// TODO: Encapsulate with DAO Message Handler layer.
		SFRMMessageDAO msgDAO = (SFRMMessageDAO) SFRMProcessor
			.getMessageHandler().getDAOInstance();
		this.msgDVO = (SFRMMessageDVO) msgDAO.createDVO();
		
		// Set all fields for this message record.
		this.msgDVO.setMessageId		(mID);
		this.msgDVO.setMessageBox	 	(SFRMConstant.MSGBOX_OUT);
		this.msgDVO.setPartnershipId	(pID);
		this.msgDVO.setPartnerEndpoint	(this.pDVO.getPartnerEndpoint());
		this.msgDVO.setIsSigned		 	(this.pDVO.isOutboundSignRequested());
		this.msgDVO.setIsEncryped	 	(this.pDVO.isOutboundEncryptRequested());
		this.msgDVO.setStatus		 	(SFRMConstant.MSGS_HANDSHAKING);
		this.msgDVO.setStatusDescription(SFRMConstant.MSGSDESC_HANDSHAKING);
		this.msgDVO.setCreatedTimestamp	(new Timestamp(System.currentTimeMillis()));
		msgDAO.create(msgDVO);
		this.msgCreated = true;
		
		// Log information.
		SFRMProcessor.core.log.info(SFRMLog.OPT_CALLER + SFRMLog.SEND_HDSK + logInfo);				
		// ------------------------------------------------------------------------
		// Step 2	: Handshaking Steps (Try to communicate with the receiver)
		// 		2.0 - Estimate the compressed size
		// 		2.1 - Check whether the payload is exceeding file limit.
		//		2.2 - Check if there are enough free space for compression
		//		2.3 - Estimate number of segments.
		//		2.4 - Generate handshaking request and send to receiver.
		// ------------------------------------------------------------------------

		// ------------------------------------------------------------------------
		// Step 2.0 - Estimate the compressed size 
		// Create a package payloads.
		PackagedPayloads pPayload = (PackagedPayloads) SFRMProcessor
				.getPackagedPayloadRepository()
				.createPayloads(new Object[]{pID, mID},	PayloadsState.PLS_UPLOADING);

		long EPSize = pPayload.estimatePackedSize(this.payloadDir);
		// ------------------------------------------------------------------------
		// Step 2.1 - Check whether the payload is exceeding file limit.
		long MPSize = SFRMProperties.getMaxPayloadSize();
		if (EPSize > MPSize)
			throw new SFRMException(
				"Payload Exceeding file size limit: "
			  +  EPSize
			  +" can allow file size under: "
			  +  MPSize);		
		
		// ------------------------------------------------------------------------
		// Step 2.2 - Check if there are enough free space for compression.
		long freeSpace = SFRMProcessor.getOSManager().getDiskFreespace(
			this.payloadDir.getRoot().getAbsolutePath(), null);		
		
		if (freeSpace < EPSize){
			// TODO: set something to fail?
			throw new IOException(
				"Not Enough Disk space. Require: "
			  +  EPSize 
			  +" but only has "
			  +  freeSpace);
		}
		// ------------------------------------------------------------------------		
		// Step 2.3	- Estimate number of segments. (+1 for last remaining bytes)
		long numOfSegment = EPSize / SFRMProperties.getPayloadSegmentSize() + 1;				
		// ------------------------------------------------------------------------
		// Step 2.4 - Generate handshaking request and send to receiver.						
		// Send to receiver.
		OutgoingMessageHandler.getInstance()
			.processOutgoingMessage(
				// Create Handshaking segment request
				SFRMMessageFactory.getInstance()
				.createHandshakingRequest(mID, pID, (int)numOfSegment, EPSize)
				,this.pDVO
				,this.msgDVO);			
		
		// Log information.
		SFRMProcessor.core.log.info(SFRMLog.OPT_CALLER + SFRMLog.PACK_MSG + logInfo);	
		
		// ------------------------------------------------------------------------
		// Step 3: packaging all file as a archive and update status.
		// ------------------------------------------------------------------------				
		this.msgDVO.setStatus		 	(SFRMConstant.MSGS_PACKAGING);
		this.msgDVO.setTotalSegment		((int)numOfSegment);
		this.msgDVO.setStatusDescription(SFRMConstant.MSGSDESC_PACKAGING);
		SFRMProcessor.getMessageHandler().updateMessage(this.msgDVO);

		// Calculate the procesing time.
		StopWatch sw = new StopWatch();
		sw.start();
		// Archive to TAR (default)
		pPayload.pack(this.payloadDir.getRoot());
		sw.stop();
		pPayload.setToPending();
		
		// Log information.
		SFRMProcessor.core.log.info(
			 SFRMLog.OPT_CALLER 
		  +  SFRMLog.PACK_MSG 
		  +  logInfo 
	      +" has archived with time: " 
		  +  sw.getElapsedTimeInSecond() 
		  +" (s).");
				
		// ------------------------------------------------------------------------
		// Step 4: Update the message status to ST for next modules 
		//		   and the payload status to processed.
		// ------------------------------------------------------------------------
		// Update the proceeding time and total size. 
		this.msgDVO.setTotalSize		(pPayload.getSize());
		this.msgDVO.setStatus			(SFRMConstant.MSGS_PACKAGED);
		this.msgDVO.setStatusDescription(SFRMConstant.MSGSDESC_PACAKGED);
		this.msgDVO.setProceedTimestamp	(new Timestamp(sw.getEndTime()));
		SFRMProcessor.getMessageHandler().updateMessage(this.msgDVO);

		if (!this.payloadDir.setToProcessed())
			throw new IOException("Error in setting status of payload: " + this.payloadDir);		
		// Terminate the thread.
		this.retryEnabled = false;
	}
	
	/**
	 * Set the retries of active task.
	 * 
	 * @since	
	 * 			1.0.0
	 * 
	 * @param retried The number of times that has been tried.
	 */
	public void 
	setRetried(int retried) 
	{
		this.currentRetried = retried;		
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
	public boolean isRetryEnabled() {
		return this.retryEnabled;
	}
	
	/**
	 * Invoke when failure.
	 * 
	 * @since	
	 * 			1.0.0
	 */
	public void 
	onFailure(Throwable e)
	{
		// TODO: Refactor.		
		// The situation we treat the message is fail to recover 
		// Case 1: 
		//	The exception is DAOException which it means the cause 
		//  may be "duplicate message id" and DB I/O Errors, etc
		// Case 2:
		//	The SFRM partnership is missing. 
		if ( /*e instanceof InternalServerErrorException	||*/
			 e instanceof SFRMException					||
			!this.isRetryEnabled() 			    		||
			 this.currentRetried >= this.getMaxRetries())
		{
			// Log information.
			SFRMProcessor.core.log.fatal(
				  SFRMLog.OPT_CALLER
			    +"Fatal error for this payloads with msg id: " 
			    + this.payloadDir.getMessageId(), e);		
			try{
				// Terminate the thread.
				this.retryEnabled = false;
				// Update the payload folders to processed (fail)
				this.payloadDir.setToProcessed();
				// Update the message to be fail if the DVO is exist
				if (this.msgDVO != null && this.msgCreated){ 
					this.msgDVO.setStatus(SFRMConstant.MSGS_DELIVERY_FAILURE);
					this.msgDVO.setStatusDescription(e.toString());			
					this.msgDVO.setCompletedTimestamp(new Timestamp(System.currentTimeMillis()));
					SFRMProcessor.getMessageHandler().updateMessage(this.msgDVO);
					SFRMProcessor.getMessageHandler().clearCache(this.msgDVO);
					SFRMProcessor.getPartnershipHandler().clearCache(
						this.msgDVO.getPartnershipId(), 
						this.msgDVO.getMessageId());
				}						
			}catch(Exception ex){
				SFRMProcessor.core.log.fatal(
					SFRMLog.OPT_CALLER 
				 + "Unable to mark failure to the outgoing payload folders:", ex);				
			}
		}
		// Try to recover the payload dir, rolling back state changes.		
		else
		if (this.isRetryEnabled() && this.getMaxRetries() > this.currentRetried){ 
			// Log Information
			SFRMProcessor.core.log.error(
				  SFRMLog.OPT_CALLER
			   +  SFRMLog.ROLL_BACK
			   +" msg id: "
			   +  this.payloadDir.getMessageId()
			   +" for : "
			   +  this.currentRetried 
			   +" time(s)"
			   +" due to reason: ", e);
				    						
		    try{
		    	SFRMMessageHandler mHandle = SFRMProcessor.getMessageHandler();
		   		// Sync the record to see if there are 
		    	// any different.
		    	this.msgDVO = mHandle.retrieveMessage(
		    		payloadDir.getMessageId(), 
		    		SFRMConstant.MSGBOX_OUT);		    	
		    	// Safely check that the message DVO is really that record
		    	// from failure DVO.
		    	if (this.msgDVO != null){
		    		String status = this.msgDVO.getStatus();
		    		if (status.equals(SFRMConstant.MSGS_PACKAGING)	||
		    			status.equals(SFRMConstant.MSGS_HANDSHAKING)){
		    			mHandle.removeMessage(this.msgDVO);
			    		this.msgCreated = false;
			    				    				    		
			    		SFRMProcessor.core.log.info(
			    			"Expected Inproper restart for msg with MSG id: "
			    		   + this.payloadDir.getMessageId());
			    		
		    		} else{
		    			// Duplicated message id with different content happen. terminate immediately.
		    			this.payloadDir.setToProcessed();
		    			this.retryEnabled = false;
		    			SFRMProcessor.core.log.error(
		    				"Unable to send duplicate msg with " 
		    			   +"different content with MSG id: "
		    			   + this.payloadDir.getMessageId());		    			
		    		}
		    	}		    						    	
		    }catch(Exception ex){
		    	SFRMProcessor.core.log.fatal(
					SFRMLog.OPT_CALLER 
				  + "Unable to recover to the outgoing payload folders:", ex);				    	
		    }		 
		}
		/*else if ( e instanceof MalformedURLException){
			SFRMProcessor.core.log.error("Unable to establish connection: ", e);
		}*/
		else{
			SFRMProcessor.core.log.error(SFRMLog.OPT_CALLER + "Unknown Error", e);			
		}
	}
}

