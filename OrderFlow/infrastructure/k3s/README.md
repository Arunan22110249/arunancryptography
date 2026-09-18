# K3s notes

This project is designed for ARM64-friendly K3s deployment on Oracle Always Free compute. Use the official `k3s` install script, mount the application images to a local registry or a container registry accessible from the VM, and apply the manifests under `infrastructure/kubernetes`.

## Installation
```bash
curl -sfL https://get.k3s.io | INSTALL_K3S_CHANNEL=stable sh -s - server --write-kubeconfig-mode 644
```

## Apply manifests
```bash
kubectl apply -f infrastructure/kubernetes/namespace.yaml
kubectl apply -f infrastructure/kubernetes/deployments.yaml
```

## Teardown
```bash
kubectl delete -f infrastructure/kubernetes/deployments.yaml
kubectl delete -f infrastructure/kubernetes/namespace.yaml
```
