package com.glowrise.service.util;


import com.glowrise.service.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationProducer {
    private static final String NOTIFICATION_TOPIC = "notification-topic";

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public void sendNotification(NotificationEvent event) {
        kafkaTemplate.send(NOTIFICATION_TOPIC, String.valueOf(event.getUserId()), event);
    }
}
