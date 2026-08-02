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
- `GET /api/v1/claims/analytics/liability` - liability summary by claim status

## Swagger Demo Test Plan

Open Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

If running with Docker, watch application and Kafka logs in another terminal:

```bash
docker compose logs -f app kafka
```

If running locally with Maven, watch the terminal where `mvn spring-boot:run` is running.

Use the generated claim id from the create claim response for later steps. The examples below use `<claimId>`.

### 1. Create Claim

Endpoint:

```text
POST /api/v1/claims
```

Input:

```json
{
  "claimantId": "C1001",
  "claimantEmail": "claimant@example.com",
  "policyType": "MOTOR",
  "description": "Rear bumper damage after a minor collision.",
  "liabilityAmount": 1200.00
}
```

Expected HTTP response:

```text
201 Created
```

Expected body:

```json
{
  "id": 1
}
```

The actual id may be different. Use that id for the rest of the demo.

Expected Kafka/application logs:

```text
Claim created: claimId=<claimId>, claimantId=C1001, status=SUBMITTED, liabilityAmount=1200.00
Claim lifecycle event published: claimId=<claimId>, action=CLAIM_CREATED, topic=claim-lifecycle-events, partition=<partition>, offset=<offset>
Claim lifecycle event received: claimId=<claimId>, action=CLAIM_CREATED, status=SUBMITTED, assignedOfficerId=null, liabilityAmount=1200.00
```

### 2. Track Claim

Endpoint:

```text
GET /api/v1/claims/<claimId>
```

Expected HTTP response:

```text
200 OK
```

Expected body:

```json
{
  "id": 1,
  "claimantId": "C1001",
  "claimantEmail": "claimant@example.com",
  "policyType": "MOTOR",
  "description": "Rear bumper damage after a minor collision.",
  "status": "SUBMITTED",
  "liabilityAmount": 1200,
  "assignedOfficerId": null,
  "staffNotes": null,
  "createdAt": "<timestamp>",
  "updatedAt": "<timestamp>"
}
```

No Kafka event is expected for read-only tracking.

### 3. View Unassigned Queue

Endpoint:

```text
GET /api/v1/claims/queue
```

Expected HTTP response:

```text
200 OK
```

Expected body:

```json
[
  {
    "id": 1,
    "claimantId": "C1001",
    "claimantEmail": "claimant@example.com",
    "policyType": "MOTOR",
    "description": "Rear bumper damage after a minor collision.",
    "status": "SUBMITTED",
    "liabilityAmount": 1200,
    "assignedOfficerId": null,
    "staffNotes": null,
    "createdAt": "<timestamp>",
    "updatedAt": "<timestamp>"
  }
]
```

No Kafka event is expected for this read-only endpoint.

### 4. Assign Claim

Endpoint:

```text
POST /api/v1/claims/<claimId>/assign
```

Input:

```json
{
  "officerId": "OFFICER-100"
}
```

Expected HTTP response:

```text
200 OK
```

Expected body:

```json
{
  "id": 1,
  "claimantId": "C1001",
  "claimantEmail": "claimant@example.com",
  "policyType": "MOTOR",
  "description": "Rear bumper damage after a minor collision.",
  "status": "IN_REVIEW",
  "liabilityAmount": 1200,
  "assignedOfficerId": "OFFICER-100",
  "staffNotes": null,
  "createdAt": "<timestamp>",
  "updatedAt": "<timestamp>"
}
```

Expected Kafka/application logs:

```text
Claim assigned: claimId=<claimId>, officerId=OFFICER-100, status=IN_REVIEW
Claim lifecycle event published: claimId=<claimId>, action=CLAIM_ASSIGNED, topic=claim-lifecycle-events, partition=<partition>, offset=<offset>
Claim lifecycle event received: claimId=<claimId>, action=CLAIM_ASSIGNED, status=IN_REVIEW, assignedOfficerId=OFFICER-100, liabilityAmount=1200.00
```

### 5. Request More Information During Assessment

Endpoint:

```text
PATCH /api/v1/claims/<claimId>/assessment
```

Input:

```json
{
  "status": "INFO_REQUESTED",
  "staffNotes": "Please provide the repair invoice."
}
```

Expected HTTP response:

```text
200 OK
```

Expected body:

```json
{
  "id": 1,
  "claimantId": "C1001",
  "claimantEmail": "claimant@example.com",
  "policyType": "MOTOR",
  "description": "Rear bumper damage after a minor collision.",
  "status": "INFO_REQUESTED",
  "liabilityAmount": 1200,
  "assignedOfficerId": "OFFICER-100",
  "staffNotes": "Please provide the repair invoice.",
  "createdAt": "<timestamp>",
  "updatedAt": "<timestamp>"
}
```

Expected Kafka/application logs:

```text
Claim assessment updated: claimId=<claimId>, status=INFO_REQUESTED, liabilityAmount=1200.00
Claim lifecycle event published: claimId=<claimId>, action=CLAIM_ASSESSED, topic=claim-lifecycle-events, partition=<partition>, offset=<offset>
Claim lifecycle event received: claimId=<claimId>, action=CLAIM_ASSESSED, status=INFO_REQUESTED, assignedOfficerId=OFFICER-100, liabilityAmount=1200.00
```

### 6. Provide Additional Information

Endpoint:

```text
PATCH /api/v1/claims/<claimId>/info
```

Input:

```json
{
  "additionalNotes": "Uploaded the repair invoice and accident photos."
}
```

Expected HTTP response:

```text
200 OK
```

Expected body:

```json
{
  "id": 1,
  "claimantId": "C1001",
  "claimantEmail": "claimant@example.com",
  "policyType": "MOTOR",
  "description": "Rear bumper damage after a minor collision.\n\nAdditional information: Uploaded the repair invoice and accident photos.",
  "status": "IN_REVIEW",
  "liabilityAmount": 1200,
  "assignedOfficerId": "OFFICER-100",
  "staffNotes": "Please provide the repair invoice.",
  "createdAt": "<timestamp>",
  "updatedAt": "<timestamp>"
}
```

Because the previous status was `INFO_REQUESTED`, the status automatically changes back to `IN_REVIEW`.

Expected Kafka/application logs:

```text
Additional claim information provided: claimId=<claimId>, claimantId=C1001, status=IN_REVIEW
Claim lifecycle event published: claimId=<claimId>, action=ADDITIONAL_INFO_PROVIDED, topic=claim-lifecycle-events, partition=<partition>, offset=<offset>
Claim lifecycle event received: claimId=<claimId>, action=ADDITIONAL_INFO_PROVIDED, status=IN_REVIEW, assignedOfficerId=OFFICER-100, liabilityAmount=1200.00
```

### 7. Update Liability Without Status Change

Endpoint:

```text
PATCH /api/v1/claims/<claimId>/assessment
```

Input:

```json
{
  "liabilityAmount": 950.00
}
```

Expected HTTP response:

```text
200 OK
```

Expected body:

```json
{
  "id": 1,
  "claimantId": "C1001",
  "claimantEmail": "claimant@example.com",
  "policyType": "MOTOR",
  "description": "Rear bumper damage after a minor collision.\n\nAdditional information: Uploaded the repair invoice and accident photos.",
  "status": "IN_REVIEW",
  "liabilityAmount": 950,
  "assignedOfficerId": "OFFICER-100",
  "staffNotes": "Please provide the repair invoice.",
  "createdAt": "<timestamp>",
  "updatedAt": "<timestamp>"
}
```

Expected application log:

```text
Claim assessment updated: claimId=<claimId>, status=IN_REVIEW, liabilityAmount=950.00
```

No Kafka event is expected here because the assessment did not change the claim status.

### 8. Settle Claim

Endpoint:

```text
PATCH /api/v1/claims/<claimId>/assessment
```

Input:

```json
{
  "status": "SETTLED",
  "staffNotes": "Claim settled after invoice review."
}
```

Expected HTTP response:

```text
200 OK
```

Expected body:

```json
{
  "id": 1,
  "claimantId": "C1001",
  "claimantEmail": "claimant@example.com",
  "policyType": "MOTOR",
  "description": "Rear bumper damage after a minor collision.\n\nAdditional information: Uploaded the repair invoice and accident photos.",
  "status": "SETTLED",
  "liabilityAmount": 950,
  "assignedOfficerId": "OFFICER-100",
  "staffNotes": "Claim settled after invoice review.",
  "createdAt": "<timestamp>",
  "updatedAt": "<timestamp>"
}
```

Expected Kafka/application logs:

```text
Claim assessment updated: claimId=<claimId>, status=SETTLED, liabilityAmount=950.00
Claim lifecycle event published: claimId=<claimId>, action=CLAIM_ASSESSED, topic=claim-lifecycle-events, partition=<partition>, offset=<offset>
Claim lifecycle event received: claimId=<claimId>, action=CLAIM_ASSESSED, status=SETTLED, assignedOfficerId=OFFICER-100, liabilityAmount=950.00
```

### 9. Check Liability Analytics

Endpoint:

```text
GET /api/v1/claims/analytics/liability
```

Expected HTTP response:

```text
200 OK
```

Expected body if this is the only claim and it has been settled:

```json
{
  "submittedCount": 0,
  "submittedLiability": 0,
  "inReviewCount": 0,
  "inReviewLiability": 0,
  "infoRequestedCount": 0,
  "infoRequestedLiability": 0,
  "settledCount": 1,
  "settledLiability": 950,
  "rejectedCount": 0,
  "rejectedLiability": 0,
  "outstandingCount": 0,
  "outstandingLiability": 0,
  "totalCount": 1,
  "totalLiability": 950
}
```

The response summarizes both claim counts and liability sums by status. `outstandingCount` and `outstandingLiability` include only `SUBMITTED`, `IN_REVIEW`, and `INFO_REQUESTED` claims.

Swagger may display money values without trailing zeroes, for example `1200` instead of `1200.00`. The numeric value is the same.

Analytics field meaning:

| Field | Meaning |
| --- | --- |
| `submittedCount` / `submittedLiability` | Count and liability sum for `SUBMITTED` claims. |
| `inReviewCount` / `inReviewLiability` | Count and liability sum for `IN_REVIEW` claims. |
| `infoRequestedCount` / `infoRequestedLiability` | Count and liability sum for `INFO_REQUESTED` claims. |
| `settledCount` / `settledLiability` | Count and liability sum for `SETTLED` claims. |
| `rejectedCount` / `rejectedLiability` | Count and liability sum for `REJECTED` claims. |
| `outstandingCount` / `outstandingLiability` | Count and liability sum for active outstanding claims: `SUBMITTED`, `IN_REVIEW`, and `INFO_REQUESTED`. |
| `totalCount` / `totalLiability` | Count and liability sum across all statuses. |

Expected application log:

```text
Liability metric calculated: outstandingCount=0, outstandingLiability=0, totalCount=1, totalLiability=950.00
Liability by status calculated: submittedCount=0, submitted=0, inReviewCount=0, inReview=0, infoRequestedCount=0, infoRequested=0, settledCount=1, settled=950.00, rejectedCount=0, rejected=0
```

## Optional Swagger Error Tests

### Blank Required Field

Endpoint:

```text
PATCH /api/v1/claims/<claimId>/info
```

Input:

```json
{
  "additionalNotes": ""
}
```

Expected HTTP response:

```text
400 Bad Request
```

Expected body:

```json
{
  "timestamp": "<timestamp>",
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "additionalNotes": "additionalNotes is required"
  }
}
```

### Invalid Enum Value

Endpoint:

```text
PATCH /api/v1/claims/<claimId>/assessment
```

Input:

```json
{
  "status": "IN-REVIEW"
}
```

Expected HTTP response:

```text
400 Bad Request
```

Expected body:

```json
{
  "timestamp": "<timestamp>",
  "status": 400,
  "message": "Invalid request body. Check JSON syntax, field types, and enum values such as status."
}
```

Use `IN_REVIEW` instead of `IN-REVIEW`.

### Missing Assessment Fields

Endpoint:

```text
PATCH /api/v1/claims/<claimId>/assessment
```

Input:

```json
{}
```

Expected HTTP response:

```text
400 Bad Request
```

Expected body:

```json
{
  "timestamp": "<timestamp>",
  "status": 400,
  "message": "At least one assessment field must be provided"
}
```

### Claim Not Found

Endpoint:

```text
GET /api/v1/claims/99999
```

Expected HTTP response:

```text
404 Not Found
```

Expected body:

```json
{
  "timestamp": "<timestamp>",
  "status": 404,
  "message": "Claim not found"
}
```

### Optional Rate Limit Check

Protected write endpoints are limited to 10 requests per 60 seconds per client IP:

```text
POST  /api/v1/claims
PATCH /api/v1/claims/<claimId>/info
POST  /api/v1/claims/<claimId>/assign
PATCH /api/v1/claims/<claimId>/assessment
```

This is easier to test with curl, Postman, or scripted requests than Swagger because it requires sending more than 10 write requests within 60 seconds.

Expected HTTP response:

```text
429 Too Many Requests
```

Expected body:

```json
{
  "timestamp": "<timestamp>",
  "status": 429,
  "message": "Too many requests. Please try again later.",
  "retryAfterSeconds": 60
}
```

## Kafka Demo Notes

Kafka is demonstrated through REST-triggered lifecycle events. Swagger calls the REST endpoints, and the application logs show producer and consumer behavior.

Kafka publish events are expected for:

```text
CLAIM_CREATED
CLAIM_ASSIGNED
CLAIM_ASSESSED
ADDITIONAL_INFO_PROVIDED
```

Kafka publish success log:

```text
Claim lifecycle event published: claimId=<claimId>, action=<action>, topic=claim-lifecycle-events, partition=<partition>, offset=<offset>
```

Kafka consumer log:

```text
Claim lifecycle event received: claimId=<claimId>, action=<action>, status=<status>, assignedOfficerId=<officerId>, liabilityAmount=<amount>
```

Kafka publish failure log:

```text
Failed to publish claim lifecycle event: claimId=<claimId>, action=<action>
```

Dead-letter topic:

```text
claim-lifecycle-events.DLT
```

The dead-letter topic is used when `NotificationService` fails to process an event after retries. Normal Swagger requests should not produce DLT messages because the current consumer only logs valid events.
