package org.kcc.llm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class OpenAiEmbeddingService {

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiEmbeddingService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<Double> getEmbedding(String text) {
        String url = "https://api.openai.com/v1/embeddings";

        // 요청 바디
        /*
            {
                "input": text,
                "model": "text-embedding-ada-002"
            }
         */
        try {
            // JSON 구조 만들기 (Jackson)
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("model", "text-embedding-ada-002");
            requestMap.put("input", text);  // text 안에 어떤 특수문자가 있든 Jackson이 알아서 이스케이프

            String requestBody = objectMapper.writeValueAsString(requestMap);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode embeddingArray = root.get("data").get(0).get("embedding");

                List<Double> vector = new ArrayList<>();
                for (JsonNode val : embeddingArray) {
                    vector.add(val.asDouble());
                }
                return vector;
            } else {
                System.err.println("OpenAI Embedding API error: " + response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}