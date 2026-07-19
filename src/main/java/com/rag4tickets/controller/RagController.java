package com.rag4tickets.controller;

import com.rag4tickets.model.IngestRequest;
import com.rag4tickets.model.QueryRequest;
import com.rag4tickets.model.QueryResponse;
import com.rag4tickets.service.IngestionService;
import com.rag4tickets.service.RagService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RagController {

    private final IngestionService ingestionService;
    private final RagService ragService;

    @Value("${rag.vector-store.path:vector-store.json}")
    private String vectorStorePath;

    public RagController(IngestionService ingestionService, RagService ragService) {
        this.ingestionService = ingestionService;
        this.ragService = ragService;
    }

    @PostMapping("/query")
    public ResponseEntity<?> query(@RequestBody QueryRequest request) {
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(ragService.query(request.getQuery()));
        } catch (Exception e) {
            System.err.println(">>> CONTROLLER ERROR in /api/query: " + e.getClass().getName() + ": " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Query failed: " + e.getMessage());
            error.put("source", "Controller Error Handler");
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> ingest(@RequestBody List<IngestRequest> requests) {
        ingestionService.ingest(requests);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Successfully ingested " + requests.size() + " documents.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ingest/mock")
    public ResponseEntity<Map<String, String>> ingestMock() {
        ingestionService.loadMockData();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Mock datasets (React 19 tickets and PRs) successfully loaded and embedded.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        File file = new File(vectorStorePath);
        boolean exists = file.exists();
        status.put("vectorStoreExists", exists);
        status.put("vectorStoreSize", exists ? file.length() : 0);
        status.put("vectorStorePath", file.getAbsolutePath());
        return ResponseEntity.ok(status);
    }
}
