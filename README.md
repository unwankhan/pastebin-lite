# Pastebin-Lite

A simple Pastebin-like application built with Spring Boot that allows users to create text pastes with optional time-based expiry and view limits, and share them via unique URLs.
A small Pastebin-like application that allows users to create text pastes and share a link to view them.
Pastes can optionally expire based on time-to-live (TTL) or a maximum view count.

## How to Run Locally
## Deployed URL

### Prerequisites:
- Java 17 or higher
- Maven 3.8+
- MongoDB (local instance or cloud)
https://pastebin-lite-production.up.railway.app

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
## How to run the app locally

4. **Access the application:**
   - Open `http://localhost:8080` in your browser
   - Health check: `http://localhost:8080/api/healthz`
### Prerequisites
- Java 17
- Maven
- MongoDB (local or MongoDB Atlas)

## Persistence Layer
### Steps

This application uses **MongoDB** as its persistence layer. MongoDB was chosen because:
1. Clone the repository
   git clone <your-github-repo-url>
   cd pastebin-lite

1. **TTL Index Support**: Native support for automatic document expiry based on time, which aligns perfectly with our time-based expiry requirement
2. **Flexible Schema**: Easy to store paste data with optional fields (ttl_seconds, max_views)
3. **Cloud-Ready**: Works well with serverless deployments and cloud platforms like Railway
4. **Scalability**: Handles concurrent paste creation and retrieval efficiently
2. Configure application properties
   Update the following properties in `src/main/resources/application.properties`:

## Important Design Decisions
   spring.data.mongodb.uri=<your-mongodb-connection-string>
   app.base-url=http://localhost:8080

1. **Dual Interface**: Implemented both REST API (`/api/pastes`) for programmatic access and HTML views (`/p/:id`) for user-friendly sharing
2. **Constraint Handling**: Pastes become unavailable when EITHER time-based expiry OR view limit is triggered, whichever comes first
3. **Test Mode Support**: Added `TEST_MODE=1` environment variable and `x-test-now-ms` header for deterministic expiry testing as required by the assignment
4. **Thread-Safe View Counting**: View count increments are handled atomically to prevent race conditions under concurrent load
5. **Clean Architecture**: Separated concerns with Controller-Service-Repository pattern for maintainability
3. Build the application
   mvn clean package

## Railway Deployment Notes
4. Run the application
   mvn spring-boot:run

To deploy on Railway:
5. The application will be available at
   http://localhost:8080

1. **Set Environment Variables:**
   ```
   SPRING_DATA_MONGODB_URI=mongodb://[your-mongodb-connection-string]
   APP_BASE_URL=https://your-app.vercel.app
   ```
## Persistence layer

2. **Build Command:**
   ```
   mvn clean package -DskipTests
   ```
This application uses MongoDB as the persistence layer.
MongoDB Atlas is used in production to ensure data persistence across requests and deployments.

3. **Start Command:**
   ```
   java -jar target/pastebin-lite-1.0.0.jar
   ```
## Important design decisions

4. **Health Check Endpoint:** Railway will use `/api/healthz` for health checks
- A persistent database (MongoDB) is used instead of in-memory storage to ensure compatibility with automated tests.
- API-based paste fetches increment the view count, while HTML page views do not.
- Time-based expiry (TTL) is enforced using stored expiry timestamps.
- View-count limits are strictly enforced, returning HTTP 404 once exceeded.
- Deterministic time testing is supported via the `x-test-now-ms` request header when test mode is enabled.
- All unavailable pastes consistently return HTTP 404 responses.
