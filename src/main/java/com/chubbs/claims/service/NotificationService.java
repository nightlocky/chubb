package com.chubbs.claims.service;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationService {

    /**
     * Logs claim lifecycle events from Kafka.
     *
     * @param event lifecycle event
     */
    @KafkaListener(topics = "${claims.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(Map<String, Object> event) {
        log.info(
                "Claim lifecycle event received: claimId={}, action={}, status={}, assignedOfficerId={}, liabilityAmount={}",
                event.get("claimId"),
                event.get("action"),
                event.get("status"),
                event.get("assignedOfficerId"),
                event.get("liabilityAmount")
        );
    }
}
