/* 
 * Copyright(c) 2005 Center for E-Commerce Infrastructure Development, The
 * University of Hong Kong (HKU). All Rights Reserved.
 *
 * This software is licensed under the GNU GENERAL PUBLIC LICENSE Version 2.0 [1]
 * 
 * [1] http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

package hk.hku.cecid.piazza.commons.os;

import java.io.BufferedReader;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Vector;
import org.apache.log4j.Logger;
//import org.apache.log4j.Logger;

import hk.hku.cecid.piazza.commons.io.FileSystem;
import hk.hku.cecid.piazza.commons.util.StringUtilities;

/**
 * 
 * Creation Date: 25/10/2006
 * 
 * @author Twinsen
 * @version 1.0.0
 * @since 1.0.1
 */
public class OSManagerLinux extends OSManager {
	
//	public static final String[] ESCAPE_LIST = {"$"};
//	public static final String ESCAPE_CHAR = "\\";
	
	/**
	 * The size of 1 gigabytes.
	 */
//	public static final long ONE_GB = 1073741824L;

	/**
	 * whether the current OS is support by this manager.
	 * 
	 * @return true if support.
	 */
	public boolean isValidOS() {
		return System.getProperty("os.name").toUpperCase().indexOf("LINUX") >= 0;
	}

	/**
	 * @return The name of this OS Manager.
	 */
	public String getName() {
		return "Linux OS Manager v1.00";
	}
	
	public String getOSName(){
		return "Linux";
	}
}
