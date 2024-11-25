package pl.gocards.api.jooq;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.UpdateConditionStep;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import pl.gocards.api.entity.BackType;
import pl.gocards.api.entity.FrontType;
import pl.gocards.api.jooq.tables.records.CardBackRecord;
import pl.gocards.api.jooq.tables.records.CardFrontRecord;
import pl.gocards.api.jooq.tables.records.CardRecord;
import pl.gocards.api.entity.CardBack;
import pl.gocards.api.entity.CardFront;
import pl.gocards.api.entity.Card;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pl.gocards.api.jooq.Tables.*;

@Repository
public class CardRepository {

    private final DSLContext dslContext;
    private final CardFrontRepository cardFrontRepository;
    private final CardBackRepository cardBackRepository;


    public CardRepository(
            DSLContext dslContext,
            CardFrontRepository cardFrontRepository,
            CardBackRepository cardBackRepository
    ) {
        this.dslContext = dslContext;
        this.cardFrontRepository = cardFrontRepository;
        this.cardBackRepository = cardBackRepository;
    }

    public Stream<Card> findByDeck(String deckId) {
        var groupedRecords = dslContext
                .select()
                .from(CARD)
                .leftOuterJoin(CARD_FRONT).on(CARD_FRONT.CARD_ID.eq(CARD.ID))
                .leftOuterJoin(CARD_BACK).on(CARD_BACK.CARD_ID.eq(CARD.ID))
                .where(CARD.DECK_ID.equal(deckId))
                .orderBy(CARD.ORDINAL)
                .fetchGroups(CARD.ID);

        return groupedRecords
                .entrySet()
                .stream()
                .map(entry -> buildCard(entry.getKey(), entry.getValue()));
    }

    private Card buildCard(String cardId, List<Record> records) {
        var card = records.stream()
                .map(record -> record.into(CARD))
                .findFirst()
                .orElse(null);

        assert card != null;

        return new Card(
                cardId,
                card.getOrdinal(),
                new CardFront(extractTerm(records)),
                new CardBack(extractDefinition(records)),
                card.get(CARD.CREATED_AT, Timestamp.class),
                card.get(CARD.UPDATED_AT, Timestamp.class)
        );
    }

    private String extractTerm(List<Record> records) {
        return records.stream()
                .map(record -> record.into(CARD_FRONT))
                .filter(record -> Objects.equals(record.getName(), FrontType.term.name()))
                .findFirst()
                .map(CardFrontRecord::getContent)
                .orElse(null);
    }

    private String extractDefinition(List<Record> records) {
        return records.stream()
                .map(record -> record.into(CARD_BACK))
                .filter(record -> Objects.equals(record.getName(), BackType.definition.name()))
                .findFirst()
                .map(CardBackRecord::getContent)
                .orElse(null);
    }

    public Set<String> findIdsByDeck(String deckId) {
        return dslContext
                .select(CARD.ID)
                .from(CARD)
                .where(CARD.DECK_ID.equal(deckId))
                .stream()
                .map(it -> it.into(CARD.ID))
                .map(it -> it.get(CARD.ID))
                .collect(Collectors.toUnmodifiableSet());
    }

    private void performCreate(
            String deckId,
            List<Card> cards,
            LocalDateTime now
    ) {
        var cardRecords = new ArrayList<CardRecord>(cards.size());
        var frontRecords = new ArrayList<Map.Entry<String, Map<FrontType, String>>>(cards.size());
        var backRecords = new ArrayList<Map.Entry<String, Map<BackType, String>>>(cards.size());

        for (Card card : cards) {
            var cardId = card.id();
            cardRecords.add(createCardRecord(deckId, card, now));
            addFrontRecord(frontRecords, cardId, card.front());
            addBackRecord(backRecords, cardId, card.back());
        }

        if (!cardRecords.isEmpty()) {
            dslContext.batchInsert(cardRecords).execute();
        }
        if (!frontRecords.isEmpty()) {
            cardFrontRepository.batchCreate(frontRecords);
        }
        if (!backRecords.isEmpty()) {
            cardBackRepository.batchCreate(backRecords);
        }
    }

    private void addFrontRecord(
            List<Map.Entry<String, Map<FrontType, String>>> frontRecords,
            String cardId,
            CardFront cardFront
    ) {
        if (cardFront != null && cardFront.term() != null) {
            frontRecords.add(Map.entry(
                    cardId,
                    Map.of(FrontType.term, cardFront.term())
            ));
        }
    }

    private void addBackRecord(
            List<Map.Entry<String, Map<BackType, String>>> backRecords,
            String cardId,
            CardBack cardBack
    ) {
        if (cardBack != null && cardBack.definition() != null) {
            backRecords.add(Map.entry(
                    cardId,
                    Map.of(BackType.definition, cardBack.definition())
            ));
        }
    }

    private CardRecord createCardRecord(
            String deckId,
            Card card,
            LocalDateTime now
    ) {
        CardRecord record = dslContext.newRecord(CARD);
        record.setId(card.id());
        record.setDeckId(deckId);
        record.setOrdinal(card.ordinal());
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return record;
    }

    public int update(
            String deckId,
            List<Card> cards,
            LocalDateTime now,
            Runnable finalizeFn
    ) {
        var existingCardRecords = fetchCardRecords(deckId);
        var existingCardIds = extractIds(existingCardRecords);
        var existingCards = findByDeck(deckId).toList();

        var updates = filterUpdates(cards, existingCardIds, existingCards);
        var creations = filterCreations(cards, existingCardIds);
        var deletions = filterDeletions(existingCardRecords, cards);

        performDelete(deletions);
        performUpdate(updates, now);
        performCreate(deckId, creations, now);
        if (finalizeFn != null) finalizeFn.run();

        return updates.size() + creations.size() + deletions.size();
    }

    private List<CardRecord> fetchCardRecords(String deckId) {
        return dslContext.select()
                .from(CARD)
                .where(CARD.DECK_ID.eq(deckId))
                .fetch()
                .into(CardRecord.class);
    }

    private Set<String> extractIds(List<CardRecord> records) {
        return records.stream()
                .map(CardRecord::getId)
                .collect(Collectors.toSet());
    }

    private List<Card> filterUpdates(
            List<Card> updatedCards,
            Set<String> existingCardIds,
            List<Card> existingCards
    ) {
        return updatedCards.stream()
                .filter(card -> existingCardIds.contains(card.id()))
                .filter(updatedCard -> {

                    var existingCard = existingCards
                            .stream()
                            .filter(card -> card.id().equals(updatedCard.id()))
                            .findFirst()
                            .orElseThrow();

                    return !updatedCard.equals(existingCard);
                })
                .toList();
    }

    private List<Card> filterCreations(List<Card> cards, Set<String> existingCardIds) {
        return cards.stream()
                .filter(card -> !existingCardIds.contains(card.id()))
                .toList();
    }

    private List<CardRecord> filterDeletions(List<CardRecord> existingCards, List<Card> cards) {
        var cardsIds = cards.stream()
                .map(Card::id)
                .collect(Collectors.toSet());

        return existingCards.stream()
                .filter(card -> !cardsIds.contains(card.getId()))
                .toList();
    }

    private void performUpdate(List<Card> cards, LocalDateTime now) {
        var frontRecords = new ArrayList<Map.Entry<String, Map<FrontType, String>>>(cards.size());
        var backRecords = new ArrayList<Map.Entry<String, Map<BackType, String>>>(cards.size());
        List<UpdateConditionStep<CardRecord>> cardUpdates = new ArrayList<>(cards.size());

        for (var card : cards) {
            addFrontRecord(frontRecords, card.id(), card.front());
            addBackRecord(backRecords, card.id(), card.back());
            cardUpdates.add(updateUpdatedAt(card.id(), now));
        }

        if (!cardUpdates.isEmpty()) {
            dslContext.batch(cardUpdates).execute();
        }

        if (!frontRecords.isEmpty()) {
            cardFrontRepository.batchUpdate(frontRecords);
        }
        if (!backRecords.isEmpty()) {
            cardBackRepository.batchUpdate(backRecords);
        }
    }

    private UpdateConditionStep<CardRecord> updateUpdatedAt(String cardId, LocalDateTime updatedAt) {
        return dslContext.update(CARD)
                .set(CARD.UPDATED_AT, updatedAt)
                .where(CARD.ID.eq(cardId));
    }

    private void performDelete(List<CardRecord> cards) {
        if (!cards.isEmpty()) {
            dslContext.batchDelete(cards).execute();
        }
    }
}
