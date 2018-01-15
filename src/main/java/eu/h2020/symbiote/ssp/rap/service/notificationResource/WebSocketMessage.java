/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.service.notificationResource;

import java.util.List;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class WebSocketMessage {
    
    public enum Action {
        SUBSCRIBE, UNSUBSCRIBE
    }
    
    private Action action;
    private List<String> ids;
    
    public WebSocketMessage(){
        
    }
    
    public WebSocketMessage(Action action,List<String> ids){
        this.action = action;
        this.ids = ids;
    }
    
    public Action getAction(){
        return this.action;
    }
    
    public List<String> getIds(){
        return this.ids;
    }
}
