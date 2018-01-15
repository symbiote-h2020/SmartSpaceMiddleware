/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.resources;

import eu.h2020.symbiote.ssp.rap.interfaces.ResourceRegistration;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Configuration
public class ResourceRegistrationQueueConfig {
    @Bean(name=RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN)
    DirectExchange resourceRegistrationExchangeIn() {
        return new DirectExchange(RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN, false, false);
    }
    
    @Bean(name=RapDefinitions.RESOURCE_REGISTRATION_QUEUE)
    Queue resourceRegistrationQueue() {
        return new Queue(RapDefinitions.RESOURCE_REGISTRATION_QUEUE, false);
    }
    
    @Bean(name=RapDefinitions.RESOURCE_UNREGISTRATION_QUEUE)
    Queue resourceUnregistrationQueue() {
        return new Queue(RapDefinitions.RESOURCE_UNREGISTRATION_QUEUE, false);
    }
    
    @Bean(name=RapDefinitions.RESOURCE_UPDATE_QUEUE)
    Queue resourceUpdatedQueue() {
        return new Queue(RapDefinitions.RESOURCE_UPDATE_QUEUE, false);
    }

    @Bean(name=RapDefinitions.RESOURCE_REGISTRATION_QUEUE + "Bindings")
    Binding resourceRegistrationBindings(@Qualifier(RapDefinitions.RESOURCE_REGISTRATION_QUEUE) Queue queue,
                             @Qualifier(RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN) DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(queue.getName());
    }
    
    
    @Bean(name=RapDefinitions.RESOURCE_UNREGISTRATION_QUEUE + "Bindings")
    Binding resourceUnregistrationBindings(@Qualifier(RapDefinitions.RESOURCE_UNREGISTRATION_QUEUE) Queue queue,
                             @Qualifier(RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN) DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(queue.getName());
    }
    
    
    @Bean(name=RapDefinitions.RESOURCE_UPDATE_QUEUE + "Bindings")
    Binding resourceUpdatedBindings(@Qualifier(RapDefinitions.RESOURCE_UPDATE_QUEUE) Queue queue,
                             @Qualifier(RapDefinitions.RESOURCE_REGISTRATION_EXCHANGE_IN) DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(queue.getName());
    }

    @Bean(name=RapDefinitions.RESOURCE_REGISTRATION_QUEUE + "Container")
    SimpleMessageListenerContainer resourceRegContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(RapDefinitions.RESOURCE_REGISTRATION_QUEUE + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.RESOURCE_REGISTRATION_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }
    
    @Bean(name=RapDefinitions.RESOURCE_UNREGISTRATION_QUEUE + "Container")
    SimpleMessageListenerContainer resourceUnregContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(RapDefinitions.RESOURCE_UNREGISTRATION_QUEUE + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.RESOURCE_UNREGISTRATION_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean(name=RapDefinitions.RESOURCE_UPDATE_QUEUE + "Container")
    SimpleMessageListenerContainer resourceUpdContainer(ConnectionFactory connectionFactory,
                                             @Qualifier(RapDefinitions.RESOURCE_UPDATE_QUEUE + "Listener") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(RapDefinitions.RESOURCE_UPDATE_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }
    
    @Bean
    ResourceRegistration resourceReceiver() {
        return new ResourceRegistration();
    }

    @Bean(name=RapDefinitions.RESOURCE_REGISTRATION_QUEUE + "Listener")
    MessageListenerAdapter resourceRegistrationListenerAdapter(ResourceRegistration receiver) {
        return new MessageListenerAdapter(receiver, "receiveRegistrationMessage");
    }
    
    @Bean(name=RapDefinitions.RESOURCE_UNREGISTRATION_QUEUE + "Listener")
    MessageListenerAdapter resourceUnRegistrationListenerAdapter(ResourceRegistration receiver) {
        return new MessageListenerAdapter(receiver, "receiveUnregistrationMessage");
    }
    
    @Bean(name=RapDefinitions.RESOURCE_UPDATE_QUEUE + "Listener")
    MessageListenerAdapter resourceUpdatedListenerAdapter(ResourceRegistration receiver) {
        return new MessageListenerAdapter(receiver, "receiveUpdateMessage");
    }
}
