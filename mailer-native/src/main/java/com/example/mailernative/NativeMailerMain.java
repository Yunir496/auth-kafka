package com.example.mailernative;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class NativeMailerMain {
    public static void main(String[] args) throws Exception {
        String bootstrap = env("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String topic     = env("KAFKA_TOPIC_VERIFICATION", "auth.email.verification.request");
        System.out.println("[mailer-native] bootstrap: " + bootstrap);
        System.out.println("[mailer-native] topic    : " + topic);

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "mailer-native");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String,String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            ObjectMapper om = new ObjectMapper();
            System.out.println("[mailer-native] listening topic: " + topic);
            while (true) {
                ConsumerRecords<String,String> recs = consumer.poll(Duration.ofSeconds(1));
                recs.forEach(r -> {
                    try {
                        JsonNode n = om.readTree(r.value());
                        String email = n.path("email").asText("");
                        String code  = n.path("code").asText("");
                        int    ttl   = n.path("ttlSeconds").asInt(600);
                        System.out.printf("[MAILER-NATIVE] Send code %s to %s (valid %ds)%n", code, email, ttl);
                    } catch (Exception e) {
                        System.err.println("[MAILER-NATIVE] invalid payload: " + r.value());
                    }
                });
            }
        }
    }

    private static String env(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }
}
