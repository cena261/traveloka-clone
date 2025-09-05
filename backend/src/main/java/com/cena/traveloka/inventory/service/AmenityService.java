package com.cena.traveloka.inventory.service;

import com.cena.traveloka.exception.AppException;
import com.cena.traveloka.exception.ErrorCode;
import com.cena.traveloka.inventory.dto.request.AmenityCreateReq;
import com.cena.traveloka.inventory.dto.request.AmenityUpdateReq;
import com.cena.traveloka.inventory.dto.response.AmenityRes;
import com.cena.traveloka.inventory.entity.Amenity;
import com.cena.traveloka.inventory.repository.AmenityRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AmenityService {
    AmenityRepository repo;

    @Transactional
    public AmenityRes create(AmenityCreateReq req) {
        Amenity e = new Amenity();
        e.setCode(req.getCode());
        e.setName(req.getName());
        repo.save(e);
        return toRes(e);
    }

    public List<AmenityRes> list() {
        return repo.findAll().stream().map(this::toRes).toList();
    }

    @Transactional
    public AmenityRes update(UUID id, AmenityUpdateReq req) {
        Amenity e = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Amenity not found: " + id));
        if (req.getName() != null) e.setName(req.getName());
        return toRes(e);
    }

    @Transactional
    public void delete(UUID id) {
        Amenity e = repo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Amenity not found: " + id));
        repo.delete(e);
    }

    private AmenityRes toRes(Amenity e) {
        return AmenityRes.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .build();
    }
}
