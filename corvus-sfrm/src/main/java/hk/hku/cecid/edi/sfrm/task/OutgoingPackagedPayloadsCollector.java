/**
 * Contains the set of active module collector for 
 * handling packaging, segmenting, sending, joining and unpacking of payloads.
 */
package hk.hku.cecid.edi.sfrm.task;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import hk.hku.cecid.edi.sfrm.pkg.SFRMConstant; 
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.edi.sfrm.spa.SFRMLog;
import hk.hku.cecid.edi.sfrm.dao.SFRMMessageDVO;

import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.module.LimitedActiveTaskList;

/**
 * The outgoing message payloads collector collects all packaged
 * message from the DB with the associated payload which then
 * create DB segments for send.<br><br>
 * 
 * The looking query for packaged message.
 * <PRE><code>
 * 		select * from sfrm_message where message_box = ? and status = ?
 * </code></PRE>
 * 
 * Creation Date: 5/10/2006.<br><br>
 * 
 * @author Twinsen Tsang
 * @version 1.0.4
 * @since	1.0.1
 */
public class OutgoingPackagedPayloadsCollector extends LimitedActiveTaskList {
	   
	private boolean isFirstLoad = true;
	
	/**
	 * Get the list that contains <code>OutgoingPackagedPayloadTask</code>
	 * transformed through <code>SFRMMessageDVO</code>.  
	 *  
	 * @param taskList
	 * 			The reference task list to insert.
	 * @param status
	 * 			The status you want to query.
	 * @return
	 * 			The reference <code>taskList</code>
	 */
	private List getTaskListByStatus(ArrayList taskList, String status){
		int index = taskList.size();
		int maxTasks = this.getMaxTasksPerList();
		try{	
			Iterator itr = SFRMProcessor.getMessageHandler().retrieveMessages(
				SFRMConstant.MSGBOX_OUT, status).iterator();
				
			while(itr.hasNext() && ++index < maxTasks){
				SFRMMessageDVO msgDVO = (SFRMMessageDVO) itr.next();
				try{					
					// Add a new payload task for each packaged payload directory.
					taskList.add(new OutgoingPackagedPayloadsTask(msgDVO));				
				}
				catch(DAOException daoe){
					SFRMProcessor.core.log.error(SFRMLog.OPPTC_CALLER + "Unable to retrieve DVO", daoe);
				}
				catch(Exception e){
					SFRMProcessor.core.log.error(SFRMLog.OPPTC_CALLER + "Unknown Error", e);
				}
			}
		}
		catch(DAOException daoe){
			SFRMProcessor.core.log.error(SFRMLog.OPPTC_CALLER + "Unable to retrieve msgs from DB", daoe);
		}				
		// Catch any exception so that the task-list does not terminate under any condition.
		catch(Exception e){
			SFRMProcessor.core.log.error(SFRMLog.OPPTC_CALLER + "Unknown Error", e); 
		}				
		return taskList;
	}
	
    /**
	 * It get the set of payload directory from the packaged 
	 * payloads repository and pass to outgoing message 
	 * payload tasks for process.
	 * 
	 * @return A list of Outgoing message payload task. 
	 */
	public List getTaskList() {
		ArrayList taskList = new ArrayList();
		if (isFirstLoad){ 
			SFRMProcessor.core.log.info(SFRMLog.OPPTC_CALLER + SFRMLog.FIRST_LOAD + " Redo ST message");
			this.getTaskListByStatus(taskList, SFRMConstant.MSGS_SEGMENTING);
			isFirstLoad = false; 
		}					
		return this.getTaskListByStatus(taskList, SFRMConstant.MSGS_PACKAGED);
	}
}
