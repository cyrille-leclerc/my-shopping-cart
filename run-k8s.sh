#!/bin/bash
set -euo pipefail

docker build -f checkout/Dockerfile -t checkout:1.1-SNAPSHOT .
docker build -f fraud-detection/Dockerfile -t fraud-detection:1.1-SNAPSHOT .
docker build -f frontend/Dockerfile -t frontend:1.1-SNAPSHOT .
docker build -f warehouse/Dockerfile -t warehouse:1.1-SNAPSHOT .
docker build -f shipping/Dockerfile -t shipping:1.1-SNAPSHOT .

k3d cluster create my-shopping-cart || true
k3d image import -c my-shopping-cart checkout:1.1-SNAPSHOT fraud-detection:1.1-SNAPSHOT frontend:1.1-SNAPSHOT warehouse:1.1-SNAPSHOT shipping:1.1-SNAPSHOT

# todo: the services should be created in the correct order
kubectl apply -f k8s/

kubectl port-forward service/frontend 8080:8080 &
kubectl port-forward service/lgtm 3000:3000 &
