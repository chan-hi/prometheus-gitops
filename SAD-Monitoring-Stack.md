# Software Architecture Document (SAD)
# 모니터링 스택 및 GitOps 관리 시스템

## 문서 정보
- **작성일**: 2025-07-16
- **버전**: 1.0
- **작성자**: System Administrator
- **문서 목적**: Kubernetes 기반 모니터링 스택의 아키텍처 및 ArgoCD를 통한 GitOps 관리 체계 문서화

---

## 1. 시스템 개요

### 1.1 목적
본 시스템은 Kubernetes 클러스터에서 운영되는 애플리케이션들의 **관찰가능성(Observability)**을 제공하기 위한 통합 모니터링 스택입니다. GitOps 방식을 통해 선언적으로 관리되며, 메트릭, 로그, 트레이스의 3가지 관찰가능성 신호를 모두 수집하고 시각화합니다.

### 1.2 핵심 기능
- **메트릭 모니터링**: Prometheus 기반 메트릭 수집 및 알림
- **로그 관리**: Loki 기반 중앙화된 로그 수집 및 검색
- **분산 트레이싱**: Tempo 기반 애플리케이션 트레이스 추적
- **통합 시각화**: Grafana를 통한 통합 대시보드
- **자동 계측**: OpenTelemetry를 통한 애플리케이션 자동 계측
- **GitOps 관리**: ArgoCD를 통한 선언적 배포 및 관리

---

## 2. 아키텍처 개요

### 2.1 전체 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                        GitOps Layer                            │
│  ┌─────────────┐    ┌──────────────────────────────────────┐   │
│  │   GitHub    │───▶│            ArgoCD                    │   │
│  │ Repository  │    │        (argocd namespace)            │   │
│  └─────────────┘    └──────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Kubernetes Cluster                           │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                Monitoring Stack                         │   │
│  │              (monitoring namespace)                     │   │
│  │                                                         │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │   │
│  │  │ Prometheus  │  │   Grafana   │  │    Tempo    │    │   │
│  │  │   Stack     │  │             │  │             │    │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘    │   │
│  │                                                         │   │
│  │  ┌─────────────┐                                       │   │
│  │  │Alertmanager │                                       │   │
│  │  └─────────────┘                                       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                Logging Stack                            │   │
│  │               (logging namespace)                       │   │
│  │                                                         │   │
│  │  ┌─────────────┐  ┌─────────────┐                     │   │
│  │  │    Loki     │  │ Fluent Bit  │                     │   │
│  │  │             │  │             │                     │   │
│  │  └─────────────┘  └─────────────┘                     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │            OpenTelemetry Infrastructure                 │   │
│  │          (opentelemetry-system namespace)               │   │
│  │                                                         │   │
│  │  ┌─────────────┐  ┌─────────────┐                     │   │
│  │  │   OTel      │  │    Java     │                     │   │
│  │  │ Collector   │  │Instrumentation│                   │   │
│  │  └─────────────┘  └─────────────┘                     │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │            OpenTelemetry Operator                       │   │
│  │      (opentelemetry-operator-system namespace)          │   │
│  │                                                         │   │
│  │  ┌─────────────┐                                       │   │
│  │  │   OTel      │                                       │   │
│  │  │  Operator   │                                       │   │
│  │  └─────────────┘                                       │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 데이터 플로우

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Application   │───▶│  OpenTelemetry  │───▶│   Monitoring    │
│   Workloads     │    │    Collector    │    │     Stack       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│      Logs       │───▶│   Fluent Bit    │───▶│      Loki       │
│   (stdout)      │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                                             │
         │                                             │
         └─────────────────────────────────────────────┼─────────┐
                                                       │         │
                                                       ▼         ▼
                                               ┌─────────────────┐
                                               │     Grafana     │
                                               │   (Unified UI)  │
                                               └─────────────────┘
```

---

## 3. 컴포넌트 상세 설계

### 3.1 ArgoCD Applications

#### 3.1.1 prometheus-stack (메트릭 모니터링)
```yaml
# 파일: argo-app.yaml
네임스페이스: monitoring
차트: kube-prometheus-stack (v56.3.0)
주요 구성요소:
  - Prometheus Server (데이터 보존: 20일)
  - Grafana (NodePort: 30007, 관리자 비밀번호: admin123!)
  - Alertmanager (고가용성: 2 replicas)
  - Node Exporter
  - kube-state-metrics
```

#### 3.1.2 logging-stack (로그 관리)
```yaml
# 파일: logging-app.yaml
네임스페이스: logging
차트: loki-stack (v2.10.2)
주요 구성요소:
  - Loki (로그 보존: 7일, 스토리지: 10Gi)
  - Fluent Bit (로그 수집기)
  - Promtail (비활성화)
```

#### 3.1.3 tempo-stack (분산 트레이싱)
```yaml
# 파일: tempo-app.yaml
네임스페이스: monitoring
차트: tempo (v1.7.2)
주요 구성요소:
  - Tempo Server (로컬 스토리지: 10Gi)
  - OTLP 수신기 (gRPC: 4317, HTTP: 4318)
  - ServiceMonitor 활성화
```

#### 3.1.4 opentelemetry-operator (계측 관리)
```yaml
# 파일: opentelemetry-operator-app.yaml
네임스페이스: opentelemetry-operator-system
차트: opentelemetry-operator (v0.91.1)
Sync Wave: 1 (우선 배포)
주요 구성요소:
  - OpenTelemetry Operator
  - Admission Webhooks
  - CRDs (Custom Resource Definitions)
```

#### 3.1.5 otel-stack (인프라 계측)
```yaml
# 파일: otel-stack-app.yaml
네임스페이스: opentelemetry-system
소스: Git Repository (otel-stack 디렉토리)
Sync Wave: 2 (Operator 이후 배포)
주요 구성요소:
  - OpenTelemetry Collector
  - Java Instrumentation (전역 설정)
```

#### 3.1.6 prometheus-crds (CRD 관리)
```yaml
# 파일: prometheus-crds.yaml
네임스페이스: monitoring
차트: prometheus-operator-crds (v8.0.1)
목적: Prometheus Operator CRDs 사전 설치
```

### 3.2 네임스페이스 구조

| 네임스페이스 | 목적 | 주요 컴포넌트 |
|-------------|------|---------------|
| `argocd` | GitOps 관리 | ArgoCD Applications |
| `monitoring` | 메트릭 & 트레이싱 | Prometheus, Grafana, Tempo |
| `logging` | 로그 관리 | Loki, Fluent Bit |
| `opentelemetry-system` | 계측 인프라 | OTel Collector, Instrumentation |
| `opentelemetry-operator-system` | 계측 운영자 | OpenTelemetry Operator |

---

## 4. 설정 및 구성

### 4.1 Prometheus 설정
```yaml
# values/values.yaml
prometheus:
  prometheusSpec:
    retention: 20d                    # 데이터 보존 기간
    replicas: 1                       # 단일 인스턴스
    serviceMonitorSelectorNilUsesHelmValues: false  # 모든 ServiceMonitor 수집
    podMonitorSelectorNilUsesHelmValues: false      # 모든 PodMonitor 수집
```

### 4.2 Grafana 설정
```yaml
grafana:
  enabled: true
  adminPassword: admin123!
  service:
    type: NodePort
    nodePort: 30007                  # 외부 접근 포트
  defaultDashboardsEnabled: true     # 기본 대시보드 활성화
```

### 4.3 Loki 설정
```yaml
# values/logging-values.yaml
loki:
  enabled: true
  persistence:
    enabled: true
    storageClassName: standard
    size: 10Gi
  config:
    limits_config:
      retention_period: 168h         # 7일 보존
```

### 4.4 OpenTelemetry Collector 설정
```yaml
# otel-stack/otel-collector-infra.yaml
receivers:
  otlp:
    protocols:
      grpc: { endpoint: "0.0.0.0:4317" }
      http: { endpoint: "0.0.0.0:4318" }

exporters:
  otlp:
    endpoint: "tempo-stack.monitoring.svc.cluster.local:4317"
    tls: { insecure: true }
```

---

## 5. 배포 및 동기화 정책

### 5.1 ArgoCD 동기화 정책
모든 애플리케이션은 다음 공통 정책을 사용합니다:

```yaml
syncPolicy:
  automated:
    prune: true                      # 불필요한 리소스 자동 제거
    selfHeal: true                   # 드리프트 자동 복구
  syncOptions:
    - CreateNamespace=true           # 네임스페이스 자동 생성
    - ServerSideApply=true           # 서버 사이드 적용
```

### 5.2 배포 순서 (Sync Waves)
1. **Wave 0**: prometheus-crds (CRDs 사전 설치)
2. **Wave 1**: opentelemetry-operator (Operator 설치)
3. **Wave 2**: otel-stack (인프라 계측 설정)
4. **Wave 3**: 나머지 스택들 (prometheus-stack, logging-stack, tempo-stack)

### 5.3 무시 차이점 (Ignore Differences)
시스템에서 자동으로 관리되는 필드들에 대한 차이점을 무시하여 불필요한 동기화를 방지합니다:

```yaml
ignoreDifferences:
  - group: apiextensions.k8s.io
    kind: CustomResourceDefinition
  - group: rbac.authorization.k8s.io
    kind: ClusterRole
  - group: admissionregistration.k8s.io
    kind: ValidatingAdmissionWebhook
```

---

## 6. 모니터링 및 관찰가능성

### 6.1 메트릭 수집
- **Prometheus**: 클러스터 및 애플리케이션 메트릭 수집
- **Node Exporter**: 노드 레벨 시스템 메트릭
- **kube-state-metrics**: Kubernetes 리소스 상태 메트릭
- **ServiceMonitor/PodMonitor**: 자동 서비스 디스커버리

### 6.2 로그 수집
- **Fluent Bit**: 모든 파드의 stdout/stderr 로그 수집
- **Loki**: 중앙화된 로그 저장 및 인덱싱
- **LogQL**: Loki 쿼리 언어를 통한 로그 검색

### 6.3 분산 트레이싱
- **OpenTelemetry**: 표준화된 계측 및 데이터 수집
- **Tempo**: 트레이스 데이터 저장 및 쿼리
- **Jaeger Query**: 트레이스 시각화 (Grafana 통합)

### 6.4 알림 시스템
- **Alertmanager**: Prometheus 알림 라우팅 및 관리
- **고가용성**: 2개 인스턴스로 구성
- **알림 채널**: 이메일, Slack, Webhook 지원

---

## 7. 보안 고려사항

### 7.1 네트워크 보안
- **네임스페이스 격리**: 각 스택별 네임스페이스 분리
- **서비스 간 통신**: 클러스터 내부 DNS 사용
- **TLS**: 외부 통신에 대한 TLS 설정 (필요시)

### 7.2 접근 제어
- **RBAC**: Kubernetes 역할 기반 접근 제어
- **ServiceAccount**: 각 컴포넌트별 전용 서비스 계정
- **Grafana 인증**: 기본 관리자 계정 (admin/admin123!)

### 7.3 데이터 보안
- **데이터 보존**: 제한된 보존 기간 설정
- **스토리지 암호화**: 클러스터 레벨 암호화 (권장)
- **백업**: 중요 데이터에 대한 정기 백업 (권장)

---

## 8. 운영 및 유지보수

### 8.1 일상 운영
- **GitOps 워크플로우**: 모든 변경사항은 Git을 통해 관리
- **자동 동기화**: ArgoCD를 통한 자동 배포 및 복구
- **모니터링**: Grafana 대시보드를 통한 시스템 상태 확인

### 8.2 업그레이드 전략
- **Helm 차트 버전**: 단계적 업그레이드
- **CRDs**: 별도 관리를 통한 호환성 보장
- **롤백**: ArgoCD를 통한 빠른 롤백 지원

### 8.3 트러블슈팅
- **로그 분석**: Loki를 통한 중앙화된 로그 검색
- **메트릭 분석**: Prometheus 쿼리를 통한 성능 분석
- **트레이스 분석**: Tempo를 통한 요청 추적

---

## 9. 확장성 및 성능

### 9.1 수평 확장
- **Prometheus**: 필요시 샤딩 및 페더레이션 지원
- **Loki**: 분산 모드로 확장 가능
- **Grafana**: 로드 밸런서를 통한 다중 인스턴스

### 9.2 스토리지 관리
- **Prometheus**: 20일 데이터 보존
- **Loki**: 7일 로그 보존
- **Tempo**: 로컬 스토리지 (확장 시 오브젝트 스토리지 권장)

### 9.3 리소스 최적화
- **리소스 제한**: 각 컴포넌트별 적절한 리소스 할당
- **샘플링**: 트레이스 샘플링을 통한 오버헤드 최소화
- **압축**: 데이터 압축을 통한 스토리지 효율성

---

## 10. 재해 복구 및 백업

### 10.1 백업 전략
- **설정 백업**: Git 리포지토리를 통한 설정 버전 관리
- **데이터 백업**: 중요 메트릭 및 로그 데이터 백업 (권장)
- **스냅샷**: 정기적인 볼륨 스냅샷

### 10.2 복구 절차
1. **클러스터 복구**: Kubernetes 클러스터 재구성
2. **ArgoCD 복구**: ArgoCD 설치 및 애플리케이션 동기화
3. **데이터 복구**: 백업된 데이터 복원 (필요시)

---

## 11. 참고 자료

### 11.1 공식 문서
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Loki Documentation](https://grafana.com/docs/loki/)
- [Tempo Documentation](https://grafana.com/docs/tempo/)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [ArgoCD Documentation](https://argo-cd.readthedocs.io/)

### 11.2 Helm Charts
- [kube-prometheus-stack](https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack)
- [loki-stack](https://github.com/grafana/helm-charts/tree/main/charts/loki-stack)
- [tempo](https://github.com/grafana/helm-charts/tree/main/charts/tempo)
- [opentelemetry-operator](https://github.com/open-telemetry/opentelemetry-helm-charts/tree/main/charts/opentelemetry-operator)

### 11.3 Git Repository
- **Repository**: https://github.com/chan-hi/prometheus-gitops.git
- **Branch**: main
- **구조**:
  ```
  prometheus-gitops/
  ├── argo-app.yaml                    # Prometheus Stack
  ├── logging-app.yaml                 # Logging Stack
  ├── tempo-app.yaml                   # Tempo Stack
  ├── otel-stack-app.yaml             # OpenTelemetry Stack
  ├── opentelemetry-operator-app.yaml  # OpenTelemetry Operator
  ├── prometheus-crds.yaml             # Prometheus CRDs
  ├── values/
  │   ├── values.yaml                  # Prometheus 설정
  │   └── logging-values.yaml          # Logging 설정
  └── otel-stack/
      ├── README.md
      ├── otel-collector-infra.yaml
      └── otel-instrumentation-infra.yaml
  ```

---

## 12. 결론

본 모니터링 스택은 현대적인 클라우드 네이티브 환경에서 요구되는 완전한 관찰가능성을 제공합니다. GitOps 방식을 통한 선언적 관리로 일관성과 재현성을 보장하며, OpenTelemetry 표준을 통해 벤더 중립적인 계측을 지원합니다.

주요 장점:
- **통합된 관찰가능성**: 메트릭, 로그, 트레이스의 통합 관리
- **자동화된 운영**: GitOps를 통한 자동 배포 및 복구
- **표준화된 계측**: OpenTelemetry를 통한 일관된 데이터 수집
- **확장 가능한 아키텍처**: 클러스터 성장에 따른 유연한 확장
- **운영 효율성**: 중앙화된 모니터링 및 알림 시스템

이 시스템은 개발팀과 운영팀 모두에게 애플리케이션의 성능과 상태에 대한 깊은 통찰력을 제공하여, 문제의 빠른 감지와 해결을 가능하게 합니다.
