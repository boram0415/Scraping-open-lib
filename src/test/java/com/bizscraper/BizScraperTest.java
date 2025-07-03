package com.bizscraper;


import com.bizscraper.dto.BusinessInfo;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.bytedeco.opencv.opencv_java;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import org.bytedeco.javacpp.Loader;


import static org.junit.jupiter.api.Assertions.*;


public class BizScraperTest {


    static {
        Loader.load(opencv_java.class);
    }

    @Test
    void html파싱해보기() throws IOException {

        //1. given
        File input = new File("src/test/resources/sample.html");
        String html = Files.readString(input.toPath());

        //2. when
        BusinessInfo info = BizScraper.parseFormHtml(html);

        //3. then
        assertNotNull(info);
        assertEquals("이춘화", info.getOwnerName());
        assertEquals("넷매니아 주식회사", info.getCompanyName());
        assertTrue(info.getAddress().contains("서울"));

    }

    @Test
    void OCR_테스트() throws Exception {

        // 1. 세션 쿠키 받아오기 (리스트 페이지)
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

        String refererUrl = "https://www.ftc.go.kr/kw/selectBizCommList.do";
        HttpRequest listPageRequest = HttpRequest.newBuilder().uri(new URI(refererUrl)).header("User-Agent", "Mozilla/5.0").build();

        HttpResponse<String> listPageResponse = client.send(listPageRequest, HttpResponse.BodyHandlers.ofString());

        // 2. Set-Cookie 헤더에서 쿠키 값 조합
        List<String> setCookies = listPageResponse.headers().allValues("set-cookie");
        // "JSESSIONID=...; Path=/..." 식이 여러개라면 세미콜론 앞부분만 연결
        String cookies = setCookies.stream().map(s -> s.split(";", 2)[0]).collect(Collectors.joining("; "));

        // 3. 받아온 쿠키를 이용해서 captcha 이미지 다운로드
        String captchaUrl = "https://www.ftc.go.kr/bizCaptchaImg.do";
        HttpRequest imageRequest = HttpRequest.newBuilder().uri(new URI(captchaUrl)).header("User-Agent", "Mozilla/5.0").header("Referer", refererUrl).header("Cookie", cookies).build();


        HttpResponse<byte[]> imageResponse = client.send(imageRequest, HttpResponse.BodyHandlers.ofByteArray());

        // Content-Type 확인
        String contentType = imageResponse.headers().firstValue("Content-Type").orElse("");
        String ext = ".jpg"; // 기본
        if (contentType.contains("png")) ext = ".png";
        else if (contentType.contains("jpeg") || contentType.contains("jpg")) ext = ".jpg";
        else if (contentType.contains("gif")) ext = ".gif";

        // 4. 임시 파일로 저장 (확장자 맞게!)
        File tempFile = File.createTempFile("captcha", ext);
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            out.write(imageResponse.body());
        }

        // Tess4J OCR 그대로
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("/opt/homebrew/share/tessdata");
        tesseract.setLanguage("eng");

        String result = tesseract.doOCR(tempFile);

        assertNotNull(result);
        System.out.println("OCR 결과: " + result);

        // cleanup
        Files.deleteIfExists(tempFile.toPath());
    }

    @Test
    public void testOcrOnImage() throws TesseractException {
        // Tess4J 인스턴스 생성
        Tesseract tesseract = new Tesseract();
        tesseract.setTessVariable("tessedit_char_whitelist", "0123456789");
        tesseract.setDatapath("/opt/homebrew/share/tessdata");
        tesseract.setLanguage("eng");

        // 이미지 파일 지정
        File imageFile = new File("src/test/resources/sample.png");

        // OCR 실행
        String result = tesseract.doOCR(imageFile);

        // 결과 출력
        System.out.println("OCR 결과:");
        System.out.println(result);

        // JUnit assertion도 가능 (예시)
        assertTrue(result.contains("2"));
    }

    @Test
    public void 전처리된이미지_OCR_성공여부_테스트() throws Exception {
        // 1. 전처리 수행
        String inputPath = "src/test/resources/sample.png";
        String outputPath = "/Users/boramkim/Desktop/processed_sample.png"; // 최종 결과 파일 경로
//        CaptchaPreprocessor.preprocess(inputPath, outputPath);

        // 2. OCR 수행
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("/opt/homebrew/share/tessdata");
        tesseract.setLanguage("eng");
        tesseract.setTessVariable("tessedit_char_whitelist", "0123456789");

        String result = tesseract.doOCR(new File(outputPath));

        // 3. 출력 및 검증
        System.out.println("전처리 후 OCR 결과: " + result);
        assertNotNull(result);
        assertTrue(result.matches(".*\\d+.*")); // 숫자가 하나 이상 포함되어 있는지 확인
    }


}