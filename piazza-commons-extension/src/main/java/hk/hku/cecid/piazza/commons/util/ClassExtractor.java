/* 
 * Copyright(c) 2005 Center for E-Commerce Infrastructure Development, The
 * University of Hong Kong (HKU). All Rights Reserved.
 *
 * This software is licensed under the GNU GENERAL PUBLIC LICENSE Version 2.0 [1]
 * 
 * [1] http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

package hk.hku.cecid.piazza.commons.util;

import hk.hku.cecid.piazza.commons.module.Component;
import hk.hku.cecid.piazza.commons.dao.DVO;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDVO;

/**
 * Creation Date: 29/9/2006
 * 
 * @author Twinsen
 * @version 1.0.0
 *  
 * @deprecated
 */
public class ClassExtractor extends Component {

	/**
	 * 
	 * @param obj
	 * @return
	 */
	protected static String extractObjectToString(Object obj){
		if (obj == null)
			return "";
		StringBuffer ret = new StringBuffer();
		ret.append("Class: " + obj.getClass().getName() + StringUtilities.LINE_SEPARATOR);
		return ret.toString();
	}
	
	/**
	 * 
	 * 
	 * @param dvo
	 * @return
	 */
	public static String extractDVOToString(DVO dvo){
		if (!(dvo instanceof DataSourceDVO) || dvo == null)
			return "";
		
		StringBuffer ret = new StringBuffer();    	
		ret.append(extractObjectToString(dvo));
    	java.util.Enumeration keys = ((DataSourceDVO)dvo).getData().keys();
    	while(keys.hasMoreElements()){
    		String key 		= keys.nextElement().toString();
    		String value 	= ((DataSourceDVO)dvo).getData().get(key).toString();
    		StringBuffer buf= new StringBuffer(StringUtilities.repeat(" ", 40));
    		buf.replace(0, key.length(), key);
    		ret.append("Key: " + buf.toString() + 
    				   "Value: " + value + StringUtilities.LINE_SEPARATOR);
    	}
    	return ret.toString();    	
	}	
	
	 /**
     * toLog method.
     * 
     * It convert the string from {@link #toString()} to a string 
     * array using delimiters "\n".
     */
	public static String [] extractDVOToLog(DVO dvo){		
		if (!(dvo instanceof DataSourceDVO) || dvo == null)
			return null;		
		return StringUtilities.toArray(extractDVOToString(dvo) ,"\n"
									  ,((DataSourceDVO)dvo).getData().size()); 
	}	
}
