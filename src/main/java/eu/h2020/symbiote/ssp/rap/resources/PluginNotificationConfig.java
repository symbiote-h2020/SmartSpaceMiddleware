/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.resources;

import eu.h2020.symbiote.ssp.rap.interfaces.PluginNotification;
import eu.h2020.symbiote.ssp.rap.interfaces.conditions.NBInterfaceWebSocketCondition;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@Conditional(NBInterfaceWebSocketCondition.class)
@Configuration
public class PluginNotificationConfig {
    
    @Bean(name=RapDefinitions.PLUGIN_NOTIFICATION_EXCHANGE_IN)
    TopicExchange pluginNotificationExchangeIn() {
        return new TopicExchange(RapDefinitions.PLUGIN_NOTIFICATION_EXCHANGE_IN, false, false);
    }
    
    @Bean(name=RapDefinitions.PLUGIN_NOTIFICATION_QUEUE)
    Queue pluginQueue() {
        return new Queue(RapDefinitions.PLUGIN_NOTIFICATION_QUEUE, false);
    }
    
    @Bean(name=RapDefinitions.PLUGIN_NOTIFICATION_QUEUE + "Container")
    SimpleMessageListenerContainer platformPluginContainer(ConnectionFactory connectionFactory,
                                                           @Qualifier(RapDefinitions.PLUGIN_NOTIFICATION_QUEUE + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.PLUGIN_NOTIFICATION_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }
    
    @Bean(name=RapDefinitions.PLUGIN_NOTIFICATION_QUEUE + "Bindings")
    List<Binding> pluginBindings(@Qualifier(RapDefinitions.PLUGIN_NOTIFICATION_QUEUE) Queue queue,
                                 @Qualifier(RapDefinitions.PLUGIN_NOTIFICATION_EXCHANGE_IN) TopicExchange exchange) {
        ArrayList bindings = new ArrayList();
        bindings.add(BindingBuilder.bind(queue).to(exchange).with(RapDefinitions.PLUGIN_NOTIFICATION_KEY));

        return bindings;
    }

    @Bean(name=RapDefinitions.PLUGIN_NOTIFICATION_QUEUE + "Receiver")
    PluginNotification platformPluginReceiver() {
        return new PluginNotification();
    }
    
    @Bean(name=RapDefinitions.PLUGIN_NOTIFICATION_QUEUE + "Listener")
    MessageListenerAdapter platformPluginListenerAdapter(@Qualifier(RapDefinitions.PLUGIN_NOTIFICATION_QUEUE + "Receiver") PluginNotification receiver) {
        return new MessageListenerAdapter(receiver, "receiveNotification");
    }
}
