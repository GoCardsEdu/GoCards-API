package pl.gocards.api.controller;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;
import pl.gocards.api.controller.model.UpdateCardRequest;
import pl.gocards.api.controller.model.UpdateCardResponse;
import pl.gocards.api.entity.Card;
import reactor.core.publisher.Flux;

import java.util.*;

@RestController
@RequestMapping("/deck/{deckId}/cards")
public class CardController {

    private final CardControllerFacade cardController;

    public CardController(
            CardControllerFacade cardController
    ) {
        this.cardController = cardController;
    }

    @GetMapping
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(array = @ArraySchema(schema = @Schema(implementation = Card.class)))}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema(nullable = true))})
    })
    public Flux<Card> fetchCards(@PathVariable String deckId) {
        return cardController.fetchCards(deckId);
    }

    @PutMapping
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", content = {@Content(array = @ArraySchema(schema = @Schema(implementation = UpdateCardResponse.class)))}),
            @ApiResponse(responseCode = "404", content = {@Content(schema = @Schema(nullable = true))})
    })
    public Flux<UpdateCardResponse> updateCards(
            @PathVariable String deckId,
            @RequestBody List<UpdateCardRequest> cards
    ) {
        return cardController.updateCards(deckId, cards);
    }
}
