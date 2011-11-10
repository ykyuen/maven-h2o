/* 
 * Copyright(c) 2005 Center for E-Commerce Infrastructure Development, The
 * University of Hong Kong (HKU). All Rights Reserved.
 *
 * This software is licensed under the GNU GENERAL PUBLIC LICENSE Version 2.0 [1]
 * 
 * [1] http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

package hk.hku.cecid.piazza.commons.activation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.ByteBuffer;

import javax.activation.DataSource;

import hk.hku.cecid.piazza.commons.io.ByteBufferInputStream;

/**
 * ByteBufferDataSource is an implementation of the javax.activation.DataSource 
 * that represents a data source of a byte buffer.
 * 
 * Creation Date: 23/10/2006
 * 
 * @author Twinsen
 * @version 1.0.0
 * @since	1.0.1
 * @see	javax.activation.DataSource
 */
public class ByteBufferDataSource implements DataSource {
	
	/**
	 * The byte buffer of data source.
	 */
	private ByteBuffer buffer;
	
	/**
	 * The name of data source.
	 */
	private String name;
	
	/**
	 * The content type of data source.
	 */
	private String contentType;	
	    	    
    /** 
	 * Explicit Constructor.
     *      
     */
    public ByteBufferDataSource(ByteBuffer buffer)
			throws IOException {
    	this.buffer = buffer;
	}
    
    /**
     * This method will return an InputStream representing the the data 
     * and will throw an IOException if it can not do so. 
     * This method will return a new instance of InputStream with each invocation.
     */
	public InputStream getInputStream() throws IOException {
		return new ByteBufferInputStream(this.buffer);
	}
     
    /**
	 * This method always throw IO exception.
     * 
     * @throws IOException as output stream is not supported by this data source.
     * @see javax.activation.DataSource#getOutputStream()
     */
	public OutputStream getOutputStream() throws IOException {
		return null;
	}
	
	/**
	 * Get the name of the data source.
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Get the name of content type.
	 */
	public String getContentType(){
		return contentType;
	}
}
