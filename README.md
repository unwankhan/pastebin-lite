# Pastebin Lite

A lightweight pastebin-like application built with Java Spring Boot and MongoDB. Users can create text pastes with optional constraints (expiration time and view limits) and share them via unique URLs.

## Features

- 📝 Create text pastes with optional constraints
- ⏱️ Set expiration time (TTL) for automatic deletion
- 👁️ Limit maximum views for pastes
- 🔗 Generate unique, shareable URLs
- 🛡️ Secure content rendering (no script execution)
- 📊 View paste metadata (remaining views, expiration time)
- 🧪 Deterministic time testing support for automated tests

## Tech Stack

- **Backend**: Java 17, Spring Boot 2.7+
- **Frontend**: Thymeleaf templates, HTML5, CSS3, JavaScript
- **Database**: MongoDB (with TTL indexes for auto-expiry)
- **Build Tool**: Maven
- **Containerization**: Docker & Docker Compose
- **Deployment**: Railway.app (or any cloud platform)

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- MongoDB 6.0+ (or Docker)
- (Optional) Docker & Docker Compose

## Running Locally

### Method 1: Using Docker Compose (Recommended)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd pastebin-lite
   ```

2. **Start the application with Docker Compose**
   ```bash
   docker-compose up
   ```

3. **Access the application**
    - Application: http://localhost:8080
    - Health check: http://localhost:8080/api/healthz

### Method 2: Manual Setup

1. **Start MongoDB**
   ```bash
   # Option A: Using Docker
   docker run -d -p 27017:27017 --name pastebin-mongo mongo:6.0

   # Option B: Install MongoDB locally
   # Follow official MongoDB installation guide
   ```

2. **Set environment variables**
   ```bash
   export MONGODB_URI=mongodb://localhost:27017/pastebin_lite
   export APP_BASE_URL=http://localhost:8080
   export TEST_MODE=0
   ```

3. **Build and run the application**
   ```bash
   # Build the project
   mvn clean package

   # Run the application
   java -jar target/pastebin-lite-1.0.0.jar
   ```

4. **Access the application**
    - Open http://localhost:8080 in your browser

### Method 3: Using Maven directly

```bash
# Run with Maven
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.data.mongodb.uri=mongodb://localhost:27017/pastebin_lite --app.base-url=http://localhost:8080"
```

## API Endpoints

### Health Check
```
GET /api/healthz
```
Returns application health status.

### Create a Paste
```
POST /api/pastes
```
**Request Body:**
```json
{
  "content": "Your text here",
  "ttl_seconds": 300,
  "max_views": 5
}
```

**Response:**
```json
{
  "id": "abc123de",
  "url": "http://localhost:8080/p/abc123de"
}
```

### Retrieve a Paste (API)
```
GET /api/pastes/{id}
```
Returns JSON with paste content and metadata.

### View a Paste (HTML)
```
GET /p/{id}
```
Returns HTML page with the paste content.

## Persistence Layer

This application uses **MongoDB** as the persistence layer with the following configuration:

### Database Schema
- **Collection**: `pastes`
- **Indexes**:
    - Unique index on `pasteId` for fast lookups
    - TTL index on `expiresAt` for automatic document expiration
    - Compound indexes for efficient querying

### Key Features
- **Automatic Expiry**: MongoDB TTL indexes automatically delete expired documents
- **Atomic Operations**: Uses MongoDB's findAndModify for race-condition-free view counting
- **Scalability**: MongoDB's document model fits the paste structure perfectly
- **Persistence**: Survives server restarts and works across serverless environments

### MongoDB Configuration
```properties
spring.data.mongodb.uri=${MONGODB_URI}
spring.data.mongodb.database=pastebin_lite
spring.data.mongodb.auto-index-creation=true
```

The TTL index is configured to automatically remove pastes when their `expiresAt` time is reached, ensuring no manual cleanup is required.

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | 8080 |
| `MONGODB_URI` | MongoDB connection string | Required |
| `APP_BASE_URL` | Base URL for generated paste links | http://localhost:8080 |
| `TEST_MODE` | Enable deterministic time for testing | 0 |
| `APP_PASTE_ID_LENGTH` | Length of generated paste IDs | 8 |
| `APP_DEFAULT_TTL_SECONDS` | Default TTL if not specified | 604800 (7 days) |

## Testing

### Automated Tests
The application supports deterministic time testing via the `x-test-now-ms` header when `TEST_MODE=1`.

### Run Tests
```bash
# Run all tests
mvn test

# Run with specific profile
mvn test -Dspring.profiles.active=test
```

### Test Examples
```bash
# Create a paste with TTL
curl -X POST http://localhost:8080/api/pastes \
  -H "Content-Type: application/json" \
  -d '{"content":"test", "ttl_seconds":60, "max_views":5}'

# Retrieve with test time header
curl -H "x-test-now-ms: 1672531200000" \
  http://localhost:8080/api/pastes/abc123de
```

## Deployment

The application is deployed on Railway.app at:
**Live URL**: https://pastebin-lite-production.up.railway.app/

### Deployment Instructions

1. **Push to Railway**
   ```bash
   # Connect your repository
   railway link
   
   # Deploy
   railway up
   ```

2. **Set environment variables on Railway**
    - `MONGODB_URI`: Your MongoDB connection string
    - `APP_BASE_URL`: Your Railway app URL
    - `PORT`: Set by Railway automatically

3. **Access your deployed application**
    - Your app will be available at `https://<your-project>.up.railway.app`

## Project Structure

```
pastebin-lite/
├── src/main/java/com/pastebinlite/
│   ├── controller/     # REST and view controllers
│   ├── model/          # Data models (Paste entity)
│   ├── repository/     # MongoDB repository interfaces
│   ├── service/        # Business logic
│   └── PastebinLiteApplication.java
├── src/main/resources/
│   ├── templates/      # Thymeleaf HTML templates
│   └── application.properties
├── Dockerfile          # Docker configuration
├── docker-compose.yml  # Local development setup
├── pom.xml            # Maven dependencies
└── README.md          # This file
```

## Development

### Code Style
- Follow Java conventions
- Use meaningful variable names
- Add comments for complex logic
- Keep methods focused and small

### Contributing
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is created for assignment purposes. Feel free to use it as a reference or starting point for your own projects.

## Support

For questions or issues:
1. Check the deployed application: https://pastebin-lite-production.up.railway.app/
2. Review the API documentation above
3. Check the application logs for errors

---

**Note**: This is a lightweight implementation for educational purposes. For production use, consider adding authentication, rate limiting, and additional security measures.