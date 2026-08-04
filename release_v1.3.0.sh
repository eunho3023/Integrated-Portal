#!/bin/bash
set -e

REPO="eunho3023/Integrated-Portal"
TAG="v1.3.0"
TITLE="v1.2.1 - 발신자/수신자 통화 제어 분리 및 문자 인공지능 자동 답장 제거"
NOTES="### 📞 주요 개선 및 릴리즈 내역

1. **📱 통화 수신 / 발신 제어 UI 완전히 분리**
   - 발신자(전화를 건 사람) 화면에서 **통화 수락(받기)** 버튼이 노출되던 현상 수정
   - 발신자는 상대방 수신 응답 대기 상태 및 **통화 취소 / 끊기** 버튼만 제공
   - 수신자(전화를 받은 사람) 화면에서만 **통화 수락(받기)** 및 **통화 거절** 버튼 분리 제공

2. **📩 문자/메시지 센터 AI 자동 답장 제거**
   - 문자 전송 시 인공지능/봇이 자동으로 생성하던 '문자를 확인했습니다' 대답 삭제
   - 회원에게 실제 메시지가 전송 및 기록되며, 가입된 수신자가 직접 확인 및 답변하도록 정상화"

GH_EXE="/tmp/gh_2.45.0_linux_amd64/bin/gh"

echo "Creating GitHub release $TAG..."
GH_TOKEN="$GoogleAIStudioAutoSync" $GH_EXE release create "$TAG" "app/build/outputs/apk/debug/app-debug.apk#app-debug.apk" \
  --title "$TITLE" \
  --notes "$NOTES" \
  -R "$REPO"

GH_TOKEN="$GoogleAIStudioAutoSync" $GH_EXE release edit "$TAG" --draft=false -R "$REPO"

echo "Release $TAG created and published successfully!"
