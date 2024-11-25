package pl.gocards.api.jooq;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import pl.gocards.api.entity.Deck;
import pl.gocards.api.exception.DeckNotFoundException;
import pl.gocards.api.jooq.tables.records.DeckRecord;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static pl.gocards.api.jooq.Tables.*;

@Repository
public class DeckRepository {

    private final DSLContext dslContext;

    public DeckRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public boolean existsWithUpdateLock(String id) {
        return dslContext.fetchExists(
                dslContext.selectFrom(DECK)
                        .where(DECK.ID.eq(id))
                .forUpdate()
        );
    }

    public Deck find(String deckId) {
        DeckRecord record = dslContext.fetchOne(DECK, DECK.ID.eq(deckId));

        if (record == null) {
            return null;
        } else {
            return new Deck(
                    record.getId(),
                    record.getName(),
                    record.get(DECK.CREATED_AT, Timestamp.class),
                    record.get(DECK.UPDATED_AT, Timestamp.class)
            );
        }
    }

    public void save(Deck deck, LocalDateTime now) {
        DeckRecord record = dslContext.newRecord(DECK);
        record.setId(deck.id());
        record.setName(deck.name());
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        record.store();
    }

    public void update(Deck deck, LocalDateTime updatedAt) {
        DeckRecord record = dslContext.fetchOne(DECK, DECK.ID.eq(deck.id()));
        if (record == null) {
            throw new DeckNotFoundException(deck.id());
        }
        record.setName(deck.name());
        record.setUpdatedAt(updatedAt);
        record.update();
    }

    public void updateUpdatedAt(String deckId, LocalDateTime updatedAt) {
        DeckRecord record = dslContext.fetchOne(DECK, DECK.ID.eq(deckId));
        if (record == null) {
            throw new DeckNotFoundException(deckId);
        }
        record.setUpdatedAt(updatedAt);
        record.update();
    }
}
