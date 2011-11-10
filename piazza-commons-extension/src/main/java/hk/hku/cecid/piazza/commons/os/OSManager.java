/* 
 * Copyright(c) 2005 Center for E-Commerce Infrastructure Development, The
 * University of Hong Kong (HKU). All Rights Reserved.
 *
 * This software is licensed under the GNU GENERAL PUBLIC LICENSE Version 2.0 [1]
 * 
 * [1] http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

package hk.hku.cecid.piazza.commons.os;

import java.util.List;
import java.util.Vector;
import java.io.File;
import java.io.IOException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;
import org.apache.commons.io.FileSystemUtils;

import hk.hku.cecid.piazza.commons.module.Component;
import hk.hku.cecid.piazza.commons.util.StringUtilities;
import hk.hku.cecid.piazza.commons.io.FileSystem;

/**
 * The OS Manager provides interface for executing
 * platform command console so that it can 
 * execute the console command through this interface.
 * <br><br>
 * 
 * Creation Date: 25/10/2006<br><br>
 * 
 * Version 1.0.2 -
 * <ul>
 * 	<li>Added method getName.</li>
 * </ul>
 * 
 * Version 1.0.1 - 
 * <ul>
 * 	<li>Added method getDiskFreespace.</li>
 * </ul>
 * 
 * @author Twinsen Tsang
 * @version 1.0.2
 * @since	1.0.1
 */
public abstract class OSManager extends Component {
	
	protected ProcessBuilder cmdBuilder;
	
	public OSManager(){
		cmdBuilder = new ProcessBuilder();
	}
	
	/**
	 * whether the current OS is support by this manager.
	 * 
	 * @since
	 * 			1.0.1
	 * 
	 * @return true if support.
	 */
	public abstract boolean isValidOS();	
	
	/**
	 * @return 
	 * 			The name of this OS Manager.
	 */
	public abstract String getName();
	
	public abstract String getOSName();
	
	/**
	 * Return the disk free space of this particular path.
	 *
	 * @param fs
	 * 			The filesystem for querying the remaining free space.
	 * @param param 
	 * 			RESERVED
	 * @return
	 * 			The free space in bytes of disk that contains that filesytem.
	 * @since	
	 * 			1.0.2
	 * @throws Exception
	 */
	public long getDiskFreespace(FileSystem fs, Object[] param)
	throws Exception {
		return this.getDiskFreespace(fs.getRoot().getAbsolutePath(), param);
	}
	
	/**
	 * Return the disk free space of this particular path.
	 *
	 * @param path
	 * 			The path for querying the remaining free space.
	 * @param param 
	 * 			RESERVED
	 * @return
	 * 			The free space in bytes of disk that contains the path.
	 * @since	
	 * 			1.0.2
	 * @throws Exception 			
	 */
	public long getDiskFreespace(String path, Object[] param) throws Exception{
		return FileSystemUtils.freeSpaceKb(path) << 10;
	}
	
	
	/**
	 * Execute a platform dependent command.
	 * 
	 * @param command
	 * 			The command string.
	 * @return 	
	 * 			The command process that execute the command parameters. 
	 * @since	
	 * 			1.0.1 
	 * @throws Exception
	 * 			Throws if the execution return non zero value.
	 * @throws IOException
	 * 			Throws if an I/O erros occurs.
	 */
	public Process executeCommand(String command, File workingDir) 
			throws Exception{
		Logger.getLogger(this.getClass()).debug("OSManager#executeCommand: " + command);
		Process process = Runtime.getRuntime().exec(command, null, workingDir);
		process.waitFor();
		// FIXME: why the process return value always return negative value?
		/*if (process.exitValue() != 0)
			throw new Exception("Non zero exit value [" + process.exitValue()
					+ "] for the executed command: " + command);*/
		return process;
	}
	
	/**
	 * Execute a platform dependent command.
	 * 
	 * @param command
	 * 			The command list
	 * @return 	
	 * 			The command process that execute the command parameters. 
	 * @since	
	 * 			1.0.1 
	 * @throws Exception
	 * 			Throws if the execution return non zero value.
	 * @throws IOException
	 * 			Throws if an I/O erros occurs.
	 */
	public Process executeCommand(List<String> commands, File workingDir) 
			throws Exception{
		Logger.getLogger(this.getClass()).debug("OSManager#executeCommand: " + printListCmd(commands));
		Process process = cmdBuilder.directory(workingDir).command(commands).start();
		process.waitFor();
		// FIXME: why the process return value always return negative value?
		/*if (process.exitValue() != 0)
			throw new Exception("Non zero exit value [" + process.exitValue()
					+ "] for the executed command: " + command);*/
		return process;
	}
	
	/**
	 * Execute a platform dependent command which return immediately
	 * (does not wait for the command finish).
	 * 
	 * @param command
	 * 			The command string.
	 * @return 
	 * 			The command process that execute the command parameters.
	 * @since	
	 * 			1.0.1
	 * @throws IOException
	 * 			Throws if an I/O error occurs
	 */
	public Process executeCommandAsync(String command, File workingDir) 
			throws IOException{
		Logger.getLogger(this.getClass()).debug("OSManager#executeCommandAsync: " + command);
		Process process = Runtime.getRuntime().exec(command, null, workingDir);
		return process;
	}
	
	/**
	 * Execute a platform dependent command which return immediately
	 * (does not wait for the command finish).
	 * 
	 * @param command
	 * 			The command list.
	 * @return 
	 * 			The command process that execute the command parameters.
	 * @since	
	 * 			1.0.1
	 * @throws IOException
	 * 			Throws if an I/O error occurs
	 */
	public Process executeCommandAsync(List<String> commands, File workingDir) 
			throws IOException{
		Logger.getLogger(this.getClass()).debug("OSManager#executeCommandAsync: " + printListCmd(commands));
		Process process = cmdBuilder.directory(workingDir).command(commands).start();
//		Process process = Runtime.getRuntime().exec(commands, null, workingDir);
		return process;
	}
	
	/**
	 * Execute a platform dependent command.
	 * 
	 * @param command
	 * 			The command string.
	 * @return 	
	 * 			A buffered input stream for the process. 
	 * @since	
	 * 			1.0.2 
	 * @throws Exception
	 * 			Throws if the execution return non zero value.
	 */
	public InputStream executeCommandAsInStream(String command, File workingDir) 
			throws Exception {
		Process process = null;
		process = this.executeCommandAsync(command, workingDir);

		process.getErrorStream().close();
		process.getOutputStream().close();
		return new BufferedInputStream(process.getInputStream());
	}
	
	/**
	 * Execute a platform dependent command.
	 * 
	 * @param command
	 * 			The command list.
	 * @return 	
	 * 			A buffered input stream for the process. 
	 * @since	
	 * 			1.0.2 
	 * @throws Exception
	 * 			Throws if the execution return non zero value.
	 */
	public InputStream executeCommandAsInStream(List<String> commands, File workingDir) 
			throws Exception {
		Process process = null;
		process = this.executeCommandAsync(commands, workingDir);
		
		process.getErrorStream().close();
		process.getOutputStream().close();
		return new BufferedInputStream(process.getInputStream());
	}
	
	/**
	 * Execute a reader command.
	 * 
	 * @param command
	 * 			The command string.
	 * @return 	
	 * 			A buffered reader for the process. 
	 * @since	
	 * 			1.0.2 
	 * @throws Exception
	 * 			Throws if the execution return non zero value.
	 */
	public BufferedReader executeCommandAsReader(String command, File workingDir)
			throws Exception {
		return new BufferedReader(new InputStreamReader(this
				.executeCommandAsInStream(command, workingDir)));
	}
	
	/**
	 * Execute a reader command.
	 * 
	 * @param command
	 * 			The command list.
	 * @return 	
	 * 			A buffered reader for the process. 
	 * @since	
	 * 			1.0.2 
	 * @throws Exception
	 * 			Throws if the execution return non zero value.
	 */
	public BufferedReader executeCommandAsReader(List<String> commands, File workingDir)
			throws Exception {
		return new BufferedReader(new InputStreamReader(this
				.executeCommandAsInStream(commands, workingDir)));
	}
	
	
	/**
	 * Release the process resource.
	 * 
	 * @param p
	 * 			The process spawn by this OSManager.
	 * @return
	 */
	protected boolean releaseProcess(Process p){
		if (p != null){
			try{
				p.getErrorStream().close();
				p.getInputStream().close();
				p.getOutputStream().close();
				return true;
			}catch(IOException ioe){
				return false;
			}
		}
		return false;
	}
	
	
	protected String escapeCommand(String command, String[] escapeList, String escapeChar){
		String tempCommand = command;
		for(int i=0; escapeList.length > i; i++){
			tempCommand = tempCommand.replace(escapeList[i], escapeChar + escapeList[i]);
		}
		return tempCommand;
	}
	
	protected String pathEscape(String path){
		String tempPath = path;
		tempPath = tempPath.replace(" ", "\u0020");
		return tempPath;
	}
	
	protected String printListCmd(List<String> commands){
		String cmdStr = "";
		for(int i=0 ; commands.size() > i ; i++){
			cmdStr += commands.get(i) + " ";
		}
		return cmdStr.trim();
	}
	
	/**
	 * Create a dummy file with the specified path and size.
	 * 
	 * @param absPath
	 * 			The absolute path of the dummy files.
	 * @param size
	 * 			The size of dummy files.
	 * @param param 
	 * 			RESERVED.
	 * @return true if the operation run successfully.
	 * @since	
	 * 			1.0.1
	 * @throws Exception
	 */
	public synchronized boolean 
	createDummyFile(
			String 	absPath, 
			long 	size, 
			final 	Object[] param)	throws 
			Exception 
	{
		if (new File(absPath).exists())
			return false;
		FileInputStream fis  = new FileInputStream("/dev/zero");
		FileOutputStream fos = new FileOutputStream(absPath, true);
		
		long bytesWritten = 0;
		byte[] bytes	  = new byte[8192];
		while((bytesWritten += fis.read(bytes)) != -1){
			//System.out.println(bytesWritten);			
			fos.write(bytes);
			if (bytesWritten >= size)
				break;
		}
		// Trim out any unwanted bytes.
		FileChannel foc = fos.getChannel();
		foc.truncate(size);
		foc.close(); foc = null;		
		fos.close(); fos = null;
		fis.close(); fis = null;
		return true;
	}
}
