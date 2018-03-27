/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.resources.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
	
	
	
	@JsonProperty("InternalIdResource") // of Resource
	private String internalIdResource;


	@JsonProperty("symId") //of SDEV/Plat
	private String symId;
	@JsonProperty("sspId") // of SDEV/Plat
	private String sspId;


	@JsonProperty("type")
	private String type;
	@JsonProperty("observedProperties")
	private List<String> observedProperties;
	@JsonIgnore
	private List<String> sessionIdList;
	@JsonIgnore
	private String pluginId;

	@Field
	@Indexed(name="session_expiration", expireAfterSeconds=DbConstants.EXPIRATION_TIME)
	private Date session_expiration;


	public ResourceInfo() {
		this.symIdResource = "";
		this.id = "";
		this.internalIdResource="";
		this.symId="";
		this.sspId="";
		this.pluginId = null;
		this.observedProperties = null;
		this.sessionIdList = null;       
		this.type = null;
	}
	@JsonCreator


	public ResourceInfo(
			@JsonProperty("symIdResource")  String symIdResource,
			@JsonProperty("sspIdResource")  String sspIdResource,
			@JsonProperty("internalIdResource")  String internalIdResource,
			@JsonProperty("symId") String symId, // of SDEV/Plat
			@JsonProperty("sspId") String sspId
			) {
		this.symIdResource = symIdResource;
		this.id = sspIdResource;
		this.internalIdResource=internalIdResource;
		this.symId=symId;
		this.sspId=sspId;
		this.pluginId = null;
		this.observedProperties = null;
		this.sessionIdList = null;       
		this.type = null;
	}

	@JsonCreator
	public ResourceInfo(
			@JsonProperty("symIdResource")  String symIdResource,
			@JsonProperty("sspIdResource")  String sspIdResource,
			@JsonProperty("internalIdResource")  String internalIdResource,
			@JsonProperty("symId") String symId, // of SDEV/Plat
			@JsonProperty("sspId") String sspId,
			@JsonProperty("sessionExpiration") Date session_expiration
			) {
		this.symIdResource = symIdResource;
		this.id = sspIdResource;
		this.internalIdResource=internalIdResource;
		this.symId=symId;
		this.sspId=sspId;
		this.pluginId = null;
		this.observedProperties = null;
		this.sessionIdList = null;       
		this.type = null;
		this.session_expiration=session_expiration;
	}

	@JsonProperty("symId")
	public String getSymId() {
		return symId;
	}

	@JsonProperty("symId")
	public void setSymId(String symId) {
		this.symId = symId;
	}

	@JsonProperty("sspId")
	public String getSspId() {
		return sspId;
	}
	
	@JsonProperty("sspIdResource")
	public String getSspIdResource() {
		return id;
	}

	@JsonProperty("sspId")
	public void setSspId(String sspId) {
		this.sspId = sspId;
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
	public String getPluginId() {
		return pluginId;
	}

	@JsonIgnore
	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}

	@JsonProperty("session_expiration")
	public Date getSessionExpiration() {
		return this.session_expiration;
	}

	@JsonProperty("session_expiration")
	public void setSessionExpiration(Date session_expiration) {
		this.session_expiration = session_expiration;
	}
}
