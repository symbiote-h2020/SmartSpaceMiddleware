package eu.h2020.symbiote.ssp.innkeeper.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.h2020.symbiote.ssp.rap.odata.OwlApiHelper;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;

public class InnkeeperResourceRegistrationResponse {

	private static Log log = LogFactory.getLog(InnkeeperResourceRegistrationResponse.class);
    @Autowired
    ResourcesRepository resourcesRepository;

    @Autowired
    AccessPolicyRepository accessPolicyRepository;
    
    @Autowired
    OwlApiHelper owlApiHelp;
	
	@JsonProperty("symIdResource")				
	private String symIdResource;
	
	@JsonProperty("internalIdResource")			
	private String internalIdResource;
	
	@JsonProperty("symId")				
	private String symId;
	
	@JsonProperty("internalId")			
	private String internalId;
	
	@JsonProperty("result") 					
	private String result;
	
	@JsonProperty("registration_expiration") 
	private Integer registration_expiration;
	
	public InnkeeperResourceRegistrationResponse() {}
	
	public InnkeeperResourceRegistrationResponse(String symIdResource, String internalIdResource,String symId, String internalId, String result) {
		this.symIdResource=symIdResource;
		this.internalIdResource=internalIdResource;
		
		this.symId=symId;
		this.internalId=internalId;
		
		this.result=result;
	}
	public InnkeeperResourceRegistrationResponse(String symIdResource, String internalIdResource,String symId, String internalId, String result, Integer registration_expiration) {
		this.symIdResource=symIdResource;
		this.internalIdResource=internalIdResource;
		
		this.symId=symId;
		this.internalId=internalId;
		
		this.result=result;
		this.registration_expiration=registration_expiration;
	}
	
	public String getSymIdResource() {
		return this.symIdResource;
	}
	
	public void setSymIdResource(String symIdResource) {
		this.symIdResource=symIdResource;
	}
	
	public String getInternalIdResource() {
		return this.internalIdResource;
	}
	
	public void setInternalIdResource(String internalIdResource) {
		this.internalIdResource=internalIdResource;
	}
	
	public String getSymId() {
		return this.symId;
	}
	
	public void setSymIdDEV(String symId) {
		this.symId=symId;
	}
	
	public String getInternalId() {
		return this.internalId;
	}
	
	public void setInternalId(String internalId) {
		this.internalId=internalId;
	}
	
	public String getResult() {
		return this.result;
	}

	public int getRegistrationExpiration() {
		return this.registration_expiration;		
	}	
}
