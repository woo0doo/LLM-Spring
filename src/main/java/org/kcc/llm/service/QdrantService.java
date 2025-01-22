package org.kcc.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Transactional
public class QdrantService {

    @Value("${qdrant.base-url}")
    private String qdrantUrl;

    @Value("${qdrant.collection-name}")
    private String collectionName;

    @Value("${qdrant.vector-size}")
    private int vectorSize;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public QdrantService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 컬렉션 생성
     */
    public void createCollectionIfNotExists() {
        try {
            // GET /collections 로 확인 or 그냥 PUT 시도
            String url = qdrantUrl + "/collections/" + collectionName;

            // {
            //   "vectors": {
            //       "size": 1536,
            //       "distance": "Cosine"
            //   }
            // }
            Map<String, Object> vectorsMap = new HashMap<>();
            vectorsMap.put("size", vectorSize);
            vectorsMap.put("distance", "Cosine");

            Map<String, Object> body = new HashMap<>();
            body.put("vectors", vectorsMap);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.put(url, entity);  // PUT 요청
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Qdrant 업서트
     * points: (id, vector, payload)
     */
    public void upsertPoints(List<Map<String, Object>> points) {
        try {
            String url = qdrantUrl + "/collections/" + collectionName + "/points?wait=true";

            // body = {
            //   "points": [
            //      { "id": 1, "vector": [...], "payload": {...} },
            //      ...
            //   ]
            // }
            Map<String, Object> body = new HashMap<>();
            body.put("points", points);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.put(url, entity, String.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 검색
     * @param queryVector 검색할 벡터
     * @param limit 상위 개수
     * @return 검색 결과 JSON
     */
    public String search(List<Double> queryVector, int limit) {
        try {
            String url = qdrantUrl + "/collections/" + collectionName + "/points/search";

            Map<String, Object> body = new HashMap<>();
            body.put("vector", queryVector);
            body.put("limit", limit);
            body.put("with_payload", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            return resp.getBody(); // JSON 스트링
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}