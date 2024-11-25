package pl.gocards.api.controller.model;

import lombok.Builder;
import pl.gocards.api.entity.Card;
import pl.gocards.api.entity.CardBack;
import pl.gocards.api.entity.CardFront;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Builder
public record UpdateCardRequest(
        String id,
        String clientId,
        CardFront front,
        CardBack back
) {

    public static List<Card> toDomain(
            List<UpdateCardRequest> cards,
            Map<String, String> cardIdMapToClientId
    ) {
        AtomicInteger ordinal = new AtomicInteger();
        return cards.stream()
                .map(it -> {
                    Card card = toDomain(it, ordinal.incrementAndGet());
                    cardIdMapToClientId.put(card.id(), it.clientId());
                    return card;
                })
                .toList();
    }

    private static Card toDomain(UpdateCardRequest card, int ordinal) {
        var cardId = card.id == null ? UUID.randomUUID().toString() : card.id;
        return new Card(
                cardId,
                ordinal,
                card.front(),
                card.back(),
                null,
                null
        );
    }
}

