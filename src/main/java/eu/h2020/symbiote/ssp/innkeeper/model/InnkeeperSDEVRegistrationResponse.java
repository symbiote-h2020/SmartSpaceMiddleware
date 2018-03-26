package eu.h2020.symbiote.ssp.innkeeper.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("symId") 				private String symId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("sspId") 			private String sspId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("result") 					private String result;
    @JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty("registrationExpiration") 	private Integer registrationExpiration;
	
	public InnkeeperSDEVRegistrationResponse() {		
	}
	
	public InnkeeperSDEVRegistrationResponse(String symId, String sspId, String result) {
		this.symId=symId;
		this.sspId=sspId;
		this.result=result;
	}
	public InnkeeperSDEVRegistrationResponse(String symId, String sspId, String result, Integer registration_expiration) {
		this.symId=symId;
		this.sspId=sspId;		
		this.result=result;
		this.registrationExpiration=registration_expiration;
	}
	public String getSymId() {
		return this.symId;
	}
	public String getSspId() {
		return this.sspId;
	}
	public String getResult() {
		return this.result;
	}
	public void setResult(String result) {
		this.result=result;
	}

	public Integer getRegistrationExpiration() {
		return this.registrationExpiration;		
	}	

}
