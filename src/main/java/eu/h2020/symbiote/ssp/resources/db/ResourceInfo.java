/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.resources.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Document(collection="resources")
public class ResourceInfo {

	@Id
	@JsonProperty("sspIdResource") // of Resource
	private String id;
	
	@JsonProperty("symIdResource") //of Resource
	private String symIdResource;
	
	@JsonProperty("internalIdResource") // of Resource
	private String internalIdResource;

	@JsonProperty("symIdParent") //of SDEV/Plat
	private String symIdParent;
	@JsonProperty("sspIdParent") // of SDEV/Plat
	private String sspIdParent;


	@JsonProperty("type")
	private String type;
	@JsonProperty("observedProperties")
	private List<String> observedProperties;
	@JsonIgnore
	private List<String> sessionIdList;
	@JsonIgnore
	private String pluginUrl;

	@Field
	@Indexed(name="session_expiration", expireAfterSeconds=DbConstants.EXPIRATION_TIME)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Date session_expiration;
	@JsonProperty("policy") //of SDEV/Plat
	private IAccessPolicy policy;
	@JsonProperty("policySpecifier") //of SDEV/Plat
	private IAccessPolicySpecifier policySpecifier;


	public ResourceInfo() {
		this.id = "";
		this.symIdResource = "";
		this.internalIdResource="";
		this.symIdParent ="";
		this.sspIdParent ="";
		this.pluginUrl = null;
		this.observedProperties = null;
		this.sessionIdList = null;       
		this.type = null;
	}

	public ResourceInfo(
			@JsonProperty("sspIdResource")  String sspIdResource,
			@JsonProperty("symIdResource")  String symIdResource,
			@JsonProperty("internalIdResource")  String internalIdResource,
			@JsonProperty("symIdParent") String symIdParent, // of SDEV/Plat
			@JsonProperty("sspIdParent") String sspIdParent
			) {
		this.id = sspIdResource;
		this.symIdResource = symIdResource;
		this.internalIdResource=internalIdResource;
		this.symIdParent =symIdParent;
		this.sspIdParent = sspIdParent;
		this.pluginUrl = null;
		this.observedProperties = null;
		this.sessionIdList = null;       
		this.type = null;
	}

	@JsonCreator
	public ResourceInfo(
			@JsonProperty("sspIdResource")  String sspIdResource,
			@JsonProperty("symIdResource")  String symIdResource,
			@JsonProperty("internalIdResource")  String internalIdResource,
			@JsonProperty("symIdParent") String symIdParent, // of SDEV/Plat
			@JsonProperty("sspIdParent") String sspIdParent,
			@JsonProperty("sessionExpiration") Date session_expiration
			) {
		this.id = sspIdResource;
		this.symIdResource = symIdResource;
		this.internalIdResource=internalIdResource;
		this.symIdParent =symIdParent;
		this.sspIdParent = sspIdParent;
		this.pluginUrl = null;
		this.observedProperties = null;
		this.sessionIdList = null;       
		this.type = null;
		this.session_expiration=session_expiration;
	}

	@JsonCreator
	public ResourceInfo(
			@JsonProperty("sspIdResource")  String sspIdResource,
			@JsonProperty("symIdResource")  String symIdResource,
			@JsonProperty("internalIdResource")  String internalIdResource,
			@JsonProperty("symIdParent") String symIdParent, // of SDEV/Plat
			@JsonProperty("sspIdParent") String sspIdParent,
			@JsonProperty("sessionExpiration") Date session_expiration,
			@JsonProperty("policy") IAccessPolicy policy,
			@JsonProperty("policySpecifier") IAccessPolicySpecifier policySpecifier
			) {
		this.id = sspIdResource;
		this.symIdResource = symIdResource;
		this.internalIdResource=internalIdResource;
		this.symIdParent =symIdParent;
		this.sspIdParent = sspIdParent;
		this.pluginUrl = null;
		this.observedProperties = null;
		this.sessionIdList = null;       
		this.type = null;
		this.session_expiration=session_expiration;
		this.policy=policy;
		this.policySpecifier=policySpecifier;
	}

	
	@JsonProperty("sspIdResource")
	public String getSspIdResource() {
		return id;
	}

	@JsonProperty("symIdParent")
	public String getSymIdParent() {
		return symIdParent;
	}

	@JsonProperty("symIdParent")
	public void setSymIdParent(String symIdParent) {
		this.symIdParent = symIdParent;
	}

	@JsonProperty("sspIdParent")
	public String getSspIdParent() { return sspIdParent; }

	@JsonProperty("sspIdParent")
	public void setSspIdParent(String sspIdParent) {
		this.sspIdParent = sspIdParent;
	}

	@JsonProperty("symIdResource")
	public String getSymIdResource() {
		return symIdResource;
	}

	@JsonProperty("symIdResource")
	public void setSymIdResource(String symIdResource) {
		this.symIdResource = symIdResource;
	}

	@JsonProperty("internalIdResource")
	public String getInternalIdResource() {
		return internalIdResource;
	}
	
	@JsonProperty("internalIdResource")
	public void setInternalIdResource(String internalIdResource) {
		this.internalIdResource = internalIdResource;
	}

	@JsonProperty("observedProperties")
	public List<String> getObservedProperties() {
		return observedProperties;
	}    

	@JsonProperty("observedProperties")
	public void setObservedProperties(List<String> observedProperties) {
		this.observedProperties = observedProperties;
	}

	@JsonProperty("type")
	public String getType() {
		return type;
	}

	@JsonProperty("type")
	public void setType(String type) {
		this.type = type;
	}

	@JsonIgnore
	public List<String> getSessionId() {
		return sessionIdList;
	}

	@JsonIgnore
	public void setSessionId(List<String> sessionIdList) {
		this.sessionIdList = sessionIdList;
	}

	@JsonIgnore
	public void addToSessionList(String sessionId) {
		if(this.sessionIdList == null)
			this.sessionIdList = new ArrayList();
		this.sessionIdList.add(sessionId);
	}

	@JsonIgnore
	public String getPluginUrl() {
		return pluginUrl;
	}

	@JsonIgnore
	public void setPluginUrl(String pluginUrl) {
		this.pluginUrl = pluginUrl;
	}

	@JsonProperty("session_expiration")
	public Date getSessionExpiration() {
		return this.session_expiration;
	}

	@JsonProperty("session_expiration")
	public void setSessionExpiration(Date session_expiration) {
		this.session_expiration = session_expiration;
	}
	
	@JsonProperty("policy")
	public IAccessPolicy getAccessPolicy() { return this.policy; }

	@JsonProperty("policy")
	public void setAccessPolicy(IAccessPolicy policy) {	this.policy = policy; }

	@JsonProperty("policySpecifier")
	public IAccessPolicySpecifier getAccessPolicySpecifier() { return this.policySpecifier; }

	@JsonProperty("policySpecifier")
	public void setAccessPolicySpecifier(IAccessPolicySpecifier policySpecifier) {	this.policySpecifier = policySpecifier; }
}
