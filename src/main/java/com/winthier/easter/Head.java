package com.winthier.easter;

import java.util.Map;
import java.util.UUID;
import lombok.Value;

@Value
final class Head {
    private final String name, texture;
    private final UUID id;

    static Head of(Map<?, ?> map) {
        String name = (String)map.get("Name");
        UUID id = UUID.fromString((String)map.get("Id"));
        String texture = (String)map.get("Texture");
        return new Head(name, texture, id);
    }
}
