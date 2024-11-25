package pl.gocards.api.controller.model;

import pl.gocards.api.entity.Deck;

import java.sql.Timestamp;

public record DeckResponse(
        String id,
        String name,
        Timestamp createdAt,
        Timestamp updatedAt
) {

    public static DeckResponse fromDomain(
            Deck deck
    ) {
        return new DeckResponse(
                deck.id(),
                deck.name(),
                deck.createdAt(),
                deck.updatedAt()
        );
    }

}
