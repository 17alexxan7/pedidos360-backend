package cl.duoc.rabbitadmin.dto;

import java.util.List;

public record ErrorResponse(int status, String message, List<String> details) {
}
