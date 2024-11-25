package pl.gocards.api.controller;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.gocards.api.controller.model.UpdateCardRequest;
import pl.gocards.api.controller.model.UpdateCardResponse;
import pl.gocards.api.controller.model.DeckRequest;
import pl.gocards.api.controller.model.DeckResponse;
import pl.gocards.api.entity.Card;
import pl.gocards.api.entity.CardBack;
import pl.gocards.api.entity.CardFront;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CardControllerIntegrationTest {

    @Autowired
    WebTestClient webClient;

    private static final List<UpdateCardRequest> INITIAL_CARDS_REQUEST = List.of(
            createCardRequest(null, "client-id-1", "term-1", "definition-1"),
            createCardRequest(null, "client-id-2", "term-2", "definition-2")
    );

    @Nested
    class Given_FindCards {

        @Test
        public void When_DeckNotExist_Return_DeckNotFound() {
            webClient.get()
                    .uri("/deck/{deckId}/cards", "deck-not-exist")
                    .exchange()
                    .expectStatus()
                    .isNotFound();
        }

        @Test
        public void When_DeckWithCards_Return_Cards() {
            var deckId = createDeck().id();
            updateCards(deckId, INITIAL_CARDS_REQUEST);

            var actual = findCards(deckId);

            var expected = List.of(
                    createCard(actual.get(0).id(), 1, "term-1", "definition-1"),
                    createCard(actual.get(1).id(), 2, "term-2", "definition-2")
            );

            assertCardsEqualIgnoringTimestamps(actual, expected);
        }
    }

    @Nested
    class UpdateCardsTests {

        @Nested
        class NonExistentDeck {

            @Test
            public void Return_DeckNotFound() {
                webClient
                        .put()
                        .uri("/deck/{deckId}/cards", "deck-not-exist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(INITIAL_CARDS_REQUEST)
                        .exchange()
                        .expectStatus()
                        .isNotFound();
            }
        }

        @Nested
        class EmptyDeck {

            @Test
            public void When_AddingCardWithoutFields_Should_AddNewCard() {
                var deckId = createDeck().id();

                List<UpdateCardResponse> actual = updateCards(deckId, "[{}]");

                List<UpdateCardResponse> expected = List.of(
                        createCardResponse(
                                actual.getFirst().id(),
                                null,
                                1,
                                null,
                                null
                        )
                );
                assertCardsEqualIgnoringTimestamps(actual, expected);
            }

            @Test
            public void When_AddingCardWithFrontAndBackNull_Should_AddNewCard() {
                var deckId = createDeck().id();

                List<UpdateCardResponse> actual = updateCards(deckId, "[{\"front\": {}, \"back\": {}}]");

                List<UpdateCardResponse> expected = List.of(
                        createCardResponse(
                                actual.getFirst().id(),
                                null,
                                1,
                                null,
                                null
                        )
                );
                assertCardsEqualIgnoringTimestamps(actual, expected);
            }
        }

        @Nested
        class DeckWithCardsTest {

            @Test
            public void When_AddingCardWithoutId_Should_AddNewCard() {
                var deckId = createDeck().id();
                List<UpdateCardResponse> initial = updateCards(deckId, INITIAL_CARDS_REQUEST);


                List<UpdateCardRequest> updateRequest = toRequests(initial);
                updateRequest.add(
                        createCardRequest(
                                null,
                                "client-id-3",
                                "new-term-3",
                                "new-definition-3"
                        )
                );
                List<UpdateCardResponse> actual = updateCards(deckId, updateRequest);


                List<UpdateCardResponse> expected = new ArrayList<>(initial);
                expected.add(
                        createCardResponse(
                                actual.get(2).id(),
                                "client-id-3",
                                3,
                                "new-term-3",
                                "new-definition-3"
                        )
                );
                assertCardsEqualIgnoringTimestamps(actual, expected);
            }

            @Test
            public void When_UpdatingCardWithExistingId_Should_UpdateExistingCard() {
                var deckId = createDeck().id();
                var initial = updateCards(deckId, INITIAL_CARDS_REQUEST);


                var updateRequest = List.of(
                        createCardRequest(
                                initial.get(0).id(),
                                "client-id-1",
                                "updated-term-1",
                                "updated-definition-1"
                        ),
                        toRequest(initial.get(1)).build()
                );
                var actual = updateCards(deckId, updateRequest);


                var expected = List.of(
                        createCardResponse(
                                initial.get(0).id(),
                                "client-id-1",
                                1,
                                "updated-term-1",
                                "updated-definition-1"
                        ),
                        initial.get(1)
                );
                assertCardsEqualIgnoringTimestamps(actual, expected);
            }

            @Test
            public void When_NoChanges_Should_NotUpdateUpdatedAt() {
                var deckId = createDeck().id();
                var initial = updateCards(deckId, INITIAL_CARDS_REQUEST);


                var updateRequest = List.of(
                        createCardRequest(
                                initial.get(0).id(),
                                "client-id-1",
                                "updated-term-1",
                                "updated-definition-1"
                        ),
                        toRequest(initial.get(1)).build()
                );
                var actual = updateCards(deckId, updateRequest);


                assertThat(initial.get(0).updatedAt()).isNotEqualTo(actual.get(0).updatedAt());
                assertThat(initial.get(1).updatedAt()).isEqualTo(actual.get(1).updatedAt());
            }

            @Test
            public void When_AddCardWithoutExistingId_Return_CardNotFound() {
                var deckId = createDeck().id();

                var updateRequest = List.of(
                        createCardResponse(
                                "not-exist4",
                                "client-id-1",
                                1,
                                "updated-term-1",
                                "updated-definition-1"
                        )
                );

                webClient.put()
                        .uri("/deck/{deckId}/cards", deckId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(updateRequest)
                        .exchange()
                        .expectStatus()
                        .isNotFound();
            }

            @Test
            public void When_AddCardExistInOtherDeck_Return_CardNotFound() {
                var deckId1 = createDeck().id();
                var cardsDeck1 = updateCards(deckId1, List.of(INITIAL_CARDS_REQUEST.getFirst()));

                var deckId2 = createDeck().id();
                var cardsDeck2 = List.of(
                        createCardResponse(
                                cardsDeck1.getFirst().id(),
                                "client-id-1",
                                1,
                                "term-1",
                                "definition-1"
                        )
                );

                webClient.put()
                        .uri("/deck/{deckId}/cards", deckId2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(cardsDeck2)
                        .exchange()
                        .expectStatus()
                        .isNotFound();
            }

            @Test
            public void When_RemoveCard_Should_RemoveCard() {
                var deckId = createDeck().id();
                List<UpdateCardResponse> initial = updateCards(deckId, INITIAL_CARDS_REQUEST);


                List<UpdateCardRequest> updateRequest = toRequests(initial);
                updateRequest.remove(1);
                var actual = updateCards(deckId, updateRequest);


                List<UpdateCardResponse> expected = toResponses(updateRequest);
                assertCardsEqualIgnoringTimestamps(actual, expected);
            }
        }
    }

    private List<UpdateCardResponse> updateCards(String deckId, Object body) {
        return webClient.put()
                .uri("/deck/{deckId}/cards", deckId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(UpdateCardResponse.class)
                .getResponseBody()
                .collectList()
                .block();
    }

    private List<Card> findCards(String deckId) {
        return webClient
                .get()
                .uri("/deck/{deckId}/cards", deckId)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(Card.class)
                .getResponseBody()
                .collectList()
                .block();
    }

    private DeckResponse createDeck() {
        var request = new DeckRequest("name");
        return webClient
                .post()
                .uri("/deck")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isCreated()
                .returnResult(DeckResponse.class)
                .getResponseBody()
                .blockFirst();
    }

    private static UpdateCardRequest createCardRequest(
            String id,
            String clientId,
            String term,
            String definition
    ) {
        return UpdateCardRequest
                .builder()
                .id(id)
                .clientId(clientId)
                .front(new CardFront(term))
                .back(new CardBack(definition))
                .build();
    }

    private static UpdateCardResponse createCardResponse(
            String id,
            String clientId,
            int ordinal,
            String term,
            String definition
    ) {
        return UpdateCardResponse
                .builder()
                .id(id)
                .clientId(clientId)
                .ordinal(ordinal)
                .front(new CardFront(term))
                .back(new CardBack(definition))
                .build();
    }

    private static Card createCard(
            String id,
            int ordinal,
            String term,
            String definition
    ) {
        return new Card(
                id,
                ordinal,
                new CardFront(term),
                new CardBack(definition),
                null,
                null
        );
    }

    public ArrayList<UpdateCardRequest> toRequests(List<UpdateCardResponse> responses) {
        return responses.stream()
                .map(request -> toRequest(request).build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<UpdateCardResponse> toResponses(List<UpdateCardRequest> responses) {
        AtomicInteger ordinal = new AtomicInteger();
        return responses.stream()
                .map(response -> toResponse(
                        response,
                        ordinal.incrementAndGet()).build()
                )
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public UpdateCardRequest.UpdateCardRequestBuilder toRequest(UpdateCardResponse response) {
        return UpdateCardRequest.builder()
                .id(response.id())
                .clientId(response.clientId())
                .front(new CardFront(response.front().term()))
                .back(new CardBack(response.back().definition()));
    }

    public UpdateCardResponse.UpdateCardResponseBuilder toResponse(
            UpdateCardRequest request,
            int ordinal
    ) {
        return UpdateCardResponse.builder()
                .id(request.id())
                .clientId(request.clientId())
                .ordinal(ordinal)
                .front(new CardFront(request.front().term()))
                .back(new CardBack(request.back().definition()));
    }

    private <T> void assertCardsEqualIgnoringTimestamps(List<T> actual, List<T> expected) {
        assertThat(actual)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("createdAt", "updatedAt")
                .containsExactlyElementsOf(expected);
    }
}
