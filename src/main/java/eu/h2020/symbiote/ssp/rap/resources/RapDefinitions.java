/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.resources;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class RapDefinitions {
    
    public static final String      RESOURCE_REGISTRATION_EXCHANGE_IN = "symbIoTe.rap";
    public static final String      RESOURCE_REGISTRATION_QUEUE = "symbIoTe.rap.registrationHandler.register_resources";
    public static final String      RESOURCE_UNREGISTRATION_QUEUE = "symbIoTe.rap.registrationHandler.unregister_resources";
    public static final String      RESOURCE_UPDATE_QUEUE = "symbIoTe.rap.registrationHandler.update_resources";

    
    public static final String      RESOURCE_ACCESS_EXCHANGE_IN = "symbIoTe.rap.accessResource";
    public static final String[]    RESOURCE_READ_KEYS  = {"symbIoTe.rap.accessResource.readResource.*"};
    public static final String      RESOURCE_READ_QUEUE = "symbiote-rap-accessResource-readResource";   
    public static final String[]    RESOURCE_WRITE_KEYS  = {"symbIoTe.rap.accessResource.writeResource.*"};
    public static final String      RESOURCE_WRITE_QUEUE = "symbIoTe-rap-accessResource-writeResource";
    
    public static final String      PLUGIN_REGISTRATION_EXCHANGE_IN = "symbIoTe.rapPluginExchange";
    public static final String      PLUGIN_REGISTRATION_KEY = "symbIoTe.rapPluginExchange.add-plugin";
    public static final String      PLUGIN_REGISTRATION_QUEUE = "symbIoTe.platform-queue";

    public static final String      PLUGIN_EXCHANGE_OUT = "plugin-exchange";
    
    public static final String      PLUGIN_NOTIFICATION_QUEUE = "symbIoTe.platform-queue-notification";
    public static final String      PLUGIN_NOTIFICATION_EXCHANGE_IN = "symbIoTe.rapPluginExchange-notification";
    public static final String      PLUGIN_NOTIFICATION_KEY = "symbIoTe.rapPluginExchange.plugin-notification";
}
