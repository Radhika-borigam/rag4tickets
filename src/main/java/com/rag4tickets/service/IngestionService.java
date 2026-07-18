package com.rag4tickets.service;

import com.rag4tickets.model.IngestRequest;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IngestionService {

    private final SimpleVectorStore vectorStore;

    @Value("${rag.vector-store.path:vector-store.json}")
    private String vectorStorePath;

    public IngestionService(SimpleVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void ingest(List<IngestRequest> requests) {
        List<Document> documents = new ArrayList<>();
        for (IngestRequest req : requests) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("id", req.getId());
            metadata.put("type", req.getType());
            metadata.put("title", req.getTitle());
            
            // Format full content including metadata for embedding context
            String fullContent = String.format("Source: %s | ID: %s | Title: %s\nContent:\n%s", 
                    req.getType(), req.getId(), req.getTitle(), req.getContent());
            
            documents.add(new Document(fullContent, metadata));
        }
        
        vectorStore.add(documents);
        saveStore();
    }

    public void loadMockData() {
        List<IngestRequest> mocks = new ArrayList<>();

        // Case 1: Concurrent Rendering UI Freeze
        mocks.add(new IngestRequest(
                "PROJECT-101",
                "TICKET",
                "React 19 UI Freeze on concurrent rendering transitions",
                "JIRA Ticket PROJECT-101. Priority: High. Status: Resolved.\n" +
                "Description: App UI freezes and becomes unresponsive when triggering route transitions containing heavy charts and lists.\n" +
                "Comments:\n" +
                "- Developer: Note that concurrent rendering issues occurred because legacy components use synchronous state updates in routing.\n" +
                "- QA: The freeze lasts around 2.5 seconds on desktop browsers."
        ));
        mocks.add(new IngestRequest(
                "PR-123",
                "PR",
                "Fix PROJECT-101: Wrap heavy state updates in useTransition hook",
                "GitHub PR-123. Commit: #ab12cd. Description: Refactored the dashboard charts to use the React 19 useTransition hook instead of synchronous state updates.\n" +
                "Diff snippet:\n" +
                "```diff\n" +
                "- const [data, setData] = useState([]);\n" +
                "- const updateData = (newData) => setData(newData);\n" +
                "+ const [isPending, startTransition] = useTransition();\n" +
                "+ const [data, setData] = useState([]);\n" +
                "+ const updateData = (newData) => {\n" +
                "+   startTransition(() => {\n" +
                "+     setData(newData);\n" +
                "+   });\n" +
                "+ };\n" +
                "```"
        ));

        // Case 2: Infinite re-render loop
        mocks.add(new IngestRequest(
                "PROJECT-102",
                "TICKET",
                "Infinite rendering loop with useEffect dependency array in React 19",
                "JIRA Ticket PROJECT-102. Priority: Critical. Status: Resolved.\n" +
                "Description: Infinite re-render loop detected when mounting the profile component dashboard.\n" +
                "Comments:\n" +
                "- Engineer: The profile component fetches user details on mount, but uses an object reference as a dependency in useEffect, triggering re-renders since React 19 does stricter identity evaluations.\n" +
                "- Team Lead: We must memoize or use string representation of the object as a dependency hook."
        ));
        mocks.add(new IngestRequest(
                "PR-124",
                "PR",
                "Fix PROJECT-102: Memoize object reference and use primitive fields in useEffect dependency",
                "GitHub PR-124. Commit: #ef34gh. Description: Fixed infinite loops by changing the useEffect dependencies from the raw config object to specific primitive fields or using JSON.stringify for deep comparison.\n" +
                "Diff snippet:\n" +
                "```diff\n" +
                "- useEffect(() => { fetchData(config); }, [config]);\n" +
                "+ const configString = JSON.stringify(config);\n" +
                "+ useEffect(() => { fetchData(config); }, [configString]);\n" +
                "```"
        ));

        // Case 3: Deprecated Lifecycle removal
        mocks.add(new IngestRequest(
                "PROJECT-103",
                "TICKET",
                "Deprecated lifecycle method componentWillReceiveProps causes crash",
                "JIRA Ticket PROJECT-103. Priority: Medium. Status: Resolved.\n" +
                "Description: Legacy code crashes after upgrading to React 19 since componentWillReceiveProps is fully removed.\n" +
                "Comments:\n" +
                "- Dev: Found legacy class components that crash with NullPointerException due to React 19 deprecation.\n" +
                "- Architect: Refactor class components to hooks or use getDerivedStateFromProps as intermediate fix."
        ));
        mocks.add(new IngestRequest(
                "PR-125",
                "PR",
                "Fix PROJECT-103: Replace componentWillReceiveProps with useEffect hook",
                "GitHub PR-125. Commit: #ij56kl. Description: Refactored legacy class component to function component to remove deprecated componentWillReceiveProps.\n" +
                "Diff snippet:\n" +
                "```diff\n" +
                "- componentWillReceiveProps(nextProps) {\n" +
                "-   if (nextProps.id !== this.props.id) {\n" +
                "-     this.loadData(nextProps.id);\n" +
                "-   }\n" +
                "- }\n" +
                "+ useEffect(() => {\n" +
                "+   loadData(id);\n" +
                "+ }, [id]);\n" +
                "```"
        ));

        ingest(mocks);
    }

    private void saveStore() {
        try {
            File file = new File(vectorStorePath);
            vectorStore.save(file);
        } catch (Exception e) {
            System.err.println("Error saving vector store to file: " + e.getMessage());
        }
    }
}
