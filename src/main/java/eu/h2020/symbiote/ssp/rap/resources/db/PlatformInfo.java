/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.resources.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Document(collection="plugins")
public class PlatformInfo {
    @Id
    @JsonProperty("platformId")
    private final String id;
    
    @JsonProperty("hasFilters")
    private final boolean hasFilters;
       
    @JsonProperty("hasNotifications")
    private final boolean hasNotifications;
    
    public PlatformInfo() {
        id = "";
        hasFilters = false;
        hasNotifications = false;
    }
    
    @JsonCreator
    public PlatformInfo(@JsonProperty("platformId") String platformId, 
            @JsonProperty("hasFilters") boolean hasFilters,
            @JsonProperty("hasNotifications") boolean hasNotifications) {
        this.id = platformId;
        this.hasFilters = hasFilters;
        this.hasNotifications = hasNotifications;
    }
    
    @JsonProperty("platformId")
    public String getPlatformId() {
        return id;
    }
    
    @JsonProperty("hasFilters")
    public boolean getHasFilters() {
        return hasFilters;
    }

    @JsonProperty("hasNotifications")
    public boolean getHasNotifications() {
        return hasNotifications;
    }
}
