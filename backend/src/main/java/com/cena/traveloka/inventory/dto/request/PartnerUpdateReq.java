package com.cena.traveloka.inventory.dto.request;

import lombok.Data;

@Data
public class PartnerUpdateReq {
    private String name;
    private String legalName;
    private String taxNumber;
    private String status; // 'active' | 'suspended' | 'pending'
}
