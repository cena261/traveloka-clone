package com.cena.traveloka.inventory.controller;

import com.cena.traveloka.inventory.dto.response.PropertyImageRes;
import com.cena.traveloka.inventory.service.PropertyImageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/inventory/properties/{propertyId}/images")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PropertyImageController {
    PropertyImageService svc;

    @PostMapping
    public List<PropertyImageRes> upload(@PathVariable UUID propertyId,
                                         @RequestParam("files") List<MultipartFile> files) throws Exception {
        return svc.upload(propertyId, files);
    }

    @GetMapping
    public List<PropertyImageRes> list(@PathVariable UUID propertyId) {
        return svc.list(propertyId);
    }

    @DeleteMapping("/{imageId}")
    public void delete(@PathVariable UUID propertyId, @PathVariable UUID imageId) {
        svc.delete(imageId);
    }
}
