document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const vectorStatusEl = document.getElementById('vector-status');
    const btnIngestMock = document.getElementById('btn-ingest-mock');
    const queryInput = document.getElementById('query-input');
    const btnSubmitQuery = document.getElementById('btn-submit-query');
    const querySpinner = document.getElementById('query-spinner');
    const resultsSection = document.getElementById('results-section');
    
    // Diagnostic elements
    const diagLatency = document.getElementById('diag-latency');
    const diagSource = document.getElementById('diag-source');
    const diagScore = document.getElementById('diag-score');
    const diagGrounding = document.getElementById('diag-grounding');
    
    const referencesContainer = document.getElementById('references-container');
    const resolutionContainer = document.getElementById('resolution-container');
    const queryPills = document.querySelectorAll('.query-pill');

    const API_BASE = '/api';

    // 1. Initial Status Check
    checkSystemStatus();

    // 2. Event Listeners
    btnIngestMock.addEventListener('click', ingestMockData);
    btnSubmitQuery.addEventListener('click', submitQuery);
    
    queryPills.forEach(pill => {
        pill.addEventListener('click', () => {
            queryInput.value = pill.getAttribute('data-query');
            submitQuery();
        });
    });

    // 3. API Functions
    async function checkSystemStatus() {
        try {
            const res = await fetch(`${API_BASE}/status`);
            const status = await res.json();
            
            if (status.vectorStoreExists && status.vectorStoreSize > 0) {
                vectorStatusEl.textContent = 'Active (Ready)';
                vectorStatusEl.className = 'status-value status-ready';
            } else {
                vectorStatusEl.textContent = 'Empty (Ingestion Required)';
                vectorStatusEl.className = 'status-value status-empty';
            }
        } catch (err) {
            console.error('Failed to check status:', err);
            vectorStatusEl.textContent = 'Offline (Server Error)';
            vectorStatusEl.className = 'status-value status-empty';
        }
    }

    async function ingestMockData() {
        btnIngestMock.disabled = true;
        const originalText = btnIngestMock.innerHTML;
        btnIngestMock.innerHTML = 'Ingesting & Embedding...';
        
        try {
            const res = await fetch(`${API_BASE}/ingest/mock`, { method: 'POST' });
            const data = await res.json();
            alert(data.message);
            await checkSystemStatus();
        } catch (err) {
            console.error('Ingestion failed:', err);
            alert('Ingestion failed. Ensure Spring Boot backend is running.');
        } finally {
            btnIngestMock.disabled = false;
            btnIngestMock.innerHTML = originalText;
        }
    }

    async function submitQuery() {
        const queryText = queryInput.value.trim();
        if (!queryText) return;

        // Toggle Loading UI
        btnSubmitQuery.disabled = true;
        querySpinner.classList.remove('hidden');
        resultsSection.classList.add('hidden');

        try {
            const res = await fetch(`${API_BASE}/query`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ query: queryText })
            });

            if (!res.ok) throw new Error('API request failed');
            const data = await res.json();

            // Populate Diagnostics
            diagLatency.textContent = `${data.latencyMs} ms`;
            diagScore.textContent = `${Math.round(data.similarityScore * 100)}%`;
            diagGrounding.textContent = `${Math.round(data.groundingScore)}%`;
            
            // Redis Caching Visual indicator
            if (data.latencyMs < 20) {
                diagSource.textContent = `${data.source} (Cached via Redis)`;
                diagSource.className = 'diag-value redis';
            } else {
                diagSource.textContent = data.source;
                diagSource.className = 'diag-value';
            }

            // Render Retrieved references
            renderReferences(data.references);

            // Render Resolution suggestion
            renderResolution(data.resolution);

            // Show results
            resultsSection.classList.remove('hidden');
            resultsSection.scrollIntoView({ behavior: 'smooth' });

        } catch (err) {
            console.error('Query failed:', err);
            alert('Failed to execute RAG query. Ensure Spring Boot server is active and index is loaded.');
        } finally {
            btnSubmitQuery.disabled = false;
            querySpinner.classList.add('hidden');
        }
    }

    // 4. Rendering Helpers
    function renderReferences(references) {
        referencesContainer.innerHTML = '';
        if (!references || references.length === 0) {
            referencesContainer.innerHTML = '<p class="pane-desc">No references found.</p>';
            return;
        }

        references.forEach(refStr => {
            // Parses string: "[TYPE] ID: Title"
            const match = refStr.match(/^\[(.*?)\]\s*(.*?):\s*(.*)$/);
            
            const card = document.createElement('div');
            card.className = 'reference-card';

            if (match) {
                const [_, type, id, title] = match;
                
                // Set color indicator class based on document source type
                card.style.borderLeftColor = type === 'TICKET' ? 'var(--primary-glow)' : 'var(--secondary-glow)';

                card.innerHTML = `
                    <div class="ref-header">
                        <span class="ref-type ${type}">${type}</span>
                        <span class="ref-id">${id}</span>
                    </div>
                    <div class="ref-title">${title}</div>
                `;
            } else {
                card.innerHTML = `<div class="ref-body">${refStr}</div>`;
            }
            referencesContainer.appendChild(card);
        });
    }

    function renderResolution(markdownText) {
        // High fidelity simple Markdown parser for beautiful layout output
        let html = markdownText
            // Headers
            .replace(/^### (.*$)/gim, '<h3>$1</h3>')
            .replace(/^#### (.*$)/gim, '<h4>$1</h4>')
            .replace(/^# (.*$)/gim, '<h1>$1</h1>')
            // Bold
            .replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>')
            // Code Blocks with Git Diff Line parsing
            .replace(/```(diff|javascript|java|xml)?([\s\S]*?)```/gim, (match, lang, code) => {
                let lines = code.trim().split('\n');
                let formattedLines = lines.map(line => {
                    // Escape HTML characters
                    let escaped = line
                        .replace(/&/g, "&amp;")
                        .replace(/</g, "&lt;")
                        .replace(/>/g, "&gt;");
                    
                    if (escaped.startsWith('+ ')) {
                        return `<span class="addition">${escaped}</span>`;
                    } else if (escaped.startsWith('- ')) {
                        return `<span class="deletion">${escaped}</span>`;
                    }
                    return escaped;
                }).join('\n');
                
                return `<pre><code>${formattedLines}</code></pre>`;
            })
            // Inline code snippets
            .replace(/`(.*?)`/g, '<code>$1</code>')
            // Paragraph gaps and lists
            .replace(/^\s*-\s+(.*$)/gim, '<li>$1</li>')
            .replace(/<\/li>\s*<li>/g, '</li><li>'); // Clean lists
            
        // Wrap adjacent li elements into ul
        html = html.replace(/(<li>[\s\S]*?<\/li>)/g, '<ul>$1</ul>');
        
        // Clean redundant nested ul tags if any
        html = html.replace(/<\/ul>\s*<ul>/g, '');

        resolutionContainer.innerHTML = html;
    }
});
