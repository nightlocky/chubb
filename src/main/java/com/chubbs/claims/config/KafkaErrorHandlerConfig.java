package com.chubbs.claims.config;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@RequiredArgsConstructor
public class KafkaErrorHandlerConfig {

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    @Value("${claims.kafka.dlt-suffix}")
    private String deadLetterTopicSuffix;

    @Value("${claims.kafka.retry-interval-ms}")
    private long retryIntervalMs;

    @Value("${claims.kafka.max-retry-attempts}")
    private long maxRetryAttempts;

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + deadLetterTopicSuffix, record.partition())
        );

        return new DefaultErrorHandler(recoverer, new FixedBackOff(retryIntervalMs, maxRetryAttempts));
    }
}
