package com.bizscraper;

import org.bytedeco.javacpp.Loader;
//import org.bytedeco.opencv.global.opencv_java;
import org.bytedeco.opencv.opencv_java;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CaptchaPreprocessor {

    static {
        // OpenCV 네이티브 라이브러리 로드
        Loader.load(opencv_java.class);
    }

    /**
     * 이미지에서 숫자만 분리하여 각각의 파일로 저장합니다.
     * @param inputPath 원본 이미지 경로
     * @param outputDir 저장될 디렉토리 경로
     * @return 추출된 숫자의 개수
     */
    public static int extractDigits(String inputPath, String outputDir) {
        Mat image = Imgcodecs.imread(inputPath);
        if (image.empty()) {
            throw new RuntimeException("이미지를 로드할 수 없습니다: " + inputPath);
        }

        // --- 1. 기본 전처리: 그레이스케일 및 이진화 ---
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

        Mat binary = new Mat();
        Imgproc.threshold(gray, binary, 128, 255, Imgproc.THRESH_BINARY_INV);

        // --- 2. 핵심 수정: 선을 더 확실하게 제거 ---
        // 이전 코드보다 더 공격적인 선 제거를 위해 두 방향으로 MORPH_OPEN을 적용합니다.
        Mat temp = new Mat();

        // (A) 가로로 긴 선 제거
        Mat horizontalKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 1));
        Imgproc.morphologyEx(binary, temp, Imgproc.MORPH_OPEN, horizontalKernel);

        // (B) 세로로 긴 선 제거
        Mat verticalKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 5));
        Imgproc.morphologyEx(temp, temp, Imgproc.MORPH_OPEN, verticalKernel);

        // --- 3. 개별 숫자 분리: Contour 찾기 ---
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        // 선이 제거된 최종 이미지(temp)에서 Contour를 찾습니다.
        Imgproc.findContours(temp, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        List<Rect> digitRects = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            // 너무 작거나 넓적한 노이즈가 아닌, 숫자처럼 보이는 영역만 필터링합니다.
            if (rect.height > 10 && rect.width > 5 && (double)rect.height / rect.width > 1.2) {
                digitRects.add(rect);
            }
        }

        // --- 4. 왼쪽에서 오른쪽 순서로 정렬 ---
        digitRects.sort((r1, r2) -> Integer.compare(r1.x, r2.x));

        // --- 5. 각 숫자 잘라서 저장 ---
        for (int i = 0; i < digitRects.size(); i++) {
            Rect rect = digitRects.get(i);
            // 원본 흑백 이미지에서 숫자 부분만 잘라냅니다.
            Mat digit = new Mat(temp, rect);

            // 저장할 파일 경로 생성
            String outputPath = outputDir + "/digit_" + i + ".png";
            Imgcodecs.imwrite(outputPath, digit);
        }

        System.out.println(digitRects.size() + "개의 숫자를 성공적으로 분리하여 저장했습니다: " + outputDir);
        return digitRects.size();
    }

    // 테스트를 위한 main 메서드
    public static void main(String[] args) {
        // 여기에 실제 이미지 경로를 정확하게 입력해주세요!
        String input = "/Users/boramkim/Desktop/sample.png";
        String outputDirectory = "/Users/boramkim/Desktop/extracted_digits";

        // 폴더가 없으면 생성
        new File(outputDirectory).mkdirs();

        extractDigits(input, outputDirectory);
    }
}
