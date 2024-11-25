package pl.gocards.api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pl.gocards.api.entity.Card;
import pl.gocards.api.entity.CardBack;
import pl.gocards.api.entity.CardFront;
import pl.gocards.api.entity.Deck;
import pl.gocards.api.jooq.CardRepository;
import pl.gocards.api.jooq.DeckRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CardServiceConcurrencyIntegrationTest {

    private final CardRepository cardRepository;

    private final DeckRepository deckRepository;

    private final CardService cardService;

    private final LocalDateTime now = LocalDateTime.now();

    @Autowired
    public CardServiceConcurrencyIntegrationTest(
            CardRepository cardRepository,
            DeckRepository deckRepository,
            CardService cardService
    ) {
        this.cardRepository = cardRepository;
        this.deckRepository = deckRepository;
        this.cardService = cardService;
    }

    /**
     * This only succeeds with SERIALIZABLE transaction isolation or when an explicit lock is applied before updating.
     * @see DeckRepository#existsWithUpdateLock(String)
     */
    @Test
    public void When_UpdatingCardsConcurrently_Should_Serializable(TestInfo testInfo) {
        var card1 = new Card(UUID.randomUUID().toString(), 1);
        var card2 = new Card(UUID.randomUUID().toString(), 2);
        var card3 = new Card(UUID.randomUUID().toString(), 1);

        var deck = createDeck(testInfo.getDisplayName());
        deckRepository.save(deck, now);


        CountDownLatch startLatch = new CountDownLatch(1);

        CompletableFuture firstUpdate =
                executeWithLatch(startLatch,
                        () -> cardService.update(deck.id(), List.of(card1, card2))
                );

        CompletableFuture secondUpdate =
                executeWithLatch(startLatch,
                        () -> cardService.update(deck.id(), List.of(card3))
                );

        firstUpdate.join();
        secondUpdate.join();

        assertThat(cardRepository.findByDeck(deck.id()).toList())
                .hasSizeBetween(1, 2)
                .satisfiesAnyOf(
                        list -> assertThat(list)
                                .extracting(Card::id)
                                .containsExactly(card1.id(), card2.id()),

                        list -> assertThat(list)
                                .extracting(Card::id)
                                .containsExactly(card3.id())
                );
    }

    private CompletableFuture executeWithLatch(
            CountDownLatch concurrentStartLatch,
            Runnable runnable
    ) {
        return executeAsync(() -> {
            concurrentStartLatch.countDown();
            concurrentStartLatch.await();
            runnable.run();
            return null;
        });
    }

    /**
     * Conclusion: Even without transactions, select queries retrieve only committed data.
     *
     * @see <a href="https://www.postgresql.org/docs/current/sql-set-transaction.html">
     * "In PostgreSQL READ UNCOMMITTED is treated as READ COMMITTED."
     * </a>
     */
    @Test
    public void When_ReadDeckDuringConcurrentUpdate_ShouldReadCommittedData(TestInfo testInfo) {
        var card1 = createNewCard(1, "term-1", "back-1");

        var card2 = createCard(card1.id(), 1, "updated-term-1", "updated-back-1");
        var card3 = createNewCard(2, "new-term-2", "new-back-2");

        var deck = createDeck(testInfo.getDisplayName());
        deckRepository.save(deck, now);
        cardService.update(deck.id(), List.of(card1));


        CountDownLatch readLatch = new CountDownLatch(2);
        CountDownLatch commitLatch = new CountDownLatch(1);

        CompletableFuture<List<Card>> readTransaction = executeAsync(() -> {
            // 2. Fetch the cards in a separate transaction.
            readLatch.countDown();
            readLatch.await();
            var cards = cardRepository.findByDeck(deck.id()).toList();
            commitLatch.countDown();
            return cards;
        });

        CompletableFuture<Object> updateTransaction = executeAsync(() -> {
            // 1. Begin an update transaction but delay committing.
            cardService.update(deck.id(), List.of(card2, card3), () -> {
                try {
                    readLatch.countDown();
                    commitLatch.await();
                    // 3. Commit the update transaction.
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            return null;
        });


        updateTransaction.join();
        assertThat(readTransaction.join())
                .hasSize(1)
                .containsExactlyInAnyOrder(card1);
    }

    private <E> CompletableFuture<E> executeAsync(Callable<E> callable) {
        Map<Object, Object> transactionResources = TransactionSynchronizationManager.getResourceMap();

        return CompletableFuture.supplyAsync(() -> {
            transactionResources.forEach(TransactionSynchronizationManager::bindResource);
            TransactionSynchronizationManager.setActualTransactionActive(true);
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                transactionResources.keySet().forEach(TransactionSynchronizationManager::unbindResource);
            }
        });
    }

    private Deck createDeck(String name) {
        return new Deck(UUID.randomUUID().toString(), name);
    }

    private Card createNewCard(Integer ordinal, String front, String back) {
        return createCard(UUID.randomUUID().toString(), ordinal, front, back);
    }

    private Card createCard(String id, Integer ordinal, String front, String back) {
        return new Card(id, ordinal, new CardFront(front), new CardBack(back), null, null);
    }
}
