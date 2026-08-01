package com.chubbs.claims.service;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DeadLetterNotificationService {

    /**
     * Logs claim lifecycle events that could not be processed.
     *
     * @param event dead letter event
     */
    @KafkaListener(topics = "${claims.kafka.topic}${claims.kafka.dlt-suffix}",
            groupId = "${spring.kafka.consumer.group-id}-dlt")
    public void listen(Map<String, Object> event) {
        log.warn(
                "Dead letter claim lifecycle event received: claimId={}, action={}, status={}",
                event.get("claimId"),
                event.get("action"),
                event.get("status")
        );
    }
}
