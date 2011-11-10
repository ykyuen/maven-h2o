package hk.hku.cecid.edi.sfrm.com;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import hk.hku.cecid.piazza.commons.io.Archiver;

/**
 * A packaged payloads represent a archive file typed payloads.<br><br>
 * 
 * Creation Date: 6/10/2006
 * 
 * @author Twinsen Tsang
 * @version 1.0.2
 * @since	1.0.0
 */
public class PackagedPayloads extends NamedPayloads{

	/**
	 * The partnership id used by this payloads.
	 */
	private String partnershipId;

	/**
	 * The message id that this payload refer to.
	 */
	private String refMessageId;
	
	/**
	 * The archiver to pack / unpack the packaged payload. 
	 */
	private Archiver archiver = null;
		
	/** 
	 * Protected Explicit Constructor.
	 * 
	 * This constructor is mainly used for creating 
	 * a new payload proxy including the physical 
	 * file and the proxy object.
	 * 
	 * <strong>NOTE:</strong>
	 * The physical file is not created until it is 
	 * necessary. 
	 * 
	 * @param payloadsName
	 * 			The name of the newly created payload.
	 * @param initialState 
	 * 			The initialState of the payloads, 
	 * 			see {@link PayloadsState} for details.
	 * @param owner 
	 * 			The owner of the payloads. 
	 * @since	
	 * 			1.0.2
	 * @throws Exception
	 * 			Any kind of exceptions.
	 */
	protected 
	PackagedPayloads(String payloadsName, int initialState, 
			PayloadsRepository owner) throws Exception
	{
		super(payloadsName, initialState, owner);
		this.decode();
		this.clearTokens();
	}
	
	/** 
	 * Protected Explicit Constructor.
	 * 
	 * @param payloads
	 * 			The payloads directory.
	 * @param owner
	 * 			The owner of this payload.
	 * @since	
	 * 			1.0.0
	 * @throws IOException
	 * 			If the payload is not directory.
	 */
	protected 
	PackagedPayloads(File payload, PayloadsRepository owner) throws IOException{
		super(payload, owner);
		if (payload.exists() && !payload.isFile())
			throw new IOException("Payloads is not a file.");
		this.decode();
		this.clearTokens();
	}		
		
	/**
	 * @return the reference to message id.
	 */
	public String 
	getRefMessageId()
	{
		return this.refMessageId;
	}
	
	/**
	 * @return the partnership id.
	 */
	public String 
	getPartnershipId()
	{
		return this.partnershipId;
	}
	
	/**
	 * Set the default archiver for pack / unpack 
	 * when archiver is not specified.
	 * 
	 * @param archiver
	 * 			
	 * 
	 * @see #pack(FoldersPayload, Archiver)
	 * @see #pack(File, Archiver)
	 */
	public void 
	setDefaultArchiver(Archiver archiver)
	{
		if (archiver != null)
			this.archiver = archiver;
	}
	
	/**
	 * Get the default archiver.
	 *  
	 * @return
	 * 			The default archiver.
	 */	
	public Archiver 
	getDefaultArchiver()
	{
		return this.archiver;
	}
	
	/**
	 * Save the content from the input stream to this payloads.<br><br>
	 * 
	 * If the content stream is null, it save the file with empty content.<br><br>
	 *
	 * This method is rarely used in this class because it's semantics
	 * here is to copy the bytes from the inputstream to 
	 * the package payload. 
	 * 
	 * @param content
	 * 			The input content stream. 
	 * @param append 
	 * 			true if the new content is added to the existing content,
	 * 			false if the new content overwrite the existing.
	 */
	public void 
	save(InputStream content, 
		 boolean 	 append) throws IOException 
	{
		super.save(content, append);
	}
		
	/**
	 * Estimate how big in bytes is the packaged payload.<br><br>
	 * 
	 * The method does not block and return immediately.<br><br>
	 * 
	 * It does not create any packaged and temporary as well.
	 * 
	 * @param src
	 * 			The source payloads. 
	 * @return	
	 * 			The estimated packed size of payload, 
	 * 			or -1 if there are error occur or 
	 * 			there is no archiver for this payload.
	 * @see #setDefaultArchiver(Archiver);  			
	 */
	public long 
	estimatePackedSize(File src)
	{
		if (this.archiver != null){
			try{
				return this.archiver.guessCompressedSize(src);		
			}catch(Exception e){
				return -1;		
			}			
		}			
		return -1;
	}
	
	/**
	 * Estimate how big in bytes is the packaged payload.<br><br>
	 * 
	 * The method does not block and return immediately.<br><br>
	 * 
	 * It does not create any packaged and temporary as well.
	 * 
	 * @param src
	 * 			The source payloads. 
	 * @return	
	 * 			The estimated packed size of payload, 
	 * 			or -1 if there are error occur or 
	 * 			there is no archiver for this payload.
	 * @see #setDefaultArchiver(Archiver);  			
	 */
	public long 
	estimatePackedSize(FoldersPayload src)
	{
		return this.estimatePackedSize(src.getRoot());
	}
	
	/**
	 * Packs this payload to become an archive 
	 * from the given <code>src</code> payload.<br><br>
	 * 
	 * It use the default archiver to pack.<br><br>   
 	 *
 	 * <strong>[Convetor Method]</strong>
	 * It converts from outgoing payloads to packaged payloads.
 	 *
	 * @param src
	 * 			The source payloads.
	 * @since	
	 * 			1.0.2 	 
	 * @throws IOException
	 * 			any kind of I/O Errors.
	 */
	public void 
	pack(FoldersPayload src) throws IOException
	{ 
		this.pack(src, this.archiver);
	}
		
	/**
	 * Packs this payload to become an archive 
	 * from the given <code>src</code> payload.<br><br>
	 * 
	 * It use the default archiver to pack.<br><br>   
 	 *
 	 * <strong>[Convetor Method]</strong>
	 * It converts from outgoing payloads to packaged payloads.
 	 *
	 * @param src
	 * 			The src of the archive file. It can 
	 * 			either be a file or directory. 
	 * 			If latter one, the directory will NOT includes
	 * 			in the archive.
	 * @since	
	 * 			1.0.2 	 
	 * @throws IOException
	 * 			any kind of I/O Errors.
	 */
	public void 
	pack(File src) throws IOException
	{
		this.pack(src, this.archiver);
	}
	
	/**
	 * Packs this payload to become an archive 
	 * from the given <code>src</code> payload.   
 	 *
 	 * <strong>[Convetor Method]</strong>
	 * It converts from outgoing payloads to packaged payloads.
 	 *
	 * @param src
	 * 			The source payloads.
	 * @param archiver
	 * 			The archiver to be used to archive the root.
	 * @since	
	 * 			1.0.2 	 
	 * @throws IOException
	 * 			any kind of I/O Errors.
	 */
	public void 
	pack(FoldersPayload src, Archiver archiver)	throws IOException 
	{
		this.pack(src.getRoot(), archiver);
	}
	
	/**
	 * Packs this payload to become an archive 
	 * from the given <code>src</code>.   
	 * 
	 * @param src
	 * 			The src of the archive file. It can 
	 * 			either be a file or directory. 
	 * 			If latter one, the directory will NOT includes
	 * 			in the archive.
	 * @param archiver 	
	 * 			The archiver to be used to archive the root.
	 * @since	
	 * 			1.0.2
	 * @throws IOException
	 * 			any kind of I/O Errors. 			
	 */
	public void 
	pack(File src, Archiver archiver) throws IOException
	{	
		if (archiver == null)
			archiver = this.getDefaultArchiver();
		if (!archiver.compress(src, this.getRoot(), false))
			throw new IOException("Unable to archive the file: "
					+ src.getAbsolutePath());
	}
	
	/**
	 * Unpack the packaged payloads back to a 
	 * folder-structured payloads set.<br><br>
	 * 
	 * It use the default archiver to pack.<br><br> 
	 * 
	 * <strong>[Convetor Method]</strong>
	 * It converts from packaged payloads to outgoing payloads.
	 * 
	 * @return
	 * @throws IOException
	 * 			if the extraction fails or 
	 * 			other I/O Errors.
	 * 
	 * @see hk.hku.cecid.edi.sfrm.FoldersPayload
	 */
	public FoldersPayload 
	unpack() throws IOException
	{
		return this.unpack(this.archiver);
	}
		
	/**
	 * Unpack the packaged payloads back to a 
	 * folder-structured payloads set.<br><br>
	 * 
	 * <strong>[Convetor Method]</strong>
	 * It converts from packaged payloads to outgoing payloads.
	 * 
	 * @param archiver
	 * @return
	 * @throws IOException
	 * 			if the extraction fails or 
	 * 			other I/O Errors.
	 * 
	 * @see hk.hku.cecid.edi.sfrm.FoldersPayload
	 */
	public FoldersPayload 
	unpack(Archiver archiver) throws IOException
	{
		String name = this.partnershipId + 
					  NamedPayloads.decodeDelimiters + 
					  this.refMessageId;
		FoldersPayload payload = new FoldersPayload(
				name, PayloadsState.PLS_UPLOADING, this.getOwner());				
		if (!archiver.extract(this.getRoot(), payload.getRoot()))
			throw new IOException("Unable to extract the file: "
				+ this.getRoot().getAbsolutePath());
		return payload;
	}
		
	/**
	 * Decode the payload root to become some useful information.
	 */
	protected void 
	decode()
	{		
		List tokens = this.getTokens();		
		if (this.getTokens().size() < 2)
			throw new ArrayIndexOutOfBoundsException(
					"Invalid Packaged Payloads Format.");		
		this.partnershipId = (String) tokens.get(0);
		this.refMessageId = (String) tokens.get(1);		
	}

	/**
	 * 
	 */
	protected void 
	encode() 
	{
		// TODO: Encode Packaged Payloads
	}
		
	/**
	 * toString method
	 */
	public String 
	toString()
	{
		StringBuffer ret = new StringBuffer(super.toString());
		ret .append("Service     : " + this.partnershipId + " \n")
			.append("RefMessageId: " + this.refMessageId +  " \n"); 
		return ret.toString();
	}
}
