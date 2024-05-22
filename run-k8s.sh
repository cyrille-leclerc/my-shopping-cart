#!/bin/bash
set -euo pipefail

docker build -f checkout/Dockerfile -t checkout:1.0-SNAPSHOT .
docker build -f fraud-detection/Dockerfile -t fraud-detection:1.0-SNAPSHOT .
docker build -f frontend/Dockerfile -t frontend:1.0-SNAPSHOT .

k3d cluster create my-shopping-cart || true
k3d image import -c my-shopping-cart checkout:1.0-SNAPSHOT fraud-detection:1.0-SNAPSHOT frontend:1.0-SNAPSHOT

# apply all the manifests from the k8s directory
# todo: the services should be created in the correct order
kubectl apply -f k8s/

kubectl port-forward service/frontend 8080:8080
