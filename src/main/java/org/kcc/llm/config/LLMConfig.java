package org.kcc.llm.config;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LLMConfig {

    @Value("${spring.ai.openai.api-key}")
    private String openaiApiKey;

    @Bean
    public OpenAiApi openAiApi() {
        return new OpenAiApi(openaiApiKey);
    }

    @Bean
    public OpenAiChatOptions openAiChatOptions() {
        return OpenAiChatOptions.builder()
                .model("gpt-4o-mini")
                .temperature(0.5)
                .maxCompletionTokens(1024)
                .topP(1.0)
                .frequencyPenalty(0.0)
                .presencePenalty(0.0)
                .build();
    }

    @Bean
    public OpenAiChatModel chatModel(OpenAiApi openAiApi, OpenAiChatOptions openAiChatOptions) {
        return new OpenAiChatModel(openAiApi, openAiChatOptions);
    }

    public String  generatePirateNames(OpenAiChatModel chatModel, String message) {
        Prompt prompt = new Prompt(message);
        ChatResponse response = chatModel.call(prompt);

        // ChatResponse에서 문자열 추출
        String pirateNames = response.getResult().getOutput().getContent(); // getContent() 메서드가 있을 경우
        // 또는 다른 메서드가 있을 수 있으니, 확인해 보세요
        return pirateNames;
    }
}
