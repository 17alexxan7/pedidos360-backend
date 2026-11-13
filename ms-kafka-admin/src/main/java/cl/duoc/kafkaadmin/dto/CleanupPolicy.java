package cl.duoc.kafkaadmin.dto;

public enum CleanupPolicy {
    DELETE("delete"),
    COMPACT("compact");

    private final String value;

    CleanupPolicy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}