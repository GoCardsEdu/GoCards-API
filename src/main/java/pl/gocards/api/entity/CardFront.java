package pl.gocards.api.entity;

import java.util.Objects;

public record CardFront(
        String term
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardFront cardFront = (CardFront) o;
        return Objects.equals(term, cardFront.term);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(term);
    }
}