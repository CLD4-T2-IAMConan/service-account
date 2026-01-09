#!/bin/bash

# Helm 리소스 정리 스크립트
# 기존 release와 충돌하는 리소스를 정리합니다.

set -e

NAMESPACE="account"
SERVICE_NAME="account-service"

echo "🧹 Helm 리소스 정리 중..."
echo ""

# 1. 기존 Helm releases 확인 및 삭제
echo "📋 기존 Helm releases 확인:"
EXISTING_RELEASES=$(helm list -n "$NAMESPACE" -q)
if [ -n "$EXISTING_RELEASES" ]; then
    echo "  발견된 releases: $EXISTING_RELEASES"
    echo "🗑️  기존 Helm releases 삭제 중..."
    for release in $EXISTING_RELEASES; do
        echo "  삭제 중: $release"
        helm uninstall "$release" -n "$NAMESPACE" || true
    done
    echo "  ✅ 기존 releases 삭제 완료"
else
    echo "  (release 없음)"
fi
echo ""

# 2. 충돌하는 리소스 정리 (Helm annotation이 있는 리소스들)
echo "🗑️  충돌하는 리소스 정리 중..."

# Ingress
echo "  Ingress 삭제 중..."
kubectl delete ingress "$SERVICE_NAME" -n "$NAMESPACE" --ignore-not-found=true || true

# Service
echo "  Service 삭제 중..."
kubectl delete service "$SERVICE_NAME" -n "$NAMESPACE" --ignore-not-found=true || true

# Deployment
echo "  Deployment 삭제 중..."
kubectl delete deployment "$SERVICE_NAME" -n "$NAMESPACE" --ignore-not-found=true || true

# ServiceAccount (Helm이 생성한 것만)
echo "  ServiceAccount 삭제 중..."
kubectl delete serviceaccount "${SERVICE_NAME}-sa" -n "$NAMESPACE" --ignore-not-found=true || true

# Secret
echo "  Secret 삭제 중..."
kubectl delete secret "${SERVICE_NAME}-secret" -n "$NAMESPACE" --ignore-not-found=true || true

# ConfigMap
echo "  ConfigMap 삭제 중..."
kubectl delete configmap "${SERVICE_NAME}-config" -n "$NAMESPACE" --ignore-not-found=true || true

# HPA (있는 경우)
echo "  HPA 삭제 중..."
kubectl delete hpa "$SERVICE_NAME" -n "$NAMESPACE" --ignore-not-found=true || true

# ServiceMonitor (있는 경우)
echo "  ServiceMonitor 삭제 중..."
kubectl delete servicemonitor "$SERVICE_NAME" -n "$NAMESPACE" --ignore-not-found=true || true

# PrometheusRule (있는 경우)
echo "  PrometheusRule 삭제 중..."
kubectl delete prometheusrule "$SERVICE_NAME" -n "$NAMESPACE" --ignore-not-found=true || true

echo "  ✅ 리소스 정리 완료"
echo ""

# 3. 남은 리소스 확인
echo "🔍 남은 리소스 확인:"
echo "  Ingress:"
kubectl get ingress -n "$NAMESPACE" 2>/dev/null | grep "$SERVICE_NAME" || echo "    (없음)"
echo "  Service:"
kubectl get service -n "$NAMESPACE" 2>/dev/null | grep "$SERVICE_NAME" || echo "    (없음)"
echo "  Deployment:"
kubectl get deployment -n "$NAMESPACE" 2>/dev/null | grep "$SERVICE_NAME" || echo "    (없음)"
echo ""

echo "✅ 정리 완료!"
echo ""
echo "다음 명령어로 배포하세요:"
echo "  cd /Users/krystal/workspace/Passit/service-account/helm"
echo "  helm upgrade --install $SERVICE_NAME . \\"
echo "    --namespace $NAMESPACE \\"
echo "    --create-namespace \\"
echo "    --values values-prod.yaml \\"
echo "    --wait \\"
echo "    --timeout 20m"

