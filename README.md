# Insurance Claims Lifecycle API

A simple Spring Boot backend for managing insurance claims from submission through review, settlement, or rejection.

## Tech Stack

- Java 17
- Spring Boot 3.x
- Spring Web REST API
- Spring Data JPA
- H2 in-memory database
- Spring Kafka
- Lombok
- Swagger UI with springdoc-openapi

## Run with Docker

Make sure Docker Desktop is running, then start the full application stack:

```bash
docker compose up --build
```

This starts both:

- Kafka
- Spring Boot API

You do not need to run `mvn spring-boot:run` when using Docker Compose.

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

H2 console is available at:

```text
http://localhost:8080/h2-console
```

Use JDBC URL `jdbc:h2:mem:claimsdb`, username `sa`, and an empty password.

## Stop Docker

```bash
docker compose down
```

## Optional Local Development

If you prefer to run the Spring Boot app locally with Maven, start only Kafka in Docker:

```bash
docker compose up -d kafka
```

Then run the app locally:

```bash
mvn spring-boot:run
```

In this mode, Kafka listens on `localhost:9092`.

The application publishes lifecycle events to `claim-lifecycle-events`.

## Main Endpoints

- `POST /api/v1/claims` - create a claim
- `GET /api/v1/claims/{id}` - track a claim
- `PATCH /api/v1/claims/{id}/info` - provide additional claimant information
- `GET /api/v1/claims/queue` - view submitted unassigned claims
- `POST /api/v1/claims/{id}/assign` - assign a claim
- `PATCH /api/v1/claims/{id}/assessment` - update assessment details
- `GET /api/v1/claims/analytics/liability` - total active liability exposure

## Example Create Claim Request

```json
{
  "claimantId": "C1001",
  "claimantEmail": "claimant@example.com",
  "policyType": "MOTOR",
  "description": "Rear bumper damage after a minor collision.",
  "liabilityAmount": 1200.00
}
```
