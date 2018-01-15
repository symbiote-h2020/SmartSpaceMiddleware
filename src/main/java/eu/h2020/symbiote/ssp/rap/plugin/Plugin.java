/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessMessage;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessSubscribeMessage;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessUnSubscribeMessage;
import eu.h2020.symbiote.ssp.rap.messages.registration.RegisterPluginMessage;
import eu.h2020.symbiote.ssp.rap.resources.RapDefinitions;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public abstract class Plugin {
    private static final Logger log = LoggerFactory.getLogger(Plugin.class);

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange exchange;

    public RabbitTemplate getRabbitTemplate() {
        return this.rabbitTemplate;
    }
    
    public Plugin(RabbitTemplate rabbitTemplate, TopicExchange exchange,
                          String platformId, boolean hasFilters, boolean hasNotifications) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange; 
        registerPlugin(platformId, hasFilters, hasNotifications);
    }  
    
    
    public String receiveMessage(String message) {
        String json = null;
        try {            
            ObjectMapper mapper = new ObjectMapper();
            ResourceAccessMessage msg = mapper.readValue(message, ResourceAccessMessage.class);
            ResourceAccessMessage.AccessType access = msg.getAccessType();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            switch(access) {
                case GET: {
                    ResourceAccessGetMessage msgGet = (ResourceAccessGetMessage) msg;
                    List<ResourceInfo> resInfoList = msgGet.getResourceInfo();
                    String internalId = null;
                    for(ResourceInfo resInfo: resInfoList){
                        String internalIdTemp = resInfo.getInternalId();
                        if(internalIdTemp != null && !internalIdTemp.isEmpty())
                            internalId = internalIdTemp;
                    }
                    List<Observation> observationLst = readResource(internalId);
                    json = mapper.writeValueAsString(observationLst);
                    break;
                }
                case HISTORY: {
                    ResourceAccessHistoryMessage msgHistory = (ResourceAccessHistoryMessage) msg;
                    List<ResourceInfo> resInfoList = msgHistory.getResourceInfo();
                    String internalId = null;
                    for(ResourceInfo resInfo: resInfoList){
                        String internalIdTemp = resInfo.getInternalId();
                        if(internalIdTemp != null && !internalIdTemp.isEmpty())
                            internalId = internalIdTemp;
                    }
                    List<Observation> observationLst = readResourceHistory(internalId);
                    json = mapper.writeValueAsString(observationLst);       
                    break;
                }
                case SET: {
                    ResourceAccessSetMessage msgSet = (ResourceAccessSetMessage)msg;
                    List<ResourceInfo> resInfoList = msgSet.getResourceInfo();
                    String internalId = null;
                    for(ResourceInfo resInfo: resInfoList){
                        String internalIdTemp = resInfo.getInternalId();
                        if(internalIdTemp != null && !internalIdTemp.isEmpty())
                            internalId = internalIdTemp;
                    }
                    json = writeResource(internalId, msgSet.getBody());
                    break;
                }
                case SUBSCRIBE: {
                    ResourceAccessSubscribeMessage mess = (ResourceAccessSubscribeMessage)msg;
                    List<ResourceInfo> infoList = mess.getResourceInfoList();
                    for(ResourceInfo info : infoList) {
                        subscribeResource(info.getInternalId());
                    }
                    break;
                }
                case UNSUBSCRIBE: {
                    ResourceAccessUnSubscribeMessage mess = (ResourceAccessUnSubscribeMessage)msg;
                    List<ResourceInfo> infoList = mess.getResourceInfoList();
                    for(ResourceInfo info : infoList) {
                        unsubscribeResource(info.getInternalId());
                    }
                    break;
                }
                default:
                    throw new Exception("Access type " + access.toString() + " not supported");
            }
        } catch (Exception e) {
            log.error("Error while processing message:\n" + message + "\n" + e);
        }
        return json;
    }
    
    /*
    *
    */
    private void registerPlugin(String platformId, boolean hasFilters, boolean hasNotifications) {
        try {
            RegisterPluginMessage msg = new RegisterPluginMessage(platformId, hasFilters, hasNotifications);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            byte[] json = mapper.writeValueAsBytes(msg);

            rabbitTemplate.convertAndSend(exchange.getName(), RapDefinitions.PLUGIN_REGISTRATION_KEY, json);
        } catch (Exception e ) {
            log.error("Error while registering plugin for platform " + platformId + "\n" + e);
        }
    }
    
    /*  
    *   OVERRIDE this, inserting the query to the platform with internal resource id
    */
    public abstract List<Observation> readResource(String resourceId);
    
    /*  
    *   OVERRIDE this, inserting here a call to the platform with internal resource id
    *   setting the actuator value
    */
    public abstract String writeResource(String resourceId, String body);
        
    /*  
    *   OVERRIDE this, inserting the query to the platform with internal resource id
    */
    public abstract List<Observation> readResourceHistory(String resourceId);
    
    /*  
    *   OVERRIDE this, inserting the subscription of the resource
    */
    public abstract void subscribeResource(String resourceId);
    
    /*  
    *   OVERRIDE this, inserting the unsubscription of the resource
    */
    public abstract void unsubscribeResource(String resourceId);
}
