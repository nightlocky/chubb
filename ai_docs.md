Build a simple Spring Boot backend project for an insurance claims lifecycle system. You may refer to the word doc in the repository for reference.

Use:

- Java 17+
- Spring Boot 3.x
- REST API
- Spring Data JPA
- H2 in-memory SQL database
- Kafka using Spring Kafka
- docker-compose for Kafka
- Lombok
- Swagger UI using springdoc-openapi-starter-webmvc-ui
- jakarta.validation.* for validation, not javax.validation.*

Keep the code simple and readable. Avoid unnecessary extra DTOs or complex abstractions.

PROJECT STRUCTURE

Use package:

com.chubbs.claims

Create this structure:

src/main/java/com/chubbs/claims
  ClaimsApplications.java
  controller/
    ClaimantController.java
    StaffController.java
    AnalyticsController.java
  service/
    ClaimService.java
    NotificationService.java
  repository/
    ClaimRepository.java
  model/
    Claim.java
    ClaimStatus.java
  dto/
    ClaimRequestDTO.java
    ClaimResponseDTO.java
    LiabilityMetricDTO.java
    AdditionalInfoRequestDTO.java
    AssignmentRequestDTO.java
    AssessmentRequestDTO.java

  exception/
    GlobalExceptionHandler.java

src/main/resources
  application.properties

root
  pom.xml
  docker-compose.yml
  Readme.md

DATABASE / MODEL

Create one JPA entity named Claim.

Claim.java must contain exactly these fields:
Long id

- Primary key
- Auto-generated

String claimantId

String claimantEmail

String policyType

- Accepted values are MOTOR or PROPERTY

String description

ClaimStatus status

- Enum values:
  SUBMITTED
  IN_REVIEW
  INFO_REQUESTED
  SETTLED
  REJECTED

BigDecimal liabilityAmount

String assignedOfficerId

- Nullable

String staffNotes

LocalDateTime createdAt

- Auto-set when the row is first created

LocalDateTime updatedAt

- Auto-set whenever the row is updated

DTOs

Create these main DTOs:

ClaimRequestDTO

- claimantId
- claimantEmail
- policyType
- description
- liabilityAmount

ClaimResponseDTO

- id
- claimantId
- claimantEmail
- policyType
- description
- status
- liabilityAmount
- assignedOfficerId
- staffNotes
- createdAt
- updatedAt

LiabilityMetricDTO

- totalLiability

Additional request DTOs are allowed only if useful for validation and Swagger clarity:

- AdditionalInfoRequestDTO with additionalNotes
- AssignmentRequestDTO with officerId
- AssessmentRequestDTO with optional liabilityAmount, status, staffNotes

Do not create unnecessary response DTOs or event DTOs.

REST API BASE PATH

All controllers should use this base path: /api/v1/claims

CLAIMANT CONTROLLER
Endpoints:

1. Create Claim

POST /api/v1/claims

Input:
ClaimRequestDTO

Action:

- Validate input
- Save to H2 database
- Set status to SUBMITTED
- Publish Kafka event

Output:
201 Created returning Map<String, Long>:

Return a simple JSON object:

2. Track Claim
   GET /api/v1/claims/{id}

Action:

- Find claim by id
- If not found, return 404 through exception handling
  Output:
  200 OK returning ClaimResponseDTO containing all claim fields.

3. Provide Additional Information

PATCH /api/v1/claims/{id}/info

Input:
AdditionalInfoRequestDTO

Action:

- Validate additionalNotes exists and is not blank
- Append additionalNotes to the existing description
- If current status is INFO_REQUESTED, automatically change status back to IN_REVIEW
- Save updated claim
- Publish Kafka lifecycle event

Output:
200 OK returning updated ClaimResponseDTO.

STAFF CONTROLLER

Endpoints:

1. See Unassigned Queue

GET /api/v1/claims/queue

Action:

- Fetch claims where status = SUBMITTED and assignedOfficerId is null

Output:
200 OK returning List of claims

2. Assign Claim

POST /api/v1/claims/{id}/assign

Input:
AssignmentRequestDTO

Action:

- Validate officerId exists and is not blank
- Set assignedOfficerId
- Change status to IN_REVIEW
- Save updated claim
- Publish Kafka lifecycle event

Output:
200 OK returning updated ClaimResponseDTO.

3. Assess Claim

PATCH /api/v1/claims/{id}/assessment

Input:
AssessmentRequestDTO

Fields:

- liabilityAmount optional
- status optional
- staffNotes optional

Action:

- liabilityAmount, status, staffNotes are optional
- at least one field must be provided

Output:
200 OK returning updated ClaimResponseDTO.

ANALYTICS CONTROLLER

Endpoint:

GET /api/v1/claims/analytics/liability

Action:

- Calculate total liability exposure
- Sum liabilityAmount for all claims where status is one of:
  SUBMITTED
  IN_REVIEW
  INFO_REQUESTED

Do not include:

- SETTLED
- REJECTED

Output:
200 OK returning:

REPOSITORY

It should extend JpaRepository<Claim, Long>

Add method-name query:

List<Claim></claim> findByStatusAndAssignedOfficerIdIsNull(ClaimStatus status);

Add custom JPQL query:

@Query("select sum(c.liabilityAmount) from Claim c where c.status in :statuses")
BigDecimal sumLiabilityAmountByStatusIn(@Param("statuses") Collection<ClaimStatus></claimstatus> statuses);

SERVICE

Create ClaimService.java.

This should contain the business logic.

Use:

@Service
@RequiredArgsConstructor

Inject:

- ClaimRepository
- KafkaTemplate<String, Map<String, Object>>

Keep the service easy to read.

Kafka event map should include:

- claimId
- claimantId
- status
- assignedOfficerId
- liabilityAmount
- action
- eventTime

KAFKA

Producer:
In ClaimService, publish a Kafka JSON event to topic:

claim-lifecycle-events

Publish when:

- claim is created
- additional info is provided
- claim is assigned
- claim status changes during assessment

Consumer:
Create NotificationService.java.

Use:

Add a Kafka listener:

@KafkaListener(topics = "${claims.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")

The listener should receive:

Map<String, Object> event

It should log:

- claimId
- action
- status
- assignedOfficerId
- liabilityAmount

EXCEPTION HANDLING

Create GlobalExceptionHandler.java.

Use:

@RestControllerAdvice

Handle these exceptions:

- ResponseStatusException
- IllegalArgumentException
- MethodArgumentNotValidException
- HttpMessageNotReadableException
- Exception

Return clean JSON errors using Map<String, Object>.

DOCKER COMPOSE

Create docker-compose.yml at the root.
It should start one lightweight modern Kafka broker.
Use Apache Kafka image.

README

Create Readme.md.

DOCUMENTATION / COMMENTS

Add short Java Javadocs above public controller methods and service methods.

Use Java style:

/**

* Description.
* 
* @param id claim id
* @return claim response
  */

Keep comments short, readable and understandable

The app should be testable through Swagger UI.

AI response is in the first commit

When I send an invalid enum value for the assessment status, such as "IN-REVIEW" instead of "IN_REVIEW", the API returns a 400 response with the message "Invalid request body".

Can you update the GlobalExceptionHandler so the HttpMessageNotReadableException response message is clearer? It should explain that the request body may have invalid JSON syntax, incorrect field types, or invalid enum values such as status

AI response:

The exception handler is working. The issue is that `"IN-REVIEW"` does not match your enum value `IN_REVIEW`. Because Spring/Jackson cannot convert the JSON body into `AssessmentRequestDTO`, it throws `HttpMessageNotReadableException` before validation runs.

I suggested changing the message in this handler:

```
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException ex
) {
    return ResponseEntity.badRequest().body(error(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid request body"
    ));
}
```

to a clearer but still general message:

```
@ExceptionHandler(HttpMessageNotReadableException.class)
public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(
        HttpMessageNotReadableException ex
) {
    return ResponseEntity.badRequest().body(error(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid request body. Check JSON syntax, field types, and enum values such as status."
    ));
}
```

Reason: `HttpMessageNotReadableException` is not only for invalid enum values. It can also happen for broken JSON, wrong field types, or empty request bodies. So the message should mention enum values but stay general.

Question:
Enable CORS for the Spring Boot backend so that a future frontend running on http://localhost:3000 can call the REST API.

Add a simple global CORS configuration class using WebMvcConfigurer. Apply it only to the API routes under /api/v1/claims/**. Allow the common HTTP methods GET, POST, PATCH, DELETE, and OPTIONS, and allow all request headers.

Keep the implementation simple and readable.

AI answer:

package com.chubbs.claims.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/v1/claims/**")
                        .allowedOriginPatterns("http://localhost:*")
                        .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
            }
        };
    }
}

I added app.cors.allowed-origins=http://localhost:* in application.properties and @Value("${app.cors.allowed-origins}") to make it the code more dynamic instead of hardcodingthe url

Question

implement rate limiting for these endpoints using bucket4j.

POST  /api/v1/claims
PATCH /api/v1/claims/{id}/info
POST  /api/v1/claims/{id}/assign
PATCH /api/v1/claims/{id}/assessment

AI Answer:

Provided RateLimitingFilter.Java file

Questions
Include a docker file for my applicaiton and update the docker-compose to include this container.

AI answer:
Provided Dockerfile

Question
Add simple structured logging to the ClaimService business operations. Log important lifecycle actions such as claim creation, additional information submission. Include useful identifiers such as claimId, claimantId, officerId, status, and liabilityAmount where relevant. Dont logg sensitive fields such as claimantEmail, description, additionalNotes, and staffNotes.

Question

Decouple Kafka event publishing from the ClaimService business logic.

Create a separate ClaimEventPublisher service responsible for building and publishing claim lifecycle events to Kafka. ClaimService should focus only on claim business operations and call ClaimEventPublisher after successful claim creation, additional information submission, assignment, and assessment status changes. Also add logging for Kafka publish success and failure. On success, log the claimId, action, topic, partition, and offset. On failure, log the claimI d, action, and exception stack trace.

Keep the event payload unchanged and keep the implementation simple and readable.

AI answer:
Created ClaimEventPublisher.java

Question
Implement a dead letter queue for kafka.

AI answer:
Added  KafkaErrorHandlerConfig.java and DeadLetterNotificationService.java

Question:
Update liability analytics API and README demo documentation. Also Update the liability analytics API so it gives managers a clearer real-time summary of claims and liability exposure.

AI answer:

updated the code for readme, analytic controller, claim repository, LiabiltyMetricDTO, and claim service
