package org.kcc.llm.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class RagService {

    private final ExcelService excelService;
    private final OpenAiEmbeddingService embeddingService;
    private final QdrantService qdrantService;

    public RagService(ExcelService excelService,
                      OpenAiEmbeddingService embeddingService,
                      QdrantService qdrantService) {
        this.excelService = excelService;
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
    }

    public void uploadExcel(InputStream excelStream) {
        // 1) 엑셀 "모든 열" 파싱 -> List<Map<String, String>>
        List<Map<String, String>> rows = excelService.parseExcelAllColumns(excelStream);

        // 2) Qdrant 컬렉션 생성
//        qdrantService.createCollectionIfNotExists();

        // 3) 각 행 -> 임베딩 -> Qdrant 포인트 목록
        List<Map<String, Object>> points = new ArrayList<>();
        AtomicInteger idCounter = new AtomicInteger(1);

        for (Map<String, String> rowMap : rows) {
            // (a) 여러 열의 값을 합쳐 하나의 텍스트로 만듦 (쉼표, 공백 등 구분)
            //    예: "Column_0=값0 Column_1=값1 Column_2=값2 ..."
            //    혹은 값들만 단순히 이어붙일 수도 있습니다.
            String combinedText = combineRowToString(rowMap);

            // (b) 임베딩
            List<Double> vector = embeddingService.getEmbedding(combinedText);
            if (vector == null) {
                continue;
            }

            int id = idCounter.getAndIncrement();

            // (c) Qdrant 포인트 구조: {"id", "vector", "payload"}
            Map<String, Object> point = new HashMap<>();
            point.put("id", id);
            point.put("vector", vector);

            // (d) payload에 "original_text" 또는 "combinedText" 등 저장
            //     rowMap 전체를 payload에 넣고 싶다면 payload.putAll(rowMap)로도 가능
            Map<String, Object> payload = new HashMap<>();
            payload.put("combined_text", combinedText); // 임베딩에 사용한 전체 텍스트
            payload.put("excel_data", rowMap);          // 원본 전체 열 데이터
            point.put("payload", payload);

            points.add(point);
        }

        // 4) 업서트
        qdrantService.upsertPoints(points);
    }

    /**
     * 예시: 여러 열의 데이터를 하나의 문자열로 합쳐 주는 헬퍼 메서드
     */
    private String combineRowToString(Map<String, String> rowMap) {
        // 방법 1) 열 이름 & 값 함께 표시
        //   Column_0=값0, Column_1=값1, ...
        // return rowMap.entrySet().stream()
        //     .map(e -> e.getKey() + "=" + e.getValue())
        //     .collect(Collectors.joining(" "));

        // 방법 2) 값들만 공백/쉼표로 연결
        //   값0 값1 값2 ...
        return String.join(" ", rowMap.values());
    }

    /**
     * 질문(쿼리)에 대해 임베딩 -> Qdrant 검색 -> 결과 JSON 반환
     */
    public String askQuestion(String question, int topK) {
        // 1) 질문 임베딩
        List<Double> queryVec = embeddingService.getEmbedding(question);
        if (queryVec == null) {
            return "Embedding failed or null";
        }

        // 2) Qdrant 검색
        String resultJson = qdrantService.search(queryVec, topK);
        return resultJson;
    }
}