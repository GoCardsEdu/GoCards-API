package pl.gocards.api.controller.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pl.gocards.api.entity.Deck;

public record DeckRequest(
        @NotBlank
        @Size(max = 50)
        String name
) {

    public static Deck toDomain(
            String deckId,
            DeckRequest deck
    ) {
        return new Deck(
                deckId,
                deck.name(),
                null,
                null
        );
    }
}