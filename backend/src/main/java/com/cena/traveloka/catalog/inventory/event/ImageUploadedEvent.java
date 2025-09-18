package com.cena.traveloka.catalog.inventory.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class ImageUploadedEvent {
    private final UUID propertyId;
    private final UUID imageId;
    private final String imageUrl;
    private final OffsetDateTime timestamp = OffsetDateTime.now();
}