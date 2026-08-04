#!/bin/bash
set -e

REPO="eunho3023/Integrated-Portal"
TAG="v1.4.4"
TITLE="v1.4.4 - 통화 아이콘 회전 애니메이션 제거 및 실시간 시그널링 통화 안내 UI 개선"
NOTES="### 🛠️ UI 및 통화 기능 개선 내역

1. **🔄 통화 화면 아이콘 회전 애니메이션 제거**
   - 통화 화면의 마이크 및 회원 아바타 아이콘에 설정되어 있던 지속 회전 애니메이션(\`.rotate(rotation)\`)을 제거하여 고정형 직관적인 아이콘 레이아웃으로 변경했습니다.

2. **📞 Firebase 실시간 전화 시그널링 & 권한/입력 안내 강화**
   - Firebase Realtime Database \`calls/{callId}\` 기반의 1:1 전화 수신(\`RINGING\`), 수락(\`ACCEPTED\`), 거절(\`REJECTED\`), 종료(\`ENDED\`) 시그널링 동작을 더 명확하게 안내합니다.
   - local 기기의 CameraX 실시간 카메라 프리뷰 및 AudioRecord 마이크 레벨 볼륨 인디케이터 동작 상태 표시를 정돈했습니다."

GH_EXE="/tmp/gh_2.45.0_linux_amd64/bin/gh"

echo "Building APK..."
gradle assembleDebug

echo "Creating GitHub release $TAG..."
GH_TOKEN="$GoogleAIStudioAutoSync" $GH_EXE release create "$TAG" "app/build/outputs/apk/debug/app-debug.apk#app-debug.apk" \
  --title "$TITLE" \
  --notes "$NOTES" \
  -R "$REPO"

GH_TOKEN="$GoogleAIStudioAutoSync" $GH_EXE release edit "$TAG" --draft=false -R "$REPO"

echo "Release $TAG created and published successfully!"
