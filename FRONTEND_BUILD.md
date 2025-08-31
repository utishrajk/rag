# Frontend Build Process

This RAG application includes a modern frontend built with Vite and integrated with Maven for seamless development and deployment.

## Overview

The frontend is located in `src/main/frontend/` and uses:
- **Vite** - Modern build tool for fast development and optimized production builds
- **Vanilla JavaScript** - Clean, dependency-free frontend code
- **Maven Frontend Plugin** - Integrates Node.js/npm build process with Maven lifecycle

## Build Integration

### Maven Plugin Configuration

The `frontend-maven-plugin` in `pom.xml` handles:
1. **Node.js Installation** - Downloads Node.js v18.19.0 if not found in cache
2. **npm Installation** - Downloads npm v10.2.3 if not found in cache  
3. **Dependency Installation** - Runs `npm install` to install Vite and other dependencies
4. **Production Build** - Runs `npm run build` to create optimized static files

### Build Process Steps

When you run `mvn clean install` or `mvn package`:

1. **Install Node.js/npm** (if not cached)
   - Downloads to `target/` directory
   - Cached in `~/.m2/repository/` for reuse across projects

2. **Install Dependencies**
   - Runs `npm install` in `src/main/frontend/`
   - Creates `node_modules/` (ignored by Git)

3. **Build Frontend**
   - Runs `npm run build` 
   - Generates optimized files in `src/main/frontend/dist/` (ignored by Git)

4. **Copy to Spring Boot**
   - Built files are copied to `target/classes/static/`
   - Spring Boot serves these files at the root URL

## Development Workflow

### Local Development
```bash
# Start backend (port 8080)
mvn spring-boot:run

# In another terminal, start frontend dev server (port 3000)
cd src/main/frontend
npm run dev
```

### Production Build
```bash
# Build everything together
mvn clean package

# Run the complete application
java -jar target/rag-*.jar
```

## File Structure

```
src/main/frontend/
├── index.html          # Main HTML file
├── main.js            # JavaScript application logic
├── package.json       # Node.js dependencies and scripts
├── vite.config.js     # Vite build configuration
├── node_modules/      # Dependencies (not in Git)
└── dist/             # Build output (not in Git)
```

## Caching Behavior

- **Node.js/npm**: Downloaded once and cached in `~/.m2/repository/`
- **Dependencies**: `node_modules/` recreated on each clean build
- **Build Output**: `dist/` regenerated on each build

The plugin only re-downloads Node.js/npm when:
- The `target/` directory is cleaned (`mvn clean`)
- The specified versions change in `pom.xml`

## Git Workflow

**Files to commit:**
- `src/main/frontend/index.html`
- `src/main/frontend/main.js`
- `src/main/frontend/package.json`
- `src/main/frontend/vite.config.js`

**Files ignored by Git:**
- `src/main/frontend/node_modules/`
- `src/main/frontend/dist/`
- `src/main/frontend/package-lock.json`

## UI Features

The frontend provides:
- **Load Data**: Import CSV data into vector store
- **Vector Search**: Search documents with similarity thresholds
- **Year-based Search**: Filter search results by specific years
- **RAG Queries**: Ask natural language questions about the data
- **External LLM**: Query external language models

## Troubleshooting

**Build fails with Node.js errors:**
- Ensure you have internet access for initial downloads
- Check that ports 3000 and 8080 are available

**Frontend not loading:**
- Verify `WebController.java` forwards root requests to `index.html`
- Check that static files are in `target/classes/static/` after build

**API calls failing:**
- Ensure backend is running on port 8080
- Check proxy configuration in `vite.config.js`