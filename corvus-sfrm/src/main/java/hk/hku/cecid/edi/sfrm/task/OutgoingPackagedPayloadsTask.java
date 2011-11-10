/**
 * Contains the set of active module collector for 
 * handling packaging, segmenting, sending, joining and unpacking of payloads.
 */
package hk.hku.cecid.edi.sfrm.task;

import java.sql.Timestamp;

import hk.hku.cecid.edi.sfrm.pkg.SFRMConstant;

import hk.hku.cecid.edi.sfrm.spa.SFRMLog;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.edi.sfrm.spa.SFRMProperties;

import hk.hku.cecid.edi.sfrm.com.PackagedPayloads;
import hk.hku.cecid.edi.sfrm.com.PayloadsState;

import hk.hku.cecid.edi.sfrm.dao.SFRMMessageDVO;
import hk.hku.cecid.edi.sfrm.dao.SFRMMessageSegmentDAO;
import hk.hku.cecid.edi.sfrm.dao.SFRMMessageSegmentDVO;
import hk.hku.cecid.edi.sfrm.dao.SFRMPartnershipDVO;

import hk.hku.cecid.edi.sfrm.handler.SFRMMessageHandler;

import hk.hku.cecid.piazza.commons.module.ActiveTaskAdaptor;
import hk.hku.cecid.piazza.commons.util.StopWatch;
import hk.hku.cecid.piazza.commons.dao.DAOException;

/**
 * <strong> What the task does </strong>
 * <ul>
 * 	<li> Update the status of message to segmentating (status: ST).</li>
 * 	<li> Analyze the payload and save the segment record to the database. </li>
 * 	<li> Update the status of message to processing (status: PR). </li>  
 * </ul>	
 * 
 * Creation Date: 24/10/2006.<br><br>
 * 
 * @author Twinsen Tsang
 * @version 1.0.3 
 * @since 	1.0.1
 */
public class OutgoingPackagedPayloadsTask extends ActiveTaskAdaptor {
		
	/**
	 * The archive payload of this task.
	 */
	private final PackagedPayloads 	 payload;
	
	/**
	 * The message record for this task.
	 */
	private final SFRMMessageDVO	 msgDVO;
	
	/**
	 * The SFRM partnership DVO for this task.
	 */
	private final SFRMPartnershipDVO pDVO;
		
	/**
	 * 
	 */
	private boolean retryEnabled;
	
	/**
	 * 
	 */
	private int currentRetried;
	
	/** 
	 * Explicit Constructor.
	 * 
	 * @param msgDVO 
	 * 			The message DB record associated to one archived payload.
	 * @param payloads
	 * 			The packaged payload file.
	 * @throws NullPointerException
	 * 			if the input payload is null.
	 * @throws DAOException
	 * 			if fail to retreve the partnership.
	 */
	public 
	OutgoingPackagedPayloadsTask(SFRMMessageDVO msgDVO) throws DAOException
	{
		if (msgDVO == null)
			throw new NullPointerException("Outgoing Packaged Task: Missing message record.");
		
		String pID = msgDVO.getPartnershipId();
		String mID = msgDVO.getMessageId();
		
		PackagedPayloads pp = (PackagedPayloads) SFRMProcessor
			.getPackagedPayloadRepository().getPayload(
			new Object[]{pID, mID},PayloadsState.PLS_PENDING);
		if (pp == null)
			throw new NullPointerException("Outgoing Packaged Task: Missing payload.");
		// Return cached result or extract from DB.
		// TODO: Can pull-up the partnership extraction to collector level when the task list is sorted by partnership.	
		this.pDVO			= SFRMProcessor.getPartnershipHandler()
			.retreivePartnership(pID, mID);				
		this.msgDVO  		= msgDVO;
		this.payload 		= pp;						
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
		// ------------------------------------------------------------------------
		// Step 0: Update the message status to 'ST' and save to database. 
		// ------------------------------------------------------------------------
		SFRMMessageHandler mHandle = SFRMProcessor.getMessageHandler();
		this.msgDVO.setStatus			(SFRMConstant.MSGS_SEGMENTING);
		this.msgDVO.setStatusDescription(SFRMConstant.MSGSDESC_SEGMENTING);		
		mHandle.updateMessage(this.msgDVO);

		// ------------------------------------------------------------------------
		// Step 1: Insert all segments record into database.		
		// ------------------------------------------------------------------------
		SFRMMessageSegmentDAO segDAO = (SFRMMessageSegmentDAO) 
			SFRMProcessor.getMessageSegmentHandler().getDAOInstance();
		SFRMMessageSegmentDVO segDVO = (SFRMMessageSegmentDVO) 
			segDAO.createDVO();
		
		// Setup all parameters that are common for each segment.
		segDVO.setMessageId	 (this.payload.getRefMessageId());
		segDVO.setMessageBox (SFRMConstant.MSGBOX_OUT);
		segDVO.setStatus	 (SFRMConstant.MSGS_PENDING);
		segDVO.setSegmentType(SFRMConstant.MSGT_PAYLOAD);
		
		long startPos = 0;
		long endPos = 0;
		long segmentSize	= SFRMProperties.getPayloadSegmentSize();
		long fileSize		= msgDVO.getTotalSize();		
		int	 numOfSegment	= msgDVO.getTotalSegment();
		long lastSegmentSize= fileSize - ((numOfSegment -1)* segmentSize);		
		
		StopWatch sw = new StopWatch();
		sw.start();
		
		// Find out the maximum segment no created in the DB.
		// This is used for when there is inproper system shutdown
		// during the creating segment, the module still can 
		// recover the system.
		// TODO: Implement same function to SFRM_MSH.
		int  maxSegmentNo	= segDAO.findMaxSegmentNoByMessageIdAndBoxAndType(
			this.payload.getRefMessageId(), 
			SFRMConstant.MSGBOX_OUT,
			SFRMConstant.MSGT_PAYLOAD);
		
		if (maxSegmentNo > 0)	
			SFRMProcessor.core.log.info(
				  SFRMLog.OPPT_CALLER
			   + "Resume segmentation with msg id: " 
			   +  this.payload.getRefMessageId()
			   +" at sgt no: "
			   +  maxSegmentNo);			   								
		
		for (int i = maxSegmentNo + 1; i <= numOfSegment; i++){						
			// Retrieve the segment type and their byte range
			// by the result. The possible case are shown in the following:
			// Support there are n segments where each segment has 
			// S bytes. then 
			// For case i == r < n
			//		it is a payload message. The start and end pos   
			// 		are (i-1)*S and (i)*S resp.
			// For case i == n
			//		it is the last payload message. The start and end pos
			//		are (i-1)*S and (start pos) + (last segment size).						
			startPos = (i - 1) * segmentSize;				
			if (i == numOfSegment)
				endPos = startPos + lastSegmentSize;
			else
				endPos = startPos + segmentSize;
									
			segDVO.setSegmentNo(i);
			segDVO.setSegmentStart(startPos);
			segDVO.setSegmentEnd(endPos);									
			segDAO.create(segDVO);
		}		 
		// ------------------------------------------------------------------------
		// Step 2: Update the proceed time and the status to PROCESSING. 
		// ------------------------------------------------------------------------
		sw.stop();
		
		this.msgDVO.setProceedTimestamp(new Timestamp(sw.getEndTime()));
		this.msgDVO.setStatus			(SFRMConstant.MSGS_PROCESSING);
		this.msgDVO.setStatusDescription(SFRMConstant.MSGSDESC_PROCESSING);
		mHandle.updateMessage(this.msgDVO);
		
		// Log information.
		SFRMProcessor.core.log.info(
			  SFRMLog.OPPT_CALLER 
		   +  SFRMLog.INSERT_SGTS
		   +" msg id: "
		   +  payload.getRefMessageId() 
		   +" have inserted " 
		   +  (numOfSegment - maxSegmentNo)
		   +" sgt with time: " 
		   +  sw.getElapsedTimeInSecond());												
		
		// We are done, terminate this thread.
		this.retryEnabled = false;
	}
	
	/**
	 * 
	 */
	public void setRetried(int retried){
		this.currentRetried = retried;
	}		

	/**
	 * 
	 */
	public boolean isRetryEnabled(){
		return this.retryEnabled;
	}	
	
	/**
	 * 
	 */
	public int getMaxRetries() {
		return this.pDVO.getRetryMax();
	}

	/**
	 * 
	 */
	public long getRetryInterval() {
		return this.pDVO.getRetryInterval();
	}
	
	/**
	 * Invoke when failure.
	 */
	public void 
	onFailure(Throwable e) 
	{
		SFRMProcessor.core.log.error(SFRMLog.OPPT_CALLER + "Error", e);
		if (!this.retryEnabled || this.currentRetried >= this.getMaxRetries()){
			try {				
				// ---------------------------------------------------------------------
				// Step 0: Update the sfrm message record to be fail and clear the cache
				// ---------------------------------------------------------------------
				SFRMMessageHandler mHandle = SFRMProcessor.getMessageHandler(); 
				this.msgDVO.setStatus(SFRMConstant.MSGS_DELIVERY_FAILURE);
				this.msgDVO.setStatusDescription(e.toString());
				mHandle.updateMessage(this.msgDVO);
				mHandle.clearCache(this.msgDVO);
				SFRMProcessor.getPartnershipHandler().clearCache(
					this.msgDVO.getPartnershipId(), 
					this.msgDVO.getMessageId());						
				// ---------------------------------------------------------------
				// Step 1: Update the payload to pending for restart by users.
				// ---------------------------------------------------------------				
				this.payload.setToPending();				
				this.retryEnabled = false;				
			} 
			catch (Exception ex) {
				SFRMProcessor.core.log.fatal(
					SFRMLog.OPPT_CALLER 
				  + "Unable to mark failure for msg: " 
				  + this.msgDVO.getMessageId(), ex); 
			}		
		}			
		else{
			SFRMProcessor.core.log.error(SFRMLog.OPPT_CALLER + "Unknown Error", e);
		}
	}
}

