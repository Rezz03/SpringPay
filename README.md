# SpringPay Payment Gateway

A modern, Spring Boot-based payment gateway API for processing payments, managing merchants, and handling refunds. This project is designed as a portfolio showcase demonstrating backend engineering best practices, modular architecture, and production-ready code.

## Features

- **Merchant Management**: Registration, authentication, and API key generation
- **Payment Lifecycle**: Create, complete, refund, and query payments
- **API Key Authentication**: Secure merchant access control
- **Audit Trail**: Complete transaction history for compliance
- **Webhook Simulation**: Payment status notifications
- **RESTful API**: Clean, well-documented endpoints
- **Docker Support**: Containerized development and deployment
- **Database Migrations**: Version-controlled schema with Flyway
- **OpenAPI Documentation**: Interactive API docs with Swagger UI

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **PostgreSQL 15**
- **Maven**
- **Docker & Docker Compose**
- **Flyway** for database migrations
- **JUnit 5, Mockito** for testing
- **SpringDoc OpenAPI** for API documentation
- **Lombok** for reducing boilerplate

## Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **Docker** and **Docker Compose**
- **Git**

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/springpay.git
cd springpay
```

### 2. Start the Database

Use Docker Compose to spin up PostgreSQL and pgAdmin:

```bash
docker-compose up -d
```

This will start:
- **PostgreSQL** on `localhost:5432`
  - Database: `springpay`
  - Username: `postgres`
  - Password: `postgres`
- **pgAdmin** on `localhost:5050`
  - Email: `admin@springpay.com`
  - Password: `admin`

### 3. Run the Application

#### Option A: Using Maven

```bash
mvn spring-boot:run
```

#### Option B: Using Docker

Build and run the application container:

```bash
docker build -t springpay:latest .
docker run -p 8080:8080 --network springpay_default springpay:latest
```

### 4. Access the API

- **Application**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs
- **Health Check**: http://localhost:8080/actuator/health

## Development Workflow

### Running Tests

```bash
# Run all tests
mvn test

# Run tests with coverage report
mvn clean test jacoco:report

# View coverage report at: target/site/jacoco/index.html
```

### Building the Project

```bash
# Clean build
mvn clean package

# Skip tests
mvn clean package -DskipTests
```

### Database Migrations

Flyway migrations are located in `src/main/resources/db/migration/`. They run automatically on application startup.

To manually apply migrations:

```bash
mvn flyway:migrate
```

To view migration status:

```bash
mvn flyway:info
```

### Profiles

The application supports multiple profiles:

- **dev**: Development (default)
  - SQL logging enabled
  - Debug level logging

- **test**: Testing
  - H2 in-memory database
  - Flyway disabled

- **prod**: Production
  - Minimal logging
  - No stack traces in responses

Set the active profile:

```bash
# Via environment variable
export SPRING_PROFILES_ACTIVE=prod

# Via Maven
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## API Usage

### Register a Merchant

```bash
curl -X POST http://localhost:8080/api/merchants/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corp",
    "email": "merchant@acme.com",
    "password": "SecurePass123!"
  }'
```

Response:
```json
{
  "id": 1,
  "name": "Acme Corp",
  "email": "merchant@acme.com",
  "apiKey": "sk_live_abc123...",
  "status": "PENDING",
  "createdAt": "2025-11-18T10:00:00"
}
```

### Create a Payment

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "X-API-Key: sk_live_abc123..." \
  -d '{
    "amount": 99.99,
    "currency": "USD",
    "description": "Product purchase"
  }'
```

### Query Payment Status

```bash
curl http://localhost:8080/api/payments/1 \
  -H "X-API-Key: sk_live_abc123..."
```

## Project Structure

```
springpay/
├── src/
│   ├── main/
│   │   ├── java/com/springpay/
│   │   │   ├── config/           # Configuration classes
│   │   │   ├── controller/       # REST controllers
│   │   │   ├── dto/              # Data transfer objects
│   │   │   ├── entity/           # JPA entities
│   │   │   ├── enums/            # Enumerations
│   │   │   ├── exception/        # Custom exceptions
│   │   │   ├── repository/       # Data repositories
│   │   │   ├── security/         # Security configuration
│   │   │   ├── service/          # Business logic
│   │   │   └── SpringPayApplication.java
│   │   └── resources/
│   │       ├── db/migration/     # Flyway migrations
│   │       ├── application.properties
│   │       └── application-{profile}.properties
│   └── test/
│       └── java/com/springpay/   # Test classes
├── documentation/                 # Project documentation
│   ├── epics/                    # Epic definitions
│   └── api-contracts/            # API specifications
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── CLAUDE.md                     # Developer guide
└── README.md
```

## Payment Lifecycle

Payments follow this state machine:

```
PENDING → SUCCESS → (optional) REFUNDED
   ↓
FAILED
```

- **PENDING**: Payment created, awaiting completion
- **SUCCESS**: Payment completed successfully
- **FAILED**: Payment failed during processing
- **REFUNDED**: Previously successful payment has been refunded

## Security

- **API Key Authentication**: All protected endpoints require an API key in the `X-API-Key` header
- **Password Hashing**: BCrypt with cost factor 12
- **Input Validation**: Request DTOs validated with Bean Validation
- **SQL Injection Prevention**: Parameterized queries via JPA
- **Rate Limiting**: (To be implemented)

⚠️ **Note**: This is a portfolio project. For production use, implement additional security measures such as:
- HTTPS/TLS
- Rate limiting
- CORS configuration
- API key rotation
- PCI DSS compliance

## Testing

The project maintains 80% code coverage minimum. Tests include:

- **Unit Tests**: Service layer logic with mocked dependencies
- **Integration Tests**: Full API endpoints with test database
- **Repository Tests**: Database queries with Testcontainers

Run tests with coverage:

```bash
mvn clean test jacoco:report
```

## Database Schema

Key entities:

- **Merchant**: Merchant accounts with API keys
- **Payment**: Payment records with lifecycle status
- **Transaction**: Audit trail of all payment actions
- **ApiKey**: Multiple API keys per merchant
- **WebhookLog**: Webhook delivery tracking
- **EmailVerificationToken**: Email verification flow
- **PasswordResetToken**: Password reset flow

See `src/main/resources/db/migration/V1__initial_schema.sql` for the complete schema.

## Troubleshooting

### Database Connection Issues

If you encounter connection errors:

1. Ensure PostgreSQL is running:
   ```bash
   docker-compose ps
   ```

2. Check database logs:
   ```bash
   docker-compose logs postgres
   ```

3. Verify connection settings in `application-dev.properties`

### Port Already in Use

If port 8080 is already in use:

```bash
# Change the port in application.properties
server.port=8081

# Or via environment variable
export SERVER_PORT=8081
```

### Maven Dependency Issues

```bash
# Clear Maven cache
mvn dependency:purge-local-repository

# Re-download dependencies
mvn clean install
```

## Contributing

This is a portfolio project, but suggestions and improvements are welcome!

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

## Roadmap

Future enhancements planned:

- [ ] Subscription billing module
- [ ] Fraud detection engine
- [ ] Multi-currency support
- [ ] Payout management
- [ ] Dashboard UI
- [ ] Microservices architecture
- [ ] Cloud deployment (AWS/GCP)

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contact

**Author**: Perez
**Email**: support@springpay.com
**Project Link**: https://github.com/yourusername/springpay

---

Built with ☕ and Spring Boot
