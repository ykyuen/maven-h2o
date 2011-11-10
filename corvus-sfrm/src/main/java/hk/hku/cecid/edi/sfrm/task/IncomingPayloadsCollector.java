/**
 * Contains the set of active module collector for 
 * handling packaging, segmenting, sending, joining and unpacking of payloads.
 */
package hk.hku.cecid.edi.sfrm.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import hk.hku.cecid.edi.sfrm.com.PackagedPayloads;
import hk.hku.cecid.edi.sfrm.com.PayloadsState;
import hk.hku.cecid.edi.sfrm.spa.SFRMLog;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;

import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.module.ActiveTaskList;

/**
 * The incoming payloads collector collects all joined files (i.e. A archive
 * files) from the incoming payloads repository and do unpackaging (status: UK).
 * <br><br>
 * 
 * Creation Date: 16/10/2006.<br>
 * <br>
 * 
 * @author 	Twinsen Tsang
 * @version 1.0.1
 * @since 	1.0.0
 */
public class IncomingPayloadsCollector extends ActiveTaskList {

	private boolean isFirstLoad = true;

	/**
	 * Get the list that contains <code>IncomingPayloadsTask</code>
	 * transformed through <code>PackagedPayloads</code>.  
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
			Iterator itr;
			PackagedPayloads payload;
			if ( ps == PayloadsState.PLS_PENDING )
				itr = SFRMProcessor.getIncomingPayloadRepository().getPayloads().iterator();
			else
				itr = SFRMProcessor.getIncomingPayloadRepository().getProcessingPayloads().iterator();				
					
			while(itr.hasNext()){
				payload = (PackagedPayloads) itr.next();				
				try{														
					// Add a new payload task for each packaged payload directory.				
					taskList.add(new IncomingPayloadsTask(payload));			
				}
				catch(IOException ioe){
					SFRMProcessor.core.log.error(SFRMLog.IPTC_CALLER + "IO Error", ioe);
				}
				catch(DAOException daoe){
					SFRMProcessor.core.log.error(SFRMLog.IPTC_CALLER + "Unable to retrieve DVO", daoe); 				
				}
				catch(Exception e){
					SFRMProcessor.core.log.error(SFRMLog.IPTC_CALLER + "Unknown Error", e);		
				}
			}				 			
		}
		// Catch any exception so that the task-list does not terminate under any condition.
		catch(Exception e){
			SFRMProcessor.core.log.error(SFRMLog.IPTC_CALLER + "Unknown Error", e);			
		}	
		return taskList;
	}
		
	/**
	 * It get the set of payload directory from the incoming 
	 * payloads repository and pass to incoming payload tasks for process.
	 * 
	 * @return A list of incoming payload task. 
	 */
	public List getTaskList() {
		ArrayList taskList = new ArrayList(); 			
		if (this.isFirstLoad){
			SFRMProcessor.core.log.info(SFRMLog.IPTC_CALLER + SFRMLog.FIRST_LOAD + " Retry processing payloads.");
			this.getTaskListByPayloadStatus(taskList, PayloadsState.PLS_PROCESSING);
			this.isFirstLoad = false;
		}		
		return this.getTaskListByPayloadStatus(taskList, PayloadsState.PLS_PENDING);
	}
}
