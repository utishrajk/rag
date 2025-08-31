# Frontend Architecture and Libraries

## Important Clarification

**The frontend implementation uses Vanilla JavaScript, NOT React or Vue.** 

There seems to be some confusion - the current implementation is built with:
- **Vanilla JavaScript** (plain JavaScript, no frameworks)
- **Vite** (build tool)
- **Axios** (HTTP client, though currently using native fetch API)

No React or Vue frameworks are used in this implementation.

## Current Frontend Structure

### Libraries and Tools Used

1. **Vite** (`^5.0.0`)
   - Modern build tool and dev server
   - Fast hot module replacement during development
   - Optimized production builds with tree-shaking
   - ES6 module support

2. **Axios** (`^1.6.0`)
   - HTTP client library (currently unused - using native fetch instead)
   - Could be used for more advanced request handling

3. **Vanilla JavaScript**
   - No framework dependencies
   - Direct DOM manipulation
   - Native fetch API for HTTP requests
   - Event listeners for user interactions

### How It Works

The frontend follows a simple architecture:

```
index.html          # Main UI structure with inline CSS
main.js            # All JavaScript logic and API calls
vite.config.js     # Build configuration with proxy to backend
```

**Key Components:**

1. **API Layer** (`main.js:5-18`)
   ```javascript
   async function apiCall(endpoint, options = {}) {
       // Centralized API calling with error handling
   }
   ```

2. **UI Sections** (each with dedicated functions):
   - Load Data: `loadData()` function
   - Vector Search: `search()` function  
   - Year Search: `searchByYear()` function
   - RAG Queries: `askRag()` function
   - External LLM: `askExternal()` function

3. **State Management**: Simple DOM updates, no state management library

## React vs Vue: Not Used, But Here's the Comparison

Since you asked about React/Vue, here's why they weren't used and how they could be:

### Why Vanilla JavaScript Was Chosen

1. **Simplicity**: Small application with straightforward UI needs
2. **No Build Complexity**: Minimal dependencies and build configuration
3. **Performance**: No framework overhead
4. **Learning Curve**: Easier to understand and modify

### If You Wanted to Add React or Vue

**React and Vue should NOT be used together** in the same application because:
- **Conflicting paradigms**: Different component models and lifecycle management
- **Bundle size**: Double the framework overhead
- **Complexity**: Managing two different state systems
- **Team confusion**: Developers need to know both frameworks

### How to Migrate to React

If you want React instead:

1. **Update package.json**:
   ```json
   {
     "dependencies": {
       "react": "^18.0.0",
       "react-dom": "^18.0.0"
     },
     "devDependencies": {
       "@vitejs/plugin-react": "^4.0.0"
     }
   }
   ```

2. **Update vite.config.js**:
   ```javascript
   import { defineConfig } from 'vite'
   import react from '@vitejs/plugin-react'
   
   export default defineConfig({
     plugins: [react()],
     // ... rest of config
   })
   ```

3. **Rewrite components** as React JSX components

### How to Migrate to Vue

If you want Vue instead:

1. **Update package.json**:
   ```json
   {
     "dependencies": {
       "vue": "^3.0.0"
     },
     "devDependencies": {
       "@vitejs/plugin-vue": "^4.0.0"
     }
   }
   ```

2. **Update vite.config.js**:
   ```javascript
   import { defineConfig } from 'vite'
   import vue from '@vitejs/plugin-vue'
   
   export default defineConfig({
     plugins: [vue()],
     // ... rest of config
   })
   ```

## Current Implementation Benefits

The current Vanilla JavaScript approach provides:

1. **Zero Learning Curve**: Standard web technologies
2. **Fast Load Times**: No framework parsing/hydration
3. **Easy Debugging**: Direct browser DevTools inspection
4. **Simple Deployment**: Works in any web server
5. **Maven Integration**: Clean build process without framework complexity

## When to Consider Frameworks

Consider React/Vue if you plan to add:
- Complex state management across components
- Reusable component libraries
- Advanced routing
- Server-side rendering
- Large team development with component standards

For the current RAG application's needs, Vanilla JavaScript provides the optimal balance of simplicity and functionality.