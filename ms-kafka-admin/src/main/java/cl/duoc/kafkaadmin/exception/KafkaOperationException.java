package cl.duoc.kafkaadmin.exception;

public class KafkaOperationException extends RuntimeException {

    public KafkaOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}