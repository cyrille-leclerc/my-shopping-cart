package com.mycompany.warehouse;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WarehouseApplication {
    public static final String AMQP_EXCHANGE = "ecommerce-exchange";
    public static final String AMQP_ROUTING_KEY = "queue.order";
    public static final String AMQP_QUEUE = "order";
    public static void main(String[] args) {
        SpringApplication.run(WarehouseApplication.class, args);
    }

    @Bean
    public Queue queue() {
        return new Queue(AMQP_QUEUE, false);
    }

    @Bean
    public Exchange exchange() {
        return new TopicExchange(AMQP_EXCHANGE, false, true);
    }

    @Bean
    Binding binding(Queue queue, Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(AMQP_ROUTING_KEY).noargs();
    }

    @Bean
    public MessageConverter amqpMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    CommandLineRunner initRabbitMq(ConnectionFactory connectionFactory, Queue queue, Exchange exchange, Binding binding) {
        return args ->
        {
            RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
            rabbitAdmin.declareQueue(queue);
            rabbitAdmin.declareExchange(exchange);
            rabbitAdmin.declareBinding(binding);
        };
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
