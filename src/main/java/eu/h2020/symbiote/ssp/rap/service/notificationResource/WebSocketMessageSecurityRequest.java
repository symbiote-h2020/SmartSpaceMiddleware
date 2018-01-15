/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.service.notificationResource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class WebSocketMessageSecurityRequest {
    
    @JsonIgnore
    @JsonProperty("secRequest")
    private Map<String,String> secRequest;
    
    @JsonProperty("payload")
    private WebSocketMessage payload;

    public WebSocketMessageSecurityRequest(Map<String, String> secRequest, WebSocketMessage payload) {
        this.secRequest = secRequest;
        this.payload = payload;
    }

    public Map<String, String> getSecRequest() {
        return secRequest;
    }

    public void setSecRequest(Map<String, String> secRequest) {
        this.secRequest = secRequest;
    }

    public WebSocketMessage getPayload() {
        return payload;
    }

    public void setPayload(WebSocketMessage payload) {
        this.payload = payload;
    }
}
