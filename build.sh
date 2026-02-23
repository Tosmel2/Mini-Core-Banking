#!/bin/bash

echo "Building Mini Core Banking Application..."

# Clean and build
./mvnw clean package -DskipTests

echo "Build complete! JAR file created at target/mini-core-banking-1.0.0.jar"