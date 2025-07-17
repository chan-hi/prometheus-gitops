# OpenTelemetry 분산 트레이싱 아키텍처 문서

## 개요
이 문서는 Kubernetes 환경에서 구성된 OpenTelemetry 기반 분산 트레이싱 시스템의 아키텍처와 구성 요소를 설명합니다.

## 전체 아키텍처

```
Application Pods
    ↓ (Auto-instrumentation)
OpenTelemetry Collector (Gateway)
    ↓ (OTLP Protocol)
Tempo (Tracing Backend)
    ↓ (Query Interface)
Grafana (Visualization)
```

## 구성 요소

### 1. OpenTelemetry Operator
**파일**: `opentelemetry-operator-app.yaml`

```yaml
# ArgoCD Application으로 관리
source:
  repoURL: https://open-telemetry.github.io/opentelemetry-helm-charts
  chart: opentelemetry-operator
  targetRevision: 0.91.1
```

**역할**:
- Kubernetes에서 OpenTelemetry 리소스들을 관리하는 컨트롤러
- Auto-instrumentation 자동 주입 기능 제공
- CRD(Custom Resource Definition) 관리

**주요 설정**:
- Admission Webhooks 활성화
- 자동 인증서 생성 (365일 주기)
- 리소스 제한: CPU 500m, Memory 128Mi

### 2. Auto-instrumentation 설정
**파일**: `otel-stack/otel-instrumentation-infra.yaml`

```yaml
apiVersion: opentelemetry.io/v1alpha1
kind: Instrumentation
metadata:
  name: java-instrumentation
  namespace: opentelemetry-system
```

**역할**:
- 애플리케이션 코드 수정 없이 자동 계측
- Java 애플리케이션에 OpenTelemetry Agent 자동 주입

**주요 설정**:
- **Exporter Endpoint**: `http://otel-collector-collector:4318`
- **Propagators**: tracecontext, baggage, b3
- **Sampling**: parentbased_traceidratio (100% 샘플링)
- **Java Agent Image**: `ghcr.io/open-telemetry/opentelemetry-operator/autoinstrumentation-java:latest`

**환경 변수**:
- `OTEL_EXPORTER_OTLP_ENDPOINT`: Collector 엔드포인트
- `OTEL_EXPORTER_OTLP_PROTOCOL`: http/protobuf
- `OTEL_RESOURCE_ATTRIBUTES`: 서비스 메타데이터

### 3. OpenTelemetry Collector (Gateway 방식)
**파일**: `otel-stack/otel-collector-infra.yaml`

**네임스페이스**: `opentelemetry-system`

#### Collector 설정

**Receivers**:
- **OTLP gRPC**: `0.0.0.0:4317`
- **OTLP HTTP**: `0.0.0.0:4318`

**Processors**:
- **Batch**: 데이터 배치 처리로 효율성 향상
- **Resource**: 클러스터 정보 추가 (`cluster.name: minikube`)

**Exporters**:
- **Debug**: 상세 로깅 (개발/디버깅용)
- **OTLP**: Tempo로 트레이스 데이터 전송
  - Endpoint: `tempo-stack.monitoring.svc.cluster.local:4317`
  - TLS: 비활성화 (내부 통신)

**파이프라인**:
- **Traces**: OTLP → Batch → Resource → [Debug, Tempo]
- **Metrics**: OTLP → Batch → Resource → Debug
- **Logs**: OTLP → Batch → Resource → Debug

#### Service 노출
```yaml
apiVersion: v1
kind: Service
metadata:
  name: otel-collector-gateway
  namespace: opentelemetry-system
```

**포트**:
- **4317**: OTLP gRPC
- **4318**: OTLP HTTP

### 4. Tempo (Tracing Backend)
**파일**: `tempo-app.yaml`

```yaml
source:
  repoURL: https://grafana.github.io/helm-charts
  chart: tempo
  targetRevision: 1.10.1
```

**역할**:
- 분산 트레이싱 데이터 저장 및 조회
- Grafana와 연동하여 트레이스 시각화
- 트레이스 데이터로부터 메트릭 자동 생성

## 데이터 흐름

### 1. 트레이스 수집 과정
```
1. Application 시작
   ↓
2. OpenTelemetry Operator가 Auto-instrumentation 주입
   ↓
3. Java Agent가 애플리케이션과 함께 실행 (In-Process)
   ↓
4. 트레이스 데이터 생성 및 Collector로 전송 (HTTP/4318)
   ↓
5. Collector가 데이터 배치 처리 및 메타데이터 추가
   ↓
6. Tempo로 트레이스 데이터 저장 (gRPC/4317)
```

### 2. 네트워크 통신
- **App → Collector**: `otel-collector-collector.opentelemetry-system.svc.cluster.local:4318`
- **Collector → Tempo**: `tempo-stack.monitoring.svc.cluster.local:4317`

## 배포 방식

### 현재 아키텍처: Gateway 방식
- **장점**: 중앙 집중식 관리, 복잡한 데이터 처리 가능
- **단점**: 단일 장애점, 네트워크 병목 가능성

### 향후 확장 고려사항
대규모 환경으로 확장 시 **Agent + Gateway** 조합 고려:
```
App → Agent (DaemonSet) → Gateway → Tempo
```

## 모니터링 및 관찰성

### 현재 설정된 관찰성
- **Debug Exporter**: Collector 로그를 통한 디버깅
- **Tempo**: 트레이스 저장 및 조회
- **Grafana**: 트레이스 시각화 (별도 설정 필요)

### 메트릭 수집
- 현재는 Debug로만 출력
- 향후 Prometheus 연동 고려 가능

## 보안 고려사항
- **TLS**: 현재 비활성화 (내부 통신)
- **인증**: 기본 설정 (클러스터 내부 통신)
- **네트워크 정책**: 별도 설정 필요 시 고려

## 성능 최적화
- **Sampling Rate**: 현재 100% (프로덕션에서는 조정 필요)
- **Batch Processing**: 활성화됨
- **Resource Attributes**: 클러스터 정보 자동 추가

## 문제 해결

### 일반적인 이슈
1. **트레이스가 수집되지 않는 경우**:
   - Instrumentation annotation 확인
   - Collector 엔드포인트 연결성 확인
   - Collector 로그 확인

2. **성능 이슈**:
   - Sampling rate 조정
   - Batch size 튜닝
   - Collector 리소스 증설

### 디버깅 명령어
```bash
# Collector 로그 확인
kubectl logs -n opentelemetry-system deployment/otel-collector-collector

# Instrumentation 상태 확인
kubectl get instrumentation -n opentelemetry-system

# Service 연결성 확인
kubectl get svc -n opentelemetry-system
```

## 참고 자료
- [OpenTelemetry Operator Documentation](https://opentelemetry.io/docs/kubernetes/operator/)
- [OpenTelemetry Collector Configuration](https://opentelemetry.io/docs/collector/configuration/)
- [Tempo Documentation](https://grafana.com/docs/tempo/)
