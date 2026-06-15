# Task Manager Java

## Overview

Task Manager Java is a full-stack task management application built to demonstrate modern Java backend development practices using Spring Boot, Spring Security, PostgreSQL, JWT authentication, automated testing, and cloud deployment.

The project evolved from a console-based application into a production-style REST API with authentication, authorization, validation, pagination, filtering, sorting, testing, and deployment.

The goal of the project was not only to implement CRUD functionality, but also to apply software engineering principles commonly used in real-world backend systems.

---

# Live Demo

### Frontend

https://gorgeous-churros-7fff2c.netlify.app/

### Backend API

https://task-manager-java-zrc8.onrender.com

### Swagger Documentation

https://task-manager-java-zrc8.onrender.com/swagger-ui/index.html

### GitHub Repository

https://github.com/Kutaba-Victor-30127/task-manager-java

---

# Key Technical Concepts Demonstrated

## Backend Development

* REST API design
* Layered Architecture
* DTO pattern
* Service layer abstraction
* Repository pattern
* Dependency Injection
* Validation using Jakarta Validation
* Exception handling
* Pagination and sorting
* Authentication and authorization
* Database persistence
* Automated testing

## Spring Ecosystem

* Spring Boot
* Spring Security
* Spring Data JPA
* Spring Validation
* Spring MVC
* Spring Test
* MockMvc

## Database

* PostgreSQL
* Hibernate ORM
* JPA
* Entity relationships
* Query optimization
* Pagination using Pageable

## Security

* JWT Authentication
* BCrypt password hashing
* Stateless authentication
* Role-based authorization
* Security filters
* Access denied handling
* Authentication entry points
* Environment variable configuration

## Testing

* Unit Tests
* Integration Tests
* Repository Tests
* Controller Tests
* End-to-End Tests
* MockMvc Tests

43 automated tests currently passing.

---

# System Architecture

```text
Frontend (HTML/CSS/JavaScript)
            │
            ▼
     REST Controllers
            │
            ▼
        Services
            │
            ▼
      Repositories
            │
            ▼
       PostgreSQL
```

The project follows a layered architecture that separates responsibilities between:

### Controller Layer

Responsible for:

* HTTP request handling
* request validation
* response mapping
* API endpoint exposure

### Service Layer

Responsible for:

* business logic
* authorization checks
* validation rules
* application workflows

### Repository Layer

Responsible for:

* database communication
* persistence operations
* query execution

### Database Layer

Responsible for:

* permanent storage
* data consistency
* relational data management

---

# Authentication & Security

Authentication is implemented using JSON Web Tokens (JWT).

Workflow:

1. User registers an account.
2. Password is hashed using BCrypt.
3. User logs in.
4. Backend generates a JWT token.
5. Client sends the token in the Authorization header.
6. JWT filter validates every request.
7. Spring Security authorizes access based on user roles.

Implemented security features:

* BCrypt password hashing
* JWT authentication
* Stateless sessions
* User/Admin roles
* Access denied handling
* Authentication entry point customization
* Environment variable secrets

Example:

```http
Authorization: Bearer <jwt-token>
```

---

# Database Model

## User

| Field    | Type   |
| -------- | ------ |
| id       | Long   |
| username | String |
| password | String |
| role     | String |

## Task

| Field            | Type       |
| ---------------- | ---------- |
| id               | Long       |
| title            | String     |
| description      | String     |
| priority         | Integer    |
| deadline         | LocalDate  |
| status           | TaskStatus |
| estimatedMinutes | Integer    |

Relationship:

```text
User (1)
   │
   ▼
Task (Many)
```

---

# API Features

## Authentication

* Register
* Login

## Task Management

* Create Task
* Read Tasks
* Update Task
* Delete Task

## Filtering

Filter tasks by status:

```http
GET /api/tasks?status=TODO
```

## Search

Search tasks by title:

```http
GET /api/tasks?title=meeting
```

## Sorting

Examples:

```http
GET /api/tasks?sort=deadline
```

```http
GET /api/tasks?sort=priority
```

## Pagination

```http
GET /api/tasks?page=0&size=5
```

Returns:

```json
{
  "content": [...],
  "page": 0,
  "size": 5,
  "totalElements": 9,
  "totalPages": 2
}
```

---

# Testing Strategy

The project contains automated tests covering multiple layers of the application.

### Unit Tests

Test isolated business logic.

Examples:

* TaskService
* UserService

### Integration Tests

Verify collaboration between Spring components and database interactions.

### Repository Tests

Validate JPA persistence behavior.

### Controller Tests

Use MockMvc to verify API endpoints.

### End-to-End Tests

Verify complete request flows.

Current status:

```text
Tests Run: 43
Failures: 0
Errors: 0
```

---

# Deployment

The application is deployed using cloud services.

Frontend:

* Netlify

Backend:

* Render

Database:

* PostgreSQL

Environment configuration:

* JWT secrets
* database credentials
* deployment configuration

are stored using environment variables.

---

# Project Evolution

Version 1

* Console Application
* File Persistence
* Core Business Logic

Version 2

* Spring Boot REST API
* PostgreSQL
* JWT Authentication
* Spring Security
* Swagger

Version 3

* Frontend Integration
* Cloud Deployment
* Automated Testing
* Security Hardening

---

# What I Learned

Through this project I gained hands-on experience with:

* Java 17
* Spring Boot
* Spring Security
* JWT Authentication
* PostgreSQL
* JPA / Hibernate
* REST API Development
* Automated Testing
* Cloud Deployment
* Git & GitHub Workflows
* Environment Variable Management
* Software Architecture Principles

---

# Future Improvements

* Docker support
* CI/CD with GitHub Actions
* Refresh Tokens
* User Profiles
* Email Notifications
* React Frontend
* Redis Caching
* Monitoring and Logging
