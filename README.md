# Task Manager Java

A small full-stack task management project built with **Java Spring Boot**, **PostgreSQL**, and a simple **HTML/CSS/JavaScript** frontend.

The project started from a console-based version and was later extended into a REST API with authentication, role-based access control, pagination, filtering, sorting, and live deployment.

## Live Links

- **Frontend:** [Netlify App](https://gorgeous-churros-7fff2c.netlify.app/)
- **Backend Swagger:** [Swagger UI](https://task-manager-java-zrc8.onrender.com/swagger-ui/index.html#/)
- **Repository:** [GitHub](https://github.com/Kutaba-Victor-30127/task-manager-java)

---

## Project Overview

This repository contains three parts:

### 1. `console-app`
The initial version of the project.  
A simple console-based task manager focused on core logic, file persistence, and clean separation of responsibilities.

### 2. `backend`
The REST API version built with Spring Boot.  
It adds:
- PostgreSQL persistence
- JWT authentication
- Spring Security
- role-based access (`USER`, `ADMIN`)
- filtering, sorting, and pagination
- Swagger documentation
- deployment on Render

### 3. `frontend`
A lightweight frontend built with HTML, CSS, and vanilla JavaScript.  
It connects to the live backend and supports:
- login / logout
- task listing
- task creation
- filtering
- pagination
- admin task visibility
- loading states and status badges

---

## Tech Stack

### Backend
- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Maven
- JWT

### Frontend
- HTML
- CSS
- JavaScript

### Deployment
- Render
- Netlify

---

## Main Features

- user registration and login
- JWT-based authentication
- role-based authorization
- CRUD operations for tasks
- admin endpoint for viewing all tasks
- filtering by status
- searching by title
- sorting by id, deadline, and priority
- pagination
- live frontend + live backend integration

---

## Architecture

The backend follows a layered structure:

```text
Client
→ Controller
→ Service
→ Repository
→ Database
