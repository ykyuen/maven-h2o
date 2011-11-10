/* 
 * Copyright(c) 2005 Center for E-Commerce Infrastructure Development, The
 * University of Hong Kong (HKU). All Rights Reserved.
 *
 * This software is licensed under the GNU GENERAL PUBLIC LICENSE Version 2.0 [1]
 * 
 * [1] http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

package hk.hku.cecid.piazza.commons.os;

import java.util.Properties;
import java.util.Enumeration;

import hk.hku.cecid.piazza.commons.module.Component;
import hk.hku.cecid.piazza.commons.util.Instance;

/**
 * An OSSelector initializes the appropriate OSManager
 * by choosing the matched OS from it's XML 
 * parameters set. It is highly flexible to plug a new 
 * OSManager that fit to your platform.<br><br> 
 * 
 * Currently, the build-in support OSManager is 
 * WINDOW family, 'X'inux family and MAC.<br><br>
 * 
 * <strong>SPA Component Guideline:</strong><br>
 * <ol>
 * 	<li>Add a new parameter with the name contains your os name.</li>
 *  <li>The value of newly parameter should be the fully qualified 
 *  	class name (including package name).</li>  
 * </ol>
 * 
 * <strong>Example</strong><br>
 * 
 * <PRE>
 * &lt;component id="os-selector" name="OS Selector"&gt;
 * 	&lt;class&gt;hk.hku.cecid.piazza.commons.os.OSSelector&lt;/class&gt; 
 * 	&lt;parameter name="<em>your os name</em>" value="<em>class name</em>"/&gt;
 * &lt;/component&gt;
 * </PRE>
 * 
 * If the OS is window XP, and the class name is hk.hku.cecid.os.WindowXP then,
 * the component XML should be liked this:
 * <PRE>
 * &lt;component id="os-selector" name="OS Selector"&gt;
 * 	&lt;class&gt;hk.hku.cecid.piazza.commons.os.OSSelector&lt;/class&gt;
 * 	&lt;parameter name="WINDOW" value="hk.hku.cecid.os.WindowXP"/&gt;
 * &lt;/component&gt;
 * </PRE>
 * 
 * Creation Date: 11/9/2006
 * 
 * @author Twinsen Tsang
 * @version 1.0.0
 * @since	1.0.2
 */
public class OSSelector extends Component {
	
	/**
	 * The sigleton OSManager.
	 */
	private static OSManager osInstance;
	
	/** 
	 * @return 
	 * 			A platform-specfic OSManager.
	 * @since	
	 * 			1.0.2 
	 */
	public OSManager getInstance(){
		if (osInstance == null){
			try{
				osInstance = this.initOSManager();
			}catch(Exception e){
				return null;
			}
		}
		return osInstance;
	}

	/**
	 * Component initialization.
	 * 
	 * @throws Exception
	 * 			if fails to initialize the OS Manager.
	 */
	protected void init() throws Exception {
		osInstance = this.initOSManager();		
		if (osInstance == null)
			throw new NullPointerException("OS Manager is empty.");
	}
	
	/**
	 * Initalize the OS Manager.
	 * 
	 * @return 
	 * 			A platform-specfic OSManager. 
	 * @since		
	 * 			1.0.2
	 * @throws Exception
	 * 			if the os class can not be loaded.
	 */
	protected OSManager initOSManager() throws Exception {
		Properties p  = this.getParameters();
		String os 	  = System.getProperty("os.name");
		String osMap, osClass;				
		Enumeration e = p.keys();
		while(e.hasMoreElements()){
			osMap 	= e.nextElement().toString();
			osClass = p.getProperty(osMap);			
			if (os.toUpperCase().indexOf(osMap) >= 0){
				// Create first matched instance.
				Instance ins = new Instance(osClass, this.getClass()
						.getClassLoader());						
				Object osMan = ins.getObject();
				if (osMan instanceof OSManager)
					return (OSManager) osMan;
			}
		}
		return null;
	}
	
	/**
	 * Add an external OS Manager during runtime.<br><br>
	 * 
	 * This method is rarely used in released application,
	 * it is mainly used for development purpose.
	 * 
	 * @param OSFamily
	 * 			The OS Family that the manager belongs to.
	 * @param className
	 * 			The class name of this manager.
	 * @return
	 */
	public void addExternalOSManager(String OSFamily, 
									 String className){
		if (this.getParameters() == null)
			this.setParameters(new Properties());			
		this.getParameters().setProperty(OSFamily, className);
	}		 
}
