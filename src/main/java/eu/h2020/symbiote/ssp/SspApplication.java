package eu.h2020.symbiote.ssp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

@SpringBootApplication
public class SspApplication {

    @Value("${rabbit.host}")
    private String rabbitHost;

    @Value("${rabbit.username}")
    private String rabbitUsername;

    @Value("${rabbit.password}")
    private String rabbitPassword;

    @Value("${registrationExpiration}")
    private String registrationExpiration;

    @Value("${makeResourceOffline}")
    private String makeResourceOffline;

    public static void main(String[] args) {
		SpringApplication.run(SspApplication.class, args);
	}

    @Bean(name="registrationExpiration")
    public Integer registrationExpiration() {
        Integer registrationsExp = Integer.parseInt(registrationExpiration);

        if (registrationsExp < 0)
            return 0;
        else
            return registrationsExp;
    }

    @Bean(name="makeResourceOffline")
    public Integer makeResourceOffline() {
        return Integer.parseInt(makeResourceOffline);
    }

    @Bean
    public Timer timer() {
        return new Timer();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
//        factory.setConcurrentConsumers(3);
//        factory.setMaxConcurrentConsumers(10);
        factory.setMessageConverter(jackson2JsonMessageConverter());
        return factory;
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        return converter;
    }


    @Bean
    public ConnectionFactory connectionFactory() throws Exception {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitHost);
        // connectionFactory.setPublisherConfirms(true);
        // connectionFactory.setPublisherReturns(true);
        connectionFactory.setUsername(rabbitUsername);
        connectionFactory.setPassword(rabbitPassword);
        return connectionFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);
        return rabbitTemplate;
    }
}
