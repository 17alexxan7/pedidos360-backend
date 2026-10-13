package cl.duoc.rabbitadmin.dto;

public record QueueInfoResponse(String name, long messageCount, int consumerCount) {
}

