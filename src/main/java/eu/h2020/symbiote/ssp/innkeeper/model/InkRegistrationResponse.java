package eu.h2020.symbiote.ssp.innkeeper.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.h2020.symbiote.ssp.rap.odata.OwlApiHelper;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.lwsp.model.LwspConstants;

public class InkRegistrationResponse {

	private static Log log = LogFactory.getLog(InkRegistrationResponse.class);
    @Autowired
    ResourcesRepository resourcesRepository;

    @Autowired
    AccessPolicyRepository accessPolicyRepository;
    
    @Autowired
    OwlApiHelper owlApiHelp;
	
	@JsonProperty("symId") 					private String symId;
	@JsonProperty("result") 					private String result;
	@JsonProperty("registration_expiration") private int registration_expiration;
	
	public InkRegistrationResponse() {}
	public InkRegistrationResponse(String symId, String result, int registration_expiration) {
		this.symId=symId;
		this.result=result;
		this.registration_expiration=registration_expiration;
	}
	public String getSymId() {
		return this.symId;
	}
	public String getResult() {
		return this.result;
	}

	public int getRegistrationExpiration() {
		return this.registration_expiration;		
	}	
}
