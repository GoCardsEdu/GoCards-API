package pl.gocards.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.gocards.api.entity.Card;
import pl.gocards.api.exception.DeckNotFoundException;
import pl.gocards.api.jooq.CardRepository;
import pl.gocards.api.jooq.DeckRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CardService {

    private final DeckRepository deckRepository;

    private final CardRepository cardRepository;

    public CardService(
            DeckRepository deckRepository,
            CardRepository cardRepository
    ) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
    }

    @Transactional
    public void update(
            String deckId,
            List<Card> cards
    ) {
       this.update(deckId, cards, null);
    }

    @Transactional
    void update(
            String deckId,
            List<Card> cards,
            Runnable finalizeFn
    ) {
        LocalDateTime now = LocalDateTime.now();
        var exists = deckRepository.existsWithUpdateLock(deckId);
        if (!exists) {
            throw new DeckNotFoundException(deckId);
        }

        var countModifications = cardRepository.update(deckId, cards, now, finalizeFn);
        var isModified = countModifications > 0;
        if (isModified) {
            deckRepository.updateUpdatedAt(deckId, now);
        }
    }
}
