/**
 * Provides implementation class for the database access object 
 * (DAO and DVO) for the database layer. 
 */
package hk.hku.cecid.edi.sfrm.dao.ds;

import java.util.List;

import hk.hku.cecid.edi.sfrm.dao.SFRMMessageSegmentDAO;
import hk.hku.cecid.edi.sfrm.dao.SFRMMessageSegmentDVO;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.dao.DVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDAO;

/**
 * The data access object controller for the database table <code>sfrm_message_segment</code>. 
 * It provides some useful database-level queries.<br><br>
 *   
 * Creation Date: 29/9/2006
 * 
 * @author Twinsen Tsang
 * @version 1.0.0
 * @since	Hermes 0818
 */
public class SFRMMessageSegmentDSDAO extends DataSourceDAO implements
		SFRMMessageSegmentDAO {
	
	/**
	 * Create a new SFRM Message Segment Object.
	 * 
	 * @return a new SFRM Message Segment Object.
	 */		
	public DVO createDVO() {
		return new SFRMMessageSegmentDSDVO();
	}
	
	/**
	 * Find a message segment record with specified parameters.<br>
	 * The field "message id", "message box" , "segment no" and 
	 * "segment type" will be used for record finding.
	 * 
	 * @param messageId
	 *            The message id of the message segment.
	 * @param messageBox
	 *            The message box of the message segment.
	 * @param type
	 *            The status of the message segment
	 * @return A set of message segment which meets the specified condition
	 * 		   or empty list if no record matched.
	 * @since
	 * 			  1.0.1
	 * @throws DAOException
	 * 			  Any kind of database error.
	 */
	public SFRMMessageSegmentDVO findMessageSegmentByMessageIdAndBoxAndType(
			String messageId, String messageBox, int segmentNo, String type)
			throws DAOException{
		return (SFRMMessageSegmentDVO) super.findByKey(
				new Object[] { messageId, messageBox, new Integer(segmentNo), type });
		
		// TODO: Deleted the commented code after testing.
		/*List l = super.find(
				"find_message_segment_by_message_id_and_box_and_type",
		if (l.iterator().hasNext())
			return (SFRMMessageSegmentDVO) l.iterator().next();*/
		//return null;
	}
	
	/**
	 * Find a message segment recrod with specified parameters.<br/>
	 * The field "message id", "message box" and "type" will be used
	 * for record searching.<br/><br/>
	 * 
	 * The message segment extracted is the last updated segments
	 * by other module. 
	 * 
	 * @param messageId
	 *            The message id of the message segment. 
	 * @param messageBox
	 *            The message box of the message segment. 
	 * @param type
	 *            The type of the message segment 
	 * @return A message segment record if found.
	 * @since	
	 * 			  1.0.4
	 * @throws DAOException
	 * 			  Any kind of database error. 
	 */
	public SFRMMessageSegmentDVO
	findLastUpdatedMessageSegmentByMessageIdAndBoxAndType(
			String 	messageId,
			String 	messageBox,
			String	type) throws 
			DAOException {
		List l = super.find("find_last_updated_message_segment", 
				  new Object[] {messageId, messageBox, type});
		if (l.iterator().hasNext())
			return (SFRMMessageSegmentDVO) l.iterator().next();
		return null;
	}
	
	/**
	 * Find a set of message segment record with specified 
	 * message box and message status.<br><br>
	 * 
	 * @param messageBox
	 *            The message box of the message segment. 
	 * @param status
	 * 			  The status of the message segment.
	 * @param limit
	 * 			  The maximum message segment can be retrieved at one invocation.  
	 * @return A set of message segment which meets the specified condition
	 * 		   or empty list if no record matched.
	 * @throws DAOException
	 * 			  Any kind of database error.
	 */
	public List findMessageSegmentsByMessageBoxAndStatus(
			String messageBox, String status, int limit) throws DAOException{
		return super.find("find_message_segment_by_message_box_and_status",
				new Object[] { messageBox, status, new Integer(limit) });
	}
	
	/**
	 * Find a set of message segment record with specified 
	 * message box and message status.<br><br>
	 *  
	 * @param messageBox
	 *            The message box of the message segment. 
	 * @param status
	 * 			  	The status of the message segment.
	 * @param messageStatus
	 * 				The associated main message status of the segment.	 			
	 * @param limit
	 * 			  	The maximum message segment can be retrieved at one invocation. 
	 * @return A set of message segment which meets the specified condition
	 * 		   or empty list if no record matched.
	 * @throws DAOException
	 * 			  Any kind of database error.
	 */
	public List findMessageSegmentsByMessageBoxAndStatusAndMessageStatusNotEqualTo(
			String messageBox, String status, String messageStatus, int limit)
			throws DAOException {
		return super.find("find_message_segment_by_message_box_and_status_with_message_status_not_equal",
				new Object[] { messageBox, status, messageStatus,
								new Integer(limit) });
	}
	
	public List
	findMessageSegmentByMessageBoxAndStatusAndTypeAndMessageStatusNotEqualTo(
			String 	messageBox, 
			String 	status, 
			String  type,
			String 	messageStatus, 
			int 	limit) throws 
			DAOException{
		return super.find("find_message_segment_by_message_box_and_type_status_and_with_message_status_not_equal", 
				new Object[] { messageBox, status, type, messageStatus, 
								new Integer(limit) });
	}

	/**
	 * Find-out all segments which are incomplete in SFRM semantic.<br/><br/>
	 *  
	 * Incomplete Segments are defined as their corresponding message 
	 * is not in the status of either 'DF' or 'PS'.<br/><br/>   
	 * 
	 * The query support wildcard on <code>status</code> by using '%' string. 
	 */
	public List
	findIncompleteSegments(
			String messageBox,
			String status,
			String type,
			int    limit) throws DAOException{	
		return super.find("find_incomplete_segments", 
				new Object[]{messageBox, status, type, new Integer(limit) }); 			
	}
	
	
	/**
	 * Find how many segments is available into the database.
	 * 
	 * @param messageId
	 *            The message id of the message segment.
	 * @param messageBox
	 *            The message box of the message segment.
	 * @return 
	 * @throws DAOException
	 */
	public int findNumOfSegmentByMessageIdAndBoxAndTypeAndStatus(
			String messageId, 
			String messageBox, 
			String type, 
			String status)
			throws DAOException
	{
		List l = super.executeRawQuery(super
				.getFinder("find_num_of_segment_by_msgid_msgbox_type_status"),
				new Object[] { messageId, messageBox, type, status });
		List resultEntry = (List) l.get(0);
        return ((Number) resultEntry.get(0)).intValue();		
	}
	
	/**
	 * Find the maximum number of segment no in
	 * the database from the specified parameters. 
	 * 
	 * @param messageId
	 *            The message id of the message segment.
	 * @param messageBox
	 *            The message box of the message segment.
	 * @param type
	 *            The type of the message segment             
	 */
	public int
	findMaxSegmentNoByMessageIdAndBoxAndType(
			String messageId,
			String messageBox,
			String type) throws 
			DAOException{
		List l = super.executeRawQuery(super
				.getFinder("find_max_segment_no_by_message_id_and_box_and_type"),
				new Object[] { messageId, messageBox, type });
		if (l.size() >= 1){
			List resultEntry = (List) l.get(0);
			if (resultEntry != null){
				Object obj = resultEntry.get(0);
				if (obj != null){
					return ((Number) obj).intValue();
				}
			}
		}
		return 0;
	}
}
