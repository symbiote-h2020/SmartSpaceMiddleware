package eu.h2020.symbiote.ssp.lwsp.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LwspMessage {
	@JsonProperty("mti") 	private String mti=null;
	
	public String getMti() { 
		return this.mti; 
	}
	public void setMti(String mti) { 
		this.mti=mti; 
	}
}
