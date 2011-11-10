/* 
 * Copyright(c) 2005 Center for E-Commerce Infrastructure Development, The
 * University of Hong Kong (HKU). All Rights Reserved.
 *
 * This software is licensed under the GNU GENERAL PUBLIC LICENSE Version 2.0 [1]
 * 
 * [1] http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

package hk.hku.cecid.piazza.commons.net;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.util.Enumeration;

import javax.mail.Header;
import javax.mail.internet.InternetHeaders;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;

/** 
 * Creation Date: 10/10/2006
 * Modified Date: 17/06/2009
 * 
 * @author Twinsen, Philip
 * @version 1.1.0
 * @since	1.0.0
 */
public class FastHttpConnector extends HttpConnector {
	
	private int responseCode;

	/**
     * Creates a new instance of HttpConnector.
     * 
     * @param destUrl the destination URL, either in String or URL format.
     * @throws MalformedURLException if the URL is malformed.
     */
	public FastHttpConnector(Object destUrl) throws MalformedURLException {
		super(destUrl);
	}
	
	 /**
     * Sends an HTTP/S request using the given HTTP connection.
     * 
     * @param request the HTTP request content or null for a simple get request.
     * @param internet headers
     * 
     * @return an input stream for reading the reply from the host. 
     * @throws ConnectionException if failed in sending the HTTP request.
     */
	 // TODO: the new method using Apache HTTPComponent to replace 
	 // the old handling of InputStream with IOHandler.pipe()
	 // it still needs more study to fine-tune the parameters and change to use NIO if necessary
	 public HttpResponse send(InputStream is, InternetHeaders headers) throws ConnectionException {
		 HttpURLConnection connection = this.createConnection();
		 HttpParams params = new BasicHttpParams();
         HttpProtocolParams.setUseExpectContinue(params, true);
         
         BasicHttpProcessor httpproc = new BasicHttpProcessor();
         // Required protocol interceptors
         httpproc.addInterceptor(new RequestContent());
         httpproc.addInterceptor(new RequestTargetHost());
         // Recommended protocol interceptors
         httpproc.addInterceptor(new RequestConnControl());
         httpproc.addInterceptor(new RequestExpectContinue());
         
         HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
         HttpContext context = new BasicHttpContext(null);

         //TODO: The URL.getPort() method will return -1, when the port number is not set, set the port number to 80 if the returned value is -1
         int port = connection.getURL().getPort();
         if (port == -1){
        	 port = 80;
         }
         
         HttpHost host = new HttpHost(connection.getURL().getHost(), port);
         DefaultHttpClientConnection conn = new DefaultHttpClientConnection();

         ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();
         context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
         context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);

         HttpEntity entity = null;
         if (is != null)
        	 entity = new InputStreamEntity(is, -1);
     
         try {
             if (!conn.isOpen()) {
                 Socket socket = new Socket(host.getHostName(), host.getPort());
                 conn.bind(socket, params);
             }

             BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST", 
                             connection.getURL().getPath());

             Enumeration e = headers.getAllHeaders();
             while (e.hasMoreElements()) {
	             Header header = (Header)e.nextElement();
	             request.addHeader(header.getName(), header.getValue());
             }

             if (is != null)
            	 request.setEntity(entity);  
             
             context.setAttribute(ExecutionContext.HTTP_REQUEST, request);
             request.setParams(params);

             httpexecutor.preProcess(request, httpproc, context);
             HttpResponse response = httpexecutor.execute(request, conn, context);
             response.setParams(params);
             httpexecutor.postProcess(response, httpproc, context);
             
             responseCode = response.getStatusLine().getStatusCode();

             if (!connStrategy.keepAlive(response, context)) 
                 conn.close();

             return response;
         } catch (HttpException e) {
        	 throw new ConnectionException("Unable to send HTTP request", e);
         } catch (IOException e) {
        	 throw new ConnectionException("Unable to send HTTP request", e);
	     } finally {
	         if (conn != null) 
	             try {
	            	 conn.close();
				 } catch (IOException e) {
					 throw new ConnectionException("Unable to close HTTP connection", e);
				 }
	     }        
	 } 
	 
	 public int getResponseCode() {
		 return responseCode;
	 }

}
