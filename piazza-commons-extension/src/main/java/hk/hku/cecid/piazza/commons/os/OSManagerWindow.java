/* 
 * Copyright(c) 2005 Center for E-Commerce Infrastructure Development, The
 * University of Hong Kong (HKU). All Rights Reserved.
 *
 * This software is licensed under the GNU GENERAL PUBLIC LICENSE Version 2.0 [1]
 * 
 * [1] http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

package hk.hku.cecid.piazza.commons.os;

import java.util.Vector;
import java.io.File;
import java.io.BufferedReader;
import java.lang.ArrayIndexOutOfBoundsException;
//import org.apache.log4j.Logger;

import hk.hku.cecid.piazza.commons.io.FileSystem;
import hk.hku.cecid.piazza.commons.util.StringUtilities;

/**
 * 
 * 
 * Creation Date: 25/10/2006
 * 
 * @author Twinsen
 * @version 1.0.1
 * @since	1.0.1
 */
public class OSManagerWindow extends OSManager {
	 	
	/**
	 * whether the current OS is support by this manager.
	 * 
	 * @return true if support.
	 */
	public boolean isValidOS(){
		return System.getProperty("os.name").toUpperCase().indexOf("WINDOW") >= 0;
	}
	
	/**
	 * @return 
	 * 			The name of this OS Manager.
	 */
	public String getName(){
		return "Window Family OS Manager v1.00";
	}
	
	public String getOSName(){
		return "Windows";
	}
	
	/**
	 * Return the disk free space of this particular path.
	 *
	 * @param path
	 * 			The abs path for querying the remaining free space.
	 * @param param
	 * 			RESERVED
	 * @return
	 * 			The free space in bytes of disk that contains the path.
	 * @since	
	 * 			1.0.2
	 * @throws ArrayIndexOutOfBoundsException 			
	 * 			If the output format of the command is invalid.
	 */
	public long getDiskFreespace(String path, Object[] param) throws Exception{
		//Use the plain command text approach
		Vector<String> command = new Vector<String>();
		command.add("fsutil");
		command.add("volume");
		command.add("diskfree");
		command.add(path);
		
		BufferedReader br = this.executeCommandAsReader(command, null);
		String line  = br.readLine();
		br.close();
		String [] token = StringUtilities.tokenize(line, ":");			
		if (token.length < 2){
			throw new ArrayIndexOutOfBoundsException(
					"Missing required token with input " + line);
		} 			
		// Trim the token (remove any space) and parse it to long.
		return StringUtilities.parseLong(
					StringUtilities.trim(token[1]), 0);			
	}		
	
	/**
	 * Create a dummy file with the specified path and size.
	 * 
	 * @param path
	 * 			The path for the dummy files.
	 * @param size
	 * 			The size of dummy files.
	 * @param param 
	 * 			RESERVED.
	 * @return
	 * @throws Exception
	 */
	public boolean createDummyFile(String path, long size, Object[] param) throws Exception {
		File f = new File(path);
		if (f.exists()) 	
			return false;
		//Use the plain command text approach
		Vector<String> command = new Vector<String>();
		command.add("fsutil");
		command.add("file");
		command.add("createnew");
		command.add(path);
		command.add(Long.toString(size));
		
		Process p = this.executeCommand(command, null);
		this.releaseProcess(p);
		return true;
	}
}
