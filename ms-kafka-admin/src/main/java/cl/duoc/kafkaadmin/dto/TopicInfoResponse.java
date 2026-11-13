package cl.duoc.kafkaadmin.dto;

public record TopicInfoResponse(String name, int partitions, int replicationFactor) {
}