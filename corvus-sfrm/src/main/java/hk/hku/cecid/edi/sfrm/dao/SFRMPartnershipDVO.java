/**
 * Provides inferace for the database access object (DAO and DVO) 
 * for the database layer. 
 */
package hk.hku.cecid.edi.sfrm.dao;

import java.sql.Timestamp;

import java.security.cert.X509Certificate;

import hk.hku.cecid.piazza.commons.dao.DVO;

/**
 * 
 * 
 * Creation Date: 27/9/2006
 * 
 * @author Twinsen
 * @version 1.0.0 
 */
public interface SFRMPartnershipDVO extends DVO {

	public static final String PARTNERSHIPID_REGEXP = "[\\w@_\\+-]+";
	/**
	 * The context path for sfrm inbound.
	 */
	public static final String CONTEXT_PATH		= "corvus/httpd/sfrm/inbound";

	public int getPartnershipSeq();

	public void setPartnershipSeq(int partnershipSeq);

	public String getDescription();

	public void setDescription(String description);

	public String getPartnershipId();

	public void setPartnershipId(String partnershipId);

	public boolean isHostnameVerified();

	public void setIsHostnameVerified(boolean isHostnameVerified);
	
	public String getPartnerEndpoint();
	
	public String getOrgPartnerEndpoint();
	
	public void setPartnerEndPoint(String endpoint);
	
	public String getPartnerCertFingerprint();
	
	public void setPartnerCertFingerprint(String partnerCertFingerprint);

	public boolean isOutboundSignRequested();

	public void setIsOutboundSignRequested(boolean isOutboundSignRequested);

	public boolean isOutboundEncryptRequested();

	public void setIsOutboundEncryptRequested(boolean isOutboundEncryptRequested);

	public boolean isInboundSignEnforced();

	public void setIsInboundSignEnforced(boolean isInboundSignEnforced);

	public boolean isInboundEncryptEnforced();

	public void setIsInboundEncryptEnforced(boolean isInboundEncryptEnforced);

	public String getSignAlgorithm();

	public void setSignAlgorithm(String signAlgorithm);

	public X509Certificate getVerifyX509Certificate();

	public String getEncryptAlgorithm();

	public void setEncryptAlgorithm(String encryptAlgorithm);

	public X509Certificate getEncryptX509Certificate();
	
	public int getRetryMax();

	public void setRetryMax(int retryMax);

	public int getRetryInterval();

	public void setRetryInterval(int retryInterval);

	public boolean isDisabled();

	public void setIsDisabled(boolean isDisabled);

	public Timestamp getCreationTimestamp();

	public void setCreationTimestamp(Timestamp creationTimestamp);

	public Timestamp getModifiedTimestamp();

	public void setModifiedTimestamp(Timestamp modifiedTimestamp);	
}
