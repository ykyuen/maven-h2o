/**
 * Contains the SPA plugin hook point for the piazza corvus
 * and the constant table.
 */
package hk.hku.cecid.edi.sfrm.spa;

import java.util.HashMap;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;

import hk.hku.cecid.edi.sfrm.handler.SFRMMessageHandler;
import hk.hku.cecid.edi.sfrm.handler.SFRMMessageSegmentHandler;
import hk.hku.cecid.edi.sfrm.handler.SFRMPartnershipHandler;
import hk.hku.cecid.edi.sfrm.handler.IncomingMessageHandler;
import hk.hku.cecid.edi.sfrm.com.PayloadsRepository;

import hk.hku.cecid.piazza.commons.Sys;
import hk.hku.cecid.piazza.commons.module.ModuleGroup;
import hk.hku.cecid.piazza.commons.module.ModuleException;
import hk.hku.cecid.piazza.commons.module.SystemModule;
import hk.hku.cecid.piazza.commons.spa.Plugin;
import hk.hku.cecid.piazza.commons.spa.PluginException;
import hk.hku.cecid.piazza.commons.spa.PluginHandler;
import hk.hku.cecid.piazza.commons.os.OSSelector;
import hk.hku.cecid.piazza.commons.os.OSManager;
import hk.hku.cecid.piazza.commons.security.KeyStoreManager;

/**
 * The SFRM main processor for all modules and component.<br><br>
 * 
 * Creation Date: 27/9/2006
 * 
 * @author Twinsen Tsang
 * @version 1.0.4
 * @since	1.0.0
 */
public class SFRMProcessor implements PluginHandler {

	/**
	 * The system module.
	 */
	public  static SystemModule core;
	
	/**
	 * The whole module group.
	 */
	private static ModuleGroup moduleGroup;

	/**
	 * The key store manager for SFRM.
	 */
	private static KeyStoreManager ksm;
	
	/**
	 * The os manager for SFRM.
	 */
	private static OSManager osm;
	
	/**
	 * The DAO message handler for SFRM.
	 */
	private static SFRMMessageHandler msgHandle;
	
	/**
	 * The DAO partnership handler for SFRM.
	 */
	private static SFRMPartnershipHandler ptshipHandle;
	
	/**
	 * The DAO segment handler for SFRM
	 */
	private static SFRMMessageSegmentHandler segHandle;
	
	/**
	 * The outgoing respository.
	 */
	private static PayloadsRepository outRes;
	
	/**
	 * The packaged respositoy.
	 */
	private static PayloadsRepository packRes;
	
	/**
	 * The incoming respository.
	 */
	private static PayloadsRepository incRes;
	
	/**
	 * The internal guard lock for each message. 
	 */
	private static transient HashMap guardLock = new HashMap(); 
			
	/**
	 * [Overriden] method.
	 * 
	 * @param plugin
	 * 
	 * @see hk.hku.cecid.piazza.commons.spa.PluginHandler#processActivation(Plugin)
	 */
	public void 
	processActivation(Plugin plugin) throws PluginException 
	{
		String mgDescriptor = plugin.getParameters().getProperty("module-group-descriptor");
		moduleGroup 		= new ModuleGroup(mgDescriptor, plugin.getClassLoader());
		Sys.getModuleGroup().addChild(moduleGroup);

		core = moduleGroup.getSystemModule();
		
		// Initialize pointer referecnce.
		SFRMProcessor.initModuleGroupRef();
		moduleGroup.startActiveModules();	

		if (core == null) {
			throw new PluginException("Ebms core system module not found");
		}
	}

	/**
 	 * [Overriden] method.
	 * 
	 * @return the Lft module group.
	 */
	public static ModuleGroup getModuleGroup() {
		if (moduleGroup == null) {
			throw new RuntimeException("Lft module group not initialized");
		} else {
			return moduleGroup;
		}
	}
	
	/**
	 * Set the module group. This SHOULD be only used for standalone program.
	 * 
	 * @param group	
	 * 			The module group to set. 
	 */
	private static void setModuleGroup(ModuleGroup group){
		moduleGroup = group;
	}
	
	/**
	 * Initialize all component pointer reference in the module 
	 * group.
	 */
	private static void 
	initModuleGroupRef()
	{
		// Initialize Key store manager [Security Layer]
		ksm = (KeyStoreManager) core.getComponent("keystore-manager");
		if (ksm == null)
			throw new ModuleException("SFRM key store manager does not found");		

		// Initialize OS manager. [OS Layer]
		OSSelector oss = (OSSelector) core.getComponent("os-selector");
		if (oss == null)
			throw new ModuleException("SFRM os selector does not found");
		osm = oss.getInstance();

		// Initialize DAO-Message Handler [DAO-Wrapper Layer]
		msgHandle = (SFRMMessageHandler) core.getComponent("message-handler");
		if (msgHandle == null)
			throw new ModuleException("SFRM Message handler does not found.");
		
		ptshipHandle = (SFRMPartnershipHandler) core.getComponent("partnership-handler");
		if (ptshipHandle == null)
			throw new ModuleException("SFRM Partnership handler does not found.");
		
		segHandle = (SFRMMessageSegmentHandler) core.getComponent("message-segment-handler");
		if (segHandle == null)
			throw new ModuleException("SFRM Segment handler does not found.");
	
		// Initialize Payload Repository
		outRes  = (PayloadsRepository) core.getComponent("outgoing-payload-repository");
		packRes = (PayloadsRepository) core.getComponent("outgoing-packaged-payload-repository");
		incRes	= (PayloadsRepository) core.getComponent("incoming-payload-repository");
		
		if (outRes == null || packRes == null || incRes == null){
			throw new ModuleException("SFRM Payload Repository does not found.");
		}
	}
	
	/**
	 * Overriden method.
	 * 
	 * @see hk.hku.cecid.piazza.commons.spa.PluginHandler#processDeactivation(Plugin)
	 */
	public void 
	processDeactivation(Plugin plugin) throws PluginException 
	{
		moduleGroup.stopActiveModules();
	}
	
	/**
	 * Initialize the procesor for the console application.
	 * 
	 * @throws PluginException
	 */
	public static void 
	processConsoleActivation() throws PluginException 
	{
		new SFRMProcessor();
		SFRMProcessor.core = Sys.getModuleGroup().getSystemModule();
		SFRMProcessor.setModuleGroup(Sys.getModuleGroup());			
		SFRMProcessor.initModuleGroupRef();
		SFRMProcessor.getModuleGroup().startActiveModules();						
	}
	
	/** 
	 * @return the security key store manager.
	 */
	public static KeyStoreManager 
	getKeyStoreManager() 
	{
		return SFRMProcessor.ksm;
	}
	
	/**
	 * @return the os manager.
	 */
	public static OSManager 
	getOSManager()
	{
		return SFRMProcessor.osm;
	}
	
	/**
	 * @return the database message handler.
	 */
	public static SFRMMessageHandler 
	getMessageHandler()
	{
		return SFRMProcessor.msgHandle;
	}		
	
	/** 
	 * @return the database partnership handler.
	 */
	public static SFRMPartnershipHandler 
	getPartnershipHandler()
	{
		return SFRMProcessor.ptshipHandle;
	}	
	
	/**
	 * @return the database message segment handler.
	 */
	public static SFRMMessageSegmentHandler 
	getMessageSegmentHandler()
	{
		return SFRMProcessor.segHandle;
	}
	
	/**
	 * @return the incoming message handler.
	 */
	public static IncomingMessageHandler 
	getIncomingMessageHandler()
	{
		// Storing as a pointer reference is faster, but overhead of just one method 
		// invocation is insigificant. And also hotspot will inline this method
		// after certain time.
		return IncomingMessageHandler.getInstance();
	}
		
	/** 
	 * @return the outgoing payloads repository.
	 */
	public static PayloadsRepository 
	getOutgoingPayloadRepository()
	{
		return SFRMProcessor.outRes;
	}
	
	/** 
	 * @return the packaged payloads repository.
	 */
	public static PayloadsRepository 
	getPackagedPayloadRepository()
	{	
		return SFRMProcessor.packRes;
	}
	
	/** 
	 * @return the incoming payloads repository.
	 */
	public static PayloadsRepository 
	getIncomingPayloadRepository()
	{
		return SFRMProcessor.incRes;
	}
	
	/**
	 * [@SYNCRHONIZED] Create a Global lock for a particular key.<br/><br/>
	 * 
	 * @param key
	 */
	public static synchronized Object createLock(String key){
		Object obj = new Object();
		guardLock.put(key, obj);
		return obj;
	}

	/**
	 * [@SYNCRHONIZED] Get a global lock for a particular key.  
	 */
	public static synchronized Object getLock(String key){
		return guardLock.get(key);
	}
	
	/**
	 * [@SYNCRHONIZED] Remove a global lock for a particular key.
	 * 
	 * @param key
	 */
	public static synchronized void	removeLock(String key){
		guardLock.remove(key);
	}
}
