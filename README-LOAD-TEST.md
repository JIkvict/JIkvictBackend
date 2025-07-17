# Load Testing Script for Task 1 Creation

This document provides instructions for using the `load_test_task1.kts` script to load test the JIkvict Backend application with task 1 creation requests.

## Overview

The `load_test_task1.kts` script is designed to send multiple concurrent requests to the `/api/assignment/create/1` endpoint to test the application's performance and stability under load. It uses Kotlin coroutines to efficiently manage concurrent requests and provides detailed metrics about the test execution.

## Prerequisites

- Kotlin 1.5+ with `kotlinc` available in your PATH
- JIkvict Backend application running (default: http://localhost:8080)
- Internet connection (to download dependencies)

## Running the Script

1. Make sure the JIkvict Backend application is running
2. Open a terminal and navigate to the project directory
3. Run the script with:

```bash
kotlinc -script load_test_task1.kts
```

The first time you run the script, it will download the required dependencies, which might take a few moments.

## Configuration

You can modify the following parameters in the script to adjust the load test:

- `BASE_URL`: The base URL of the JIkvict Backend application (default: "http://localhost:8080")
- `ENDPOINT`: The endpoint to create task 1 (default: "/api/assignment/create/1")
- `CONCURRENT_REQUESTS`: Number of concurrent requests to send in each batch (default: 50)
- `TOTAL_REQUESTS`: Total number of requests to send (default: 500)
- `REQUEST_DELAY_MS`: Delay between batches of requests in milliseconds (default: 10)

## Expected Output

The script will output detailed information about the test execution, including:

- Start time and configuration details
- Progress updates for each batch of requests
- Completion time and summary statistics
- Success and failure counts
- Response status distribution
- Average request time

Example output:

```
[2025-07-17 01:45:00.123] Starting load test for task 1 creation
[2025-07-17 01:45:00.124] Configuration:
[2025-07-17 01:45:00.124] - Base URL: http://localhost:8080
[2025-07-17 01:45:00.124] - Endpoint: /api/assignment/create/1
[2025-07-17 01:45:00.124] - Concurrent requests: 50
[2025-07-17 01:45:00.124] - Total requests: 500
[2025-07-17 01:45:00.124] - Request delay: 10 ms
[2025-07-17 01:45:00.125] Sending batch of 50 requests (1 to 50 of 500)
[2025-07-17 01:45:01.234] Batch completed in 1109 ms
...
[2025-07-17 01:45:10.567] Load test completed in 10442 ms
[2025-07-17 01:45:10.567] Results:
[2025-07-17 01:45:10.567] - Successful requests: 500
[2025-07-17 01:45:10.567] - Failed requests: 0
[2025-07-17 01:45:10.567] - Success rate: 100.0%
[2025-07-17 01:45:10.567] - Average request time: 20.884 ms
[2025-07-17 01:45:10.567] Response status distribution:
[2025-07-17 01:45:10.567] - Status 202: 500 (100.0%)
```

## Monitoring the Application

While the load test is running, you should monitor the JIkvict Backend application to observe:

1. CPU and memory usage
2. Database connection pool utilization
3. RabbitMQ queue size and message processing rate
4. Response times and error rates

You can use tools like:
- JConsole or VisualVM for JVM monitoring
- RabbitMQ Management UI for queue monitoring
- Database monitoring tools
- Application logs

## Troubleshooting

If you encounter issues running the script:

1. **Dependency resolution fails**: Make sure you have an internet connection and try running with `--repository=https://repo1.maven.org/maven2/` flag
2. **Connection refused**: Verify the JIkvict Backend application is running and the BASE_URL is correct
3. **Out of memory**: Reduce the CONCURRENT_REQUESTS value
4. **Script execution fails**: Make sure you have Kotlin 1.5+ installed and kotlinc is in your PATH

## Adjusting Load Parameters

- For a light load test: Set CONCURRENT_REQUESTS=10, TOTAL_REQUESTS=100
- For a moderate load test: Set CONCURRENT_REQUESTS=50, TOTAL_REQUESTS=500 (default)
- For a heavy load test: Set CONCURRENT_REQUESTS=100, TOTAL_REQUESTS=1000
- For a stress test: Set CONCURRENT_REQUESTS=200, TOTAL_REQUESTS=2000

Remember to adjust the REQUEST_DELAY_MS parameter based on your server's capacity. A lower value will increase the load intensity.
