package hk.hku.cecid.edi.sfrm.service;

import java.sql.Timestamp;
import javax.xml.soap.SOAPElement;

import org.w3c.dom.Element;

import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;
import hk.hku.cecid.edi.sfrm.spa.SFRMLog;
import hk.hku.cecid.edi.sfrm.pkg.SFRMConstant;
import hk.hku.cecid.edi.sfrm.dao.SFRMMessageDVO;
import hk.hku.cecid.edi.sfrm.handler.SFRMMessageHandler;
import hk.hku.cecid.edi.sfrm.handler.SFRMMessageSegmentHandler;

import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.soap.SOAPRequestException;
import hk.hku.cecid.piazza.commons.soap.WebServicesAdaptor;
import hk.hku.cecid.piazza.commons.soap.WebServicesRequest;
import hk.hku.cecid.piazza.commons.soap.WebServicesResponse;

/**
 * The <code>SFRMMessageStatusQueryService</code> is a SOAP-based Web services
 * for querying the current status of particular SFRM message.<br/><br/>
 * 
 * The web services schema is defined here:<br/><br/>
 * 
 * Input XML tag:<br/><br/>
 * 
 * <pre>
 *  &lt;messageId&gt; <em>messageId</em>&lt;/messageId&gt;
 * </pre>
 * 
 * Output XML tag:<br/><br/>
 * 
 * <pre>
 *  &lt;messageInfo&gt;
 *  	&lt;status&gt; <em>The current status of message</em> &lt;/status&gt;
 *  	&lt;statusDescription&gt; <em>The current status description of message</em> &lt;/statusDescription&gt;
 *  	&lt;numberOfSegments&gt; <em>Maximum number of segments</em> &lt;/numberOfSegments&gt;
 *  	&lt;numberOfProcessedSegments&gt; <em>Number of processed segments</em> &lt;/numberOfProcessedSegments&gt;
 *  	&lt;lastUpdatedTime&gt; <em> The last updated timestamp </em> &lt;/lastUpdatedTime&gt;
 *  &lt;/messageInfo&gt;
 * </pre><br/><br>
 * 
 * There are some situation that the returned value may differ than your expectation and they are 
 * listed below: 
 * <ol>
 * 	<li>When the SFRM message is in status of handshaking (HS), the number of segments is <strong>
 * 		maximum</strong> integer. This is because the number of segments can not be determined at that
 * 		moment.
 * 	</li>
 * 	<li>When the SFRM message can't be found by the web service parameter. The returned tuple is 
 * 		<strong> ["N/A","", "2147483647","0","<em>current time</em>] </strong>.
 * 	</li>
 * </ol><br/><br/> 
 *  
 * Creation Date: 26/4/2007
 * 
 * @author Twinsen Tsang
 * @version 1.0.0
 * @since Dwarf 10329
 */
public class SFRMMessageStatusQueryService extends WebServicesAdaptor {

	private static String SFRM_XMLNS = "http://service.sfrm.edi.cecid.hku.hk/"; 
	
	/**
	 * Process the web services call by the client.
	 * 
	 * @param request
	 * 			The SOAP-based Web services request. 
	 * @param response
	 * 			The SOAP-based Web services response.
	 * @throws SOAPRequestException
	 *			Unable to process the SOAP request. 			
	 * @throws DAOException
	 * 			Error in database connectivity.
	 */
	public void serviceRequested(
		WebServicesRequest 	request,
		WebServicesResponse response) throws SOAPRequestException, DAOException 
	{
		Element[] bodies = request.getBodies();
		String messageID = getText(bodies, "messageId");
		
		if (messageID == null) {
			throw new SOAPRequestException("Missing Parameters - Message ID");
		}
		
		SFRMMessageHandler mHandle = SFRMProcessor.getMessageHandler();
		SFRMMessageSegmentHandler msHandle = SFRMProcessor.getMessageSegmentHandler();
		
		// Extract the SFRM message by the web services parameter
		SFRMMessageDVO mDVO = mHandle.retrieveMessage(messageID, SFRMConstant.MSGBOX_OUT);		
				
		if (mDVO != null){
			String status 		 = mDVO.getStatus();
			String statusDesc 	 = mDVO.getStatusDescription();
			String numOfSegments = String.valueOf(mDVO.getTotalSegment());
			
			// Extract number of processed segments.			
			String numOfProcessedSegments = String.valueOf(msHandle.retrieveMessageSegmentCount(
				messageID, SFRMConstant.MSGBOX_OUT,	SFRMConstant.MSGT_PAYLOAD, SFRMConstant.MSGS_PROCESSED));
			
			// Extract the last update timestamp.
			Timestamp lastUpdatedTime  = msHandle.retrieveLastUpdatedTimestamp(
				messageID, SFRMConstant.MSGBOX_OUT, SFRMConstant.MSGT_PAYLOAD);

			/*
			 * The column of number of segments present in the SFRM message 
			 * is not entered until the status changes to MSGS_PACKAGING.
			 * so it will use Integer.MAX_VALUE as undetermined.   
			 */
			if (status.equals(SFRMConstant.MSGS_HANDSHAKING))
				numOfSegments = String.valueOf(Integer.MAX_VALUE);
						
			/*
			 * The last updated timestamp may be NULL for message segment if the 
			 * SFRM message is in the stage of HS, PK, PKD and ST. So it uses the 
			 * SFRM message proceeding timestamp instead of last segment timestamp.
			 * If the proceeding timestamp is NULL also, it uses the creation 
			 * timestamp of the message.      
			 */  
			if (lastUpdatedTime == null){
				lastUpdatedTime = mDVO.getProceedTimestamp();
				if (lastUpdatedTime == null)
					lastUpdatedTime = mDVO.getCreatedTimestamp();
					// Unknown situation, mark it timeout immediately.
					if (lastUpdatedTime == null)
						lastUpdatedTime = new Timestamp(System.currentTimeMillis());
			}
			
			// Log information.
			String detail = SFRMLog.SQS_CALLER + SFRMLog.QUERY_STATUS
				+" msg id: " + messageID
				+" status: " + status
				+" sgt ct: " + numOfSegments
				+" proc sgt ct: " + numOfProcessedSegments
				+" last uptime: " + lastUpdatedTime;
			
			SFRMProcessor.core.log.debug(detail);
									
			/*
			 * Generate the SOAP reply message.
			 */
			this.generateReply(
				response, status, statusDesc, numOfSegments,
				numOfProcessedSegments, lastUpdatedTime.toString());
		}
		else{
			this.generateReply(
				response, "N/A", "SFRM message does not found", String.valueOf(Integer.MAX_VALUE), 
				"0" , new Timestamp(System.currentTimeMillis()).toString());
		}
	}
	
	/**
	 * Generate the SOAP response according to the specified parameters. 
	 * 
	 * @param response
	 * 			The SOAP-based Web services response.
	 * @param status
	 * 			The current status of the SFRM message.
	 * @param statusDescription
	 * 			The current status description of the SFRM message. 		
	 * @param numOfSegments
	 * 			The maximum number of segments of this SFRM message. 
	 * @param numOfProcessedSegments
	 * 			The number of processed segments of this SFRM message.
	 * @param lastUpdateTimestamp
	 * 			The last update timestamp of this SFRM message.
	 * @throws SOAPRequestException
	 * 			If unable to generate the response element.
	 */
	private void generateReply(
			WebServicesResponse response, 
			String status, String statusDescription, 
			String numOfSegments, String numOfProcessedSegments, 
			String lastUpdatedTime) throws SOAPRequestException 
	{
		try {
			
			SOAPElement rootElement = createElement("messageInfo", "", SFRM_XMLNS, "MessageInfo");
			
			rootElement.addChildElement(
				createText("status", status, SFRM_XMLNS));
			rootElement.addChildElement(
				createText("statusDescription", replaceNullToEmpty(statusDescription), SFRM_XMLNS));
			rootElement.addChildElement(
				createText("numberOfSegments", numOfSegments, SFRM_XMLNS));
			rootElement.addChildElement(
				createText("numberOfProcessedSegments", numOfProcessedSegments, SFRM_XMLNS));
			rootElement.addChildElement(
				createText("lastUpdatedTime", lastUpdatedTime, SFRM_XMLNS));
			
			response.setBodies(new SOAPElement[] { rootElement });
			
		} catch (Exception e) {
			throw new SOAPRequestException("Unable to generate reply message", e);
		}
	}

	/**
	 * Replace all NULL String to Empty String.
	 * 
	 * @param value The string to replace.
	 */
	private String replaceNullToEmpty(String value) {
		return value == null ? new String ("") : value;
	}

	protected boolean isCacheEnabled() {
		return false;
	}
}

