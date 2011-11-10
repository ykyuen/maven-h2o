package hk.hku.cecid.edi.sfrm.pkg;

/**
 * The constant field for SFRM Message.<br><br>
 * 
 * Creation Date: 13/11/2006<br><br>
 * 
 * Version 1.0.1 - Added Message status Description Constant.<br><br>
 * 
 * @author Twinsen Tsang
 * @version 1.0.1
 * @since	1.0.2
 */
public interface SFRMConstant {

	public String WILDCARD		= "%";
	
	/*
	 * The constant fields related to the message box. 
	 * It can either be "outbox" and "inbox". 
	 */
	public String MSGBOX_IN		= "INBOX";	
	public String MSGBOX_OUT	= "OUTBOX";
	
	/*
	 * The constant fields related to the segment type.
	 */
	public String MSGT_META		 	 = "META";
	public String MSGT_PAYLOAD		 = "PAYLOAD";
	public String MSGT_RECEIPT		 = "RECEIPT";
	public String MSGT_LAST_RECEIPT  = "RECEIPT_LAST";
	public String MSGT_RECOVERY		 = "RECOVERY";
	public String MSGT_ERROR		 = "ERROR";
	
	/*
	 * The constant fields related to message status. 
	 */
	public String MSGS_HANDSHAKING		= "HS";
	public String MSGS_PACKAGING  		= "PK";
	public String MSGS_PACKAGED			= "PKD";
	public String MSGS_SEGMENTING 		= "ST";
	public String MSGS_PENDING	 		= "PD";
	public String MSGS_PROCESSING 		= "PR";
	public String MSGS_DELIVERED  		= "DL";
	public String MSGS_UNPACKAGING		= "UK";
	public String MSGS_PROCESSED 		= "PS";			
	public String MSGS_PROCESSING_ERROR	= "PE";
	public String MSGS_DELIVERY_FAILURE	= "DF";
	
	/*
	 * The constant fields related to message status desc. 
	 */
	public String MSGSDESC_HANDSHAKING	= "Connecting to partner.";
	public String MSGSDESC_PACKAGING	= "Message is packaging.";
	public String MSGSDESC_PACAKGED		= "Message is packaged.";
	public String MSGSDESC_PROCESSING	= "Message is processing.";
	public String MSGSDESC_PROCESSED	= "Message is processed.";
	public String MSGSDESC_SEGMENTING	= "Message is segmenting.";
	public String MSGSDESC_UNPACKAGING	= "Message is un-packaging.";	
	public String MSGSDESC_NODISKSPACE	= "Not enough disk space";
	
	/*
	 * 
	 */
	public String DEFAULT_CONTENT_TYPE 	= "application/octet-stream";
	
}
