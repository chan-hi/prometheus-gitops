# OpenTelemetry Stack

이 디렉토리는 인프라 레벨의 OpenTelemetry 구성 요소들을 포함합니다.

## 구성 요소

### 1. OpenTelemetry Collector (`otel-collector-infra.yaml`)
- **네임스페이스**: `opentelemetry-system`
- **기능**: 
  - OTLP 트레이스/메트릭/로그 수집
  - Tempo로 트레이스 전송
  - 클러스터 전체에서 사용 가능한 공통 Collector

### 2. Java Instrumentation (`otel-instrumentation-infra.yaml`)
- **네임스페이스**: `opentelemetry-system`
- **기능**:
  - 전역 Java 자동 계측 설정
  - 다른 네임스페이스에서 참조 가능
  - 표준화된 계측 정책

## 사용 방법

애플리케이션에서 이 인프라를 사용하려면 다음과 같이 어노테이션을 추가하세요:

```yaml
metadata:
  annotations:
    instrumentation.opentelemetry.io/inject-java: "opentelemetry-system/java-instrumentation"
```

## 아키텍처

```
opentelemetry-system (Infrastructure)
├── OpenTelemetry Collector
└── Java Instrumentation (Global)
        ↓
Application Namespaces
├── msa-demo
├── other-app-1
└── other-app-2
        ↓
monitoring
├── Tempo (Traces)
└── Grafana (Visualization)
```

## 배포

이 스택은 ArgoCD를 통해 자동으로 배포됩니다:
- **Application**: `otel-stack`
- **Namespace**: `opentelemetry-system`
- **Sync Policy**: Automated
