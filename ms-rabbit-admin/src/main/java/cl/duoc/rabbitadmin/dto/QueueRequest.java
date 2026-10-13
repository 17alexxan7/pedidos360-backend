package cl.duoc.rabbitadmin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record QueueRequest(
        @NotBlank(message = "El nombre de la cola es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Solo se permiten letras, numeros, punto, guion y guion bajo")
        String name,

        Boolean durable,

        @Pattern(regexp = "^$|^[a-zA-Z0-9._-]+$", message = "El nombre del dead letter exchange no es valido")
        String deadLetterExchange,

        @Pattern(regexp = "^$|^[a-zA-Z0-9._-]+$", message = "La routing key del dead letter no es valida")
        String deadLetterRoutingKey,

        @Min(value = 1, message = "El TTL debe ser mayor a 0 ms")
        Long messageTtlMs
) {
}
