#!/bin/bash

# AWS ElastiCache를 사용한 캐시 테스트 실행 스크립트
#
# 사용 방법:
# 1. ElastiCache 엔드포인트를 환경 변수로 설정:
#    export ELASTICACHE_ENDPOINT=your-cluster.xxxxx.cache.amazonaws.com
#
# 2. 또는 Terraform output에서 자동으로 가져오기:
#    ./scripts/test-with-elasticache.sh dev
#    ./scripts/test-with-elasticache.sh prod

set -e

ENVIRONMENT=${1:-dev}

echo "=========================================="
echo "AWS ElastiCache 캐시 테스트 실행"
echo "Environment: $ENVIRONMENT"
echo "=========================================="

# ElastiCache 엔드포인트 설정
if [ -z "$ELASTICACHE_ENDPOINT" ]; then
    echo "ELASTICACHE_ENDPOINT가 설정되지 않았습니다."
    echo "Terraform output에서 가져오는 중..."
    
    TERRAFORM_DIR="terraform/envs/$ENVIRONMENT"
    
    if [ ! -d "$TERRAFORM_DIR" ]; then
        echo "Error: $TERRAFORM_DIR 디렉토리가 존재하지 않습니다."
        exit 1
    fi
    
    cd "$TERRAFORM_DIR"
    ELASTICACHE_ENDPOINT=$(terraform output -raw valkey_primary_endpoint 2>/dev/null || echo "")
    cd - > /dev/null
    
    if [ -z "$ELASTICACHE_ENDPOINT" ]; then
        echo "Error: ElastiCache 엔드포인트를 가져올 수 없습니다."
        echo "다음 중 하나를 수행하세요:"
        echo "  1. 환경 변수로 직접 설정: export ELASTICACHE_ENDPOINT=your-endpoint"
        echo "  2. Terraform apply를 먼저 실행하여 ElastiCache를 생성하세요"
        exit 1
    fi
    
    export ELASTICACHE_ENDPOINT
    echo "ElastiCache 엔드포인트: $ELASTICACHE_ENDPOINT"
fi

# 기본 설정
export ELASTICACHE_PORT=${ELASTICACHE_PORT:-6379}
export ELASTICACHE_PASSWORD=${ELASTICACHE_PASSWORD:-""}
export ELASTICACHE_SSL=${ELASTICACHE_SSL:-"false"}

# AWS 자격 증명 확인
if [ -z "$AWS_PROFILE" ] && [ -z "$AWS_ACCESS_KEY_ID" ]; then
    echo "Warning: AWS 자격 증명이 설정되지 않았습니다."
    echo "AWS_PROFILE 또는 AWS_ACCESS_KEY_ID를 설정하세요."
fi

echo ""
echo "설정 확인:"
echo "  ELASTICACHE_ENDPOINT: $ELASTICACHE_ENDPOINT"
echo "  ELASTICACHE_PORT: $ELASTICACHE_PORT"
echo "  ELASTICACHE_SSL: $ELASTICACHE_SSL"
echo ""

# 테스트 클래스 수정 안내
echo "=========================================="
echo "테스트 실행 전 확인사항:"
echo "=========================================="
echo "1. 테스트 클래스가 AwsElastiCacheTest를 상속받도록 수정되었는지 확인하세요."
echo "   예: class AuthServiceCacheIntegrationTest extends AwsElastiCacheTest"
echo ""
echo "2. ElastiCache 보안 그룹에서 현재 IP 주소가 허용되어 있는지 확인하세요."
echo ""
echo "3. 네트워크 연결이 가능한지 확인하세요 (VPN, Direct Connect, 또는 EC2 인스턴스)."
echo ""
read -p "계속하시겠습니까? (y/N): " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "테스트를 취소했습니다."
    exit 0
fi

# 테스트 실행
cd service-account

echo ""
echo "=========================================="
echo "테스트 실행 중..."
echo "=========================================="

./gradlew test --tests "*Cache*" --info

echo ""
echo "=========================================="
echo "테스트 완료"
echo "=========================================="

