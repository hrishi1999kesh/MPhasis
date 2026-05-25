# Event Ledger API

## Prerequisites
- Java 17+
- Maven 3.9+

## Run Application
```bash
mvn spring-boot:run
```

Application runs on:
```text
http://localhost:8080
```

## Run Tests
```bash
mvn test
```

## H2 Console
```text
http://localhost:8080/h2-console
```

JDBC URL:
```text
jdbc:h2:mem:eventdb
```

## Endpoints

### Create Event
POST `/events`

### Get Event By ID
GET `/events/{id}`

### Get Events By Account
GET `/events?account={accountId}`

### Get Account Balance
GET `/accounts/{accountId}/balance`