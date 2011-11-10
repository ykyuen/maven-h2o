/**
 * 
 */
package hk.hku.cecid.piazza.commons.io;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;
import junit.framework.TestCase;
import hk.hku.cecid.piazza.commons.test.utils.FixtureStore;
/**
 * @author Patrick Yip
 *
 */
public class ArchiverTarTest extends TestCase {
	
	private static ClassLoader FIXTURE_LOADER = FixtureStore.createFixtureLoader(false, ArchiverTarTest.class);
	
	@Before
	public void setUp() throws Exception{
		System.out.println("---------------- " + this.getName() + " Start ----------------");		
	}
	
	@After
	public void tearDown() throws Exception{
		System.out.println("---------------- " + this.getName() + " Finished ----------------");
	}
	
	@Test
	public void testGuessCompressedSize() throws Exception{
		compareGuessActual();
	}
	
	@Test
	public void testGuessCompressedSizeLongFilename() throws Exception{
		compareGuessActual();
	}
	
	@Test
	public void testGuessCompressedSizeHasDir() throws Exception{
		compareGuessActual();
	}
	
	@Test
	public void testGuessCompressedSizeFileMoreThen10K() throws Exception{
		compareGuessActual();
	}
	
	private void compareGuessActual() throws Exception{
		File packedFile = null;
		try{
			ArchiverTar tar = new ArchiverTar();
			File baseFile = new File(FIXTURE_LOADER.getResource(getName()).getFile(), "files");		
			long guessedSize = tar.guessCompressedSize(baseFile);
			File destBase = new File(FIXTURE_LOADER.getResource(getName()).getFile(), "packed");
			packedFile = new File(destBase, "packed.tar");
			tar.compress(baseFile, packedFile, false);
			
			Assert.assertEquals("Packed size is " + Long.toString(packedFile.length()) + " but guessed size is " + Long.toString(guessedSize), packedFile.length(), guessedSize);
		}catch(Exception e){
			//re-throw
			throw e;
		}finally{
			packedFile.delete();
		}
	}
}
