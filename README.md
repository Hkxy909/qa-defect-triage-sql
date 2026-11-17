# qa-defect-triage-sql

Lightweight Java and SQLite tool for logging Android QA test execution results, recording defect tickets, and querying pass rates and failing modules.

## Stack

- Java 17
- SQLite (`sqlite-jdbc`)
- JUnit 5
- Maven

## Running it

Run the JUnit 5 test suite:
```bash
mvn test
```

Run the sample triage report:
```bash
mvn compile exec:java
```
