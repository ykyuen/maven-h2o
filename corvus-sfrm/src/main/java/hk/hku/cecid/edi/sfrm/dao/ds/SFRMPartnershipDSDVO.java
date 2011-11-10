/**
 * Provides implementation class for the database access object 
 * (DAO and DVO) for the database layer. 
 */
package hk.hku.cecid.edi.sfrm.dao.ds;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.sql.Timestamp;
import java.lang.ref.SoftReference;

import hk.hku.cecid.edi.sfrm.spa.SFRMProperties;
import hk.hku.cecid.edi.sfrm.spa.SFRMProcessor;

import hk.hku.cecid.edi.sfrm.dao.SFRMPartnershipDVO;

import hk.hku.cecid.piazza.commons.io.IOHandler;
import hk.hku.cecid.piazza.commons.dao.ds.DataSourceDVO;


/**
 * The <code>SFRMPartnershipDSDVO</code> is a data value object representing
 * a tabular row in the <em>sfrm_partnership</em> in the persistence layer.<br/><br/>
 * 
 * Creation Date: 27/9/2006<br/>
 * 
 * It possesses caching automatically for most frequently fields shown below:
 * <ol>
 * 	<li>partnership Id</li>
 * 	<li>partnership endpoint</li>
 * 	<li>maximum retry allowed</li>
 * 	<li>retry interval</li>
 * 	<li>X509 verfication / encryption cerfiticates</li>
 * </ol><br/> 
 * 
 * So developers do not need to worry the issue of thread contention and 
 * can freely call the <em>get</em> and <em>set</em> with no performance impact.<br/>  
 * 
 * Version 1.0.1 - 
 * <ol>
 * 	<li>Added cache for hot access field, it requires extra <em>17</em> bytes + 1 soft reference 
 *      per <code>SFRMPartnershipDSDVO</code> object.</li>
 * 
 * 
 * @author Twinsen Tsang
 * @version 1.0.1
 * @since	1.0.0
 */
public class SFRMPartnershipDSDVO extends DataSourceDVO implements
		SFRMPartnershipDVO {		
	/**
	 * Backward Compatible serial version UID.
	 */
	private static final long serialVersionUID = 4453567496887231495L;
	
	/**
	 * The cached partnership Id; [4B]
	 */
	private String partnershipId;
	
	/**
	 * The cached partnership endpoint; [4B]
	 */
	private String partnerEndpoint;
	
	/**
	 * The cached reference verification or encryption certificates. [Unknown]
	 */
	private X509Certificate X509cert;  
	
	/**
	 * The flag indicating whether the certificates is cached or not. [1B]   
	 */
	private boolean X509CacheFlag;
	
	/**
	 * The cached retry max. [4B]
	 */
	private int retryMax = Integer.MIN_VALUE;
	
	/**
	 * The cached retry interval. [4B]
	 */
	private int retryInterval = Integer.MIN_VALUE;
	
	/** 
	 * Constructor.
	 */
	public SFRMPartnershipDSDVO(){}
	
	/**
	 * [@GET, THREAD-SAFETY] Get the partnership sequence no from this partnership DVO.
	 */
	public int getPartnershipSeq(){
		return super.getInt("partnershipSeq");
	}

	/**
	 * [@SET, THREAD-SAFETY] Set the partnership sequence
	 */
	public void setPartnershipSeq(int partnershipSeq){
		super.setInt("partnershipSeq", partnershipSeq);
	}
		
	/**
	 * [@GET, THREAD-SAFETY, CACHABLE]<br/><br/>
	 * 
	 * Get the partnership from this partnership DVO.
	 */
	public String getPartnershipId(){
		// TODO: Make it become thread-safety
		// Multiple access requires super.getString twice or more in order to cache it 
		// into variable.
		if (this.partnershipId == null){
			String partnershipId = super.getString("partnershipId");
			this.partnershipId = partnershipId; 
		}
		return this.partnershipId;
	}

	/**
	 * [@SET, THREAD-SAFETY] Set the new partnership id to this partnership DVO.
	 * 
	 * @param partnershipId The new partnership Id.
	 */
	public void setPartnershipId(String partnershipId){		
		super.setString("partnershipId", partnershipId);
		// cache value.
		this.partnershipId = partnershipId;
	}

	/**
	 * [@GET, THREAD-SAFETY] Get the description of the partnership DVO.
	 */
	public String getDescription(){
		return super.getString("description");
	}

	/**
	 * [@SET, THREAD-SAFETY] Set the new description to this partnership DVO. 
	 * 
	 * @param description The new description.
	 */
	public void setDescription(String description){
		super.setString("description", description);
	}

	/**
	 * [@GET, THREAD-SAFETY] Get the sending endpoint of the partnership.
	 * 
	 * The endpoint in the database stores only the 
	 * address of receiver. For example, like
	 * <strong>http://127.0.0.1:8080/</strong> or
	 * <strong>http://sfrm.partnership.com:8080/</strong><br><br>
	 * 
	 * But the endpoint returned here will concat a designated
	 * conext path = "corvus/httpd/sfrm/inbound".
	 */
	public String getPartnerEndpoint() {
		String endPoint =  super.getString("partnerEndpoint");
		if (this.partnerEndpoint == null && endPoint != null){
			if (!endPoint.endsWith("/"))
				endPoint += "/";		
			endPoint += CONTEXT_PATH;
			this.partnerEndpoint = endPoint;
		}		
		return this.partnerEndpoint;
	}
	
	/**
	 * Get the sending endpoint of the partnership without appended the context path
	 */
	public String getOrgPartnerEndpoint(){
		return super.getString("partnerEndpoint");
	}

	/**
	 * [@GET, THREAD-SAFETY] Set the partnership endpoint of the partnership DVO.
	 * 
	 * @param endpoint The new partnership endpoint.
	 */
	public void setPartnerEndPoint(String endpoint) {
		super.setString("partnerEndpoint", endpoint);
		this.partnerEndpoint = endpoint;
	}
	
	/**
	 * [@GET, THREAD-SAFETY] Get the partnership endpoint of this partnership DVO.
	 */
	public String getPartnerCertFingerprint(){
		return super.getString("partnerCertFingerprint");
	}
	
	/**
	 * [@SET, THREAD-SAFETY] Set the partnership endpoint of this partnership DVO.
	 */
	public void setPartnerCertFingerprint(String partnerCertFingerprint){
		super.setString("partnerCertFingerprint", partnerCertFingerprint);
	}	

	/**
	 * [@GET, THREAD-SAFETY] whether the partnership requires SSL hostname verified.
	 */
	public boolean isHostnameVerified() {
		return super.getBoolean("isHostnameVerified");
	}

	/**
	 * [@SET, THREAD-SAFETY] set whether the partnership requires SSL hostname verified.   
	 */
	public void setIsHostnameVerified(boolean isHostnameVerified) {
		super.setBoolean("isHostnameVerified", isHostnameVerified);
	}

	/**
	 * [@GET, THREAD-SAFETY]
	 * 
	 * @return whether the partnership requires signing for outbound message.
	 */
	public boolean isOutboundSignRequested() {
		return super.getBoolean("isOutboundSignRequested");
	}

	/**
	 * [@SET, THREAD-SAFETY] set whether the partnership requires signing for outbound message.
	 * 
	 * @param isOutboundSignRequested true if requires signing, vice versa.
	 */
	public void setIsOutboundSignRequested(boolean isOutboundSignRequested) {
		super.setBoolean("isOutboundSignRequested", isOutboundSignRequested);
	}

	public boolean isOutboundEncryptRequested() {
		return super.getBoolean("isOutboundEncryptRequested");
	}

	public void setIsOutboundEncryptRequested(boolean isOutboundEncryptRequested) {
		super.setBoolean("isOutboundEncryptRequested", isOutboundEncryptRequested);
	}

	public boolean isInboundSignEnforced() {
		return super.getBoolean("isInboundSignEnforced");
	}

	public void setIsInboundSignEnforced(boolean isInboundSignEnforced) {
		super.setBoolean("isInboundSignEnforced", isInboundSignEnforced);
	}

	public boolean isInboundEncryptEnforced() {
		return super.getBoolean("isInboundEncryptEnforced");
	}

	public void setIsInboundEncryptEnforced(boolean isInboundEncryptEnforced) {
		super.setBoolean("isInboundEncryptEnforced", isInboundEncryptEnforced);
	}

	public String getSignAlgorithm() {
		return super.getString("signAlgorithm");
	}

	public void setSignAlgorithm(String signAlgorithm) {
		super.setString("signAlgorithm", signAlgorithm);
	}

	/**
	 * [@GET, NON-THREAD-SAFETY, CACHABLE] Get the verification X509 certificates.
	 */
	public X509Certificate getVerifyX509Certificate(){
		return getX509Certificate(this.getPartnerCertFingerprint());
	}

	public String getEncryptAlgorithm() {
		return super.getString("encryptAlgorithm");
	}

	public void setEncryptAlgorithm(String encryptAlgorithm) {
		super.setString("encryptAlgorithm", encryptAlgorithm);
	}
	
	/**
	 * [@GET, NON-THREAD-SAFETY, CACHABLE] Get the encryption X509 certificates.
	 */
	public X509Certificate getEncryptX509Certificate(){		
		return getX509Certificate(this.getPartnerCertFingerprint());	
	}
	
	/**
	 * @deprecated
	 */
//	public int getCompressionLevel(){
//		return super.getInt("compressionLevel");
//	}
	
	/**
	 * @deprecated
	 */
//	public void setCompressionLevel(int compressionLevel){
//		super.setInt("compressionLevel", compressionLevel);
//	}
	
	/**
	 * [@GET, THREAD-SAFETY] Get the maximum retry allowed for this partnership DVO.  
	 */
	public int getRetryMax() {
		if (this.retryMax == Integer.MIN_VALUE){
			int ret = this.getInt("retryMax");
			this.retryMax = ret;
		}
		return this.retryMax;
	}

	/**
	 * [@SET, THREAD-SAFETY] Set the maximum retry allowed for this partnership DVO.
	 */
	public void setRetryMax(int retryMax) {		
		this.setInt("retryMax", retryMax);
		this.retryMax = retryMax;
	}

	/**
	 * [@GET, NON-THREAD-SAFETY] Get the retry interval of this partnership DVO.
	 */
	public int getRetryInterval() {
		if (this.retryInterval == Integer.MIN_VALUE){
			int ret = this.getInt("retryInterval");
			this.retryInterval = ret;
		}
		return this.retryInterval;
	}

	/**
	 * [@SET, THREAD-SAFETY] Set the retry interval of this partnership DVO.
	 */
	public void setRetryInterval(int retryInterval) {
		super.setInt("retryInterval", retryInterval);
		this.retryInterval = retryInterval;
	}

	public boolean isDisabled() {
		return super.getBoolean("isDisabled");
	}

	public void setIsDisabled(boolean isDisabled) {
		super.setBoolean("isDisabled", isDisabled);
	}

	/**
	 * [@GET, THREAD-SAFETY] 
	 * 
	 * @param get the creation timestamp of this partnership record.
	 */
	public Timestamp getCreationTimestamp() {
		return (Timestamp)super.get("createdTimestamp");
	}

	/**
	 * [@SET, THREAD-SAFETY] Set the creation timestamp.
	 * 
	 * @param creationTimestamp the new value of the creation time stamp for this partnership DVO.
	 */
	public void setCreationTimestamp(Timestamp creationTimestamp) {
		super.put("createdTimestamp", creationTimestamp);
	}

	/**
	 * [@GET, THREAD-SAFETY] 
	 * 
	 * @return Get the last modified timestamp
	 */
	public Timestamp getModifiedTimestamp() {
		return (Timestamp)this.get("modifiedTimestamp");
	}

	/**
	 * [@GET, THREAD-SAFETY] Set the last modified timestamp
	 * 
	 * @param modifiedTimestamp the last modified timestamp.
	 */
	public void setModifiedTimestamp(Timestamp modifiedTimestamp) {
		this.put("modifiedTimestamp", modifiedTimestamp);
	}

	/**
	 * [@GET, NON-THREAD-SAFETY, CACHABLE] Get the X509 Verification / Encryption
	 * certificates.
	 * 
	 * @param certFingerprint The fingerprint of the certificates. 			
	 * @return the X509 certificates with <code>certFingerprint</code>
	 */
	private X509Certificate getX509Certificate(String certFingerprint){
		try{
			X509Certificate cert;
			// TODO: Multiple thread can see this value is null / stale data.
			if (X509CacheFlag == false){
				File certFile = new File(SFRMProperties.getTrustedCertStore()
										,certFingerprint);
				if (!certFile.exists())
					throw new Exception("Missing certs with finger print:" + 
										certFingerprint);
				BufferedInputStream bis = new BufferedInputStream(
					new FileInputStream(certFile));			
				byte[] bs = IOHandler.readBytes(bis);							 			
				InputStream certStream = new ByteArrayInputStream(bs);
				cert = (X509Certificate) CertificateFactory
					.getInstance("X.509").generateCertificate(certStream);
				bis.close();
				certStream.close();
				this.X509cert 		= cert;
				this.X509CacheFlag	= true;	// Atomic on write machine instruction
				return cert;
			}
			return this.X509cert;
		}catch(Exception e){
			SFRMProcessor.core.log.error(
				"Unable to load the certificates with fingerprint: "
			   + certFingerprint);
			return null;
		}
	}
}
