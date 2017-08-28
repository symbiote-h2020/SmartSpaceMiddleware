/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.resources.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class RequestMessage {
    @JsonProperty("id")
    private String id;    
    
    @JsonProperty("resourceId")
    private String resourceId;

    public RequestMessage(String id, String resourceId) {
        this.id = id;
        this.resourceId = resourceId;
    }
    
    public String getId() {
        return id;
    }
    
    public String getResourceId() {
        return resourceId;
    }

}
