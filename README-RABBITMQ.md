# RabbitMQ Integration for Asynchronous Task Processing

This document describes the RabbitMQ integration for handling long-running tasks asynchronously in the JIkvictBackend application.

## Overview

The application uses RabbitMQ to process long-running tasks asynchronously. This allows the API to respond quickly while the tasks are processed in the background. The implementation supports various types of asynchronous tasks, including assignment creation and solution verification.

## Components

The RabbitMQ integration consists of the following components:

1. **TaskMessage Interface**: Defines the structure for task messages
2. **TaskStatus Entity**: Tracks the status of tasks in the database
3. **TaskProcessor Interface**: Defines the contract for task processors
4. **TaskRegistry**: Manages the registration of task processors
5. **RabbitMQ Configuration**: Configures the RabbitMQ connection and queues. Some queues are declared dynamically based on registered task processors, while others (like verification.queue) are explicitly declared to ensure they exist before listeners try to connect to them
6. **TaskQueueService**: Enqueues tasks and provides methods for tracking task status
7. **Task Processors**: Process specific types of tasks (AssignmentTaskProcessor, VerificationTaskProcessor)
8. **Controllers**: Provide endpoints for submitting tasks and checking task status

## Usage Examples

### Creating an Assignment Asynchronously

To create an assignment asynchronously, send a POST request to the `/api/assignment/create/{assignmentNumber}` endpoint:

```http
POST /api/assignment/create/1
```

The response will include a task ID and a pending status:

```json
{
  "data": 123,
  "status": "PENDING"
}
```

### Checking Assignment Task Status

To check the status of an assignment creation task, send a GET request to the `/api/assignment/status/{taskId}` endpoint:

```http
GET /api/assignment/status/123
```

The response will include the task status and the assignment ID if the task is completed:

```json
{
  "data": 456,
  "status": "DONE"
}
```

### Getting an Assignment

Once the task is completed, you can get the assignment by sending a GET request to the `/api/assignment/{id}` endpoint:

```http
GET /api/assignment/456
```

### Submitting a Solution for Verification

To submit a solution for verification asynchronously, send a POST request to the `/api/v1/solution-checker/submit` endpoint:

```http
POST /api/v1/solution-checker/submit
Content-Type: multipart/form-data

file=@solution.zip
timeoutSeconds=300
```

The response will include a task ID and a pending status:

```json
{
  "data": 789,
  "status": "PENDING"
}
```

### Checking Verification Task Status

To check the status of a verification task, send a GET request to the `/api/v1/solution-checker/status/{taskId}` endpoint:

```http
GET /api/v1/solution-checker/status/789
```

The response will include the task status:

```json
{
  "data": null,
  "status": "DONE"
}
```

## Task Status Values

Possible status values for tasks:
- `PENDING`: The task is in progress
- `DONE`: The task has completed successfully
- `FAILED`: The task has failed
- `REJECTED`: The task was rejected

## Configuration

The RabbitMQ connection can be configured using the following properties in the `application.properties` file:

```properties
spring.rabbitmq.host=${RABBITMQ_HOST:localhost}
spring.rabbitmq.port=${RABBITMQ_PORT:5672}
spring.rabbitmq.username=${RABBITMQ_USERNAME:guest}
spring.rabbitmq.password=${RABBITMQ_PASSWORD:guest}
spring.rabbitmq.listener.simple.retry.enabled=true
spring.rabbitmq.listener.simple.retry.initial-interval=5000
spring.rabbitmq.listener.simple.retry.max-attempts=3
spring.rabbitmq.listener.simple.retry.multiplier=2.0
spring.rabbitmq.listener.simple.concurrency=3
spring.rabbitmq.listener.simple.max-concurrency=10
```

These properties can be overridden using environment variables:
- `RABBITMQ_HOST`: The RabbitMQ host (default: localhost)
- `RABBITMQ_PORT`: The RabbitMQ port (default: 5672)
- `RABBITMQ_USERNAME`: The RabbitMQ username (default: guest)
- `RABBITMQ_PASSWORD`: The RabbitMQ password (default: guest)

## Extending the Infrastructure

To add support for new types of tasks:

1. Create a new task message class that implements the `TaskMessage` interface
2. Create a new task processor class that implements the `TaskProcessor` interface
3. Register the processor with the `TaskRegistry` (this is done automatically if the processor is a Spring bean)
4. Add a method to the `TaskQueueService` class to enqueue the new type of task
5. Add endpoints to the appropriate controller to expose the functionality to clients

The infrastructure is designed to be easily extensible, allowing new types of tasks to be added with minimal changes to the existing code.
