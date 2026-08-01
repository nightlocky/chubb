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

## Run Kafka

```bash
docker compose up -d
```

Kafka listens on `localhost:9092` and the application publishes lifecycle events to `claim-lifecycle-events`.

## Run the Application

```bash
mvn spring-boot:run
```

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

H2 console is available at:

```text
http://localhost:8080/h2-console
```

Use JDBC URL `jdbc:h2:mem:claimsdb`, username `sa`, and an empty password.

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
