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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * 
 * Creation Date: 13/11/2006
 * 
 * @author Twinsen Tsang
 * @version 1.0.0
 * @since	1.0.2
 */
public class ArchiverZIP extends ArchiverNULL {

	/**
	 * Check whether the <code>archive</code> 
	 * is supported by this type of archiver.<br><br>
	 *  
	 * ArchiverZIP open the archive and test 
	 * the zip header is correct or not.
	 * 
	 * @param archive
	 * 			The archive to be tested.
	 * @return 
	 * 			true if the archiver support this <code>archive</code>.
	 */
	public boolean isSupportArchive(File archive){
		try{
			return (new ZipFile(archive) != null);
		}catch(Exception e){
			return false;
		}		
	}
	
	/**
	 * Compress the <code>src</code> to <code>dest</code> 
	 * in the archive form.<br><br>
	 *   
	 * If the <code>src</code> is a file, then the resulting
	 * archive contains only that file.<br><br> 
	 * 
	 * If the <code>src</code> is a directory, then the resulting
	 * archive contains all files (recursively) in the <code>
	 * src</code>. If the flag <code>includeItself</code> is true
	 * , then the <code>src</code> will also include in the archive 
	 * as the root.  
	 * 
	 * @param src
	 * 			The source of the file(s) to be archive. 
	 * @param dest 			
	 * 			The destination of the arhived file.
	 * @param includeItself
	 * 			the source directory includes in the archive if it is
	 * 			true, vice versa.   
	 * @throws IOException
	 * 			if any kind of I/O Erros
	 * @return true if the operations run successfully.
	 * @see	hk.hku.cecid.piazza.commons.io.Archiver#compress(File, File, boolean) 
	 */
	public boolean compress(File src, File dest, boolean includeItself)
			throws IOException{
		super.compress(src, dest, includeItself);		
		FileOutputStream fos  = new FileOutputStream(dest);
		ZipOutputStream outs  = new ZipOutputStream(fos);
		WritableByteChannel zipChannel = Channels.newChannel(outs); 
		
		Iterator allFiles = this.listFilesToArchive(src);					
		String dirpath = this.getBaseArchivingDirectory(src, includeItself); 			
		
		// Temporary variable inside the loop.
		ZipEntry zipEntry;
		String filepath;
		String entryName;	
		File srcFile;		// the file object to tar.
		long size;			// the size of the files.
		long tSize;			// the transfer size for transferTo calls.
		long aSize;			// the actual transfer size for transferTo calls.		
		long sPos;			// the start position for transferTo calls.
		
		while (allFiles.hasNext()) {
			srcFile 	= (File) allFiles.next();
			filepath 	= srcFile.getAbsolutePath();		
			entryName 	= filepath.substring(dirpath.length() + 1)
					.replace('\\', '/');
			// Create zip entry
			zipEntry = new ZipEntry(entryName);
			zipEntry.setTime(srcFile.lastModified());
			// I/O Piping
			outs.putNextEntry(zipEntry);
			// Here we don't use NIOHandler because we don't  
			// want to create a zip channel for each pipe.
			FileChannel fc = new FileInputStream(srcFile).getChannel();
			// NIO Bugs 
			// transferTo can only transfer up to Integer.MAX_VALUE.
			size  = fc.size();
			tSize = size;
			sPos  = 0;
			do{					
				tSize = aSize = (size - sPos); 
				if (tSize > Integer.MAX_VALUE)
					aSize = Integer.MAX_VALUE - 1;				
				fc.transferTo(sPos, aSize, zipChannel);			
				sPos += aSize;					
			}
			while(tSize > Integer.MAX_VALUE);		
			fc.close();
			fc = null; // For gc
			outs.closeEntry();						
		}	
		zipChannel.close();
		zipChannel = null; // For gc
		outs.close();
		outs = null; // For gc	
		fos.close();
		fos = null;
		return true;		
	}
	
	/**
	 * Guess how big is the compressed file without
	 * compressing actually.
	 * 
	 * @param src
	 * 			The source of the file(s) to be archive. 
	 */
	public long guessCompressedSize(File src)
			throws IOException{
		throw new IOException("Unsupported Operations.");
	}
	
	/**
	 * Guess how big is the compressed file without
	 * compressing actually.
	 * 
	 * @param src
	 * 			The source of the file(s) to be archive. 
	 */
	public long guessCompressedSize(FileSystem src)
			throws IOException{
		throw new IOException("Unsupported Operations.");
	}
	
	/**
	 * Compress the <code>src</code> to <code>dest</code> in the archive
	 * form.<br>
	 * <br>
	 * 
	 * @param src
	 *            The source of the file(s) to be archive.
	 * @param dest
	 *            The destination of the arhived file.
	 * @param includeItself
	 *            the source directory includes in the archive if it is true,
	 *            vice versa.
	 * @throws IOException
	 *             if any kind of I/O Errors.
	 * @return true if the operations run successfully.
	 */
	public boolean compress(FileSystem src, File dest)
			throws IOException{		
		return this.compress(src.getRoot(), dest, true);		
	}
	
	/**
	 * List the files inside the <code>archive</code>. 
	 * 
	 * @param archive
	 * 			The archive to be listed.
	 * @return
	 * 			A list of java.io.File object that represents
	 * 			each entry in the archive. 
	 */
	public List listAsFile(File archive) throws IOException{
		if (archive == null)
			throw new NullPointerException("Archive file is empty.");
		ZipFile zipFile = new ZipFile(archive);
		Enumeration zipEntries = zipFile.entries();
		ArrayList fileEntries = new ArrayList();
		while(zipEntries.hasMoreElements())
			fileEntries.add(new File(((ZipEntry) zipEntries.nextElement())
					.getName()));
		zipFile.close();
		zipFile = null;
		return fileEntries;
	}
	
	/**
	 * List the files inside the <code>archive</code>. 
	 * 
	 * @param archive
	 * 			The archive to be listed.
	 * @return
	 * 			A list of String objects that represents
	 * 			the filename of each entry in the 
	 * 			archive. 
	 */
	public List listAsFilename(File archive) throws IOException{
		if (archive == null)
			throw new NullPointerException("Archive file is empty.");
		ZipFile zipFile = new ZipFile(archive);
		Enumeration zipEntries = zipFile.entries();
		ArrayList fileEntries = new ArrayList();
		while(zipEntries.hasMoreElements())
			fileEntries.add(((ZipEntry) zipEntries.nextElement()).getName());
		zipFile.close();
		zipFile = null;
		return fileEntries;
	}
	
	/**
	 * Extract the <code>archive</code> to the <code>dest</code> 
	 * directory.<br><br>
	 *  
	 * @param archive
	 * 			The archive to be extract.
	 * @param dest
	 * 			The destination directory extract to. 
	 * @throws IOException
	 * 			Any kind of I/O Errors.
	 * @throws IllegalArgumentException
	 * 			If the <code>dest</code> is not a directory.
	 * @return true if the operations run successfully.	
	 */
	public boolean extract(File archive, File dest) 
			throws IOException{
		 super.extract(archive, dest);         
         ZipFile zipFile = new ZipFile(archive);
         Enumeration zipEntries = zipFile.entries();
         while (zipEntries.hasMoreElements()) {
             ZipEntry zipEntry = (ZipEntry) zipEntries.nextElement();
             if (zipEntry.isDirectory())
                 new File(dest, zipEntry.getName()).mkdirs();
             else {            	 
                 File destFile = new File(dest, zipEntry.getName());
                 destFile.setLastModified(zipEntry.getTime());
                 destFile.getParentFile().mkdirs();
                 FileOutputStream outs = new FileOutputStream(destFile);
                 InputStream ins = zipFile.getInputStream(zipEntry);
                 NIOHandler.pipe(ins, outs);
                 ins.close();
                 outs.close();
                 outs = null;	// For gc
                 ins = null; 	// For gc
             }             
         }
         zipFile.close();
         zipFile = null;		// For gc
         return true;
	}
	
	/**
	 * Extract the <code>archive</code> to the <code>dest</code> 
	 * directory.<br><br>
	 * 
	 * @param archive
	 * 			The archive to be extract. 
	 * @param dest
	 * 			The destination directory extract to.
	 * @throws IOException
	 * 			Any kind of I/O Errors.  
	 * @return true if the operations run successfully.
	 */
	public boolean extract(File archive, FileSystem dest) 
			throws IOException{
		return this.extract(archive, dest.getRoot());
	}
}
