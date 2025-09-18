package com.cena.traveloka.catalog.inventory.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishPartnerCreated(UUID partnerId, String partnerName) {
        PartnerCreatedEvent event = new PartnerCreatedEvent(partnerId, partnerName);
        eventPublisher.publishEvent(event);
        log.info("Published PartnerCreatedEvent for partner: {}", partnerId);
    }

    public void publishPartnerActivated(UUID partnerId, String partnerName) {
        PartnerActivatedEvent event = new PartnerActivatedEvent(partnerId, partnerName);
        eventPublisher.publishEvent(event);
        log.info("Published PartnerActivatedEvent for partner: {}", partnerId);
    }

    public void publishPropertyCreated(UUID propertyId, String propertyName, UUID partnerId) {
        PropertyCreatedEvent event = new PropertyCreatedEvent(propertyId, propertyName, partnerId);
        eventPublisher.publishEvent(event);
        log.info("Published PropertyCreatedEvent for property: {}", propertyId);
    }

    public void publishPropertyActivated(UUID propertyId, String propertyName, UUID partnerId) {
        PropertyActivatedEvent event = new PropertyActivatedEvent(propertyId, propertyName, partnerId);
        eventPublisher.publishEvent(event);
        log.info("Published PropertyActivatedEvent for property: {}", propertyId);
    }

    public void publishImageUploaded(UUID propertyId, UUID imageId, String imageUrl) {
        ImageUploadedEvent event = new ImageUploadedEvent(propertyId, imageId, imageUrl);
        eventPublisher.publishEvent(event);
        log.info("Published ImageUploadedEvent for property: {} image: {}", propertyId, imageId);
    }
}