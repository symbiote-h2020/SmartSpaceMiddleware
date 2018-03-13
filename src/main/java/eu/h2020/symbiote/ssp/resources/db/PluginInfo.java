/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.resources.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Document(collection="plugins")
public class PluginInfo {
    @Id
    @JsonProperty("pluginId")
    private final String id;
    
    @JsonProperty("pluginURL")
    private final String url;
    
    @JsonProperty("hasFilters")
    private final boolean hasFilters;
       
    @JsonProperty("hasNotifications")
    private final boolean hasNotifications;
    
    public PluginInfo() {
        id = "";
        url = "";
        hasFilters = false;
        hasNotifications = false;
    }
    
    @JsonCreator
    public PluginInfo(@JsonProperty("pluginId") String pluginId, 
            @JsonProperty("pluginURL") String pluginURL, 
            @JsonProperty("hasFilters") boolean hasFilters,
            @JsonProperty("hasNotifications") boolean hasNotifications) {
        this.id = pluginId;
        this.url = pluginURL;
        this.hasFilters = hasFilters;
        this.hasNotifications = hasNotifications;
    }
    
    @JsonProperty("pluginId")
    public String getPluginId() {
        return id;
    }
    
    @JsonProperty("pluginURL")
    public String getPluginURL() {
        return url;
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
