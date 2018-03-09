package eu.h2020.symbiote.ssp.innkeeper.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.fasterxml.jackson.annotation.JsonProperty;

import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.ssp.lwsp.model.LwspConstants;


public class InkRegistrationInfo {

	private static Log log = LogFactory.getLog(InkRegistrationInfo.class);
	
	@JsonProperty("symId") 					private String symId;
	@JsonProperty("dk1") 					private String dk1;
	@JsonProperty("hashField") 				private String hashField;
	@JsonProperty("semanticDescription") 	private List<CloudResource> semanticDescription;
	@JsonProperty("connectedTo") 			private String connectedTo;
	@JsonProperty("available") 				private boolean available;
	@JsonProperty("agentType") 				private String agentType;

	public InkRegistrationInfo() {
		this.symId=null;
		this.dk1=null;
		this.hashField=null;
		this.semanticDescription=null;

		this.connectedTo=null;
		this.available=false;
		this.agentType = LwspConstants.SDEV;		
	}
	
	public void setSymId(String symId) {
		this.symId = symId;
		
	}	
	public String getSymId() {
		return this.symId;
	}
	
	
	public String getdk1() {
		return this.dk1;
	}

	public String getHashField() {
		return this.hashField;
	}
	public List<CloudResource> getSemanticDescription() {
		return this.semanticDescription;
	}
	
	public Map<String, Resource> getSemanticDescriptionMap() {
		Map<String,Resource> m = new HashMap<String,Resource>();
		for (CloudResource s : this.semanticDescription) {
			System.out.println("here");
			m.put(s.getInternalId(), s.getResource());
			
		}
		return m;
	}
	
	public void setSemanticDescription(List<CloudResource> cr) {
		this.semanticDescription=cr;
	}

	public String getConnectedTo() {
		return this.connectedTo;
	}

	public void setConnectedTo(String connectedTo) {
		this.connectedTo=connectedTo;
	}

	public boolean getAvailable() {
		return this.available;
	}

	public String getAgentType() {
		return this.agentType;
	}

}
