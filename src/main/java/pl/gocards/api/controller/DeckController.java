package pl.gocards.api.controller;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.gocards.api.controller.model.DeckRequest;
import pl.gocards.api.controller.model.DeckResponse;
import pl.gocards.api.entity.Card;
import pl.gocards.api.entity.Deck;
import pl.gocards.api.exception.DeckNotFoundException;
import pl.gocards.api.jooq.DeckRepository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
public class DeckController {

    private final DeckControllerFacade deckController;

    public DeckController(DeckControllerFacade deckController) {
        this.deckController = deckController;
    }

    @GetMapping("/deck/{id}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = DeckResponse.class))}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema(nullable = true))})
    })
    public Mono<ResponseEntity<DeckResponse>> find(
            @PathVariable String id
    ) {
        return Mono.fromCallable(() -> deckController.find(id))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(deck -> wrapResponse(deck, HttpStatus.OK));
    }

    @PostMapping("/deck")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", content = {@Content(schema = @Schema(implementation = DeckResponse.class))}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema(nullable = true))})
    })
    public Mono<ResponseEntity<DeckResponse>> create(
            @RequestBody @Valid DeckRequest request
    ) throws DeckNotFoundException {
        return Mono.fromCallable(() -> deckController.createDeck(request))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(updated -> wrapResponse(updated, HttpStatus.CREATED));
    }

    @PutMapping("/deck/{id}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(schema = @Schema(implementation = DeckResponse.class))}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema(nullable = true))})
    })
    public Mono<ResponseEntity<DeckResponse>> update(
            @PathVariable String id,
            @RequestBody @Valid DeckRequest request
    ) {
        return Mono.fromCallable(() -> deckController.updateDeck(id, request))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(updated -> wrapResponse(updated, HttpStatus.OK));
    }

    private Mono<ResponseEntity<DeckResponse>> wrapResponse(Deck updated, HttpStatus status) {
        return wrapResponse(DeckResponse.fromDomain(updated), status);
    }

    private <T> Mono<ResponseEntity<T>> wrapResponse(T body, HttpStatus status) {
        return Mono.just(ResponseEntity.status(status).body(body));
    }
}
