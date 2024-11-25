package pl.gocards.api.controller;

import org.springframework.stereotype.Service;
import pl.gocards.api.controller.model.UpdateCardRequest;
import pl.gocards.api.controller.model.UpdateCardResponse;
import pl.gocards.api.entity.Card;
import pl.gocards.api.exception.CardNotFoundException;
import pl.gocards.api.exception.DeckNotFoundException;
import pl.gocards.api.jooq.CardRepository;
import pl.gocards.api.jooq.DeckRepository;
import pl.gocards.api.service.CardService;
import reactor.core.publisher.Flux;

import java.util.*;

@Service
public class CardControllerFacade {

    private final DeckRepository deckRepository;

    private final CardRepository cardRepository;

    private final CardService cardService;

    public CardControllerFacade(
            DeckRepository deckRepository,
            CardRepository cardRepository,
            CardService cardService
    ) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.cardService = cardService;
    }

    public Flux<Card> fetchCards(String deckId) {
        validateDeckExists(deckId);
        return Flux.fromStream(cardRepository.findByDeck(deckId));
    }

    public Flux<UpdateCardResponse> updateCards(String deckId, List<UpdateCardRequest> cards) {
        validateDeckExists(deckId);
        validateCardIdsExist(deckId, cards);

        Map<String, String> cardIdMapToClientId = new HashMap<>();
        cardService.update(deckId, UpdateCardRequest.toDomain(cards, cardIdMapToClientId));

        return Flux.fromStream(cardRepository.findByDeck(deckId))
                .map(it -> UpdateCardResponse.fromDomain(it, cardIdMapToClientId));
    }

    private void validateDeckExists(String deckId) {
        if (deckRepository.find(deckId) == null) {
            throw new DeckNotFoundException(deckId);
        }
    }

    private void validateCardIdsExist(String deckId, List<UpdateCardRequest> updateCardRequests) {
        Set<String> existingCardIds = cardRepository.findIdsByDeck(deckId);

        List<String> invalidCardIds = updateCardRequests.stream()
                .map(UpdateCardRequest::id)
                .filter(Objects::nonNull)
                .filter(cardId -> !existingCardIds.contains(cardId))
                .toList();

        if (!invalidCardIds.isEmpty()) {
            throw new CardNotFoundException("The following card IDs were not found: " + invalidCardIds);
        }
    }
}
