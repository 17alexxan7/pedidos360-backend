package cl.duoc.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ExchangeRequest(
        @NotBlank(message = "El nombre del exchange es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Solo se permiten letras, numeros, punto, guion y guion bajo")
        String name,

        @NotNull(message = "El tipo es obligatorio (DIRECT, TOPIC o FANOUT)")
        ExchangeType type,

        Boolean durable
) {
}
