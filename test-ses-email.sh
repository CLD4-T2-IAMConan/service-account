#!/bin/bash

# AWS SES 이메일 전송 테스트 스크립트

set -e

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 기본값
BASE_URL=${BASE_URL:-http://localhost:8081}
TEST_EMAIL=${TEST_EMAIL:-}

echo -e "${BLUE}=================================${NC}"
echo -e "${BLUE}  AWS SES 이메일 전송 테스트${NC}"
echo -e "${BLUE}=================================${NC}"
echo ""

# 이메일 입력
if [ -z "$TEST_EMAIL" ]; then
    echo -e "${YELLOW}📧 수신할 이메일 주소를 입력하세요:${NC}"
    read -r TEST_EMAIL
fi

echo ""
echo -e "${GREEN}✅ 테스트 설정:${NC}"
echo -e "   Base URL: $BASE_URL"
echo -e "   Test Email: $TEST_EMAIL"
echo ""

# 서비스 정보 확인
echo -e "${BLUE}[1/3] 활성화된 이메일 서비스 확인${NC}"
SERVICE_INFO=$(curl -s "$BASE_URL/api/test/email/info")
echo "$SERVICE_INFO" | jq '.'
echo ""

# 인증 이메일 테스트
echo -e "${BLUE}[2/3] 인증 이메일 전송 테스트${NC}"
VERIFICATION_RESULT=$(curl -s -X POST "$BASE_URL/api/test/email/verification?email=$TEST_EMAIL")
echo "$VERIFICATION_RESULT" | jq '.'

if [ "$(echo "$VERIFICATION_RESULT" | jq -r '.success')" = "true" ]; then
    echo -e "${GREEN}✅ 인증 이메일 전송 성공!${NC}"
    VERIFICATION_CODE=$(echo "$VERIFICATION_RESULT" | jq -r '.verificationCode')
    echo -e "${YELLOW}   인증 코드: $VERIFICATION_CODE${NC}"
else
    echo -e "${RED}❌ 인증 이메일 전송 실패${NC}"
    exit 1
fi
echo ""

# 환영 이메일 테스트
echo -e "${BLUE}[3/3] 환영 이메일 전송 테스트${NC}"
WELCOME_RESULT=$(curl -s -X POST "$BASE_URL/api/test/email/welcome?email=$TEST_EMAIL&name=테스터")
echo "$WELCOME_RESULT" | jq '.'

if [ "$(echo "$WELCOME_RESULT" | jq -r '.success')" = "true" ]; then
    echo -e "${GREEN}✅ 환영 이메일 전송 성공!${NC}"
else
    echo -e "${RED}❌ 환영 이메일 전송 실패${NC}"
    exit 1
fi
echo ""

# 완료
echo -e "${GREEN}=================================${NC}"
echo -e "${GREEN}  ✅ 모든 테스트 완료!${NC}"
echo -e "${GREEN}=================================${NC}"
echo ""
echo -e "${YELLOW}📬 이메일함을 확인하세요:${NC}"
echo -e "   - 인증 이메일 (인증 코드: $VERIFICATION_CODE)"
echo -e "   - 환영 이메일"
echo ""
echo -e "${YELLOW}⚠️  주의사항:${NC}"
echo -e "   - SES Sandbox 모드에서는 인증된 이메일 주소로만 전송됩니다"
echo -e "   - 수신이 안 되면 AWS SES 콘솔에서 이메일 주소를 인증하세요"
echo -e "   - 스팸 폴더도 확인해보세요"
echo ""
