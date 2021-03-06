/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.messages.access;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import java.util.List;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class ResourceAccessGetMessage extends ResourceAccessMessage {
    
    @JsonProperty("resourceInfo")
    List<ResourceInfo> resInfo;
   
    
    /**
     * JSON Constructor
     * @param resInfo               the resource data information  
     */
    @JsonCreator
    public ResourceAccessGetMessage(@JsonProperty("resourceInfo") List<ResourceInfo> resInfo){
        this.accessType = AccessType.GET;
        this.resInfo = resInfo;
    }
    
    @JsonProperty("resourceInfo")
    public List<ResourceInfo> getResourceInfo(){
        return this.resInfo;
    }
}
