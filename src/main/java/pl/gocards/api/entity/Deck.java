package pl.gocards.api.entity;

import java.sql.Timestamp;

public record Deck(
        String id,
        String name,
        Timestamp createdAt,
        Timestamp updatedAt
) {
    public Deck(String id, String name) {
        this(id, name, null, null);
    }
}
