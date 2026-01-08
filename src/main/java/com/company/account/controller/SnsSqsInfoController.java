package com.company.account.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.Topic;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SNS/SQS 정보 조회 및 테스트 컨트롤러
 * 프로파일 제한 없이 항상 활성화 (정보 조회용)
 */
@Slf4j
@RestController
@RequestMapping("/api/test/sns-sqs")
@RequiredArgsConstructor
@ConditionalOnBean({SnsClient.class, SqsClient.class})
public class SnsSqsInfoController {

    private final SnsClient snsClient;
    private final SqsClient sqsClient;

    @Value("${aws.sns.project-name:passit}")
    private String projectName;

    @Value("${aws.sns.environment:dev}")
    private String environment;

    // ============================================
    // SNS Topic + Subscription 정보 조회
    // ============================================

    /**
     * 모든 SNS Topic 목록 조회
     */
    @GetMapping("/topics")
    public ResponseEntity<?> listTopics() {
        log.info("📋 Listing SNS Topics");

        try {
            ListTopicsResponse response = snsClient.listTopics();
            List<Map<String, Object>> topics = new ArrayList<>();

            for (Topic topic : response.topics()) {
                String topicArn = topic.topicArn();
                Map<String, Object> topicInfo = new HashMap<>();
                topicInfo.put("arn", topicArn);
                topicInfo.put("name", extractTopicName(topicArn));

                // Topic 속성 조회
                try {
                    GetTopicAttributesResponse attributes = snsClient.getTopicAttributes(
                            GetTopicAttributesRequest.builder()
                                    .topicArn(topicArn)
                                    .build());

                    topicInfo.put("displayName", attributes.attributes().get("DisplayName"));
                    topicInfo.put("subscriptionsConfirmed", attributes.attributes().get("SubscriptionsConfirmed"));
                    topicInfo.put("subscriptionsPending", attributes.attributes().get("SubscriptionsPending"));
                    topicInfo.put("subscriptionsDeleted", attributes.attributes().get("SubscriptionsDeleted"));
                } catch (Exception e) {
                    log.warn("Failed to get topic attributes for: {}", topicArn, e);
                }

                topics.add(topicInfo);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "topics", topics,
                    "count", topics.size()
            ));
        } catch (Exception e) {
            log.error("Failed to list SNS topics", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "SNS Topic 목록 조회 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 특정 Topic의 Subscription 목록 조회
     */
    @GetMapping("/topics/{topicName}/subscriptions")
    public ResponseEntity<?> listSubscriptions(@PathVariable String topicName) {
        log.info("📋 Listing subscriptions for topic: {}", topicName);

        try {
            String topicArn = buildTopicArn(topicName);
            
            ListSubscriptionsByTopicResponse response = snsClient.listSubscriptionsByTopic(
                    ListSubscriptionsByTopicRequest.builder()
                            .topicArn(topicArn)
                            .build());

            List<Map<String, Object>> subscriptions = response.subscriptions().stream()
                    .map(sub -> {
                        Map<String, Object> subInfo = new HashMap<>();
                        subInfo.put("subscriptionArn", sub.subscriptionArn());
                        subInfo.put("protocol", sub.protocol());
                        subInfo.put("endpoint", sub.endpoint());
                        subInfo.put("owner", sub.owner());
                        return subInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "topicArn", topicArn,
                    "subscriptions", subscriptions,
                    "count", subscriptions.size()
            ));
        } catch (Exception e) {
            log.error("Failed to list subscriptions for topic: {}", topicName, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Subscription 목록 조회 실패: " + e.getMessage()
            ));
        }
    }

    // ============================================
    // SQS Queue + Metrics 정보 조회
    // ============================================

    /**
     * 모든 SQS Queue 목록 조회
     */
    @GetMapping("/queues")
    public ResponseEntity<?> listQueues() {
        log.info("📋 Listing SQS Queues");

        try {
            ListQueuesResponse response = sqsClient.listQueues();
            List<Map<String, Object>> queues = new ArrayList<>();

            for (String queueUrl : response.queueUrls()) {
                Map<String, Object> queueInfo = new HashMap<>();
                queueInfo.put("url", queueUrl);
                queueInfo.put("name", extractQueueName(queueUrl));

                // Queue 속성 조회
                try {
                    GetQueueAttributesResponse attributes = sqsClient.getQueueAttributes(
                            GetQueueAttributesRequest.builder()
                                    .queueUrl(queueUrl)
                                    .attributeNames(
                                            QueueAttributeName.ALL,
                                            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                                            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
                                            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED,
                                            QueueAttributeName.CREATED_TIMESTAMP,
                                            QueueAttributeName.LAST_MODIFIED_TIMESTAMP,
                                            QueueAttributeName.VISIBILITY_TIMEOUT,
                                            QueueAttributeName.MESSAGE_RETENTION_PERIOD
                                    )
                                    .build());

                    Map<QueueAttributeName, String> attrs = attributes.attributes();
                    queueInfo.put("approximateNumberOfMessages", attrs.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES));
                    queueInfo.put("approximateNumberOfMessagesNotVisible", attrs.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE));
                    queueInfo.put("approximateNumberOfMessagesDelayed", attrs.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED));
                    queueInfo.put("visibilityTimeout", attrs.get(QueueAttributeName.VISIBILITY_TIMEOUT));
                    queueInfo.put("messageRetentionPeriod", attrs.get(QueueAttributeName.MESSAGE_RETENTION_PERIOD));
                    queueInfo.put("createdTimestamp", attrs.get(QueueAttributeName.CREATED_TIMESTAMP));
                    queueInfo.put("lastModifiedTimestamp", attrs.get(QueueAttributeName.LAST_MODIFIED_TIMESTAMP));
                    String redrivePolicy = attrs.get(QueueAttributeName.REDRIVE_POLICY);
                    queueInfo.put("maxReceiveCount", redrivePolicy != null ? 
                            extractMaxReceiveCount(redrivePolicy) : null);
                } catch (Exception e) {
                    log.warn("Failed to get queue attributes for: {}", queueUrl, e);
                }

                queues.add(queueInfo);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "queues", queues,
                    "count", queues.size()
            ));
        } catch (Exception e) {
            log.error("Failed to list SQS queues", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "SQS Queue 목록 조회 실패: " + e.getMessage()
            ));
        }
    }

    /**
     * 특정 Queue의 메시지 조회 (테스트용)
     */
    @GetMapping("/queues/{queueName}/messages")
    public ResponseEntity<?> receiveMessages(
            @PathVariable String queueName,
            @RequestParam(defaultValue = "10") int maxMessages) {
        log.info("📥 Receiving messages from queue: {} (max: {})", queueName, maxMessages);

        try {
            String queueUrl = buildQueueUrl(queueName);
            
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(Math.min(maxMessages, 10))
                    .waitTimeSeconds(0) // Short polling
                    .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(request);

            List<Map<String, Object>> messages = response.messages().stream()
                    .map(msg -> {
                        Map<String, Object> msgInfo = new HashMap<>();
                        msgInfo.put("messageId", msg.messageId());
                        msgInfo.put("receiptHandle", msg.receiptHandle());
                        msgInfo.put("body", msg.body());
                        msgInfo.put("attributes", msg.attributes());
                        msgInfo.put("messageAttributes", msg.messageAttributes());
                        return msgInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "queueUrl", queueUrl,
                    "messages", messages,
                    "count", messages.size()
            ));
        } catch (Exception e) {
            log.error("Failed to receive messages from queue: {}", queueName, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "메시지 조회 실패: " + e.getMessage()
            ));
        }
    }

    // ============================================
    // 이벤트 발행
    // ============================================

    /**
     * SNS Topic으로 이벤트 발행
     */
    @PostMapping("/publish")
    public ResponseEntity<?> publishEvent(
            @RequestParam String topicName,
            @RequestParam String eventType,
            @RequestBody(required = false) Map<String, Object> eventData) {
        log.info("📤 Publishing event to topic: {}, eventType: {}", topicName, eventType);

        try {
            String topicArn = buildTopicArn(topicName);

            // 이벤트 메시지 구성
            Map<String, Object> eventMessage = new HashMap<>();
            eventMessage.put("eventType", eventType);
            eventMessage.put("source", "service-account");
            eventMessage.put("timestamp", Instant.now().toString());
            eventMessage.put("correlationId", UUID.randomUUID().toString());
            if (eventData != null) {
                eventMessage.put("data", eventData);
            }

            String message = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(eventMessage);

            // SNS로 발행
            PublishResponse response = snsClient.publish(
                    PublishRequest.builder()
                            .topicArn(topicArn)
                            .message(message)
                            .messageAttributes(Map.of(
                                    "eventType", MessageAttributeValue.builder()
                                            .dataType("String")
                                            .stringValue(eventType)
                                            .build()
                            ))
                            .build());

            log.info("✅ Event published successfully. MessageId: {}", response.messageId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "messageId", response.messageId(),
                    "topicArn", topicArn,
                    "eventType", eventType,
                    "eventMessage", eventMessage
            ));
        } catch (Exception e) {
            log.error("Failed to publish event to topic: {}", topicName, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "이벤트 발행 실패: " + e.getMessage()
            ));
        }
    }

    // ============================================
    // 유틸리티 메서드
    // ============================================

    private String buildTopicArn(String topicName) {
        // topicName이 이미 ARN인 경우 그대로 반환
        if (topicName.startsWith("arn:aws:sns:")) {
            return topicName;
        }
        // topicName만 있는 경우 ARN 생성
        String fullName = topicName.contains("-") ? topicName : 
                String.format("%s-%s-%s", projectName, environment, topicName);
        return String.format("arn:aws:sns:ap-northeast-2:727646470302:%s", fullName);
    }

    private String buildQueueUrl(String queueName) {
        // queueName이 이미 URL인 경우 그대로 반환
        if (queueName.startsWith("https://")) {
            return queueName;
        }
        // queueName만 있는 경우 URL 생성
        String fullName = queueName.contains("-") ? queueName : 
                String.format("%s-%s-%s", projectName, environment, queueName);
        return String.format("https://sqs.ap-northeast-2.amazonaws.com/727646470302/%s", fullName);
    }

    private String extractTopicName(String topicArn) {
        return topicArn.substring(topicArn.lastIndexOf(':') + 1);
    }

    private String extractQueueName(String queueUrl) {
        return queueUrl.substring(queueUrl.lastIndexOf('/') + 1);
    }

    @SuppressWarnings("unchecked")
    private String extractMaxReceiveCount(String redrivePolicy) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> policy = mapper.readValue(redrivePolicy, Map.class);
            return String.valueOf(policy.get("maxReceiveCount"));
        } catch (Exception e) {
            return null;
        }
    }
}

