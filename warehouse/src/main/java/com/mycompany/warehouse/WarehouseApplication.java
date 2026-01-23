package com.mycompany.warehouse;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
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
        return new JacksonJsonMessageConverter();
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
    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
                                             MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(AMQP_QUEUE);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(OrderProcessor receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }
}
