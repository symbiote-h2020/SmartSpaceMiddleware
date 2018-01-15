/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.messages.registration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class RegisterPluginMessage extends PluginRegistrationMessage {
    @JsonProperty("hasFilters")
    private final boolean hasFilters;
       
    @JsonProperty("hasNotifications")
    private final boolean hasNotifications;
    
    @JsonCreator
    public RegisterPluginMessage(@JsonProperty("platformId") String platformId,
            @JsonProperty("hasFilters") boolean hasFilters,
            @JsonProperty("hasNotifications") boolean hasNotifications) {
        this.actionType = RegistrationAction.REGISTER_PLUGIN;
        this.platformId = platformId;
        this.hasFilters = hasFilters;
        this.hasNotifications = hasNotifications;
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
