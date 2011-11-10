/**
 * Provides implementation class for the database access object 
 * (DAO and DVO) for the database layer. 
 */
package hk.hku.cecid.edi.sfrm.dao.ds;

import java.sql.Timestamp;
import java.util.Hashtable;

import hk.hku.cecid.edi.sfrm.dao.SFRMMessageDVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDVO;

/**
 * The <code>SFRMMessageDSDVO</code> is a data value object that represent
 * a tabular row in <em>sfrm_message</em> at the persistence layer.<br><br>   
 * 
 * It possesses caching automatically for most frequently fields shown below:
 * <ol>
 * 	<li>message id</li>
 * 	<li>message box</li>
 * 	<li>partnership id</li>
 * 	<li>partnership endpoint</li>
 * 	<li>requires signing / encryption</li>
 * 	<li>status</li>
 * </ol><br/>
 * 
 * So developers do not need to worry the issue of thread contention and 
 * can freely call the <em>get</em> and <em>set</em> with no performance impact.<br/> 
 * 
 * Creation Date: 29/9/2006<br><br>
 * 
 * Version 1.0.3 - 
 * <ul>
 * 	<li>Added cache for hot access field, it requires extra <em>22</em> bytes 
 *      per <code>SFRMMessageDSDVO</code> object.</li>
 * </ul>
 * 
 * Version 1.0.2 - 
 * <ul>
 * 	<li>Added conversation id</li>
 * </ul>
 * 
 * @author Twinsen Tsang
 * @version 1.0.3
 * @since	1.0.0
 */
public class SFRMMessageDSDVO extends DataSourceDVO implements SFRMMessageDVO {

	/**
	 * Compiler Generated Serial Version ID.
	 */
	private static final long serialVersionUID = 9058156951853018190L;
	
	/**
	 * The cached message id. [4B]
	 */
	private String messageId;

	/**
	 * The cached message box. [4B] 
	 */
	private String messageBox;
	
	/**
	 * The cached partnership id [4B]
	 */
	private String partnershipId; 
	
	/**
	 * The cached partnership endpoint [4B]
	 */
	private String partnerEndpoint;
	
	/**
	 * The cached signing flag [1B]
	 */
	private boolean isSigned;
	
	/**
	 * The cached signing flag [1B]
	 */
	private boolean isEncrypted;
	
	/**
	 * The cached status value. [4B]
	 */
	private String status;
	
	/**
	 * [@OVERRIDE] set the DVO interval dataset and update some 
	 * <code>boolean<code> cached values. 
	 */
	public void setData(Hashtable hs){
		super.setData(hs);
		this.isSigned 	 = super.getBoolean("isSigned");
		this.isEncrypted = super.getBoolean("isEncrypted");
	}

	/**
	 * [@GET, THREAD-SAFETY, CACHABLE] Get the message id from the message DVO.
	 */
	public String getMessageId(){
		if (this.messageId == null)
			this.messageId = super.getString("messageId");
		return this.messageId;
	}
	
	/**
	 * [@SET, THREAD-SAFETY, CACHABLE] Set the message id from the message DVO.
	 * 
	 * @param messageId the new message id.
	 */
	public void setMessageId(String messageId){
		super.setString("messageId", messageId);
		this.messageId = messageId;
	}
		
	/**
	 * [@GET, THREAD-SAFETY, CACHABLE] 
	 * 
	 * @return the message box from the message DVO.
	 */
	public String getMessageBox(){
		if (this.messageBox == null)
			this.messageBox = super.getString("messageBox");
		return this.messageBox;
	}
	
	/**
	 * [@SET, THREAD-SAFETY, CACHABLE] Set the message box to the message DVO.
	 *  
	 * @param message box either <strong>INBOX</strong> OR <strong>OUTBOX</strong> 
	 */
	public void setMessageBox(String messageBox){
		super.setString("messageBox", messageBox);
		this.messageBox = messageBox;
	}
	
	/**
	 * [@GET, THREAD-SAFETY, CACHABLE] 
	 * 
	 * @return the partnership id from the message DVO.
	 */
	public String getPartnershipId(){
		if (this.partnershipId == null)			
			this.partnershipId = super.getString("partnershipId");
		return this.partnershipId;
	}
		
	/**
	 * [@SET, THREAD-SAFETY, CACHABLE] Set the partnership id to the message DVO.
	 *  
	 * @param partnershipId the partnership id of this message DVO. 
	 */
	public void setPartnershipId(String partnershipId){		
		super.setString("partnershipId", partnershipId);
		this.partnershipId = partnershipId;
	}
		
	/**
	 * [@GET, THREAD-SAFETY, CACHABLE] 
	 * 
	 * @return the partnership endpoint from the message DVO.
	 */
	public String getPartnerEndpoint(){
		if (this.partnerEndpoint == null) 
			this.partnerEndpoint = super.getString("partnerEndpoint");
		return this.partnerEndpoint;
	}
	
	/**
	 * [@SET, THREAD-SAFETY, CACHABLE] Set the partnership endpoint to the message DVO.
	 *  
	 * @param partnershipId the partnership endpoint of this message DVO. 
	 */
	public void setPartnerEndpoint(String partnerEndpoint){
		super.setString("partnerEndpoint", partnerEndpoint);
		this.partnerEndpoint = partnerEndpoint;
	}
	
	/**
	 * [@GET, THREAD-SAFETY]
	 * 
	 * @return the total segment of this message DVO.
	 */
	public int getTotalSegment(){
		return super.getInt("totalSegment");
	}
	
	/**
	 * [@SET, THREAD-SAFETY] Set the total segment of this message DVO.
	 * 
	 * @param totalSegment the total segment of this message DVO.
	 */
	public void setTotalSegment(int totalSegment){
		super.setInt("totalSegment", totalSegment);
	}
	
	/**
	 * [@GET, THREAD-SAFETY]
	 * 
	 * @return the total size of this message DVO.
	 */
	public long getTotalSize(){
		return super.getLong("totalSize");
	}	
	
	/**
	 * [@SET, THREAD-SAFETY] Set the total size of this message DVO.
	 * 
	 * @param totalSegment the total size of this message DVO.
	 */
	public void setTotalSize(long totalSize){
		super.setLong("totalSize", totalSize);
	}		
	
	/**
	 * [@GET, THREAD-SAFETY, CACHABLE] 
	 * 
	 * @return whether the message requires to be signed.
	 */
	public boolean getIsSigned(){
		return this.isSigned;
	}
	
	/**
	 * [@SET, THREAD-SAFETY, CACHABLE] Set whether the message requires signing.
	 *  
	 * @param isSigned it sign if it is set, vice versa. 
	 */
	public void setIsSigned(boolean isSigned){
		super.setBoolean("isSigned", isSigned);
		this.isSigned = isSigned;
	}
	
	/**
	 * [@GET, THREAD-SAFETY, CACHABLE] Set whether the message requires encryption.
	 * 
	 * @return whether the message requires encryption.
	 */
	public boolean getIsEncrypted(){
		return this.isEncrypted;
	}
	
	/**
	 * [@SET, THREAD-SAFETY, CACHABLE] Set whether the message requires signing.
	 *  
	 * @param isSigned it sign if it is set, vice versa. 
	 */
	public void setIsEncryped(boolean isEncrypted){
		super.setBoolean("isEncrypted", isEncrypted);
		this.isEncrypted = isEncrypted;
	}
	
	/**
	 * [@GET, THREAD-SAFETY, CACHABLE] 
	 * 
	 * @return get the status of the message DVO. 
	 */
	public String getStatus(){
		if (this.status == null)
			this.status = super.getString("status");
		return this.status;
	}
	
	/**
	 * [@SET, THREAD-SAFETY, CACHABLE]
	 * 
	 * @param status The new status of message DVO.
	 */
	public void setStatus(String status){
		super.setString("status", status);
		this.status = status;
	}
	
	/**
	 * [@GET, THREAD-SAFETY]
	 * 
	 * @return the brief description about the message status.
	 */
	public String getStatusDescription(){
		return super.getString("statusDescription");
	}
	
	/**
	 * [@SET, THREAD-SAFETY] Set the brief description about the message status.
	 * 
	 * @param statusDescription the brief description about the message status. 
	 */
	public void setStatusDescription(String statusDescription){
		super.setString("statusDescription", statusDescription);
	}
	
	/**
	 * [@GET, THREAD-SAFETY] 
	 * 
	 * @return the creation timestamp of this message.
	 */
	public Timestamp getCreatedTimestamp(){
		return (Timestamp) super.get("createdTimestamp");
	}
	
	/**
	 * [@SET, THREAD-SAFETY]
	 * 
	 * @param createdTimestamp set the creation timestamp of this message.
	 */
	public void setCreatedTimestamp(Timestamp createdTimestamp){
		super.put("createdTimestamp", createdTimestamp);
	}
	
	/**
	 * [@GET, THREAD-SAFETY] 
	 * 
	 * @return the timestamp that message is proceeding.
	 */
	public Timestamp getProceedTimestamp(){
		return (Timestamp) super.get("proceedTimestamp");
	}
	
	/**
	 * [@SET, THREAD-SAFETY]
	 * 
	 * @param proceedTimestamp set the timestamp that message is proceeding.
	 */
	public void setProceedTimestamp(Timestamp proceedTimestamp){
		super.put("proceedTimestamp", proceedTimestamp);
	}
	
	/**
	 * [@GET, THREAD-SAFETY] 
	 * 
	 * @return the timstamp that the message has been processed completely. 	
	 */
	public Timestamp getCompletedTimestamp(){
		return (Timestamp) super.get("completedTimestamp");
	}
	
	/**
	 * [@SET, THREAD-SAFETY]
	 * 
	 * @param the timestamp that the message has been processed completely. 
	 */
	public void setCompletedTimestamp(Timestamp completedTimestamp){
		super.put("completedTimestamp", completedTimestamp);
	}
}
