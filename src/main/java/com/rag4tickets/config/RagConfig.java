package com.rag4tickets.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class RagConfig {

    @Value("${rag.vector-store.path:vector-store.json}")
    private String vectorStorePath;

    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = new SimpleVectorStore(embeddingModel);
        File file = new File(vectorStorePath);
        if (file.exists()) {
            try {
                store.load(file);
            } catch (Exception e) {
                System.err.println("Failed to load vector store from: " + vectorStorePath + ". Creating new.");
            }
        }
        return store;
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
