package com.cena.traveloka.catalog.inventory.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class PartnerActivatedEvent {
    private final UUID partnerId;
    private final String partnerName;
    private final OffsetDateTime timestamp = OffsetDateTime.now();
}