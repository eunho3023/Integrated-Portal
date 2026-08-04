#!/bin/bash
set -e

REPO="eunho3023/Integrated-Portal"
TAG="v1.4.2"
TITLE="v1.4.2 - Firebase Realtime Database 기반 회원 간 1:1 실시간 전화 통화 시그널링 구축"
NOTES="### 📞 주요 업데이트 내역

1. **🔥 Firebase Realtime Database 1:1 실시간 통화 시그널링 연동**
   - 발신 시 \`calls/{callId}\` 노드로 통화 신호(\`RINGING\`)가 전송됩니다.
   - 가입된 상대방 회원의 앱에 전화 수신 알림 화면(\`RINGING_INCOMING\`)이 즉시 팝업됩니다.
   - 상대방 회원이 **[📞 받기 / 통화 수락]**을 누르면 시그널 상태가 \`ACCEPTED\`로 전환되어 양쪽 기기가 모두 실시간 통화 상태(\`CONNECTED\`)로 연동됩니다.

2. **📲 통화 제어 및 시뮬레이션 개선**
   - 단일 기기 테스트를 위해 발신 화면에 **[📲 상대방 수락 응답]** 테스트 버튼 추가
   - 전화 거절(\`REJECTED\`) 및 통화 종료(\`ENDED\`) 시 실시간 상태 반영"

GH_EXE="/tmp/gh_2.45.0_linux_amd64/bin/gh"

echo "Creating GitHub release $TAG..."
GH_TOKEN="$GoogleAIStudioAutoSync" $GH_EXE release create "$TAG" "app/build/outputs/apk/debug/app-debug.apk#app-debug.apk" \
  --title "$TITLE" \
  --notes "$NOTES" \
  -R "$REPO"

GH_TOKEN="$GoogleAIStudioAutoSync" $GH_EXE release edit "$TAG" --draft=false -R "$REPO"

echo "Release $TAG created and published successfully!"
