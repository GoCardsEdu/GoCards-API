package pl.gocards.api.controller;

import org.springframework.stereotype.Service;
import pl.gocards.api.controller.model.DeckRequest;
import pl.gocards.api.entity.Deck;
import pl.gocards.api.jooq.DeckRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DeckControllerFacade {

    private final DeckRepository deckRepository;

    public DeckControllerFacade(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;
    }

    public Deck find(String deckId) {
        return deckRepository.find(deckId);
    }

    public Deck createDeck(DeckRequest request) {
        var deckId = UUID.randomUUID().toString();
        var deck = DeckRequest.toDomain(deckId, request);
        var now = LocalDateTime.now();

        deckRepository.save(deck, now);
        return deckRepository.find(deckId);
    }

    public Deck updateDeck(String id, DeckRequest request) {
        var deck = DeckRequest.toDomain(id, request);
        deckRepository.update(deck, LocalDateTime.now());
        return deckRepository.find(id);
    }
}
