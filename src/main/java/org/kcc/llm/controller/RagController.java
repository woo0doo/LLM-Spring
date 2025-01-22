package org.kcc.llm.controller;

import org.kcc.llm.service.RagService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    // 1) 엑셀 업로드 -> Qdrant 업서트
    @PostMapping("/uploadExcel")
    public String uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            ragService.uploadExcel(file.getInputStream());
            return "Excel data is uploaded & embedded successfully!";
        } catch (IOException e) {
            return "Error: " + e.getMessage();
        }
    }

    // 2) 질문 -> 검색 (단순 JSON 결과)
    @GetMapping("/ask")
    public String askQuestion(@RequestParam String question, @RequestParam(defaultValue = "3") int topK) {
        return ragService.askQuestion(question, topK);
    }
}