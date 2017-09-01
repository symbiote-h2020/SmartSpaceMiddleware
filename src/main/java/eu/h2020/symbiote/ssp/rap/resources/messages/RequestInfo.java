/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.resources.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.ssp.rap.resources.ResourceInfo;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class RequestInfo extends ResourceInfo {
    
    @JsonProperty("devicePath")
    private final String devicePath;
    
    /**
     * JSON Constructor
     * @param resourceId
     * @param platformId     
     * @param internalId  
     * @param devicePath
     * 
     */
    @JsonCreator
    public RequestInfo(String resourceId, String internalId, String platformId, String devicePath) {
        super(resourceId, internalId, platformId);
        this.devicePath = devicePath;
    }   
    
    public String getDevicePath() {
        return devicePath;
    }
}
