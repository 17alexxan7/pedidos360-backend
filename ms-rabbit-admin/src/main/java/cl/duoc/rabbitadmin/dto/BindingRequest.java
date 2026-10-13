package cl.duoc.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BindingRequest(
        @NotBlank(message = "La cola es obligatoria")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Nombre de cola no valido")
        String queue,

        @NotBlank(message = "El exchange es obligatorio")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Nombre de exchange no valido")
        String exchange,

        @Pattern(regexp = "^[a-zA-Z0-9._*#-]*$", message = "Routing key no valida")
        String routingKey
) {
}
