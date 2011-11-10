/* 
 * Copyright(c) 2005 Center for E-Commerce Infrastructure Development, The
 * University of Hong Kong (HKU). All Rights Reserved.
 *
 * This software is licensed under the GNU GENERAL PUBLIC LICENSE Version 2.0 [1]
 * 
 * [1] http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

package hk.hku.cecid.piazza.commons.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.apache.tools.tar.TarBuffer;
import org.apache.tools.tar.TarOutputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

/**
 * @author Patrick Yip
 *
 */
public class ArchiverTar extends ArchiverNULL {
	
	private static final int TAR_CONTENT_SIZE = 10240;
	private static final int TAR_HEADER_SIZE = 512;

	/**
	 * Compress the <code>src</code> to <code>dest</code> 
	 * in the archive form.<br><br>
	 *   
	 * If the <code>src</code> is a file, then the resulting
	 * archive contains only that file.<br><br> 
	 * 
	 * If the <code>src</code> is a directory, then the resulting
	 * archive contains all files (recursively) in the <code>
	 * src</code>.
	 * 
	 * The <code>src</code> file sets will be archived to TAR
	 * format which is comes from Apache Ant Tools Tar.<br><br>
	 * 
	 * For more details, 
	 * read <a href="http://www.jajakarta.org/ant/ant-1.6.1/docs/mix/manual/api/">Apache Ant Tool Tar</a>
	 * 
	 * @param src
	 * 			The source of the file(s) to be archive. 
	 * @param dest 			
	 * 			The destination of the arhived file.
	 * @param includeItself
	 * 			the source directory includes in the archive if it is
	 * 			true, vice versa.
	 * @since	
	 * 			1.0.2
	 * @throws IOException
	 * 			if any kind of I/O Erros
	 * @return true if the operations run successfully.
	 * @see	hk.hku.cecid.piazza.commons.io.Archiver#compress(File, File, boolean) 
	 */
	public boolean compress(File src, File dest, boolean includeItself) throws IOException{
		super.compress(src, dest, includeItself);		
		
		FileOutputStream fos  = new FileOutputStream(dest);
		TarOutputStream	 outs = new TarOutputStream(fos);
		WritableByteChannel tarChannel = Channels.newChannel(outs);	 
		Iterator allFiles = this.listFilesToArchive(src);		
		String dirpath = this.getBaseArchivingDirectory(src, includeItself);
		
		outs.setLongFileMode(TarOutputStream.LONGFILE_GNU);
		// Temporary variable inside the loop.
		TarEntry tarEntry;
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
			tarEntry = new TarEntry(srcFile);
			tarEntry.setModTime(srcFile.lastModified());
			// Tricky fix to make the tar entry back
			// to relative path from the source 
			// directory.
			tarEntry.setName(entryName);
			// I/O Piping
			outs.putNextEntry(tarEntry);		
					
			FileChannel fc = new FileInputStream(srcFile).getChannel();
			// NIO Bugs 
			// transferTo can only transfer up to Integer.MAX_VALUE -1;
			size  = fc.size();
			tSize = size;
			sPos  = 0;
			do{					
				tSize = aSize = (size - sPos); 
				if (tSize > Integer.MAX_VALUE)
					aSize = Integer.MAX_VALUE - 1;				
				fc.transferTo(sPos, aSize, tarChannel);			
				sPos += aSize;					
			}
			while(tSize > Integer.MAX_VALUE);								
			outs.closeEntry();									
			fc.close();
			fc = null; // For gc			
		}				
		outs.close();		
		outs = null;							
		fos.close();
		fos = null;	
		
		return true;
	}
	
	/* (non-Javadoc)
	 * @see hk.hku.cecid.piazza.commons.io.Archiver#compress(hk.hku.cecid.piazza.commons.io.FileSystem, java.io.File)
	 */
	
	public boolean compress(FileSystem src, File dest) throws IOException {
		return compress(src.getRoot(), dest, true);
	}

	/**
	 * Extract the <code>archive</code> to the <code>dest</code> 
	 * directory.<br><br>
	 *  
	 * @param archive
	 * 			The archive to be extract.
	 * @param dest
	 * 			The destination directory extract to.
	 * @since	
	 * 			1.0.2  
	 * @throws IOException
	 * 			Any kind of I/O Errors.
	 * @throws IllegalArgumentException
	 * 			If the <code>dest</code> is not a directory.
	 * @return true if the operations run successfully.	
	 */
	
	public boolean extract(File archive, File dest) throws IOException{
		super.extract(archive, dest);
		BufferedInputStream bis = new BufferedInputStream(
			new FileInputStream(archive));
		TarInputStream tis = new TarInputStream(bis);
		int count = 0;
		for (;; count++) {
			TarEntry entry = tis.getNextEntry();
					
			if (entry == null) {				
				break;
			}			
			
			String name = entry.getName();
			name = name.replace('/', File.separatorChar);
			File destFile = new File(dest, name);
			if (entry.isDirectory()) {
				if (!destFile.exists()) {
					if (!destFile.mkdirs()) {
						throw new IOException(
							"Error making directory path :"
						   + destFile.getPath());
					}
				}
			} else {
				File subDir = new File(destFile.getParent());
				if (!subDir.exists()) {
					if (!subDir.mkdirs()) {
						throw new IOException(
							"Error making directory path :"
						   + subDir.getPath());
					}
				} 
				FileOutputStream out = new FileOutputStream(destFile);
				// FIXME: TUNE PLACE
				byte[] rdbuf = new byte[32 * 1024];
				for (;;){
					int numRead = tis.read(rdbuf);
					if (numRead == -1)
						break;						
					out.write(rdbuf, 0, numRead);
				}
				out.close();
			}
		}
		// For gc
		tis.close(); tis = null;
		bis.close(); bis = null;
		// NO FILE EXTRACTED, throw IOException
		if (count == 0)
			throw new IOException("At least one file should be a TAR.");
			
		return true;
	}
	
	/**
	 * Extract the <code>archive</code> to the <code>dest</code> directory.<br>
	 * <br>
	 * 
	 * @param archive
	 *            The archive to be extract.
	 * @param dest
	 *            The destination directory extract to.
	 * @since 1.0.2
	 * @throws IOException
	 *             Any kind of I/O Errors.
	 * @return true if the operations run successfully.
	 */
	public boolean extract(File archive, FileSystem dest) throws IOException {
		// TODO Auto-generated method stub
		return extract(archive, dest.getRoot());
	}

	/**
	 * Guess how big is the compressed file without
	 * compressing actually. The algorithm of guessing the tar size as follow:<br>
	 * For each of file Each header size is TAR_ENTRY_SIZE bytes, and for the data content block. It use TAR_ENTRY_SIZE
	 * as a block of data. If for last block of data is not TAR_ENTRY_SIZE, then the rest will padding with the empty bytes.
	 * Such that the final guessed size is ceil((file_length/TAR_ENTRY_SIZE)+1)*TAR_ENTRY_SIZE. More details of tar file format can 
	 * found from <a href="http://en.wikipedia.org/wiki/Tarball">this</a>.
	 * 
	 * @param src
	 * 			The source of the file(s) to be archive.
	 * @return guessed file size in byte
	 * @since	
	 * 			1.0.3
	 * @throws NullPointerException
	 * 			if the <code>src</code> is null.
	 * @throws IOException
	 * 			if one of the file in the folders 
	 * 			does not exist in some reason.
	 */
	
	public long guessCompressedSize(File src) throws IOException {
		System.out.println("guessCompressedSize root path:" + src.getAbsolutePath());
		
		int rootIndex = 0;
		
		if (src.getParentFile() != null)
			rootIndex = src.getParentFile().getAbsolutePath().length();
		
		Iterator allFiles = listFilesToArchive(src);
		long size = 0;
		while(allFiles.hasNext()){
			File file = (File)allFiles.next();
			long fileSize = file.length();
			
			System.out.println("guessCompressedSize archive file:" + src.getAbsolutePath());
			System.out.println("guessCompressedSize archive file size:" + fileSize);
			
			if(fileSize % TAR_HEADER_SIZE > 0){
				size += (fileSize/TAR_HEADER_SIZE + 1)*TAR_HEADER_SIZE;
			}else{
				size += fileSize;
			}
			
			// calculate filename header size
			// if filename including folder is longer than 100 byte, 3 headers will be consumed
			// assuming path no larger than 512 byte
			int fileNameByte = file.getAbsolutePath().substring(rootIndex).getBytes().length;
			if (fileNameByte <= 100)
				size += TAR_HEADER_SIZE;
			else 
				size += 3 * TAR_HEADER_SIZE;
		}
		
		// 2 empty blocks at the end
		size += 2 * TAR_HEADER_SIZE;
		
		//Padding the tar file to to make it divided by 10240
		if(size % TAR_CONTENT_SIZE != 0){
			size = (size/TAR_CONTENT_SIZE + 1) * TAR_CONTENT_SIZE;
		}
		System.out.println("guessCompressedSize total size:" + size);
		return size;
	}
	
	
//	private long estimateFileNameSize(String filename){
//		int length = filename.length();
//		if(length <= TarEntry.MAX_NAMELEN)
//			return 0;
//		return (long) Math.ceil((double)(length / TarBuffer.DEFAULT_RCDSIZE)) + TarBuffer.DEFAULT_RCDSIZE;		
//	}
	
	
	/* (non-Javadoc)
	 * @see hk.hku.cecid.piazza.commons.io.Archiver#guessCompressedSize(hk.hku.cecid.piazza.commons.io.FileSystem)
	 */
	
	public long guessCompressedSize(FileSystem src) throws IOException {
		return guessCompressedSize(src.getRoot());
	}

	/* (non-Javadoc)
	 * @see hk.hku.cecid.piazza.commons.io.Archiver#isSupportArchive(java.io.File)
	 */
	
	public boolean isSupportArchive(File archive) {
		return PathHelper.getExtension(archive.getAbsolutePath()).equalsIgnoreCase("TAR"); 
	}
	
	/**
	 * List the files inside the <code>archive</code>.<br>
	 * 
	 * This operation is quite slow and pending to optimize.
	 * 
	 * @param archive
	 * 			The archive to be listed.
	 * @since	
	 * 			1.0.2 
	 * @return
	 * 			A list of java.io.File object that represents
	 * 			each entry in the archive. 
	 */
	public List listAsFile(File archive) throws IOException{
		TarInputStream tarInStream = new TarInputStream(new FileInputStream(archive));
		TarEntry entry = null;
		ArrayList list = new ArrayList();
		while((entry = tarInStream.getNextEntry())!=null){
			list.add(entry.getFile());
		}
		tarInStream.close();
		return list;
	}
	
	/**
	 * List the files inside the <code>archive</code>. 
	 * 
	 * @param archive
	 * 			The archive to be listed.
	 * @since	
	 * 			1.0.2 
	 * @return
	 * 			A list of String objects that represents
	 * 			the filename of each entry in the 
	 * 			archive. 
	 */
	public List listAsFilename(File archive) throws IOException{
		TarInputStream tarInStream = new TarInputStream(new FileInputStream(archive));
		TarEntry entry = null;
		ArrayList list = new ArrayList();
		while((entry = tarInStream.getNextEntry())!=null){
			list.add(entry.getName());
		}
		tarInStream.close();
		return list;
	}
}
