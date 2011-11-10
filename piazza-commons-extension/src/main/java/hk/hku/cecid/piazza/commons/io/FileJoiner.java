/* 
 * Copyright(c) 2005 Center for E-Commerce Infrastructure Development, The
 * University of Hong Kong (HKU). All Rights Reserved.
 *
 * This software is licensed under the GNU GENERAL PUBLIC LICENSE Version 2.0 [1]
 * 
 * [1] http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

package hk.hku.cecid.piazza.commons.io;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.Comparator;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import hk.hku.cecid.piazza.commons.util.StringUtilities;

/**
 * The file joiner join a files from many segment specified 
 * in the command line option.
 * 
 * The common usage is shown on the below: <br>
 * 
 * <PRE>
 * 		FileJoiner fj = new FileJoiner();
 * 		fj.set
 *  
 * 
 * Version 1.0.1 - Performance highly enhanced by using NIO package.<br>
 * 	 
 * Creation Date: Unknown 
 * 
 * @author Twinsen Tsang
 * @version 1.0.1
 * @since	1.0.0
 */
public class FileJoiner {

	/**
	 * The lookup directory of the file joiner.
	 */
	private String lookupDirectory;
	
	/**
	 * The output directory of the file joiner.
	 */
	private String outputPath;
	
	/**
	 * The processing output path of the file joiner.
	 */
	private String processingOutputPath;
		
	/**
	 * The flag indicating whether delete all file segment 
	 * after joining.
	 */
	private boolean deleteSegmentUponJoin = false;
	
	/**
	 * The pattern used for each segment.
	 */
	private String pattern = "%f.%i";
	
	/**
	 * The regular pattern used for parsing the files.
	 */
	private Pattern regularPattern = Pattern.compile(pattern);
	
	/**
	 * Default Constructor.
	 */
	public FileJoiner()
	{		
	}
	
	/**
	 * Set the directory for searching the fragment files during
	 * joining.
	 */
	public void 
	setLookupDirectory(String path)
	{
		this.lookupDirectory = path;
	}
	
	/**
	 * Set the flag indicating whether delete all file segment 
	 * after joining.
	 * 
	 * @param delete the flag whether it deletes or not.
	 */
	public void 
	setDeleteSegmentUponJoin(boolean delete)
	{
		deleteSegmentUponJoin = delete;
	}
	
	/**
	 * @param pattern the regular pattern to set
	 */
	public void 
	setPattern(String pattern) 
	{
		this.pattern = pattern;
		this.compilePattern(pattern);
	}

	/**
	 * @param outputDirecotry the outputDirecotry to set
	 */
	public void 
	setOutputPath(String outputPath) 
	{
		this.outputPath = outputPath;
	}
	
	/**
	 * @param processingOutputPath the processingOutputPath to set
	 */
	public void 
	setProcessingOutputPath(String processingOutputPath) 
	{
		this.processingOutputPath = processingOutputPath;
	}

	/**
	 * Get the directory for searching the fragment files during
	 * joining.
	 */
	public String 
	getLookupDirectory(String path)
	{
		return this.lookupDirectory;
	}
		
	/**
	 * @return the pattern
	 */
	public String 
	getPattern() 
	{
		return pattern;
	}
	
	/**
	 * @return the pattern in regular expression form.
	 */
	public String
	getRegularPattern()
	{
		return this.regularPattern.pattern();
	}
	
	/**
	 * @return the outputDirecotry
	 */
	public String
	getOutputDirecotry() 
	{
		return outputPath;
	}
	
	/**
	 * @return the processingOutputPath
	 */
	public String 
	getProcessingOutputPath() 
	{
		return processingOutputPath;
	}
		
	/**
	 * Get the segment number from the filename.
	 *  
	 * @param filename
	 * 			The input filename.
	 * @return
	 * 			The segment number.
	 */
	private int 
	getSegmentNo(String filename)
	{				
		String actualPattern = this.pattern;
		int index = -1;
		int capturingGroup = 0;
		index = actualPattern.indexOf("%f");		
		capturingGroup = index != -1 ? 3 : 1;
		Matcher matcher = this.regularPattern.matcher(filename);		
		if (matcher.find())
			return StringUtilities.parseInt(matcher.group(capturingGroup), Integer.MIN_VALUE);
		return Integer.MIN_VALUE;
	}
	
	/**
	 * Get the flag indicating whether delete all file segment 
	 * after joining.
	 */ 
	public boolean 
	isDeleteSegmentUponJoin()
	{
		return this.deleteSegmentUponJoin;
	}
	
	/**
	 * Compile the regular expr pattern.
	 * 
	 * @param pattern 			
	 */
	protected void 
	compilePattern(String pattern)
	{
		if (pattern == null)
			return;
		String  escapePatternStr = "([\\D&&\\W&&[^\\%]&&[^\\.]&&[^\\@]&&[^\\-]])";		
		Pattern escapePattern = Pattern.compile(escapePatternStr);
		Matcher matcher	      = escapePattern.matcher(pattern);
		StringBuffer sb		  = new StringBuffer();
		while(matcher.find()){
			matcher.appendReplacement(sb, "\\\\\\" + matcher.group(1));
		}
		matcher.appendTail(sb);
		String actualPattern = sb.toString();
		actualPattern = actualPattern.replaceAll("%f", "([a-zA-z_0-9]*)");
		actualPattern = actualPattern.replaceAll("%i", "(([0-9]*))");
		this.regularPattern = Pattern.compile(actualPattern);
	}
	
	/**
	 * Join the files using the current setting.
	 * 
	 * @throws IOException
	 */
	public void 
	join() throws IOException
	{
		this.join(new File(this.lookupDirectory));
	}
	
	/**
	 * 
	 * @param directory
	 * @param prefix
	 * @throws Exception
	 */
	public void 
	join(File directory) throws IOException
	{
		// Record start time.
		long startTime = System.currentTimeMillis();
		
		//final String prefix  = new File(filepath).getName();
		final String userDir = System.getProperty("user.dir");
				
		System.out.println("Pattern                    " + this.getPattern());
		System.out.println("Regular Pattern            " + this.regularPattern.pattern());
		System.out.println("File directory to be found " + directory.getCanonicalPath());
		System.out.println("Delete segment upon join   " + this.deleteSegmentUponJoin);				
		
		// Const refernce;
		final FileJoiner ownReference = this;
		
		// A filename filter so that it filter all files without
		// filename starting with prefix value.
		class PatternFilenameFilter implements FilenameFilter{
			public boolean accept(File dir
								 ,String name){
				if (name != null){
					System.out.println(name);
					Matcher m = Pattern.compile(
							ownReference.getRegularPattern()).matcher(name);
					return m.find(); 
				}
				return false;
			}
		}		
		
		class SegmentNoComparator implements Comparator{
			public int compare(Object o1, Object o2){
				if (o1 instanceof File && o2 instanceof File){
					File[] files = {(File)o1, (File)o2};					
					int[] ext = new int[2];
					for (int i = 0 ; i < 2; i ++){						
						ext[i] = ownReference.getSegmentNo(files[i].getName());
						System.out.println("Segment Number:" + ext[i]);
					}
					if 		(ext[0] >  ext[1])			return 1;
					else if (ext[0] == ext[1])			return 0;
					else if (ext[0] <  ext[1])			return -1;
				}
				return -1;
			}	           			
		}
		
		// List all the files meeting the specified criteria.
		// For example, if there are 3 files segment after splitting
		// and four files in the lookup directory shown below,
		// test.gif.1
		// test.gif.2
		// test.gif.3
		// abc.gif.4
		// The prefix filename filter filter out "abc.gif.4" because
		// it is not started with "test.gif".
		File[] files 	  = directory.listFiles(new PatternFilenameFilter());
		
		// Sort the array using the extension comparator.
		// For example, if there are 10 files segment after splitting
		// shown below
		// test.gif.1
		// test.gif.10
		// test.gif.2		
		// You need to sort them by the extension number in order
		// to combine it property.				
		long segmentCount = files.length;			
		Arrays.sort(files, new SegmentNoComparator());
		
		//System.out.println("Num of Segment Found " + segmentCount);
									
		// Open the stream for the combined file.
		String outputFilePath = PathHelper.getCanonicalPath(userDir,
				this.processingOutputPath);
		File outputFile 	  = new File(outputFilePath);
		FileOutputStream fos  = new FileOutputStream(outputFile);
		
		//System.out.println("Output Files Path " + outputFile);		
		
		for (int i = 0; i < segmentCount; i++){			
			System.out.println("File " + i + " " + files[i].getName());
			// Open segment file.
			FileInputStream fis = new FileInputStream(files[i]);
			// Forward the content from buffer to combined file. 
			NIOHandler.pipe(fis, fos);
			fis.close();
		}				
		fos.close();
		fos = null;		
		// Open the actual output files.
		String actualOutputPath = PathHelper.getCanonicalPath(userDir,
				this.outputPath);
		System.out.println("Ori    Path " + outputFile.getName());
		System.out.println("Actual Path " + actualOutputPath);				
		
		if (!outputFile.renameTo(new File(actualOutputPath))){
			throw new IOException("Can not rename the joined file" +
								   outputFile.getName());	
		}				
		// Delete all segment files.
		if (this.deleteSegmentUponJoin){
			//System.out.println("Deleting segment files");			
			for (int i = 0; i < segmentCount; i++)
				files[i].delete();
		}		
		// Record end time.
		long endTime = System.currentTimeMillis();		
		System.out.println("The joining processing takes " + (((double)(endTime-startTime))/1000) + "(s).");
	}				
}
