package com.cena.traveloka.inventory.service;

import com.cena.traveloka.exception.AppException;
import com.cena.traveloka.exception.ErrorCode;
import com.cena.traveloka.inventory.dto.response.PropertyImageRes;
import com.cena.traveloka.inventory.entity.Property;
import com.cena.traveloka.inventory.entity.PropertyImage;
import com.cena.traveloka.inventory.repository.PropertyImageRepository;
import com.cena.traveloka.inventory.repository.PropertyRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PropertyImageService {
    PropertyRepository propertyRepo;
    PropertyImageRepository imageRepo;
    StorageService storage;

    @Transactional
    public List<PropertyImageRes> upload(UUID propertyId, List<MultipartFile> files) throws Exception {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + propertyId));

        int baseOrder = imageRepo.findTopByPropertyOrderBySortOrderDesc(property)
                .map(img -> img.getSortOrder() + 1)
                .orElse(0);
        List<PropertyImageRes> result = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile f = files.get(i);
            String filename = Objects.requireNonNullElse(f.getOriginalFilename(), "image");
            // upload lên MinIO → trả public URL
            String url = storage.put(propertyId + "/" + UUID.randomUUID() + "-" + filename, f);

            PropertyImage img = PropertyImage.builder()
                    .property(property)
                    .url(url)
                    .sortOrder(baseOrder + i)
                    .build();
            imageRepo.save(img);

            result.add(PropertyImageRes.builder()
                    .id(img.getId())
                    .propertyId(propertyId)
                    .url(img.getUrl())
                    .sortOrder(img.getSortOrder())
                    .createdAt(img.getCreatedAt())
                    .build());
        }
        return result;
    }

    public List<PropertyImageRes> list(UUID propertyId) {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Property not found: " + propertyId));
        return imageRepo.findByPropertyOrderBySortOrderAsc(property).stream()
                .map(i -> PropertyImageRes.builder()
                        .id(i.getId())
                        .propertyId(propertyId)
                        .url(i.getUrl())
                        .sortOrder(i.getSortOrder())
                        .createdAt(i.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void delete(UUID imageId) {
        PropertyImage img = imageRepo.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Image not found: " + imageId));
        imageRepo.delete(img);
    }
}
