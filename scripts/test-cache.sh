#!/bin/bash

# Valkey/Redis 캐싱 기능 테스트 스크립트

set -e

echo "=========================================="
echo "Valkey/Redis 캐싱 기능 테스트"
echo "=========================================="

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 현재 디렉토리 확인
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"
cd "$PROJECT_DIR"

echo -e "${BLUE}1. 단위 테스트 실행 (CacheKeyGenerator)${NC}"
./gradlew test --tests CacheKeyGeneratorTest

echo ""
echo -e "${BLUE}2. 통합 테스트 실행 (캐싱 기능)${NC}"
echo -e "${YELLOW}주의: Testcontainers가 Docker를 사용하므로 Docker가 실행 중이어야 합니다.${NC}"
./gradlew integrationTest --tests "*Cache*"

echo ""
echo -e "${GREEN}=========================================="
echo "모든 테스트 완료!"
echo "==========================================${NC}"

echo ""
echo "추가 테스트 옵션:"
echo "  - 특정 테스트만 실행: ./gradlew test --tests CacheKeyGeneratorTest"
echo "  - 통합 테스트만 실행: ./gradlew integrationTest"
echo "  - 수동 테스트: doc/VALKEY-CACHE-TESTING.md 참조"

