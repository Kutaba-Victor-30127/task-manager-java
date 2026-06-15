# Task Manager Java

## Overview

Task Manager Java is a full-stack task management application built to demonstrate modern Java backend development practices using Spring Boot, Spring Security, PostgreSQL, JWT authentication, automated testing, and cloud deployment.

The project evolved from a console-based application into a production-style REST API with authentication, authorization, validation, pagination, filtering, sorting, testing, and deployment.

The goal of the project was not only to implement CRUD functionality, but also to apply software engineering principles commonly used in real-world backend systems.

---

# Live Demo

### Frontend

https://gorgeous-churros-7fff2c.netlify.app/

### API Documentation (Swagger)

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

**43 automated tests passing**

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

The project follows a layered architecture that separates responsibilities between presentation, business logic, persistence, and storage layers.

---

# Authentication & Security

Authentication is implemented using JSON Web Tokens (JWT).

Workflow:

1. User registers an account
2. Password is hashed using BCrypt
3. User logs in
4. Backend generates a JWT token
5. Client sends the token in the Authorization header
6. JWT filter validates incoming requests
7. Spring Security authorizes access based on roles

Implemented security features:

* BCrypt password hashing
* JWT authentication
* Stateless sessions
* USER and ADMIN roles
* Custom access denied handling
* Custom authentication entry point
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

```http
GET /api/tasks?status=TODO
```

## Search

```http
GET /api/tasks?title=meeting
```

## Sorting

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

Example response:

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

* TaskService
* UserService

### Integration Tests

* Service integration testing
* Database integration testing

### Repository Tests

* JPA persistence verification

### Controller Tests

* MockMvc endpoint testing

### End-to-End Tests

* Complete API workflow validation

Current test status:

```text
Tests Run: 43
Failures: 0
Errors: 0
```

---

# Deployment

## Frontend

* Netlify

## Backend

* Render

## Database

* PostgreSQL

Sensitive configuration is stored using environment variables:

* JWT secrets
* database credentials
* deployment configuration

---

# Project Structure

```text
task-manager-java
│
├── backend
│   ├── controllers
│   ├── services
│   ├── repositories
│   ├── security
│   ├── dto
│   └── tests
│
├── frontend
│
├── console-app
│
└── README.md
```

---

# Project Evolution

### Version 1

* Console Application
* File Persistence
* Core Business Logic

### Version 2

* Spring Boot REST API
* PostgreSQL
* JWT Authentication
* Spring Security
* Swagger

### Version 3

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
