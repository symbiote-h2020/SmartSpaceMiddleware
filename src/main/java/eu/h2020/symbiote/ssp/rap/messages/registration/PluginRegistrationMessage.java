/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.messages.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RegisterPluginMessage.class,   name = "REGISTER_PLUGIN"),
        @JsonSubTypes.Type(value = UnregisterPluginMessage.class, name = "UNREGISTER_PLUGIN")
})
abstract public class PluginRegistrationMessage extends RegistrationMessage {
    public enum RegistrationAction {
        REGISTER_PLUGIN, UNREGISTER_PLUGIN
    }
    
    @JsonIgnore
    RegistrationAction actionType;
    String platformId;
    
    public RegistrationAction getActionType() {
        return actionType;
    }
    
    
    @JsonProperty("platformId")
    public String getPlatformId() {
        return platformId;
    }
}
