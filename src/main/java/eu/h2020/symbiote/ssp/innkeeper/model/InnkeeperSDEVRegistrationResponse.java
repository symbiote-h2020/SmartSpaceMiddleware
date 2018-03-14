package eu.h2020.symbiote.ssp.innkeeper.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.h2020.symbiote.ssp.rap.odata.OwlApiHelper;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;

public class InnkeeperSDEVRegistrationResponse {
	private static Log log = LogFactory.getLog(InnkeeperResourceRegistrationResponse.class);
    @Autowired
    ResourcesRepository resourcesRepository;

    @Autowired
    AccessPolicyRepository accessPolicyRepository;
    
    @Autowired
    OwlApiHelper owlApiHelp;
	
	@JsonProperty("symIdSDEV") 				private String symIdSDEV;
	@JsonProperty("internalIdSDEV") 			private String internalIdSDEV;
	@JsonProperty("result") 					private String result;
	@JsonProperty("registrationExpiration") 	private int registrationExpiration;
	
	public InnkeeperSDEVRegistrationResponse() {}
	
	public InnkeeperSDEVRegistrationResponse(String symId, String internalIdSDEV, String result) {
		this.symIdSDEV=symId;
		this.internalIdSDEV=internalIdSDEV;
		this.result=result;
	}
	public InnkeeperSDEVRegistrationResponse(String symId, String internalIdSDEV, String result, int registration_expiration) {
		this.symIdSDEV=symId;
		this.internalIdSDEV=internalIdSDEV;		
		this.result=result;
		this.registrationExpiration=registration_expiration;
	}
	public String getSymIdSDEV() {
		return this.symIdSDEV;
	}
	public String getInternalIdSDEV() {
		return this.symIdSDEV;
	}
	public String getResult() {
		return this.result;
	}

	public int getRegistrationExpiration() {
		return this.registrationExpiration;		
	}	

}
