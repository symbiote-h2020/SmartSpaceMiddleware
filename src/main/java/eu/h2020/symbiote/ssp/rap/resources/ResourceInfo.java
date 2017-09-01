/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Document(collection="resources")
public class ResourceInfo {
    
    @Id
    @JsonProperty("symbioteId")
    private final String id;    
    
    @JsonProperty("internalId")
    private final String internalId;
    
    @JsonProperty("platformId")
    private final String platformId;
    private List<String> observedProperties;
    private List<String> sessionIdList;
    
    private String host;
    
    
    public ResourceInfo() {
        this.id = "";
        this.internalId = "";
        this.platformId = null;
        this.observedProperties = null;
        this.sessionIdList = null;
        this.host = null;
    }
    
    @JsonCreator
    public ResourceInfo(@JsonProperty("symbioteId")String resourceId, 
            @JsonProperty("internalId")String internalId, 
            @JsonProperty("platformId")String platformId) {
        this.id = resourceId;
        this.internalId = internalId;
        this.platformId = platformId;
        this.observedProperties = null;
        this.sessionIdList = null;        
        this.host = null;
    }
    
    public ResourceInfo(String resourceId,String internalId, String platformId,
            String host) {
        this.id = resourceId;
        this.internalId = internalId;
        this.platformId = platformId;
        this.observedProperties = null;
        this.sessionIdList = null;        
        this.host = host;
    }
    
    public String getSymbioteId() {
        return id;
    }
    
    public String getInternalId() {
        return internalId;
    }
    
    public String getPlatformId() {
        return platformId;
    }
    
    public List<String> getObservedProperties() {
        return observedProperties;
    }    
    
    public void setObservedProperties(List<String> observedProperties) {
        this.observedProperties = observedProperties;
    }
    
    public List<String> getSessionId() {
        return sessionIdList;
    }
    
    public void setSessionId(List<String> sessionIdList) {
        this.sessionIdList = sessionIdList;
    }
    
    public void addToSessionList(String sessionId) {
        if(this.sessionIdList == null)
            this.sessionIdList = new ArrayList();
        this.sessionIdList.add(sessionId);
    }

    public String getHost() {
        return this.host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
