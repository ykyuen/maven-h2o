/* 
 * Copyright(c) 2005 Center for E-Commerce Infrastructure Development, The
 * University of Hong Kong (HKU). All Rights Reserved.
 *
 * This software is licensed under the GNU GENERAL PUBLIC LICENSE Version 2.0 [1]
 * 
 * [1] http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

package hk.hku.cecid.piazza.commons.io;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import hk.hku.cecid.piazza.commons.module.Component;
import hk.hku.cecid.piazza.commons.util.StringUtilities;

/**
 * The file spitter spilt a files into many same segment with
 * equal size.<br><br>
 * 
 * The output format is shown in the following example:<br><br>
 * 
 * if the file used for split called "test.jar", then the splitted segment 
 * have the name of using default pattern: <br>
 * <em>	test.jar.1<br>
 *		test.jar.2<br>
 *		test.jar.3<br>
 *		    .     <br>
 *			. 	  <br>
 *		test.jar.10<br>
 * </em> if the file can be splitted to ten segments. <br><br>
 * 
 * <strong>Code Sample</strong><br>
 * <PRE>
 * 		FileSplitter fs = new FileSplitter(10240);	// each segment has the size 10kb.
 *		fs.setOutputPath("/output");				// The output segment directory.
 *		fs.split("../testdata/test.jar");			// Split the specified files.
 * </PRE>
 * <br><br>
 * 
 * <strong>Guideline on using as SPA Component</strong><br>
 *   
 * Version 1.0.2 - 
 * 		Performance Sightly enhanced and now support the processing pattern
 * 		for distinguishing the processing and splitted segments.
 * Version 1.0.1 - 
 * 		Performance highly enhanced by using NIO package.<br>
 *  
 * @author Twinsen
 * @version 1.0.2
 * @since	1.0.0
 */
public class FileSplitter extends Component{		
			
	/**
	 * Segment Size for each file fragment.
	 */
	private int segmentSizeInBytes = 50240; // 500kb
		
	/**
	 * The output path of the spitted files. 
	 */
	private String outputPath	   = "output";
	
	/**
	 * The splitter result information.
	 */
	private FileSplitterResult result = null;
	
	/**
	 * The pattern used for each segment.
	 */
	private String pattern = "%f.%i";
	
	/**
	 * The pattern used when processing.
	 */
	private String processingPattern = pattern;
		
	/**
	 * Explicit Constructor.
	 * 
	 * @param segmentSizeInBytes 
	 * 			The size of each segment in bytes.
	 * @since	
	 * 			1.0.0
	 */
	public FileSplitter(int segmentSizeInBytes){
		this.segmentSizeInBytes = segmentSizeInBytes;
	}
	
	/**
     * Invoked for initialization.
     * 
     * @throws Exception 
     * 			if there is any error in the initialization.
     * @since	
     * 			1.0.0
	 */
	protected void init() throws Exception{
		Properties props = this.getParameters();		
		// Set the output pattern of the file.
		this.pattern	 		= props.getProperty("pattern");
		// Set how large for each segment.
		this.segmentSizeInBytes = StringUtilities.parseInt(props.getProperty("segment-size"));
		// Set output directory.
		this.outputPath			= props.getProperty("output-directory");
	}
			
	/**
	 * Set the segment size in bytes units.
	 * 
	 * @param newSegmentSize 
	 * 			The new segment size for spitting.
	 * @since	
	 * 			1.0.0
	 */
	public void setSegmentSizeInBytes(int newSegmentSize){
		this.segmentSizeInBytes = newSegmentSize;
	}
			
	/**
	 * Set the output path of spitted file. The path 
	 * must be pointed to a directory.
	 * 
	 * @param path 
	 * 			The directory path.
	 * @since	
	 * 			1.0.0
	 */
	public void setOutputPath(String path) throws IOException{
		PathHelper.createPath(path);		
		// Repair the path if the path does not end up with '/' or '\';
		path = PathHelper.getCanonicalPath("./", path);
		char lastCharacter = path.charAt(path.length()-1);
		if (lastCharacter != File.separatorChar){
			path = path + File.separatorChar;
		}			
		this.outputPath = path;		
	}
	
	/** 
	 * @param pattern 
	 * 			The output pattern for each segment.
	 * @since	
	 * 			1.0.1
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
	
	/** 
	 * The processing pattern is used during the splitting 
	 * process. That is the said, during the coping of the 
	 * file segment, the filename of the segment will use the 
	 * processing pattern acting as it's filename. it then rename
	 * back to the output pattern after copying. 
	 * 
	 * @param pattern 
	 * 			The output pattern for each segment.
	 * @since		
	 * 			1.0.2
	 */
	public void setProcessingPattern(String processingPattern) {
		this.processingPattern = processingPattern;
	}
		
	/**
	 * Get the segment size in bytes units.
	 * 
	 * @return 
	 * 			The segment size for spitting the files.
	 * @since	
	 * 			1.0.0
	 */
	public int getSegmentSizeInBytes(){
		return this.segmentSizeInBytes;
	}
	
	/**
	 * Get the output path of the file splitting.
	 * 
	 * @return 
	 * 			The segment size for spitting the files. 
	 * @since	
	 * 			1.0.0
	 */
	public String getOutputPath(){
		return this.outputPath;
	}

	/**
	 * @return 
	 * 			The pattern used for generating the name of segment.
	 * @since	
	 * 			1.0.1
	 */
	public String getPattern() {
		return pattern;
	}
	
	/**
	 * @return 
	 * 			The processing pattern used for generating the name of segment
	 * 			during processing.
	 * @since	
	 * 			1.0.2
	 */
	public String getProcessingPattern(){
		return this.processingPattern;
	}
	
	/**
	 * @return 
	 * 			Get the result of file splitting.  
	 * @since	
	 * 			1.0.0
	 */
	public FileSplitterResult getResultInfo(){
		return this.result;
	}	
	
	/**
	 * Generate the actual filepath by replacing the special
	 * symbol %f, %i to filename and segment no. correspondingly.
	 * 
	 * @param pattern
	 * 			The pattern used for generated the actual path.
	 * @return 
	 * 			The generated path for each segment according to the 
	 * 		   	pattern.
	 * @since	
	 * 			1.0.0
	 */
	private String getGeneratedPath(String pattern, File f, int segmentNumber){
		String ret = this.pattern;		
		ret = ret.replaceAll("%f", f.getName());
		ret = ret.replaceAll("%i", String.valueOf(segmentNumber));		
		return this.outputPath + ret;				
	}
	
	/**
	 * Split the files by the specified filepath.
	 * 
	 * @param filepath
	 * 			The filepath to be split to. 
	 * @throws IOException
	 */
	public void split(String filepath) throws IOException{
		this.split(new File(filepath));
	}
		 	
	/**
	 * Split the files by the specified file object. 
	 * 
	 * @param file
	 * 			The file to split.
	 * @throws IOException
	 */
	public void split(File file) throws IOException{		
		// Create FileSplitter Result
		this.result = new FileSplitterResult();
		
		// Record start time.
		long startTime = System.currentTimeMillis();
		
		if (file == null)
			throw new IOException("File is null or missing.");
		if (file.isDirectory())
			throw new IOException("File is direcotry.");
				
		FileInputStream fis = new FileInputStream(file);
		
		// Calculate how many segment need to be split.
		long fileSize 		= fis.available(); 
		long numOfSegment	= fileSize / this.segmentSizeInBytes;
		long lastSegmentSize= fileSize - (numOfSegment * this.segmentSizeInBytes);
					
		for (int i = 1; i <= numOfSegment + 1; i++){
			String filepath = this.getGeneratedPath(this.processingPattern, file, i);			
			FileOutputStream fos = new FileOutputStream(filepath);
			NIOHandler.pipe(fis, fos, (i - 1) * this.segmentSizeInBytes,
					this.segmentSizeInBytes);
			fos.close();
			PathHelper.renameTo(new File(filepath), this.getGeneratedPath(
					this.pattern, file, i));
		}							
		fis.close();
		
		// Record end time.
		long endTime = System.currentTimeMillis();

		// Update the result.
		this.result.originalFilename		= file.getName();
		this.result.filesize				= fileSize;
		this.result.filesegment 			= numOfSegment + 1;
		this.result.segmentSizeInBytes		= this.segmentSizeInBytes;
		this.result.lastSegmentSizeInBytes	= lastSegmentSize;
		this.result.processingTimeInMs		= endTime - startTime;
	}	
		
	/**
	 * Pre-analyze the file splitting process.
	 * 
	 * @param file
	 * 			The file to be used for analyze.
	 * @throws IOException
	 * 			if the file is null.
	 */
	public void analyzeFileSplitting(File file) throws IOException{
		// Create FileSplitter Result
		this.result = new FileSplitterResult();
	
		if (file == null)
			throw new IOException("File is null or missing.");	
			
		FileInputStream fis = new FileInputStream(file);
			
		// Calculate how many segment need to be split.
		long fileSize 		= fis.getChannel().size();
		long numOfSegment	= fileSize / this.segmentSizeInBytes;
		long lastSegmentSize= fileSize - (numOfSegment * this.segmentSizeInBytes);		
		fis.close();
		
		// update the result.
		this.result.originalFilename		= file.getName();
		this.result.filesize				= fileSize;
		this.result.filesegment 			= numOfSegment + 1;
		this.result.segmentSizeInBytes		= this.segmentSizeInBytes;
		this.result.lastSegmentSizeInBytes	= lastSegmentSize;				
	}
		
	/**
	 * Testing Entry for this class.
	 * 
	 * @param args 
	 */
	/*public static void main(String[] args){
		try{			
			if (args.length < 3){
				System.out.println("Usage: filesplitter-test "
								  +"[file-name] "
								  +"[output-dir] "
								  +"[segment size in bytes] "
								  +"[pattern] "
								  +"[multi-thread] "
								  +"[max-thread-count]");
				System.out.println();
				System.out.println("Example: filesplitter-test ../testdata-input/test.gif ../testdata-output/ 1024 %f.%i");
				System.exit(1);
			}					
			System.out.println("  File splitter test case start  ");
			System.out.println("---------------------------------");
			System.out.println("File to be tested     " + args[0]);
			System.out.println("Output Directory      " + args[1]);
			System.out.println("Segment size in bytes " + args[2]);				
			
			FileSplitter fs = new FileSplitter(Integer.parseInt(args[2]));
			fs.setOutputPath(args[1]);
			
			if (args.length >= 4){
				System.out.println("Pattern               " + args[3]);			
				fs.setPattern(args[3]);
			}										
			
			fs.split(new File(args[0]));						
			
			FileSplitterResult fsr = fs.getResultInfo();
			
			System.out.println("Elapsed Time: " + ((double)fsr.processingTimeInMs / 1000));			
			
			System.out.println("---------------------------------");
		}catch(Exception e){
			e.printStackTrace();
		}		
	}*/
}
