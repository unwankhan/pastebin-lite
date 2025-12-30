# Pastebin-Lite

A simple Pastebin-like application built with Spring Boot that allows users to create text pastes with optional time-based expiry and view limits, and share them via unique URLs.

## How to Run Locally

### Prerequisites:
- Java 17 or higher
- Maven 3.8+
- MongoDB (local instance or cloud)

### Steps:
1. **Clone the repository:**
   ```bash
   git clone [your-repo-url]
   cd pastebin-lite
   ```

2. **Configure MongoDB:**
   - Option A: Use local MongoDB
     ```bash
     docker run -d -p 27017:27017 --name mongodb mongo:6.0
     ```
   - Option B: Update the connection string in `src/main/resources/application.properties` to your MongoDB URI

3. **Build and run:**
   ```bash
   mvn clean package
   java -jar target/pastebin-lite-1.0.0.jar
   ```

4. **Access the application:**
   - Open `http://localhost:8080` in your browser
   - Health check: `http://localhost:8080/api/healthz`

## Persistence Layer

This application uses **MongoDB** as its persistence layer. MongoDB was chosen because:

1. **TTL Index Support**: Native support for automatic document expiry based on time, which aligns perfectly with our time-based expiry requirement
2. **Flexible Schema**: Easy to store paste data with optional fields (ttl_seconds, max_views)
3. **Cloud-Ready**: Works well with serverless deployments and cloud platforms like Railway
4. **Scalability**: Handles concurrent paste creation and retrieval efficiently

## Important Design Decisions

1. **Dual Interface**: Implemented both REST API (`/api/pastes`) for programmatic access and HTML views (`/p/:id`) for user-friendly sharing
2. **Constraint Handling**: Pastes become unavailable when EITHER time-based expiry OR view limit is triggered, whichever comes first
3. **Test Mode Support**: Added `TEST_MODE=1` environment variable and `x-test-now-ms` header for deterministic expiry testing as required by the assignment
4. **Thread-Safe View Counting**: View count increments are handled atomically to prevent race conditions under concurrent load
5. **Clean Architecture**: Separated concerns with Controller-Service-Repository pattern for maintainability

## Railway Deployment Notes

To deploy on Railway:

1. **Set Environment Variables:**
   ```
   SPRING_DATA_MONGODB_URI=mongodb://[your-mongodb-connection-string]
   APP_BASE_URL=https://your-app.vercel.app
   ```

2. **Build Command:**
   ```
   mvn clean package -DskipTests
   ```

3. **Start Command:**
   ```
   java -jar target/pastebin-lite-1.0.0.jar
   ```

4. **Health Check Endpoint:** Railway will use `/api/healthz` for health checks
