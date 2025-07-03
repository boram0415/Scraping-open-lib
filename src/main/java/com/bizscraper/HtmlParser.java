package com.bizscraper;

import com.bizscraper.dto.BusinessInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class HtmlParser {

    public static BusinessInfo parseFormHtml(String html) {

        Document doc = Jsoup.parse(html);

        // TODO: 아래에 selector 사용해서 파싱
        String companyName = getValueByKeyword(doc, "상호");
        String ownerName = getValueByKeyword(doc, "대표자명");
        String businessType = getValueByKeyword(doc, "판매방식");
        String address = getValueByKeyword(doc, "사업장소재지(도로명)");
        String registrationNumber = getValueByKeyword(doc, "사업자등록번호");

        System.out.println("check = " + companyName + " " + ownerName + " " + businessType + " " + address + " " + registrationNumber);
        return BusinessInfo.builder()
                .companyName(companyName)
                .ownerName(ownerName)
                .businessType(businessType)
                .address(address)
                .registrationNumber(registrationNumber)
                .build();

    }


    private static String getValueByKeyword(Document doc, String keyword) {
        Element th = doc.selectFirst("th:containsOwn(" + keyword + ")");
        if (th == null) return "";
        Element td = th.nextElementSibling();
        return td != null ? td.text().trim() : "";
    }



}
