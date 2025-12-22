#!/bin/bash

# 로컬 Valkey 빌드 및 실행 스크립트

set -e

echo "=========================================="
echo "로컬 Valkey 빌드 및 실행"
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

VALKEY_DIR="$PROJECT_DIR/../valkey"

echo -e "${BLUE}옵션을 선택하세요:${NC}"
echo "1. Valkey 소스에서 빌드 및 실행"
echo "2. Docker Compose로 Valkey 실행 (권장)"
echo "3. 기존 Valkey 사용 (이미 실행 중)"

read -p "선택 (1-3): " choice

case $choice in
    1)
        echo -e "${BLUE}Valkey 소스에서 빌드 중...${NC}"
        
        # Valkey 디렉토리 확인
        if [ ! -d "$VALKEY_DIR" ]; then
            echo -e "${YELLOW}Valkey 디렉토리가 없습니다. 클론합니다...${NC}"
            cd "$PROJECT_DIR/.."
            git clone https://github.com/valkey-io/valkey.git
            cd valkey
        else
            cd "$VALKEY_DIR"
        fi
        
        # 빌드
        echo -e "${BLUE}빌드 중... (시간이 걸릴 수 있습니다)${NC}"
        make
        
        # 실행
        echo -e "${GREEN}Valkey 실행 중... (포트 6379)${NC}"
        echo -e "${YELLOW}종료하려면 Ctrl+C를 누르세요${NC}"
        ./src/valkey-server
        ;;
    2)
        echo -e "${BLUE}Docker Compose로 Valkey 실행 중...${NC}"
        cd "$PROJECT_DIR"
        docker-compose up -d valkey
        
        echo -e "${GREEN}Valkey가 실행되었습니다!${NC}"
        echo -e "${BLUE}포트: 6380 (호스트) -> 6379 (컨테이너)${NC}"
        echo ""
        echo "테스트 실행:"
        echo "  export REDIS_HOST=localhost"
        echo "  export REDIS_PORT=6380"
        echo "  ./gradlew integrationTest --tests '*Cache*'"
        ;;
    3)
        echo -e "${BLUE}기존 Valkey 사용${NC}"
        echo ""
        echo "환경 변수를 설정하세요:"
        echo "  export REDIS_HOST=localhost"
        echo "  export REDIS_PORT=6379  # 또는 6380 (docker-compose의 경우)"
        echo ""
        echo "테스트 실행:"
        echo "  ./gradlew integrationTest --tests '*Cache*'"
        ;;
    *)
        echo "잘못된 선택입니다."
        exit 1
        ;;
esac

