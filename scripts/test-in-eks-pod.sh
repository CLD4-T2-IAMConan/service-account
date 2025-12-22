#!/bin/bash

# EKS Pod에서 캐시 테스트 실행 스크립트
#
# 사용 방법:
# ./scripts/test-in-eks-pod.sh [namespace] [pod-name]
#
# 예시:
# ./scripts/test-in-eks-pod.sh default service-account-xxx
# ./scripts/test-in-eks-pod.sh  # 자동으로 Pod 찾기

set -e

NAMESPACE=${1:-default}
POD_NAME=${2:-""}

echo "=========================================="
echo "EKS Pod에서 캐시 테스트 실행"
echo "Namespace: $NAMESPACE"
echo "=========================================="

# Pod 이름이 지정되지 않은 경우 자동으로 찾기
if [ -z "$POD_NAME" ]; then
    echo "Pod 이름을 자동으로 찾는 중..."
    
    # service-account 관련 Pod 찾기
    POD_NAME=$(kubectl get pods -n $NAMESPACE -l app=service-account -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
    
    if [ -z "$POD_NAME" ]; then
        echo "Error: service-account Pod를 찾을 수 없습니다."
        echo ""
        echo "사용 가능한 Pod 목록:"
        kubectl get pods -n $NAMESPACE
        echo ""
        echo "사용법: ./scripts/test-in-eks-pod.sh <namespace> <pod-name>"
        exit 1
    fi
    
    echo "찾은 Pod: $POD_NAME"
fi

# Pod 존재 확인
if ! kubectl get pod -n $NAMESPACE $POD_NAME > /dev/null 2>&1; then
    echo "Error: Pod '$POD_NAME'가 존재하지 않습니다."
    exit 1
fi

echo ""
echo "Pod 정보:"
kubectl get pod -n $NAMESPACE $POD_NAME -o wide

echo ""
echo "=========================================="
echo "테스트 실행 방법 선택"
echo "=========================================="
echo "1. 기존 Pod에서 실행 (Gradle이 설치되어 있어야 함)"
echo "2. 새 Pod에서 실행 (이미지에 테스트 코드 포함 필요)"
echo ""
read -p "선택 (1 또는 2): " -n 1 -r
echo ""

if [[ $REPLY == "1" ]]; then
    echo ""
    echo "=========================================="
    echo "기존 Pod에서 테스트 실행"
    echo "=========================================="
    
    # Pod에 Gradle이 있는지 확인
    if ! kubectl exec -n $NAMESPACE $POD_NAME -- which gradle > /dev/null 2>&1; then
        echo "Warning: Pod에 Gradle이 설치되어 있지 않습니다."
        echo "방법 2를 사용하거나, Pod에 Gradle을 설치하세요."
        exit 1
    fi
    
    # 테스트 실행
    kubectl exec -n $NAMESPACE $POD_NAME -- ./gradlew test --tests "*Cache*"
    
elif [[ $REPLY == "2" ]]; then
    echo ""
    echo "=========================================="
    echo "새 Pod에서 테스트 실행"
    echo "=========================================="
    
    # 이미지 이름 확인
    IMAGE=$(kubectl get pod -n $NAMESPACE $POD_NAME -o jsonpath='{.spec.containers[0].image}')
    echo "사용할 이미지: $IMAGE"
    echo ""
    read -p "계속하시겠습니까? (y/N): " -n 1 -r
    echo ""
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "테스트를 취소했습니다."
        exit 0
    fi
    
    # 새 Pod에서 테스트 실행
    kubectl run cache-test-$(date +%s) \
        --image=$IMAGE \
        --rm -it \
        --restart=Never \
        --namespace=$NAMESPACE \
        --overrides='
    {
        "spec": {
            "containers": [{
                "name": "test",
                "image": "'$IMAGE'",
                "command": ["./gradlew", "test", "--tests", "*Cache*"],
                "stdin": true,
                "tty": true
            }]
        }
    }' -- sh
    
else
    echo "잘못된 선택입니다."
    exit 1
fi

echo ""
echo "=========================================="
echo "테스트 완료"
echo "=========================================="

