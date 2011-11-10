/* 
 * Copyright(c) 2005 Center for E-Commerce Infrastructure Development, The
 * University of Hong Kong (HKU). All Rights Reserved.
 *
 * This software is licensed under the GNU GENERAL PUBLIC LICENSE Version 2.0 [1]
 * 
 * [1] http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

package hk.hku.cecid.piazza.commons.os;

import hk.hku.cecid.piazza.commons.io.FileSystem;
import hk.hku.cecid.piazza.commons.util.StringUtilities;
//import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.util.List;
import java.util.Vector;
/**
 * 
 * Creation Date: 
 * 
 * @author Philip, Twinsen Tsang
 * @version 1.0.0
 * @since	1.0.1
 */
public class OSManagerMac extends OSManager {
//	public static final String[] ESCAPE_LIST = {"$", " "};
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
	public boolean isValidOS(){	
		return System.getProperty("os.name").toUpperCase().indexOf("MAC") >= 0;
	}
	
	/**
	 * @return 
	 * 			The name of this OS Manager.
	 */
	public String getName(){
		return "Mac OS Manager v1.00";
	}
	
	public String getOSName(){
		return "Mac";
	}
}
