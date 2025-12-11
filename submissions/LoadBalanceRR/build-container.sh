#!/bin/bash

set -e

echo "======================================"
echo "Building LoadBalanceRR JVM Container"
echo "======================================"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="quarkus/loadbalancerr-jvm"
DOCKERFILE="src/main/docker/Dockerfile.jvm"

echo ""
echo -e "${BLUE}Step 1: Packaging application with Maven...${NC}"
./mvnw clean package

echo ""
echo -e "${BLUE}Step 2: Building Docker image...${NC}"
docker build -f ${DOCKERFILE} -t ${IMAGE_NAME} .

echo ""
echo -e "${GREEN}✓ Build complete!${NC}"
echo ""
echo "Image: ${IMAGE_NAME}"
echo ""
echo "To run the container:"
echo "  docker run -i --rm -p 8080:8080 ${IMAGE_NAME}"
echo ""
echo "To run with debug port enabled:"
echo "  docker run -i --rm -p 8080:8080 --volume=\"<path/to/your/config.yml>:/opt/config/config.yml\" ${IMAGE_NAME}"
echo ""
