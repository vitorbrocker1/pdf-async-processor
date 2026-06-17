package com.vitor.pdfapi.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PDF_QUEUE       = "pdf.queue";
    public static final String PDF_DLQ         = "pdf.dead-letter.queue";
    public static final String PDF_EXCHANGE    = "pdf.exchange";
    public static final String PDF_DLX        = "pdf.dlx";
    public static final String PDF_ROUTING_KEY = "pdf.process";

    @Bean
    public DirectExchange pdfExchange() {
        return ExchangeBuilder.directExchange(PDF_EXCHANGE).durable(true).build();
    }

    @Bean
    public FanoutExchange pdfDlx() {
        return ExchangeBuilder.fanoutExchange(PDF_DLX).durable(true).build();
    }

    @Bean
    public Queue pdfQueue() {
        return QueueBuilder.durable(PDF_QUEUE)
                .withArgument("x-dead-letter-exchange", PDF_DLX)
                .withArgument("x-message-ttl", 600_000)
                .withArgument("x-max-length", 10_000)
                .build();
    }

    @Bean
    public Queue pdfDeadLetterQueue() {
        return QueueBuilder.durable(PDF_DLQ).build();
    }

    @Bean
    public Binding pdfBinding(Queue pdfQueue, DirectExchange pdfExchange) {
        return BindingBuilder.bind(pdfQueue).to(pdfExchange).with(PDF_ROUTING_KEY);
    }

    @Bean
    public Binding pdfDlqBinding(Queue pdfDeadLetterQueue, FanoutExchange pdfDlx) {
        return BindingBuilder.bind(pdfDeadLetterQueue).to(pdfDlx);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf, Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(converter);
        factory.setPrefetchCount(5);
        factory.setDefaultRequeueRejected(false);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        return factory;
    }
}
