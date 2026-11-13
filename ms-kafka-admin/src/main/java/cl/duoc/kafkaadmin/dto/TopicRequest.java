package cl.duoc.kafkaadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TopicRequest(
        @NotBlank(message = "El nombre del topico es obligatorio")
        @Size(max = 249, message = "El nombre no puede superar 249 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Solo se permiten letras, numeros, punto, guion y guion bajo")
        String name,

        @NotNull(message = "Las particiones son obligatorias")
        @Min(value = 1, message = "Debe haber al menos 1 particion")
        Integer partitions,

        @NotNull(message = "El factor de replicacion es obligatorio")
        @Min(value = 1, message = "El factor de replicacion debe ser al menos 1")
        Short replicationFactor,

        @NotNull(message = "La politica de limpieza es obligatoria (DELETE o COMPACT)")
        CleanupPolicy cleanupPolicy,

        @Positive(message = "La retencion debe ser mayor a 0 ms")
        Long retentionMs) {
}