/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.messages.access;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import org.json.JSONObject;

import java.util.List;

/**
 *
* @author Luca Tomaselli <l.tomaselli@nextworks.it>
*/
public class ResourceAccessSetMessage extends ResourceAccessMessage{
    @JsonProperty("resourceInfo")
    private final List<ResourceInfo> resInfo;
    
    @JsonProperty("body")
    private final JSONObject body;
    
    /**
     * JSON Constructor
     * @param resInfo               the resource data information
     * @param body                  the body of request
     */
    @JsonCreator
    public ResourceAccessSetMessage(@JsonProperty("resourceInfo") List<ResourceInfo> resInfo, 
                                    @JsonProperty("body") JSONObject body) {
        this.accessType = ResourceAccessMessage.AccessType.SET;
        this.resInfo = resInfo;
        this.body = body;
    }
    
    @JsonProperty("body")
    public JSONObject getBody() {
        return body;
    }
    
    @JsonProperty("resourceInfo")
    public List<ResourceInfo> getResourceInfo(){
        return this.resInfo;
    }
}
