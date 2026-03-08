A backend system built using **Java and Spring Boot** that manages tasks through a controlled **state-driven workflow**. The application exposes REST APIs to create and manage tasks while ensuring valid lifecycle transitions and transactional consistency.

---

## Tech Stack

* Java
* Spring Boot
* MySQL / H2
* Maven
* JUnit
* Postman

---

## Architecture

The project follows a **layered architecture**:

```
Controller → Service → Repository → Database
```

* **Controller** – Handles REST API requests
* **Service** – Contains business logic and task lifecycle management
* **Repository** – Handles database operations using Spring Data JPA

---

## Task Workflow

Tasks move through defined states:

```
PENDING → PROCESSING → COMPLETED
                 ↘
                  FAILED
```

This ensures valid task transitions and prevents inconsistent updates.

---

## Key Features

* RESTful APIs built with Spring Boot
* DTO-based request/response separation
* Transaction management using `@Transactional`
* Centralized exception handling
* API testing using Postman
* Unit tests using JUnit

---
