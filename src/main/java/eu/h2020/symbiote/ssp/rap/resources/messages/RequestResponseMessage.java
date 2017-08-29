/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.resources.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class RequestResponseMessage {
    @JsonProperty("id")
    private String id;    
    
    @JsonProperty("value")
    private Map<String, String> value;

    
    /**
     * JSON Constructor
     * @param id
     * @param value
     */
    @JsonCreator
    public RequestResponseMessage(@JsonProperty("id")String id, 
            @JsonProperty("value") Map<String, String> value) {
        this.id = id;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getValue() {
        return value;
    }

}
