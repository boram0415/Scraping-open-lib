package com.bizscraper;

import com.bizscraper.dto.BusinessInfo;

public class BizScraper {


    /**
     * HTML 전문에서 사업자 정보를 파싱합니다.
     * @param html HTML 원문 문자열
     * @return BusinessInfo 추출 결과
     */
    public static BusinessInfo parseFormHtml(String html) {
        return HtmlParser.parseFormHtml(html);
    }

}
