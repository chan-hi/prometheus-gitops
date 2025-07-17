# 통합 관찰가능성 스택 - GitOps 운영 문서

## 📋 개요

이 문서는 Kubernetes 환경에서 **메트릭(Metrics), 로그(Logs), 트레이스(Traces)** 3가지 관찰가능성 신호를 통합하여 GitOps 방식으로 운영하는 시스템을 설명합니다.

### 핵심 구성 요소
- **메트릭**: Prometheus + Grafana
- **로그**: Loki + Fluent Bit + Grafana  
- **트레이스**: Tempo + OpenTelemetry + Grafana
- **GitOps**: ArgoCD 기반 자동화 배포

---

## 🏗️ 통합 아키텍처

### 전체 시스템 구조
```
┌─────────────────────────────────────────────────────────────────┐
│                        GitOps Layer                            │
│                                                                 │
│  ┌─────────────┐    ┌──────────────────────────────────────┐   │
│  │   GitHub    │───▶│            ArgoCD                    │   │
│  │ Repository  │    │     (Declarative Management)        │   │
│  └─────────────┘    └──────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Kubernetes Cluster                           │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Observability Stack                       │   │
│  │                                                         │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │   │
│  │  │  METRICS    │  │    LOGS     │  │   TRACES    │    │   │
│  │  │             │  │             │  │             │    │   │
│  │  │ Prometheus  │  │    Loki     │  │   Tempo     │    │   │
│  │  │ + Grafana   │  │ + FluentBit │  │ + OTel      │    │   │
│  │  │             │  │ + Grafana   │  │ + Grafana   │    │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘    │   │
│  │                           │                            │   │
│  │                           ▼                            │   │
│  │                  ┌─────────────┐                      │   │
│  │                  │   Grafana   │                      │   │
│  │                  │ (Unified UI)│                      │   │
│  │                  └─────────────┘                      │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                Application Layer                        │   │
│  │                                                         │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │   │
│  │  │    App A    │  │    App B    │  │    App C    │    │   │
│  │  │             │  │             │  │             │    │   │
│  │  │ + OTel      │  │ + OTel      │  │ + OTel      │    │   │
│  │  │   Agent     │  │   Agent     │  │   Agent     │    │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 GitOps 워크플로우

### 1. Git Repository 구조
```
prometheus-gitops/
├── argo-app.yaml                    # Prometheus Stack (메트릭)
├── logging-app.yaml                 # Loki Stack (로그)
├── tempo-app.yaml                   # Tempo Stack (트레이스)
├── opentelemetry-operator-app.yaml  # OpenTelemetry Operator
├── otel-stack-app.yaml             # OpenTelemetry Infrastructure
├── prometheus-crds.yaml             # Prometheus CRDs
├── values/
│   ├── values.yaml                  # Prometheus 설정
│   └── logging-values.yaml          # Loki 설정
└── otel-stack/
    ├── otel-collector-infra.yaml    # OTel Collector
    └── otel-instrumentation-infra.yaml  # 자동 계측 설정
```

### 2. ArgoCD Applications 배포 순서
```
Sync Wave 0: prometheus-crds         # CRDs 사전 설치
    ↓
Sync Wave 1: opentelemetry-operator  # OpenTelemetry Operator
    ↓
Sync Wave 2: otel-stack             # OpenTelemetry 인프라
    ↓
Sync Wave 3: prometheus-stack       # 메트릭 스택
           + logging-stack          # 로그 스택  
           + tempo-stack           # 트레이스 스택
```

### 3. 자동화된 배포 정책
```yaml
# 모든 ArgoCD Application 공통 정책
syncPolicy:
  automated:
    prune: true        # 불필요한 리소스 자동 제거
    selfHeal: true     # 드리프트 자동 복구
  syncOptions:
    - CreateNamespace=true    # 네임스페이스 자동 생성
    - ServerSideApply=true    # 서버 사이드 적용
```

---

## 📊 관찰가능성 통합 데이터 플로우

### 메트릭 수집 플로우
```
Application Pods
    ↓ (Prometheus 메트릭 노출)
ServiceMonitor/PodMonitor (자동 디스커버리)
    ↓
Prometheus Server (메트릭 수집 및 저장)
    ↓
Grafana (메트릭 시각화)
    ↓
Alertmanager (알림 발송)
```

### 로그 수집 플로우
```
Application Pods (stdout/stderr)
    ↓
Fluent Bit (로그 수집 에이전트)
    ↓
Loki (로그 저장 및 인덱싱)
    ↓
Grafana (로그 검색 및 시각화)
```

### 트레이스 수집 플로우
```
Application (Java Agent 자동 주입)
    ↓ HTTP/4318
OpenTelemetry Collector (Gateway)
    ↓ gRPC/4317
Tempo (트레이스 저장)
    ↓
Grafana (트레이스 시각화)
```

---

## 🎯 통합 관찰가능성 구현

### 1. 메트릭 모니터링 (Prometheus Stack)

**배포 파일**: `argo-app.yaml`
```yaml
source:
  repoURL: https://prometheus-community.github.io/helm-charts
  chart: kube-prometheus-stack
  targetRevision: 56.3.0
```

**주요 구성 요소**:
- **Prometheus Server**: 메트릭 수집 및 저장 (20일 보존)
- **Grafana**: 시각화 대시보드 (NodePort 30007)
- **Alertmanager**: 알림 관리 (고가용성 2 replicas)
- **Node Exporter**: 노드 메트릭 수집
- **kube-state-metrics**: Kubernetes 리소스 메트릭

**네임스페이스**: `monitoring`

### 2. 로그 관리 (Loki Stack)

**배포 파일**: `logging-app.yaml`
```yaml
source:
  repoURL: https://grafana.github.io/helm-charts
  chart: loki-stack
  targetRevision: 2.10.2
```

**주요 구성 요소**:
- **Loki**: 로그 저장 및 인덱싱 (7일 보존, 10Gi 스토리지)
- **Fluent Bit**: 로그 수집 에이전트 (모든 Pod의 stdout/stderr)
- **Grafana 통합**: LogQL을 통한 로그 검색

**네임스페이스**: `logging`

### 3. 분산 트레이싱 (Tempo Stack)

**배포 파일**: `tempo-app.yaml`
```yaml
source:
  repoURL: https://grafana.github.io/helm-charts
  chart: tempo
  targetRevision: 1.7.2
```

**주요 구성 요소**:
- **Tempo**: 트레이스 저장 및 쿼리 (로컬 스토리지 10Gi)
- **OTLP 수신기**: gRPC(4317), HTTP(4318) 프로토콜 지원
- **ServiceMonitor**: Prometheus 메트릭 노출

**네임스페이스**: `monitoring`

### 4. 자동 계측 (OpenTelemetry)

**Operator 배포**: `opentelemetry-operator-app.yaml`
```yaml
source:
  repoURL: https://open-telemetry.github.io/opentelemetry-helm-charts
  chart: opentelemetry-operator
  targetRevision: 0.91.1
```

**인프라 배포**: `otel-stack-app.yaml` → `otel-stack/` 디렉토리

**자동 계측 설정**:
```yaml
# otel-instrumentation-infra.yaml
apiVersion: opentelemetry.io/v1alpha1
kind: Instrumentation
metadata:
  name: java-instrumentation
  namespace: opentelemetry-system
spec:
  java:
    image: ghcr.io/open-telemetry/opentelemetry-operator/autoinstrumentation-java:latest
  exporter:
    endpoint: http://otel-collector-collector:4318
  sampler:
    type: parentbased_traceidratio
    argument: "1"  # 100% 샘플링
```

**Collector 설정**:
```yaml
# otel-collector-infra.yaml
receivers:
  otlp:
    protocols:
      grpc: { endpoint: "0.0.0.0:4317" }
      http: { endpoint: "0.0.0.0:4318" }

exporters:
  otlp:
    endpoint: "tempo-stack.monitoring.svc.cluster.local:4317"
    tls: { insecure: true }
  debug:
    verbosity: detailed

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch, resource]
      exporters: [debug, otlp]
```

---

## 🔗 통합 시각화 (Grafana)

### 1. 데이터 소스 통합
```yaml
# Grafana에서 설정되는 데이터 소스들
datasources:
  - name: Prometheus
    type: prometheus
    url: http://prometheus-server:80
    
  - name: Loki
    type: loki
    url: http://loki:3100
    
  - name: Tempo
    type: tempo
    url: http://tempo:3100
```

### 2. 통합 대시보드 기능
- **메트릭 대시보드**: 시스템 및 애플리케이션 성능 지표
- **로그 탐색**: LogQL을 통한 실시간 로그 검색
- **트레이스 분석**: 분산 요청 추적 및 성능 분석
- **상관관계 분석**: 메트릭-로그-트레이스 간 연관성 분석

### 3. 접근 방법
- **URL**: `http://<node-ip>:30007`
- **계정**: admin / admin123!
- **NodePort**: 30007 (외부 접근)

---

## 🚀 애플리케이션 계측 방법

### 1. Java 애플리케이션 자동 계측
```yaml
# Deployment에 annotation 추가
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-java-app
spec:
  template:
    metadata:
      annotations:
        instrumentation.opentelemetry.io/inject-java: "true"
    spec:
      containers:
      - name: app
        image: my-java-app:latest
```

### 2. 자동 주입 과정
1. **OpenTelemetry Operator**가 annotation 감지
2. **Init Container**로 Java Agent JAR 주입
3. **JVM 옵션** 자동 설정: `-javaagent:/otel-auto-instrumentation/javaagent.jar`
4. **환경 변수** 자동 설정:
   - `OTEL_EXPORTER_OTLP_ENDPOINT`: Collector 엔드포인트
   - `OTEL_RESOURCE_ATTRIBUTES`: 서비스 메타데이터

### 3. 수집되는 데이터
- **트레이스**: HTTP 요청, 데이터베이스 쿼리, 외부 API 호출
- **메트릭**: 요청 수, 응답 시간, 에러율 (RED 메트릭)
- **로그**: 애플리케이션 로그 (stdout/stderr)

---

## 📈 운영 모니터링

### 1. 시스템 상태 확인
```bash
# ArgoCD Applications 상태 확인
kubectl get applications -n argocd

# 각 네임스페이스별 Pod 상태 확인
kubectl get pods -n monitoring
kubectl get pods -n logging  
kubectl get pods -n opentelemetry-system
kubectl get pods -n opentelemetry-operator-system
```

### 2. 데이터 수집 상태 확인
```bash
# Prometheus 타겟 상태
curl http://<grafana-url>:30007/api/v1/targets

# Loki 로그 수집 상태
kubectl logs -n logging deployment/loki-fluent-bit

# OpenTelemetry Collector 로그
kubectl logs -n opentelemetry-system deployment/otel-collector-collector
```

### 3. 주요 메트릭 모니터링
- **Prometheus**: `up`, `prometheus_tsdb_head_samples_appended_total`
- **Loki**: `loki_ingester_streams`, `loki_distributor_lines_received_total`
- **Tempo**: `tempo_ingester_traces_created_total`, `tempo_request_duration_seconds`
- **OpenTelemetry**: `otelcol_receiver_accepted_spans`, `otelcol_exporter_sent_spans`

---

## 🔧 문제 해결 가이드

### 1. 트레이스가 수집되지 않는 경우
```bash
# 1. Instrumentation 설정 확인
kubectl get instrumentation -n opentelemetry-system

# 2. Pod annotation 확인
kubectl describe pod <pod-name> | grep instrumentation

# 3. Collector 로그 확인
kubectl logs -n opentelemetry-system deployment/otel-collector-collector

# 4. Tempo 연결 확인
kubectl logs -n monitoring deployment/tempo-stack
```

### 2. 로그가 수집되지 않는 경우
```bash
# 1. Fluent Bit 상태 확인
kubectl get pods -n logging -l app=fluent-bit

# 2. Fluent Bit 로그 확인
kubectl logs -n logging daemonset/loki-fluent-bit

# 3. Loki 상태 확인
kubectl logs -n logging deployment/loki
```

### 3. 메트릭이 수집되지 않는 경우
```bash
# 1. Prometheus 타겟 확인
kubectl port-forward -n monitoring svc/prometheus-server 9090:80
# http://localhost:9090/targets

# 2. ServiceMonitor 확인
kubectl get servicemonitor -A

# 3. Prometheus 설정 확인
kubectl get prometheus -n monitoring -o yaml
```

---

## 📊 성능 및 리소스 관리

### 1. 리소스 사용량
| 컴포넌트 | CPU | Memory | Storage |
|---------|-----|--------|---------|
| Prometheus | 500m | 2Gi | 20일 보존 |
| Grafana | 100m | 128Mi | - |
| Loki | 200m | 256Mi | 10Gi (7일) |
| Tempo | 200m | 256Mi | 10Gi |
| OTel Collector | 100m | 128Mi | - |
| Fluent Bit | 50m | 64Mi | - |

### 2. 데이터 보존 정책
- **메트릭**: 20일 (Prometheus)
- **로그**: 7일 (Loki)
- **트레이스**: 기본 설정 (Tempo)

### 3. 샘플링 전략
- **트레이스**: 100% 샘플링 (개발 환경)
- **메트릭**: 15초 간격 수집
- **로그**: 모든 로그 수집

---

## 🔄 GitOps 운영 절차

### 1. 설정 변경 프로세스
```bash
# 1. Git Repository에서 설정 파일 수정
git clone https://github.com/chan-hi/prometheus-gitops.git
cd prometheus-gitops

# 2. 설정 변경 (예: Prometheus 보존 기간 변경)
vim values/values.yaml

# 3. 변경사항 커밋 및 푸시
git add .
git commit -m "Update Prometheus retention to 30d"
git push origin main

# 4. ArgoCD 자동 동기화 (또는 수동 동기화)
# ArgoCD UI에서 확인 또는 CLI 사용
argocd app sync prometheus-stack
```

### 2. 새로운 애플리케이션 추가
```bash
# 1. 새로운 ArgoCD Application 파일 생성
cat > new-app.yaml << EOF
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: new-monitoring-app
  namespace: argocd
spec:
  # ... 설정
EOF

# 2. Git에 추가 및 푸시
git add new-app.yaml
git commit -m "Add new monitoring application"
git push origin main
```

### 3. 롤백 절차
```bash
# 1. Git에서 이전 커밋으로 롤백
git revert <commit-hash>
git push origin main

# 2. 또는 ArgoCD에서 직접 롤백
argocd app rollback prometheus-stack <revision>
```

---

## 📋 체크리스트

### 일일 운영 체크리스트
- [ ] ArgoCD Applications 동기화 상태 확인
- [ ] Grafana 대시보드 정상 작동 확인
- [ ] 주요 메트릭 임계값 확인
- [ ] 로그 수집 상태 확인
- [ ] 트레이스 데이터 수집 확인

### 주간 운영 체크리스트
- [ ] 스토리지 사용량 확인
- [ ] 성능 메트릭 분석
- [ ] 알림 규칙 검토
- [ ] 백업 상태 확인
- [ ] 보안 업데이트 확인

### 월간 운영 체크리스트
- [ ] 리소스 사용량 최적화
- [ ] 데이터 보존 정책 검토
- [ ] 대시보드 업데이트
- [ ] 문서 업데이트
- [ ] 재해 복구 테스트

---

## 🎯 결론

이 통합 관찰가능성 스택은 **메트릭, 로그, 트레이스**를 하나의 플랫폼에서 관리하며, **GitOps** 방식을 통해 선언적이고 자동화된 운영을 제공합니다.

### 주요 장점
- **완전한 관찰가능성**: 3가지 신호 통합 모니터링
- **자동화된 운영**: GitOps 기반 배포 및 관리
- **코드 수정 불필요**: OpenTelemetry 자동 계측
- **통합 시각화**: Grafana 단일 UI
- **확장 가능**: 클러스터 성장에 따른 유연한 확장

### 운영 효과
- **MTTR 단축**: 빠른 문제 감지 및 해결
- **개발 생산성**: 자동 계측으로 개발자 부담 감소  
- **운영 효율성**: 자동화된 배포 및 복구
- **비용 최적화**: 적절한 데이터 보존 정책

이 시스템을 통해 현대적인 클라우드 네이티브 환경에서 요구되는 완전한 관찰가능성과 효율적인 운영을 달성할 수 있습니다.
