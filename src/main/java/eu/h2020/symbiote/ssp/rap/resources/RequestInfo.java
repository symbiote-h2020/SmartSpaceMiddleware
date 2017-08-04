/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.resources;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class RequestInfo {
    
    private final String obj;
    private final String id;
    private final String internalId;

    /**
     * Constructor
     * @param obj  
     * @param id     
     * @param internalId  
     */
    @JsonCreator
    public RequestInfo(String obj, String id, String internalId) {
        this.obj = obj;
        this.id = id;
        this.internalId = internalId;
    }   
    
    public String getObj() {
        return obj;
    }

    public String getId() {
        return id;
    }

    public String getInternalId() {
        return internalId;
    }
}
