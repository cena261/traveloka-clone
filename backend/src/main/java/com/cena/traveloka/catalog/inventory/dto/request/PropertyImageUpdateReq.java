package com.cena.traveloka.catalog.inventory.dto.request;

import com.cena.traveloka.catalog.inventory.entity.PropertyImage;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertyImageUpdateReq {
    PropertyImage.ImageType imageType;

    @Min(value = 0, message = "Sort order cannot be negative")
    Integer sortOrder;

    @Size(max = 200, message = "Caption must not exceed 200 characters")
    String caption;

    @Size(max = 100, message = "Alt text must not exceed 100 characters")
    String altText;
}