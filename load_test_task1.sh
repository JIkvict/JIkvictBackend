#!/bin/bash

BASE_URL="http://localhost:8080"
ENDPOINT="/api/assignment/create/1"
CONCURRENT_REQUESTS=10
TOTAL_REQUESTS=100

echo "Starting load test..."
echo "Base URL: $BASE_URL"
echo "Endpoint: $ENDPOINT"
echo "Concurrent requests: $CONCURRENT_REQUESTS"
echo "Total requests: $TOTAL_REQUESTS"

success_count=0
failure_count=0

# Function to send request and count results
send_request() {
    local response=$(curl -s -w "%{http_code}" -X POST "$BASE_URL$ENDPOINT" \
    -H "Content-Type: application/json" \
    -o /dev/null 2>/dev/null)

    if [[ $response == "200" || $response == "201" ]]; then
    echo "✓ Success: $response"
    return 0
    else
    echo "✗ Failed: $response"
    return 1
    fi
}

# Export function for parallel execution
    export -f send_request
    export BASE_URL ENDPOINT

echo "Sending $TOTAL_REQUESTS requests with $CONCURRENT_REQUESTS concurrent workers..."

# Create a temporary file to store results
temp_file=$(mktemp)

# Run requests in parallel and collect results
seq 1 $TOTAL_REQUESTS | xargs -n1 -P$CONCURRENT_REQUESTS -I{} bash -c 'send_request; echo $? >> '"$temp_file"

# Count successes and failures
    success_count=$(grep -c "0" "$temp_file" 2>/dev/null || echo "0")
failure_count=$(grep -c "1" "$temp_file" 2>/dev/null || echo "0")

# Clean up
    rm -f "$temp_file"

echo ""
echo "Load test completed!"
echo "Results:"
echo "- Successful requests: $success_count"
echo "- Failed requests: $failure_count"
echo "- Success rate: $(echo "scale=2; $success_count * 100 / $TOTAL_REQUESTS" | bc)%"
