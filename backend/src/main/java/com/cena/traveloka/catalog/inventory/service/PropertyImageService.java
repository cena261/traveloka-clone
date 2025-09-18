package com.cena.traveloka.catalog.inventory.service;

import com.cena.traveloka.common.exception.AppException;
import com.cena.traveloka.common.exception.ErrorCode;
import com.cena.traveloka.catalog.inventory.dto.request.PropertyImageUpdateReq;
import com.cena.traveloka.catalog.inventory.dto.response.PropertyImageRes;
import com.cena.traveloka.catalog.inventory.entity.Property;
import com.cena.traveloka.catalog.inventory.entity.PropertyImage;
import com.cena.traveloka.catalog.inventory.repository.PropertyImageRepository;
import com.cena.traveloka.catalog.inventory.repository.PropertyRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PropertyImageService {
    PropertyRepository propertyRepo;
    PropertyImageRepository imageRepo;
    StorageService storage;

    @Transactional
    @CacheEvict(value = "propertyImages", allEntries = true)
    public List<PropertyImageRes> upload(@NotNull UUID propertyId, @NotNull List<MultipartFile> files) throws Exception {
        log.info("Uploading {} images for property: {}", files.size(), propertyId);

        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + propertyId));

        validateImageUpload(files);

        Integer maxSortOrder = imageRepo.findMaxSortOrderByProperty(property);
        int baseOrder = (maxSortOrder != null ? maxSortOrder : -1) + 1;
        List<PropertyImageRes> result = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String filename = Objects.requireNonNullElse(file.getOriginalFilename(), "image");
            String objectKey = "properties/" + propertyId + "/" + UUID.randomUUID() + "-" + filename;
            String imageUrl = storage.put(objectKey, file);

            PropertyImage.ImageType imageType = determineImageType(filename);

            PropertyImage image = PropertyImage.builder()
                    .property(property)
                    .imageUrl(imageUrl)
                    .imageType(imageType)
                    .sortOrder(baseOrder + i)
                    .isPrimary(i == 0 && !imageRepo.existsByPropertyAndIsPrimary(property, true))
                    .build();

            image = imageRepo.save(image);
            result.add(toRes(image));
        }

        log.info("Uploaded {} images successfully for property: {}", files.size(), propertyId);
        return result;
    }

    @Transactional
    @CacheEvict(value = "propertyImages", allEntries = true)
    public PropertyImageRes setPrimaryImage(@NotNull UUID propertyId, @NotNull UUID imageId) {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + propertyId));

        PropertyImage newPrimary = imageRepo.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Image not found: " + imageId));

        if (!newPrimary.getProperty().getId().equals(propertyId)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Image does not belong to this property");
        }

        imageRepo.findPrimaryImageByProperty(property)
                .ifPresent(currentPrimary -> {
                    currentPrimary.setIsPrimary(false);
                    imageRepo.save(currentPrimary);
                });

        newPrimary.setIsPrimary(true);
        newPrimary = imageRepo.save(newPrimary);

        log.info("Set primary image for property {}: {}", propertyId, imageId);
        return toRes(newPrimary);
    }

    @Cacheable(value = "propertyImages", key = "#propertyId")
    public List<PropertyImageRes> list(@NotNull UUID propertyId) {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + propertyId));
        return imageRepo.findByPropertyOrderBySortOrderAsc(property).stream()
                .map(this::toRes)
                .toList();
    }

    public List<PropertyImageRes> findByImageType(UUID propertyId, PropertyImage.ImageType imageType) {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + propertyId));
        return imageRepo.findByPropertyAndImageType(property, imageType).stream()
                .map(this::toRes)
                .toList();
    }

    public Optional<PropertyImageRes> getPrimaryImage(UUID propertyId) {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + propertyId));
        return imageRepo.findPrimaryImageByProperty(property).map(this::toRes);
    }

    public List<PropertyImageRes> getGalleryImages(UUID propertyId) {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + propertyId));
        return imageRepo.findGalleryImages(property, PropertyImage.ImageType.GALLERY).stream()
                .map(this::toRes)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "propertyImages", allEntries = true)
    public PropertyImageRes updateImage(@NotNull UUID imageId, @NotNull PropertyImageUpdateReq req) {
        PropertyImage image = imageRepo.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Image not found: " + imageId));

        if (req.getImageType() != null) image.setImageType(req.getImageType());
        if (req.getSortOrder() != null) image.setSortOrder(req.getSortOrder());
        if (req.getCaption() != null) image.setCaption(req.getCaption());
        if (req.getAltText() != null) image.setAltText(req.getAltText());

        image = imageRepo.save(image);
        log.info("Image updated: {}", imageId);

        return toRes(image);
    }

    @Transactional
    @CacheEvict(value = "propertyImages", allEntries = true)
    public void delete(@NotNull UUID imageId) {
        PropertyImage image = imageRepo.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Image not found: " + imageId));

        if (image.getIsPrimary()) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                "Cannot delete primary image. Set another image as primary first.");
        }

        try {
            storage.delete(extractObjectKeyFromUrl(image.getImageUrl()));
        } catch (Exception e) {
            log.warn("Failed to delete image from storage: {}", image.getImageUrl(), e);
        }

        imageRepo.delete(image);
        log.info("Image deleted: {}", imageId);
    }

    public long countByProperty(UUID propertyId) {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + propertyId));
        return imageRepo.countByPropertyAndImageType(property, PropertyImage.ImageType.GALLERY);
    }

    private void validateImageUpload(List<MultipartFile> files) {
        if (files.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "No files provided");
        }

        if (files.size() > 20) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Maximum 20 images allowed per upload");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Empty file provided");
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Only image files are allowed");
            }

            if (file.getSize() > 10 * 1024 * 1024) { // 10MB
                throw new AppException(ErrorCode.INVALID_REQUEST, "Image size must be less than 10MB");
            }
        }
    }

    private PropertyImage.ImageType determineImageType(String filename) {
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.contains("room")) {
            return PropertyImage.ImageType.ROOM;
        } else if (lowerFilename.contains("amenity") || lowerFilename.contains("facility")) {
            return PropertyImage.ImageType.AMENITY;
        } else if (lowerFilename.contains("exterior") || lowerFilename.contains("facade")) {
            return PropertyImage.ImageType.EXTERIOR;
        } else {
            return PropertyImage.ImageType.GALLERY;
        }
    }

    private String extractObjectKeyFromUrl(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
    }

    private PropertyImageRes toRes(PropertyImage image) {
        return PropertyImageRes.builder()
                .id(image.getId())
                .propertyId(image.getProperty().getId())
                .imageUrl(image.getImageUrl())
                .imageType(image.getImageType())
                .sortOrder(image.getSortOrder())
                .caption(image.getCaption())
                .altText(image.getAltText())
                .isPrimary(image.getIsPrimary())
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt())
                .build();
    }
}
