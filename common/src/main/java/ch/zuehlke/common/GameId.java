package ch.zuehlke.common;

import java.util.UUID;

public record GameId(String value) {

    public GameId() {
        this(UUID.randomUUID().toString());
    }
}