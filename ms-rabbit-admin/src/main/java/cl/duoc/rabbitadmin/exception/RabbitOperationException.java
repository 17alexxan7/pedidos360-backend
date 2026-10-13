package cl.duoc.rabbitadmin.exception;

public class RabbitOperationException extends RuntimeException {
    public RabbitOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
