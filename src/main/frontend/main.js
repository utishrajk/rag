// API base URL
const API_BASE = '/api/rag';

// Utility function for making API calls
async function apiCall(endpoint, options = {}) {
    try {
        const response = await fetch(`${API_BASE}${endpoint}`, {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            },
            ...options
        });
        return await response.json();
    } catch (error) {
        console.error('API call failed:', error);
        return { status: 'error', message: 'Network error: ' + error.message };
    }
}

// Show loading state
function showLoading(elementId) {
    document.getElementById(elementId).innerHTML = '<div class="loading">Loading...</div>';
}

// Show result
function showResult(elementId, result) {
    const element = document.getElementById(elementId);
    
    if (result.status === 'error') {
        element.innerHTML = `<div class="error">${result.message}</div>`;
        return;
    }
    
    if (result.status === 'success' && result.message) {
        element.innerHTML = `<div class="success">${result.message}</div>`;
        return;
    }
    
    element.innerHTML = '';
}

// Load CSV data
async function loadData() {
    const button = document.getElementById('loadDataBtn');
    button.disabled = true;
    button.textContent = 'Loading...';
    
    showLoading('loadDataResult');
    
    const result = await apiCall('/load-data', {
        method: 'POST'
    });
    
    showResult('loadDataResult', result);
    
    button.disabled = false;
    button.textContent = 'Load CSV Data';
}

// Search documents
async function search() {
    const query = document.getElementById('searchQuery').value.trim();
    const topK = document.getElementById('topK').value;
    const threshold = document.getElementById('threshold').value;
    
    if (!query) {
        showResult('searchResults', { status: 'error', message: 'Please enter a search query' });
        return;
    }
    
    showLoading('searchResults');
    
    const result = await apiCall('/search', {
        method: 'POST',
        body: JSON.stringify({
            query: query,
            topK: topK,
            similarityThreshold: threshold
        })
    });
    
    if (result.status === 'success') {
        displaySearchResults('searchResults', result);
    } else {
        showResult('searchResults', result);
    }
}

// Search by year
async function searchByYear() {
    const query = document.getElementById('yearSearchQuery').value.trim();
    const year = document.getElementById('year').value;
    const topK = document.getElementById('yearTopK').value;
    
    if (!query) {
        showResult('yearSearchResults', { status: 'error', message: 'Please enter a search query' });
        return;
    }
    
    showLoading('yearSearchResults');
    
    const result = await apiCall('/search-by-year', {
        method: 'POST',
        body: JSON.stringify({
            query: query,
            year: year,
            topK: topK
        })
    });
    
    if (result.status === 'success') {
        displaySearchResults('yearSearchResults', result);
    } else {
        showResult('yearSearchResults', result);
    }
}

// RAG query
async function askRag() {
    const prompt = document.getElementById('ragPrompt').value.trim();
    
    if (!prompt) {
        showResult('ragResults', { status: 'error', message: 'Please enter a prompt' });
        return;
    }
    
    showLoading('ragResults');
    
    const result = await apiCall('/ask', {
        method: 'POST',
        body: JSON.stringify({
            prompt: prompt
        })
    });
    
    if (result.status === 'success') {
        document.getElementById('ragResults').innerHTML = `
            <div class="results">
                <h4>Response:</h4>
                <div style="background: white; padding: 15px; border-radius: 4px; white-space: pre-wrap;">${result.response}</div>
            </div>
        `;
    } else {
        showResult('ragResults', result);
    }
}

// External LLM query
async function askExternal() {
    const prompt = document.getElementById('externalPrompt').value.trim();
    const url = document.getElementById('externalUrl').value.trim();
    
    if (!prompt) {
        showResult('externalResults', { status: 'error', message: 'Please enter a prompt' });
        return;
    }
    
    if (!url) {
        showResult('externalResults', { status: 'error', message: 'Please enter an external LLM URL' });
        return;
    }
    
    showLoading('externalResults');
    
    const result = await apiCall('/ask-external', {
        method: 'POST',
        body: JSON.stringify({
            prompt: prompt,
            url: url
        })
    });
    
    if (result.status === 'success') {
        document.getElementById('externalResults').innerHTML = `
            <div class="results">
                <h4>External LLM Response:</h4>
                <div style="background: white; padding: 15px; border-radius: 4px; white-space: pre-wrap;">${result.response}</div>
            </div>
        `;
    } else {
        showResult('externalResults', result);
    }
}

// Display search results
function displaySearchResults(elementId, result) {
    const element = document.getElementById(elementId);
    
    if (!result.results || result.results.length === 0) {
        element.innerHTML = '<div class="results">No results found.</div>';
        return;
    }
    
    let html = `
        <div class="results">
            <h4>Found ${result.totalResults} results for "${result.query}"${result.year ? ` in ${result.year}` : ''}</h4>
    `;
    
    result.results.forEach((item, index) => {
        html += `
            <div class="result-item">
                <div><strong>Result ${index + 1}:</strong></div>
                <div>${item.content}</div>
                <div class="metadata">
                    <strong>Metadata:</strong> 
                    ${Object.entries(item.metadata).map(([key, value]) => `${key}: ${value}`).join(' | ')}
                </div>
            </div>
        `;
    });
    
    html += '</div>';
    element.innerHTML = html;
}

// Make functions globally available for onclick handlers
window.loadData = loadData;
window.search = search;
window.searchByYear = searchByYear;
window.askRag = askRag;
window.askExternal = askExternal;

// Handle Enter key for search inputs
document.addEventListener('DOMContentLoaded', function() {
    document.getElementById('searchQuery').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') search();
    });
    
    document.getElementById('yearSearchQuery').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') searchByYear();
    });
    
    document.getElementById('ragPrompt').addEventListener('keypress', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            askRag();
        }
    });
    
    document.getElementById('externalPrompt').addEventListener('keypress', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            askExternal();
        }
    });
});