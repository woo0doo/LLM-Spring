package org.kcc.llm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.util.logging.Slf4j;
import org.kcc.llm.service.OpenAiEmbeddingService;
import org.kcc.llm.service.QdrantService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@lombok.extern.slf4j.Slf4j
@RestController
@Slf4j
public class ChatController {

    private final ChatClient chatClient;
    private final OpenAiEmbeddingService embeddingService;   // 질문 임베딩
    private final QdrantService qdrantService;

    ChatController(ChatClient.Builder builder,
                   OpenAiEmbeddingService embeddingService,
                   QdrantService qdrantService) {
        this.chatClient = builder.build();
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        // 1) 질문 임베딩
        List<Double> queryVector = embeddingService.getEmbedding(message);
        log.info("Query vector: {}", queryVector);

        // 2) Qdrant 검색 (Top K)
        String qdrantResultJson = qdrantService.search(queryVector, 3);
        log.info("Qdrant result: {}", qdrantResultJson);

        //   -> JSON 파싱해서 "원본 텍스트" 목록 추출 (payload.original_text 등)
        List<String> topDocuments = parseTopDocuments(qdrantResultJson);
        log.info("Top documents: {}", topDocuments);

        // 3) 컨텍스트를 합치기
        String context = String.join("\n", topDocuments);
        //    ex) "문서1 내용\n문서2 내용\n..."

        // 4) LLM에게 시스템/유저 메시지로 전달
        //    - "컨텍스트가 이러하니, 아래 질문에 대답해줘" 식
        return chatClient.prompt()
                .system("""
                        너는 전문가적인 말투로 답변해야 하며,
                        아래 컨텍스트를 참고해서 사용자의 질문에 간결하고 정확하게 답변해줘.
                        """)
                .user(
                        "Context:\n" + context + "\n\n" +
                                "Question: " + message
                )
                .call()
                .content();
    }

    /**
     * Qdrant 검색 결과(JSON)에서 문서 텍스트(예: payload.original_text)를 뽑아내는 예시
     */
    private List<String> parseTopDocuments(String qdrantResultJson) {
        try {
            JsonNode root = new ObjectMapper().readTree(qdrantResultJson);

            // root는 오브젝트. "result" 필드가 배열
            JsonNode resultArray = root.path("result");
            if (!resultArray.isArray()) {
                // 만약 result가 배열이 아닐 경우 빈 리스트
                return Collections.emptyList();
            }

            List<String> docs = new ArrayList<>();
            for (JsonNode item : resultArray) {
                // item: { "id":..., "score":..., "payload": { "combined_text": "..."} }
                JsonNode payload = item.path("payload");
                String text = payload.path("combined_text").asText("");
                if (!text.isBlank()) {
                    docs.add(text);
                }
            }
            return docs;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
