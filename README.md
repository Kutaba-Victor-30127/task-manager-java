# Task Manager Java

A full-stack task management application built with **Java Spring Boot** and **Vanilla JavaScript**.

It includes authentication with JWT, role-based access control, task CRUD operations, filtering, sorting, pagination, and live deployment for both backend and frontend.

## Live Demo

**Frontend**  
[Netlify App](PUNE_AICI_LINKUL_TAU_NETLIFY)

**Backend API Docs**  
[Swagger UI](https://task-manager-java-zrc8.onrender.com/swagger-ui/index.html#/)

---

## Tech Stack

**Backend**
- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Maven
- JWT

**Frontend**
- HTML
- CSS
- JavaScript

**Deployment**
- Render
- Netlify

---

## Features

- user registration and login
- JWT-based authentication
- role-based authorization (`USER`, `ADMIN`)
- create and view tasks
- admin access to all tasks
- filtering by status
- search by title
- sorting by id, deadline, and priority
- pagination
- loading states and task status badges in UI

---

## Architecture

The backend follows a layered architecture:

```text
Client
→ Controller
→ Service
→ Repository
→ Database
