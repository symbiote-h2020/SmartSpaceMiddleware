/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.pushNotificationService;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.model.cim.Observation;
import java.util.Map;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class WebSocketMessageSecurityResponse {
    
    @JsonIgnore
    @JsonProperty("secResponse")
    private Map<String,String> secResponse;
    
    @JsonProperty("payload")
    private Observation payload;

    public WebSocketMessageSecurityResponse(Map<String, String> secResponse, Observation payload) {
        this.secResponse = secResponse;
        this.payload = payload;
    }

    public Map<String, String> getSecResponse() {
        return secResponse;
    }

    public void setSecResponse(Map<String, String> secResponse) {
        this.secResponse = secResponse;
    }

    public Observation getPayload() {
        return payload;
    }

    public void setPayload(Observation payload) {
        this.payload = payload;
    }
    
    
}
