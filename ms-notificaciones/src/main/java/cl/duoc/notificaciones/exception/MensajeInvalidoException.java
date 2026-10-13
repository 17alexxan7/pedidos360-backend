package cl.duoc.notificaciones.exception;

/**
 * Error irrecuperable: el mensaje es invalido y reintentar no sirve.
 * El listener lo envia directo a la DLQ (basicNack sin reencolar).
 */
public class MensajeInvalidoException extends RuntimeException {

    public MensajeInvalidoException(String message) {
        super(message);
    }
}