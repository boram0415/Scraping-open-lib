package com.bizscraper.dto;

import lombok.*;

// 스크래핑 데이터 담을 객체
@Getter
@Builder
@ToString
@AllArgsConstructor
public class BusinessInfo {

    private final String companyName;
    private final String ownerName;
    private final String businessType;
    private final String address;
    private final String registrationNumber;

}




