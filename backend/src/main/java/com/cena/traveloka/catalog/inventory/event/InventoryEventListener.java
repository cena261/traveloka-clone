package com.cena.traveloka.catalog.inventory.event;

import com.cena.traveloka.catalog.inventory.metrics.InventoryMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryEventListener {

    private final InventoryMetrics metrics;

    @EventListener
    @Async
    public void handlePartnerCreated(PartnerCreatedEvent event) {
        log.info("Handling PartnerCreatedEvent for partner: {} at {}",
                 event.getPartnerId(), event.getTimestamp());

        metrics.incrementPartnerCreated();

        // Here you could:
        // - Send welcome email to partner
        // - Create audit log entry
        // - Trigger external integrations
        // - Update analytics
    }

    @EventListener
    @Async
    public void handlePartnerActivated(PartnerActivatedEvent event) {
        log.info("Handling PartnerActivatedEvent for partner: {} at {}",
                 event.getPartnerId(), event.getTimestamp());

        metrics.incrementPartnerActivated();

        // Here you could:
        // - Send activation confirmation email
        // - Enable partner dashboard access
        // - Update external systems
        // - Trigger onboarding workflow
    }

    @EventListener
    @Async
    public void handlePropertyCreated(PropertyCreatedEvent event) {
        log.info("Handling PropertyCreatedEvent for property: {} at {}",
                 event.getPropertyId(), event.getTimestamp());

        metrics.incrementPropertyCreated();

        // Here you could:
        // - Index property in search engine
        // - Create property verification tasks
        // - Send notifications to relevant teams
        // - Update inventory statistics
    }

    @EventListener
    @Async
    public void handlePropertyActivated(PropertyActivatedEvent event) {
        log.info("Handling PropertyActivatedEvent for property: {} at {}",
                 event.getPropertyId(), event.getTimestamp());

        metrics.incrementPropertyActivated();

        // Here you could:
        // - Make property visible in search results
        // - Enable booking functionality
        // - Update availability systems
        // - Send activation notification
    }

    @EventListener
    @Async
    public void handleImageUploaded(ImageUploadedEvent event) {
        log.info("Handling ImageUploadedEvent for property: {} image: {} at {}",
                 event.getPropertyId(), event.getImageId(), event.getTimestamp());

        metrics.incrementImageUploaded();

        // Here you could:
        // - Generate image thumbnails
        // - Run image quality checks
        // - Update search index with new images
        // - Trigger CDN cache invalidation
    }
}