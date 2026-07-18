package com.rag4tickets.service;

import com.rag4tickets.model.QueryResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final SimpleVectorStore vectorStore;
    private final ChatClient chatClient;

    @Value("${spring.ai.google.genai.api-key:}")
    private String apiKey;

    public RagService(SimpleVectorStore vectorStore, ChatClient chatClient) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
    }

    @Cacheable(value = "resolutions", key = "#queryText")
    public QueryResponse query(String queryText) {
        long startTime = System.currentTimeMillis();

        // 1. Retrieve relevant historical documents
        List<Document> similarDocs = new ArrayList<>();
        try {
            similarDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(queryText)
                            .topK(2)
                            .build()
            );
        } catch (Exception e) {
            System.err.println("Error searching vector store: " + e.getMessage());
        }

        // 2. Extract reference titles and compile context
        List<String> references = similarDocs.stream()
                .map(doc -> {
                    String type = (String) doc.getMetadata().getOrDefault("type", "UNKNOWN");
                    String id = (String) doc.getMetadata().getOrDefault("id", "UNKNOWN");
                    String title = (String) doc.getMetadata().getOrDefault("title", "UNKNOWN");
                    return String.format("[%s] %s: %s", type, id, title);
                })
                .collect(Collectors.toList());

        String context = similarDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        // 3. Check for API key availability and execute generation
        String resolution;
        String source;
        double similarityScore = similarDocs.isEmpty() ? 0.0 : 0.88; // Default mock score

        // If no items were found, pre-populate standard mocks to ensure we retrieve something
        if (similarDocs.isEmpty()) {
            references.add("[SYSTEM] No vector index found. Run Ingest first!");
            context = "No historical context available.";
        }

        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("YOUR_API_KEY")) {
            // High fidelity local mock fallback for zero-setup demo
            resolution = getFallbackResolution(queryText);
            source = "Local Engine (Simulation Mode - Set GEMINI_API_KEY for live LLM)";
            similarityScore = 0.95;
            // Add custom visual mock sleep to simulate LLM execution time
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
        } else {
            try {
                String systemPrompt = """
                        You are an AI-powered Senior DevOps & Software Maintenance assistant for JIRA and GitHub.
                        Generate a grounded ticket resolution step-by-step plan based on historical JIRA tickets and GitHub PR diffs.
                        
                        RETRIEVED CONTEXT (HISTORICAL EVIDENCE):
                        {context}
                        
                        Provide a grounded suggestions including:
                        1. A clear analysis of the root cause.
                        2. Step-by-step resolution guide.
                        3. Code block with git diff snippet showing exactly what to fix.
                        4. Cite the retrieved tickets (e.g. PROJECT-101) and PRs (e.g. PR-123) as sources.
                        
                        Keep response concise, structured, and easy to read.
                        """;

                resolution = chatClient.prompt()
                        .system(systemPrompt.replace("{context}", context))
                        .user("INCOMING QUERY TICKET:\n" + queryText)
                        .call()
                        .content();
                source = "Google Gemini LLM (Live)";
            } catch (Exception e) {
                System.err.println("Gemini API invocation failed: " + e.getMessage());
                resolution = "### Gemini API Error\nFailed to invoke Gemini LLM (check API Key configuration).\n\n" + getFallbackResolution(queryText);
                source = "Local Fallback Engine (Gemini API failed)";
            }
        }

        long latencyMs = System.currentTimeMillis() - startTime;
        return new QueryResponse(resolution, references, latencyMs, source, similarityScore);
    }

    private String getFallbackResolution(String query) {
        String lower = query.toLowerCase();
        if (lower.contains("freeze") || lower.contains("concurrent") || lower.contains("transition")) {
            return """
                    ### Resolution Suggestion: Wrap state transitions in `useTransition`

                    Based on past ticket **PROJECT-101** and resolution **PR-123**, the issue is caused by performing blocking, synchronous state updates inside routing or heavy view rendering in React 19.

                    #### Root Cause
                    React 19 concurrent rendering is stricter on blocking transitions. If a component state change is slow (such as re-rendering multiple widgets, tables, or charts) and not wrapped in a transition, the main thread locks, causing a **UI Freeze**.

                    #### Steps to Fix:
                    1. Identify the heavy state setters (e.g., `setData` or navigation triggers).
                    2. Import `useTransition` from React.
                    3. Wrap the state updates in the `startTransition` callback to allow React to render it in the background without blocking the main UI thread.

                    #### Actionable Code Fix:
                    Compare the diff below to modify your component:

                    ```diff
                    - const [data, setData] = useState([]);
                    - const handleUpdate = (newData) => {
                    -   setData(newData); // Blocking state update
                    - };
                    + import { useTransition } from 'react';
                    + 
                    + const [isPending, startTransition] = useTransition();
                    + const [data, setData] = useState([]);
                    + const handleUpdate = (newData) => {
                    +   startTransition(() => {
                    +     setData(newData); // Non-blocking concurrent state update
                    +   });
                    + };
                    ```
                    """;
        } else if (lower.contains("infinite") || lower.contains("loop") || lower.contains("effect")) {
            return """
                    ### Resolution Suggestion: Memoize object reference in `useEffect` dependency

                    Based on past ticket **PROJECT-102** and resolution **PR-124**, the issue is caused by using reference types (objects or arrays) as dependencies in `useEffect`.

                    #### Root Cause
                    In React 19, stricter identity checks are performed. If you define a config object inline or pass it from a parent, its memory reference changes on every render. Putting it in a `useEffect` dependency array will cause the hook to run on every render, triggering an **Infinite rendering loop**.

                    #### Steps to Fix:
                    1. Avoid passing raw object references directly to the `useEffect` dependency array.
                    2. Extract primitive values from the object (e.g., `config.id` or `config.token`) to use in the dependency array.
                    3. Alternatively, serialize the config object using `JSON.stringify(config)` or memoize it using `useMemo`.

                    #### Actionable Code Fix:
                    Compare the diff below to modify your component:

                    ```diff
                    - useEffect(() => {
                    -   fetchData(config);
                    - }, [config]); // Triggers infinite loops
                    + const configString = JSON.stringify(config);
                    + useEffect(() => {
                    +   fetchData(config);
                    + }, [configString]); // Evaluates changes based on string contents
                    ```
                    """;
        } else if (lower.contains("lifecycle") || lower.contains("receiveprops") || lower.contains("removed")) {
            return """
                    ### Resolution Suggestion: Replace `componentWillReceiveProps` with functional hook

                    Based on past ticket **PROJECT-103** and resolution **PR-125**, the legacy class lifecycle method `componentWillReceiveProps` is deprecated and has been completely removed in React 19.

                    #### Root Cause
                    React 19 drops support for legacy lifecycles. Calling them causes runtime crashes. Class components must be refactored to functional components with `useEffect` or updated to use `getDerivedStateFromProps`.

                    #### Steps to Fix:
                    1. Convert the legacy class component to a modern functional component.
                    2. Use the `useEffect` hook to watch changes in props and trigger local state sync or data fetches.

                    #### Actionable Code Fix:
                    Compare the diff below to modify your component:

                    ```diff
                    - componentWillReceiveProps(nextProps) {
                    -   if (nextProps.id !== this.props.id) {
                    -     this.loadData(nextProps.id);
                    -   }
                    - }
                    + useEffect(() => {
                    +   loadData(id);
                    + }, [id]); // Hook triggers only when id changes
                    ```
                    """;
        } else {
            return """
                    ### Resolution Suggestion: General React 19 Migration Fix

                    We retrieved relevant JIRA and GitHub PR records, but they do not match a specific known root cause.

                    #### Recommended Action
                    1. Verify if you have breaking changes related to React 19 upgrade (e.g., ref removal, strict mode additions, or context API updates).
                    2. Check console stack trace to identify which library component is failing.
                    3. Consider reviewing linked PR records such as **PR-123** or **PR-124** for pattern matches in dependency upgrades.
                    """;
        }
    }
}
