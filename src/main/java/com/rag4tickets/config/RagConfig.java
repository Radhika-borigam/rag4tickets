package com.rag4tickets.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class RagConfig {

    @Value("${rag.vector-store.path:vector-store.json}")
    private String vectorStorePath;

    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
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

    /**
     * Fallback mock EmbeddingModel bean that executes if no real embedding model is registered
     * (e.g. when GEMINI_API_KEY is not configured during local/simulation mode).
     * This guarantees the application always boots and runs successfully out of the box.
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel mockEmbeddingModel() {
        return new EmbeddingModel() {
            @Override
            public float[] embed(String text) {
                // Generate a standard mock vector of 384 dimensions (standard MiniLM format)
                float[] vector = new float[384];
                int seed = text.hashCode();
                java.util.Random rand = new java.util.Random(seed);
                for (int i = 0; i < 384; i++) {
                    vector[i] = rand.nextFloat();
                }
                return vector;
            }

            @Override
            public float[] embed(Document document) {
                return embed(document.getText());
            }

            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> list = new ArrayList<>();
                int index = 0;
                for (String text : request.getInstructions()) {
                    list.add(new Embedding(embed(text), index++));
                }
                return new EmbeddingResponse(list);
            }
        };
    }
}
