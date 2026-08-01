package com.chubbs.claims.service;

import com.chubbs.claims.model.Claim;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimEventPublisher {

    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    @Value("${claims.kafka.topic}")
    private String claimLifecycleTopic;

    /**
     * Publishes a claim lifecycle event to Kafka.
     *
     * @param claim claim details
     * @param action lifecycle action
     */
    public void publishLifecycleEvent(Claim claim, String action) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("claimId", claim.getId());
        event.put("claimantId", claim.getClaimantId());
        event.put("status", claim.getStatus().name());
        event.put("assignedOfficerId", claim.getAssignedOfficerId());
        event.put("liabilityAmount", claim.getLiabilityAmount());
        event.put("action", action);
        event.put("eventTime", LocalDateTime.now().toString());

        kafkaTemplate.send(claimLifecycleTopic, String.valueOf(claim.getId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error(
                                "Failed to publish claim lifecycle event: claimId={}, action={}",
                                claim.getId(),
                                action,
                                ex
                        );
                        return;
                    }

                    log.info(
                            "Claim lifecycle event published: claimId={}, action={}, topic={}, partition={}, offset={}",
                            claim.getId(),
                            action,
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }
}
