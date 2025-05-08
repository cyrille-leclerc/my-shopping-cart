


```commandline
kubectl create configmap k6-load-test-webshop-frontend --from-file ./load-generator/src/main/k6/k6.js
```

```commandline
kubectl get configmaps k6-load-test-webshop-frontend -o yaml
```

```commandline
kubectl port-forward service/frontend 8080:8080
```