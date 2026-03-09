# Task Manager API

A backend REST API built with *Java and Spring Boot* for managing tasks.
The project demonstrates clean architecture, validation, undo/redo functionality, filtering, and unit testing.

## Features

* Create, update, delete tasks
* Change task status
* Filtering, sorting and pagination
* Undo / Redo functionality
* Task statistics by status
* Input validation
* REST API with DTOs
* Swagger/OpenAPI documentation
* Unit and integration tests

## Tech Stack

* Java
* Spring Boot
* Maven
* H2 Database
* JUnit 5
* Swagger / OpenAPI

## Project Structure

src/main/java/ro/kutaba/taskmanager

api – REST controllers
config – Spring configuration
mapper – DTO mappers
model – domain models
service – business logic
storage – repositories
ui – console utilities

## Running the application

Clone the repository:

git clone https://github.com/YOUR_USERNAME/task-manager-java.git

Navigate to the project:

cd task-manager-java

Run the application:

mvn spring-boot:run

## API Documentation

Swagger UI:

http://localhost:8080/swagger-ui/index.html

## Example Endpoints

GET /api/tasks
POST /api/tasks
PUT /api/tasks/{id}
PATCH /api/tasks/{id}/status
DELETE /api/tasks/{id}

Query endpoint:

POST /api/tasks/query

## Testing

Run tests using Maven:

mvn test

## Author

Victor Kutaba