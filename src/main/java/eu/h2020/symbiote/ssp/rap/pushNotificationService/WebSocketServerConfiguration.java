/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.pushNotificationService;

import eu.h2020.symbiote.ssp.rap.interfaces.conditions.NBInterfaceWebSocketCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 *
* @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@Conditional(NBInterfaceWebSocketCondition.class)
@Configuration
@EnableWebSocket
public class WebSocketServerConfiguration implements WebSocketConfigurer {

    @Autowired
    protected WebSocketController webSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        if(webSocketHandler != null) {
            registry.addHandler(webSocketHandler, "/notification").setAllowedOrigins("*");
        }
    }
}
