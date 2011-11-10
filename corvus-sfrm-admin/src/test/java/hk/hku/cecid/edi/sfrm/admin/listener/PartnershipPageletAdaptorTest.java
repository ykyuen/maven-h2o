/* 
 * Copyright(c) 2005 Center for E-Commerce Infrastructure Development, The
 * University of Hong Kong (HKU). All Rights Reserved.
 *
 * This software is licensed under the GNU GENERAL PUBLIC LICENSE Version 2.0 [1]
 * 
 * [1] http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

package hk.hku.cecid.edi.sfrm.admin.listener;

import junit.framework.TestCase;
import java.lang.reflect.Method;
import hk.hku.cecid.edi.sfrm.admin.listener.PartnershipPageletAdaptor;
import hk.hku.cecid.piazza.commons.test.utils.FixtureStore;

import java.util.Hashtable;
import java.io.InputStream;

public class PartnershipPageletAdaptorTest extends TestCase{
	
	private PartnershipPageletAdaptor adaptor = new PartnershipPageletAdaptor();
	private InputStream validCertInStream = null;
	private InputStream invalidCertInStream = null;
	
	public void setUp() throws Exception{
		ClassLoader FIXTURE_LOADER = FixtureStore.createFixtureLoader(false, PartnershipPageletAdaptorTest.class);
		validCertInStream = FIXTURE_LOADER.getResourceAsStream("validCert.crt");
		invalidCertInStream = FIXTURE_LOADER.getResourceAsStream("invalidCert.crt");
		System.out.println("---------" + this.getName() + " Start -------");
	}
	
	public void tearDown() throws Exception{
		System.out.println("---------" + this.getName() + " End   -------");
	}
	
	//Test for validating the partnership with valid data
	public void testValidatePartnershipTest() throws Exception{
		Method m = Class.forName("hk.hku.cecid.edi.sfrm.admin.listener.PartnershipPageletAdaptor").getDeclaredMethod("validatePartnership", new Class[] {Hashtable.class});
		m.setAccessible(true);
		//Build the valid partnership
		Hashtable<String, Object> sample = new Hashtable<String, Object>();
		sample.put("partnership_id", "sample_partnership");
		sample.put("description", "This is the sample partnership");
		sample.put("partner_endpoint", "http://localhost:8080");
		sample.put("is_hostname_verified", "false");
		sample.put("is_outbound_sign_requested", "false");
		sample.put("is_outbound_encrypt_requested", "false");
		sample.put("is_inbound_sign_enforced", "false");
		sample.put("is_inbound_encrypt_enforced", "false");
		sample.put("sign_algorithm", "sha1");
		sample.put("encrypt_algorithm", "3des");
		sample.put("retry_max", "3");
		sample.put("retry_interval", "60000");
		sample.put("is_disabled", "false");
		sample.put("partner_cert", validCertInStream);
		Hashtable errors = (Hashtable) m.invoke(adaptor, new Object[] {sample});
		TestCase.assertEquals("There have an error(s) for the valid partnership", 0, errors.size());
	}
	
	//Test for validating the partnership with invalid partnership ID
	public void testValidatePartnershipNeg1Test() throws Exception{
		Method m = Class.forName("hk.hku.cecid.edi.sfrm.admin.listener.PartnershipPageletAdaptor").getDeclaredMethod("validatePartnership", new Class[] {Hashtable.class});
		m.setAccessible(true);
		//Build the valid partnership
		Hashtable<String, Object> sample = new Hashtable<String, Object>();
		//Invalid partnership ID with empty string
		sample.put("partnership_id", "");
		sample.put("description", "This is the sample partnership");
		sample.put("partner_endpoint", "http://localhost:8080");
		sample.put("is_hostname_verified", "false");
		sample.put("is_outbound_sign_requested", "false");
		sample.put("is_outbound_encrypt_requested", "false");
		sample.put("is_inbound_sign_enforced", "false");
		sample.put("is_inbound_encrypt_enforced", "false");
		sample.put("sign_algorithm", "sha1");
		sample.put("encrypt_algorithm", "3des");
		sample.put("retry_max", "3");
		sample.put("retry_interval", "60000");
		sample.put("is_disabled", "false");
		sample.put("partner_cert", validCertInStream);
		Hashtable errors = (Hashtable) m.invoke(adaptor, new Object[] {sample});
		TestCase.assertEquals("There didn't have error(s) for the invalid partnership" , 1, errors.size());
		TestCase.assertNotNull("Partnership ID is invalid, but it didn't detected", errors.get("partnership_id"));
		TestCase.assertEquals("Wrong partnership error message", "Partnership ID cannot be empty", (String)errors.get("partnership_id"));
	}
	
	//Test for validating the partnership with the invalid partnership ID
	public void testValidatePartnershipNeg2Test() throws Exception{
		Method m = Class.forName("hk.hku.cecid.edi.sfrm.admin.listener.PartnershipPageletAdaptor").getDeclaredMethod("validatePartnership", new Class[] {Hashtable.class});
		m.setAccessible(true);
		//Build the valid partnership
		Hashtable<String, Object> sample = new Hashtable<String, Object>();
		//Invalid partnership ID filled with space
		sample.put("partnership_id", "      ");
		sample.put("description", "This is the sample partnership");
		sample.put("partner_endpoint", "http://localhost:8080");
		sample.put("is_hostname_verified", "false");
		sample.put("is_outbound_sign_requested", "false");
		sample.put("is_outbound_encrypt_requested", "false");
		sample.put("is_inbound_sign_enforced", "false");
		sample.put("is_inbound_encrypt_enforced", "false");
		sample.put("sign_algorithm", "sha1");
		sample.put("encrypt_algorithm", "3des");
		sample.put("retry_max", "3");
		sample.put("retry_interval", "60000");
		sample.put("is_disabled", "false");
		sample.put("partner_cert", validCertInStream);
		
		Hashtable errors = (Hashtable) m.invoke(adaptor, new Object[] {sample});
		TestCase.assertEquals("There didn't have error(s) for the invalid partnership" , 1, errors.size());
		TestCase.assertNotNull("Partnership ID is invalid, but it didn't detected", errors.get("partnership_id"));
		TestCase.assertEquals("Wrong partnership error message", "Partnership ID cannot be empty", (String)errors.get("partnership_id"));
	}
	
	//Test for validating the partnership endpoint with invalid endpoint
	public void testValidatePartnershipNeg3Test() throws Exception{
		Method m = Class.forName("hk.hku.cecid.edi.sfrm.admin.listener.PartnershipPageletAdaptor").getDeclaredMethod("validatePartnership", new Class[] {Hashtable.class});
		m.setAccessible(true);
		//Build the valid partnership
		Hashtable<String, Object> sample = new Hashtable<String, Object>();
		sample.put("partnership_id", "dummy");
		sample.put("description", "This is the sample partnership");
		//Invalid endpoint with empty string
		sample.put("partner_endpoint", "");
		sample.put("is_hostname_verified", "false");
		sample.put("is_outbound_sign_requested", "false");
		sample.put("is_outbound_encrypt_requested", "false");
		sample.put("is_inbound_sign_enforced", "false");
		sample.put("is_inbound_encrypt_enforced", "false");
		sample.put("sign_algorithm", "sha1");
		sample.put("encrypt_algorithm", "3des");
		sample.put("retry_max", "3");
		sample.put("retry_interval", "60000");
		sample.put("is_disabled", "false");
		sample.put("partner_cert", validCertInStream);
		
		Hashtable errors = (Hashtable) m.invoke(adaptor, new Object[] {sample});
		TestCase.assertEquals("There didn't have error(s) for the invalid partnership" , 1, errors.size());
		TestCase.assertNotNull("Partner Endpoint is invalid, but it didn't detected", errors.get("partner_endpoint"));
		TestCase.assertEquals("Wrong partnership error message", "Transport Endpoint cannot be empty", (String)errors.get("partner_endpoint"));
	}
	
	//Test for validating the partnership endpoint with invalid endpoint
	public void testValidatePartnershipNeg4Test() throws Exception{
		Method m = Class.forName("hk.hku.cecid.edi.sfrm.admin.listener.PartnershipPageletAdaptor").getDeclaredMethod("validatePartnership", new Class[] {Hashtable.class});
		m.setAccessible(true);
		//Build the valid partnership
		Hashtable<String, Object> sample = new Hashtable<String, Object>();
		sample.put("partnership_id", "dummy");
		sample.put("description", "This is the sample partnership");
		//Invalid endpoint filled with space
		sample.put("partner_endpoint", "           ");
		sample.put("is_hostname_verified", "false");
		sample.put("is_outbound_sign_requested", "false");
		sample.put("is_outbound_encrypt_requested", "false");
		sample.put("is_inbound_sign_enforced", "false");
		sample.put("is_inbound_encrypt_enforced", "false");
		sample.put("sign_algorithm", "sha1");
		sample.put("encrypt_algorithm", "3des");
		sample.put("retry_max", "3");
		sample.put("retry_interval", "60000");
		sample.put("is_disabled", "false");
		sample.put("partner_cert", validCertInStream);
		
		Hashtable errors = (Hashtable) m.invoke(adaptor, new Object[] {sample});
		TestCase.assertEquals("There didn't have error(s) for the invalid partnership" , 1, errors.size());
		TestCase.assertNotNull("Partner Endpoint is invalid, but it didn't detected", errors.get("partner_endpoint"));
		TestCase.assertEquals("Wrong partnership error message", "Transport Endpoint cannot be empty", (String)errors.get("partner_endpoint"));
	}
	
	//Test for validating the partnership endpoint with the invalid endpoint format
	public void testValidatePartnershipNeg5Test() throws Exception{
		Method m = Class.forName("hk.hku.cecid.edi.sfrm.admin.listener.PartnershipPageletAdaptor").getDeclaredMethod("validatePartnership", new Class[] {Hashtable.class});
		m.setAccessible(true);
		//Build the valid partnership
		Hashtable<String, Object> sample = new Hashtable<String, Object>();
		sample.put("partnership_id", "dummy");
		sample.put("description", "This is the sample partnership");
		//Invalid endpoint with using ftp as protocol
		sample.put("partner_endpoint", "ftp://localhost:8080");
		sample.put("is_hostname_verified", "false");
		sample.put("is_outbound_sign_requested", "false");
		sample.put("is_outbound_encrypt_requested", "false");
		sample.put("is_inbound_sign_enforced", "false");
		sample.put("is_inbound_encrypt_enforced", "false");
		sample.put("sign_algorithm", "sha1");
		sample.put("encrypt_algorithm", "3des");
		sample.put("retry_max", "3");
		sample.put("retry_interval", "60000");
		sample.put("is_disabled", "false");
		sample.put("partner_cert", validCertInStream);
		
		Hashtable errors = (Hashtable) m.invoke(adaptor, new Object[] {sample});
		TestCase.assertEquals("There didn't have error(s) for the invalid partnership" , 1, errors.size());
		TestCase.assertNotNull("Partner Endpoint is invalid, but it didn't detected", errors.get("partner_endpoint"));
		TestCase.assertEquals("Wrong partnership error message", "Transport Endpoint protocol is invalid", (String)errors.get("partner_endpoint"));
	}
	
	//Test for validating the invalid retry max with format of not a number
	public void testValidatePartnershipNeg6Test() throws Exception{
		Method m = Class.forName("hk.hku.cecid.edi.sfrm.admin.listener.PartnershipPageletAdaptor").getDeclaredMethod("validatePartnership", new Class[] {Hashtable.class});
		m.setAccessible(true);
		//Build the valid partnership
		Hashtable<String, Object> sample = new Hashtable<String, Object>();
		sample.put("partnership_id", "dummy");
		sample.put("description", "This is the sample partnership");
		sample.put("partner_endpoint", "http://localhost:8080");
		sample.put("is_hostname_verified", "false");
		sample.put("is_outbound_sign_requested", "false");
		sample.put("is_outbound_encrypt_requested", "false");
		sample.put("is_inbound_sign_enforced", "false");
		sample.put("is_inbound_encrypt_enforced", "false");
		sample.put("sign_algorithm", "sha1");
		sample.put("encrypt_algorithm", "3des");
		//Invalid retry max with using the format of not a number
		sample.put("retry_max", "hello");
		sample.put("retry_interval", "60000");
		sample.put("is_disabled", "false");
		sample.put("partner_cert", validCertInStream);
		
		Hashtable errors = (Hashtable) m.invoke(adaptor, new Object[] {sample});
		TestCase.assertEquals("There didn't have error(s) for the invalid partnership" , 1, errors.size());
		TestCase.assertNotNull("Retry Maximum is invalid, but it didn't detected", errors.get("retry_max"));
		TestCase.assertEquals("Wrong partnership error message", "Maximum Retries must be an integer", (String)errors.get("retry_max"));
	}
	
	//Test for validating the invalid retry interval with format of not a number
	public void testValidatePartnershipNeg7Test() throws Exception{
		Method m = Class.forName("hk.hku.cecid.edi.sfrm.admin.listener.PartnershipPageletAdaptor").getDeclaredMethod("validatePartnership", new Class[] {Hashtable.class});
		m.setAccessible(true);
		//Build the valid partnership
		Hashtable<String, Object> sample = new Hashtable<String, Object>();
		sample.put("partnership_id", "dummy");
		sample.put("description", "This is the sample partnership");
		sample.put("partner_endpoint", "http://localhost:8080");
		sample.put("is_hostname_verified", "false");
		sample.put("is_outbound_sign_requested", "false");
		sample.put("is_outbound_encrypt_requested", "false");
		sample.put("is_inbound_sign_enforced", "false");
		sample.put("is_inbound_encrypt_enforced", "false");
		sample.put("sign_algorithm", "sha1");
		sample.put("encrypt_algorithm", "3des");
		sample.put("retry_max", "3");
		//Invalid retry interval with using the format of not a number
		sample.put("retry_interval", "good");
		sample.put("is_disabled", "false");
		sample.put("partner_cert", validCertInStream);
		
		Hashtable errors = (Hashtable) m.invoke(adaptor, new Object[] {sample});
		TestCase.assertEquals("There didn't have error(s) for the invalid partnership" , 1, errors.size());
		TestCase.assertNotNull("Retry Interval is invalid, but it didn't detected", errors.get("retry_interval"));
		TestCase.assertEquals("Wrong partnership error message", "Retry Interval must be an integer", (String)errors.get("retry_interval"));
	}
	
	//Test for validating the invalid certificate format
	public void testValidatePartnershipNeg8Test() throws Exception{
		Method m = Class.forName("hk.hku.cecid.edi.sfrm.admin.listener.PartnershipPageletAdaptor").getDeclaredMethod("validatePartnership", new Class[] {Hashtable.class});
		m.setAccessible(true);
		//Build the valid partnership
		Hashtable<String, Object> sample = new Hashtable<String, Object>();
		sample.put("partnership_id", "dummy");
		sample.put("description", "This is the sample partnership");
		sample.put("partner_endpoint", "http://localhost:8080");
		sample.put("is_hostname_verified", "false");
		sample.put("is_outbound_sign_requested", "false");
		sample.put("is_outbound_encrypt_requested", "false");
		sample.put("is_inbound_sign_enforced", "false");
		sample.put("is_inbound_encrypt_enforced", "false");
		sample.put("sign_algorithm", "sha1");
		sample.put("encrypt_algorithm", "3des");
		sample.put("retry_max", "3");
		sample.put("retry_interval", "60000");
		sample.put("is_disabled", "false");
		//Invalid partnership certificate
		sample.put("partner_cert", invalidCertInStream);
		Hashtable errors = (Hashtable) m.invoke(adaptor, new Object[] {sample});
		TestCase.assertEquals("There didn't have error(s) for the invalid partnership" , 1, errors.size());
		TestCase.assertNotNull("Retry Interval is invalid, but it didn't detected", errors.get("partner_cert"));
		TestCase.assertEquals("Wrong partnership error message", "Uploaded cert is not an X.509 cert", (String)errors.get("partner_cert"));
	}
	
	//Test for validating the invalid partnership, with 2 field is invalid
	public void testValidatePartnershipNeg9Test() throws Exception{
		Method m = Class.forName("hk.hku.cecid.edi.sfrm.admin.listener.PartnershipPageletAdaptor").getDeclaredMethod("validatePartnership", new Class[] {Hashtable.class});
		m.setAccessible(true);
		//Build the valid partnership
		Hashtable<String, Object> sample = new Hashtable<String, Object>();
		sample.put("partnership_id", "");
		sample.put("description", "This is the sample partnership");
		sample.put("partner_endpoint", "http://localhost:8080");
		sample.put("is_hostname_verified", "false");
		sample.put("is_outbound_sign_requested", "false");
		sample.put("is_outbound_encrypt_requested", "false");
		sample.put("is_inbound_sign_enforced", "false");
		sample.put("is_inbound_encrypt_enforced", "false");
		sample.put("sign_algorithm", "sha1");
		sample.put("encrypt_algorithm", "3des");
		sample.put("retry_max", "3");
		sample.put("retry_interval", "60000");
		sample.put("is_disabled", "false");
		//Invalid partnership certificate
		sample.put("partner_cert", invalidCertInStream);
		Hashtable errors = (Hashtable) m.invoke(adaptor, new Object[] {sample});
		TestCase.assertEquals("There didn't have error(s) for the invalid partnership" , 2, errors.size());
		TestCase.assertNotNull("Retry Interval is invalid, but it didn't detected", errors.get("partner_cert"));
		TestCase.assertEquals("Wrong partnership error message", "Uploaded cert is not an X.509 cert", (String)errors.get("partner_cert"));
		TestCase.assertNotNull("Partnership ID is invalid, but it didn't detected", errors.get("partnership_id"));
		TestCase.assertEquals("Wrong partnership error message", "Partnership ID cannot be empty", (String)errors.get("partnership_id"));
	}
	
	//Test for validating the partnership ID which id containing the invalid character
	public void testValidatePartnershipNeg10Test() throws Exception{
		Method m = Class.forName("hk.hku.cecid.edi.sfrm.admin.listener.PartnershipPageletAdaptor").getDeclaredMethod("validatePartnership", new Class[] {Hashtable.class});
		m.setAccessible(true);
		//Build the valid partnership
		Hashtable sample = new Hashtable();
		//Invalid partnership_id which containing space
		sample.put("partnership_id", "abc def");
		sample.put("description", "This is the sample partnership");
		sample.put("partner_endpoint", "http://localhost:8080");
		sample.put("is_hostname_verified", "false");
		sample.put("is_outbound_sign_requested", "false");
		sample.put("is_outbound_encrypt_requested", "false");
		sample.put("is_inbound_sign_enforced", "false");
		sample.put("is_inbound_encrypt_enforced", "false");
		sample.put("sign_algorithm", "sha1");
		sample.put("encrypt_algorithm", "3des");
		sample.put("retry_max", "3");
		sample.put("retry_interval", "60000");
		sample.put("is_disabled", "false");
		sample.put("partner_cert", validCertInStream);
		Hashtable errors = (Hashtable) m.invoke(adaptor, new Object[] {sample});
		TestCase.assertEquals("There didn't have error(s) for the invalid partnership" , 1, errors.size());
		TestCase.assertNotNull("Partnership ID is invalid, but it didn't detected", errors.get("partnership_id"));
		TestCase.assertEquals("Wrong partnership error message", "Partnership ID should contains the alphanumeric characters and @ _ + - only", (String)errors.get("partnership_id"));
	}
	
}

