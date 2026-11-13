package cl.duoc.kafkaadmin.dto;

import java.util.List;

public record ErrorResponse(int status, String message, List<String> details) {
}