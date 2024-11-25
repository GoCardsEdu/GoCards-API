package pl.gocards.api.controller.model;

import lombok.Builder;
import pl.gocards.api.entity.CardBack;
import pl.gocards.api.entity.CardFront;
import pl.gocards.api.entity.Card;

import java.sql.Timestamp;
import java.util.Map;

@Builder
public record UpdateCardResponse(
        String id,
        String clientId,
        int ordinal,
        CardFront front,
        CardBack back,
        Timestamp createdAt,
        Timestamp updatedAt
) {

    public static UpdateCardResponse fromDomain(
            Card card,
            Map<String, String> cardIdMapToClientId
    ) {
        return new UpdateCardResponse(
                card.id(),
                cardIdMapToClientId.get(card.id()),
                card.ordinal(),
                card.front(),
                card.back(),
                card.createdAt(),
                card.updatedAt()
        );
    }
}

