/* 
 * Copyright(c) 2005 Center for E-Commerce Infrastructure Development, The
 * University of Hong Kong (HKU). All Rights Reserved.
 *
 * This software is licensed under the GNU GENERAL PUBLIC LICENSE Version 2.0 [1]
 * 
 * [1] http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

package hk.hku.cecid.piazza.commons.io;

import hk.hku.cecid.piazza.commons.data.Data;

/**
 * This is the <code>file-splitter</code> result 
 * structure.<br><br>
 * 
 * Creation Date: Unknown
 * 
 * @author Twinsen Tsang
 * @version 1.0.0
 * @since	1.0.0
 * 
 * @see hk.hku.cecid.piazza.commons.io.FileSplitter
 */
public class FileSplitterResult implements Data{

	/**
	 * The original file name to split.
	 */
	public String originalFilename		= "";
	
	/**
	 * The file size of splitted file.
	 */
	public long filesize				= 0;
	
	/**
	 * The number of segment of splitted file.
	 */
	public long filesegment				= 0;
	
	/**
	 * The number of segment of splitted file.
	 */
	public long segmentSizeInBytes		= 0;
	
	/**
	 * The size of last segment for splitted file.
	 */
	public long lastSegmentSizeInBytes	= 0;
	
	/**
	 * The elapsed time for splitting.
	 */
	public long processingTimeInMs 		= 0;
	
	/**
	 * Constructor.
	 */
	public FileSplitterResult(){}
		
	/**
	 * Explicit Constructor.
	 * 
	 * @param originalFilename
	 * 			The original file name to split.
	 * @param filesize
	 * 			The file size of splitted file.
	 * @param filesegment
	 * 			The number of segment of splitted file.
	 * @param segmentSizeInBytes
	 * 			The size of each segment for splitted file.
	 * @param lastSegmentSizeInBytes
	 * 			The size of last segment for splitted file.
	 * @param processingTimeInMs
	 * 			The elapsed time for splitting.
	 */
	public FileSplitterResult(String originalFilename
							 ,long   filesize
							 ,long	 filesegment
							 ,long 	 segmentSizeInBytes
							 ,long	 lastSegmentSizeInBytes
							 ,long	 processingTimeInMs){
		this.originalFilename 		= originalFilename;
		this.filesize		  		= filesize;
		this.filesegment			= filesegment;
		this.segmentSizeInBytes		= segmentSizeInBytes;
		this.lastSegmentSizeInBytes	= lastSegmentSizeInBytes;
		this.processingTimeInMs		= processingTimeInMs;
	}
}
