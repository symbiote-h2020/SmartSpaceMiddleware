/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.messages.registration;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class PluginRegistrationMessage {
    
    private final String pluginId;    
    private final String pluginUrl;        
    private final boolean hasFilters;       
    private final boolean hasNotifications;
    
    @JsonCreator
    public PluginRegistrationMessage(String pluginId, String pluginUrl,
            boolean hasFilters,boolean hasNotifications) {
        this.pluginId = pluginId;
        this.pluginUrl = pluginUrl;
        this.hasFilters = hasFilters;
        this.hasNotifications = hasNotifications;
    }
    
    public String getPluginId() {
        return pluginId;
    }
    
    public String getPluginUrl() {
        return pluginUrl;
    }    
        
    public boolean getHasFilters() {
        return hasFilters;
    }

    public boolean getHasNotifications() {
        return hasNotifications;
    }
}
