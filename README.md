# Prometheus GitOps

This repository contains GitOps configurations for monitoring and MSA demo applications using ArgoCD.

## Structure

- `msa-demo/` - Kubernetes manifests for MSA demo applications
- `msa-app/` - Application source code and Dockerfiles
- `otel-stack/` - OpenTelemetry stack configurations
- `values/` - Helm values files
- `scripts/` - Utility scripts for image tag management

## Applications

### Monitoring Stack
- **prometheus-stack** - Prometheus, Grafana, AlertManager
- **logging-stack** - Loki, Promtail
- **tempo-stack** - Tempo for distributed tracing
- **opentelemetry-operator** - OpenTelemetry Operator
- **otel-stack** - OpenTelemetry Collector and Instrumentation

### MSA Demo
- **msa-demo** - Microservices demo application (user-service, order-service, payment-service)

## CI/CD Pipeline

### Infrastructure CI/CD
- Triggers on changes to monitoring/logging configurations
- Syncs ArgoCD applications automatically

### Application CI/CD
- Triggers on changes to `msa-app/` directory
- Builds Docker images and pushes to ECR
- Updates Kubernetes manifests with new image tags
- Triggers ArgoCD sync

## ECR Repository
- **Registry**: `557690584596.dkr.ecr.ap-northeast-2.amazonaws.com/gitops/msa-app`
- **Image Tags**: `{service-name}-{git-sha}` and `{service-name}-latest`

## Usage

### Manual Image Tag Update
```bash
./scripts/update-image-tag.sh user-service v1.0.0
```

### ArgoCD Applications
All applications are managed through ArgoCD with automated sync enabled.

## Services

### MSA Demo Services
- **user-service**: User management service
- **order-service**: Order processing service  
- **payment-service**: Payment processing service

### External Access
- **user-service**: NodePort 30080 for external access
- **ArgoCD**: Port-forward to access UI

## Monitoring & Observability

- **Metrics**: Prometheus + Grafana
- **Logs**: Loki + Promtail
- **Traces**: Tempo + OpenTelemetry
- **Auto-instrumentation**: OpenTelemetry Java agent injection