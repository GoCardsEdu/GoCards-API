package pl.gocards.api.jooq;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import pl.gocards.api.entity.BackType;
import pl.gocards.api.jooq.tables.records.CardBackRecord;

import java.util.*;
import java.util.stream.Collectors;

import static pl.gocards.api.jooq.Tables.CARD;
import static pl.gocards.api.jooq.Tables.CARD_BACK;

@Repository
public class CardBackRepository {

    private final DSLContext dslContext;

    public CardBackRepository(
            DSLContext dslContext
    ) {
        this.dslContext = dslContext;
    }

    public void batchCreate(List<Map.Entry<String, Map<BackType, String>>> cardBackData) {
        var creations = cardBackData.stream()
                .flatMap(entry -> entry.getValue().entrySet().stream()
                        .map(backTypeEntry -> {
                            var record = dslContext.newRecord(CARD_BACK);
                            record.setCardId(entry.getKey());
                            record.setName(backTypeEntry.getKey().toString());
                            record.setContent(backTypeEntry.getValue());
                            return record;
                        }))
                .collect(Collectors.toList());

        dslContext.batchInsert(creations).execute();
    }

    public void batchUpdate(List<Map.Entry<String, Map<BackType, String>>> cardBackData) {
        var updates = new LinkedList<CardBackRecord>();
        var deletions = new LinkedList<CardBackRecord>();
        var creations = new LinkedList<Map.Entry<String, Map<BackType, String>>>();
        var cardsId = cardBackData.stream().map(Map.Entry::getKey).collect(Collectors.toSet());

        var groupedRecords = dslContext
                .selectFrom(CARD_BACK)
                .where(CARD_BACK.CARD_ID.in(cardsId))
                .fetchGroups(CARD_BACK.CARD_ID);

        cardBackData.forEach(record -> {
            var cardId = record.getKey();
            var backData = record.getValue();
            var existingRecords = groupedRecords.get(cardId);

            updates.addAll(filterUpdates(existingRecords, backData));
            deletions.addAll(filterDeletions(existingRecords, backData));
            creations.add(Map.entry(cardId, filterCreations(existingRecords, backData)));

        });

        batchCreate(creations);
        dslContext.batchUpdate(updates).execute();
        dslContext.batchDelete(deletions).execute();
    }

    private List<CardBackRecord> filterUpdates(
            List<CardBackRecord> backRecords,
            Map<BackType, String> backData
    ) {
        return backRecords.stream()
                .filter(record -> backData.containsKey(BackType.valueOf(record.getName())))
                .peek(record -> record.setContent(backData.get(BackType.valueOf(record.getName()))))
                .collect(Collectors.toList());
    }

    private List<CardBackRecord> filterDeletions(
            List<CardBackRecord> existingRecords,
            Map<BackType, String> backData
    ) {
        return existingRecords.stream()
                .filter(record -> !backData.containsKey(BackType.valueOf(record.getName())))
                .collect(Collectors.toList());
    }

    private Map<BackType, String> filterCreations(
            List<CardBackRecord> existingRecords,
            Map<BackType, String> backData
    ) {
        Set<BackType> existingTypes = existingRecords
                .stream()
                .map(record -> BackType.valueOf(record.getName()))
                .collect(Collectors.toSet());

        return backData.entrySet().stream()
                .filter(entry -> !existingTypes.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
