package pl.gocards.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DeckNotFoundException extends RuntimeException {

    public DeckNotFoundException(String deckId) {
        super("Deck with ID '" + deckId + "' not found.");
    }

}
