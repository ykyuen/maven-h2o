/**
 * Contains the set of active module collector for 
 * handling packaging, segmenting, sending, joining and unpacking of payloads.
 */
package hk.hku.cecid.edi.sfrm.task;

import java.io.IOException;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.edi.sfrm.spa.SFRMLog;
import hk.hku.cecid.edi.sfrm.com.FoldersPayload;
import hk.hku.cecid.edi.sfrm.com.PayloadsState;
import hk.hku.cecid.edi.sfrm.dao.SFRMPartnershipDVO;
import hk.hku.cecid.edi.sfrm.handler.SFRMPartnershipHandler;

import hk.hku.cecid.piazza.commons.module.LimitedActiveTaskList;
import hk.hku.cecid.piazza.commons.dao.DAOException;

/**
 * The outgoing payloads collector collects all folders which  
 * are ready to do package and handshaking(status: HS, PK, PKD).<br><br>
 * 
 * Creation Date: Unknown
 * 
 * @author Twinsen Tsang
 * @version 1.0.2
 * @since	1.0.0
 * 
 * @see hk.hku.cecid.piazza.commons.module.ActiveTaskList #getTaskList()
 */
public class OutgoingPayloadsCollector extends LimitedActiveTaskList{
 		
	private boolean isFirstLoad = true;

	/**
	 * Get the list that contains <code>OutgoingPayloadsTask</code>
	 * transformed through <code>FoldersPayload</code>.  
	 *  
	 * @param taskList
	 * 			The reference task list to insert.
	 * @param ps
	 * 			The payload state you want to extract
	 * @return
	 * 			The reference <code>taskList</code>
	 */
	private List getTaskListByPayloadStatus(ArrayList taskList, int ps){		
		try{
			int index = taskList.size();
			Iterator itr;
			
			if ( ps == PayloadsState.PLS_PENDING )
				itr = SFRMProcessor.getOutgoingPayloadRepository().getPayloads().iterator();
			else 
				itr = SFRMProcessor.getOutgoingPayloadRepository().getProcessingPayloads().iterator();
		
			// Get partnership, message and message segment handler.
			SFRMPartnershipHandler pHandle	= SFRMProcessor.getPartnershipHandler();
			SFRMPartnershipDVO pDVO			= null;
			
			while(itr.hasNext() && ++index <= this.getMaxTasksPerList()){
				
				FoldersPayload payloadDir = (FoldersPayload) itr.next();
				
				String pID = payloadDir.getPartnershipId();
				String mID = payloadDir.getMessageId();
				try{									
					// Check do we need to change the partnership record.
					if ( pDVO == null || !pDVO.getPartnershipId().equals(pID))				
						pDVO = pHandle.retreivePartnership(pID, mID);					
					
					// Add a new payload task for each payload directory.
					taskList.add(new OutgoingPayloadsTask(payloadDir, pDVO));
				}
				catch(IOException ioe){
					SFRMProcessor.core.log.error(SFRMLog.OPTC_CALLER + "IO Error", ioe);
				}
				catch(DAOException daoe){
					SFRMProcessor.core.log.error(SFRMLog.OPTC_CALLER + "Unable to retrieve partnership: " + pID, daoe); 				
				}
				catch(Exception e){
					SFRMProcessor.core.log.error(SFRMLog.OPTC_CALLER + "Unknown Error", e);		
				}
			}				 			
		}
		// Catch any exception so that the task-list does not terminate under any condition.
		catch(Exception e){
			SFRMProcessor.core.log.error(SFRMLog.OPTC_CALLER + "Unknown Error", e);			
		}	
		return taskList;
	}
	
	/**
	 * It get the set of payload directory from the outgoing payloads repository 
	 * and pass to <code>OutgoingPayloadTask</code> for pre-processing stage.
	 * 
	 * @return A list of Outgoing file task. 
	 */
	public List getTaskList(){
		ArrayList taskList = new ArrayList(this.getMaxTasksPerList());
		if (this.isFirstLoad){
			SFRMProcessor.core.log.info(SFRMLog.OPTC_CALLER + SFRMLog.FIRST_LOAD + " Redo processing payloads");
			this.getTaskListByPayloadStatus(taskList, PayloadsState.PLS_PROCESSING);
			this.isFirstLoad = false;
		}		
		return this.getTaskListByPayloadStatus(taskList, PayloadsState.PLS_PENDING);
	}
}

