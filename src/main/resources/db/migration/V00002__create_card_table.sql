CREATE TABLE IF NOT EXISTS card
(
    id VARCHAR(36) NOT NULL,
    deck_id VARCHAR(36) NOT NULL REFERENCES deck(id),
    ordinal INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    UNIQUE (deck_id, ordinal),
    CONSTRAINT card_pk PRIMARY KEY (id)
);

CREATE INDEX idx_card_deck_id ON card (deck_id);
CREATE INDEX idx_card_ordinal ON card(ordinal);
CREATE INDEX idx_card_id_deck_ordinal ON card(id, deck_id, ordinal);
