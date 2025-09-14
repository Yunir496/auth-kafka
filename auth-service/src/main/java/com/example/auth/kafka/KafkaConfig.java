package com.example.auth.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic verificationTopic(@Value("${kafka.topic.verification}") String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }
}
