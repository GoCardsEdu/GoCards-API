package pl.gocards.api.entity;

import java.util.Objects;

public record CardBack(
        String definition
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardBack cardBack = (CardBack) o;
        return Objects.equals(definition, cardBack.definition);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(definition);
    }
}