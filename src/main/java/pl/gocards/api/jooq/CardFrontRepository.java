package pl.gocards.api.jooq;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import pl.gocards.api.entity.Deck;
import pl.gocards.api.entity.FrontType;
import pl.gocards.api.jooq.tables.records.CardFrontRecord;
import pl.gocards.api.jooq.tables.records.DeckRecord;

import java.util.*;

import static pl.gocards.api.jooq.Tables.CARD_FRONT;
import static pl.gocards.api.jooq.Tables.DECK;

@Repository
public class CardFrontRepository {

    @Autowired
    private DSLContext dslContext;

    public void batchCreate(ArrayList<Map.Entry<String, Map<FrontType, String>>> records) {
        records.forEach(it -> create(it.getKey(), it.getValue()));
    }

    public void create(
            String cardId,
            Map<FrontType, String> map
    ) {
        map.forEach((type, content) -> {
            CardFrontRecord record = dslContext.newRecord(CARD_FRONT);
            record.setCardId(cardId);
            record.setName(type.toString());
            record.setContent(content);
            record.insert();
        });
    }

    public void batchUpdate(List<Map.Entry<String, Map<FrontType, String>>> records) {
        records.forEach(it -> update(it.getKey(), it.getValue()));
    }

    public void update(
            String cardId,
            Map<FrontType, String> orginalMap
    ) {
        var existingFronts = dslContext
                .selectFrom(CARD_FRONT)
                .where(CARD_FRONT.CARD_ID.eq(cardId))
                .fetchInto(CardFrontRecord.class);
        var map = new HashMap<>(orginalMap);

        List<CardFrontRecord> updates = new ArrayList<>();
        List<CardFrontRecord> deletions = new ArrayList<>();

        existingFronts.forEach(existing -> {
            FrontType type = FrontType.valueOf(existing.getName());
            if (map.containsKey(type)) {
                existing.setContent(map.remove(type));
                updates.add(existing);
            } else {
                deletions.add(existing);
            }
        });

        if (!updates.isEmpty()) {
            dslContext.batchUpdate(updates).execute();
        }

        if (!deletions.isEmpty()) {
            dslContext.batchDelete(deletions).execute();
        }

        create(cardId, map);
    }

}
