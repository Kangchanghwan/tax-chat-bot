package org.service.taxchatbot.config;

import org.springframework.ai.chroma.ChromaApi;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.ChromaVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ChatModelConfig {

    @Bean
    RestClient.Builder builder() {
        return RestClient.builder().requestFactory(new SimpleClientHttpRequestFactory());
    }

    @Bean
    EmbeddingModel embeddingModel() {
        return new OpenAiEmbeddingModel(new OpenAiApi(System.getenv("SPRING_AI_OPENAI_API_KEY")));
    }

    @Bean
    ChromaApi chromaApi(RestClient.Builder restClientBuilder) {
        String chromaUrl = "http://localhost:8000";
        return new ChromaApi(chromaUrl, restClientBuilder);
    }

    @Bean
    ChromaVectorStore chromaVectorStore(EmbeddingModel embeddingModel, ChromaApi chromaApi) {
        return new ChromaVectorStore(embeddingModel, chromaApi, "tax_docs", true);
    }
}
