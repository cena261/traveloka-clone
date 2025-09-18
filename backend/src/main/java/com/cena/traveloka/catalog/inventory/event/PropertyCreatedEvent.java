package com.cena.traveloka.catalog.inventory.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class PropertyCreatedEvent {
    private final UUID propertyId;
    private final String propertyName;
    private final UUID partnerId;
    private final OffsetDateTime timestamp = OffsetDateTime.now();
}