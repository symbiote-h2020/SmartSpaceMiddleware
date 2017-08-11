/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.resources;

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
    private final String id;    
    private final String internalId;
    private final String platformId;
    private List<String> observedProperties;
    private List<String> sessionIdList;
    
    
    public ResourceInfo(String resourceId, String internalId, String platformId) {
        this.id = resourceId;
        this.internalId = internalId;
        this.platformId = platformId;
        this.observedProperties = null;
        this.sessionIdList = null;        
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
}
