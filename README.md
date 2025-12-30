# Pastebin-Lite

A small Pastebin-like application that allows users to create text pastes and share a link to view them.
Pastes can optionally expire based on time-to-live (TTL) or a maximum view count.

## Deployed URL

https://pastebin-lite-production.up.railway.app

## How to run the app locally

### Prerequisites
- Java 17
- Maven
- MongoDB (local or MongoDB Atlas)

### Steps

1. Clone the repository
   git clone <your-github-repo-url>
   cd pastebin-lite

2. Configure application properties
   Update the following properties in `src/main/resources/application.properties`:

   spring.data.mongodb.uri=<your-mongodb-connection-string>
   app.base-url=http://localhost:8080

3. Build the application
   mvn clean package

4. Run the application
   mvn spring-boot:run

5. The application will be available at
   http://localhost:8080

## Persistence layer

This application uses MongoDB as the persistence layer.
MongoDB Atlas is used in production to ensure data persistence across requests and deployments.

## Important design decisions

- A persistent database (MongoDB) is used instead of in-memory storage to ensure compatibility with automated tests.
- API-based paste fetches increment the view count, while HTML page views do not.
- Time-based expiry (TTL) is enforced using stored expiry timestamps.
- View-count limits are strictly enforced, returning HTTP 404 once exceeded.
- Deterministic time testing is supported via the `x-test-now-ms` request header when test mode is enabled.
- All unavailable pastes consistently return HTTP 404 responses.
