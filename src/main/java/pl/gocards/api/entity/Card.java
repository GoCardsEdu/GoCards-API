package pl.gocards.api.entity;

import java.sql.Timestamp;
import java.util.Objects;

public record Card(
        String id,
        Integer ordinal,
        CardFront front,
        CardBack back,
        Timestamp createdAt,
        Timestamp updatedAt
) {
    public Card(String id, Integer ordinal) {
        this(id, ordinal, null, null, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return Objects.equals(id, card.id)
                && Objects.equals(ordinal, card.ordinal)
                && Objects.equals(front, card.front)
                && Objects.equals(back, card.back);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ordinal, front, back, createdAt, updatedAt);
    }
}

