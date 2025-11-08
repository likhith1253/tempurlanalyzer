# SentinelAI Guard - Backend

## Overview
SentinelAI Guard is an AI-powered cybersecurity tool that analyzes logs and URLs for threats in real-time. This is the backend service built with Spring Boot.

## Project Structure
```
backend-java/
├── src/
│   └── main/
│       ├── java/com/sentinelai/guard/
│       │   ├── config/         # Configuration classes
│       │   ├── controller/     # REST controllers
│       │   ├── model/          # Domain models
│       │   ├── repository/     # Data access layer
│       │   ├── security/       # Security configuration
│       │   ├── service/        # Business logic
│       │   └── util/           # Utility classes
│       └── resources/          # Configuration files
└── pom.xml                    # Maven configuration
```

## Prerequisites
- Java 17 or higher
- Maven 3.6.3 or higher
- H2 Database (embedded)
- (Optional) Firebase Admin SDK credentials

## Getting Started

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd sentinel-ai-guard/backend-java
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**
   - API Documentation: http://localhost:8080/api/swagger-ui.html
   - H2 Console: http://localhost:8080/api/h2-console
     - JDBC URL: jdbc:h2:mem:sentinelai
     - Username: sa
     - Password: password

## API Endpoints

### Authentication
- `POST /api/auth/login` - User authentication
- `POST /api/auth/refresh` - Refresh access token

### Log Analysis
- `POST /api/analysis/logs` - Analyze log entries
- `GET /api/analysis/results/{id}` - Get analysis results

### URL Scanning
- `POST /api/scan/url` - Scan a URL for threats
- `GET /api/scan/status/{id}` - Get scan status

## Configuration
Edit `src/main/resources/application.yml` to configure:
- Server port and context path
- Database settings
- JWT security settings
- Firebase integration
- Logging levels

## Development

### Code Style
This project follows the Google Java Style Guide with the following exceptions:
- Line length: 120 characters
- Indentation: 4 spaces

### Building
```bash
# Compile and run tests
mvn clean verify

# Run the application with Maven
mvn spring-boot:run

# Build a Docker image
mvn spring-boot:build-image
```

## Deployment

### Docker
```bash
docker build -t sentinel-ai-guard .
docker run -p 8080:8080 sentinel-ai-guard
```

### Kubernetes
Example deployment files are available in the `k8s/` directory.

## Monitoring
Application metrics are available via Spring Boot Actuator endpoints:
- Health: `/api/actuator/health`
- Metrics: `/api/actuator/metrics`
- Prometheus: `/api/actuator/prometheus`

## License
This project is licensed under the MIT License - see the LICENSE file for details.
