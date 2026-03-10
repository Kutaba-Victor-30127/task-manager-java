# Task Manager API (Spring Boot)

A backend REST API for managing tasks built with *Java and Spring Boot*.
The project demonstrates a clean layered architecture, DTO usage, validation, undo/redo functionality, advanced querying and automated tests.

---

# Swagger UI

![Swagger UI](docs/swagger.png)

The API documentation is automatically generated using *OpenAPI / Swagger*.

Run the project and access:

http://localhost:8080/swagger-ui/index.html

---

# Features

* Full CRUD operations for tasks
* Change task status (TODO / IN_PROGRESS / DONE / BLOCKED)
* Undo / Redo operations
* Filtering, sorting and pagination
* Task statistics and analytics
* JSON and CSV export
* REST API with Swagger documentation
* Unit tests and integration tests

---

# Tech Stack

* Java 17
* Spring Boot
* Maven
* H2 Database (in-memory)
* OpenAPI / Swagger
* JUnit 5

---

# Architecture

The project follows a clean layered architecture:

Controller → Service → Repository

DTO → Mapper → Entity

Structure:

src/main/java
api → REST controllers
service → business logic
model → domain models
storage → persistence layer
mapper → entity to DTO mapping
config → Spring configuration

---

# API Endpoints

Tasks

GET /api/tasks
GET /api/tasks/{id}
POST /api/tasks
PUT /api/tasks/{id}
PATCH /api/tasks/{id}/status
DELETE /api/tasks/{id}
POST /api/tasks/query

Stats

GET /api/stats/count-by-status
GET /api/stats/overdue-by-status
GET /api/stats/bottleneck
GET /api/stats/audit/avg-minutes-by-status

---

# Example Request

POST /api/tasks

{
"title": "Finish project",
"description": "Complete the task manager API",
"priority": 3,
"deadline": "2026-04-01",
"status": "TODO",
"estimatedMinutes": 120
}

---

# Running the project

Clone the repository

git clone https://github.com/Kutaba-Victor-30127/task-manager-java.git

Run the application

mvn spring-boot:run

Swagger UI

http://localhost:8080/swagger-ui/index.html

---

# Running tests

mvn test

The project contains:

* Unit tests
* Integration tests

---

# Author

Victor Kutaba