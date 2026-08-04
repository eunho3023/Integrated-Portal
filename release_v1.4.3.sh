#!/bin/bash
set -e

REPO="eunho3023/Integrated-Portal"
TAG="v1.4.3"
TITLE="v1.4.3 - 카메라 & 마이크 권한 연동 및 실시간 CameraX 프리뷰 / 오디오 인풋 모니터 구축"
NOTES="### 📹/🎤 카메라 & 마이크 작동 내역 업데이트

1. **📷 CameraX 실시간 영상통화 프리뷰 연동**
   - \`AndroidManifest.xml\`에 \`CAMERA\` 및 \`RECORD_AUDIO\` 필수 하드웨어 권한 추가
   - 영상통화 진입 시 CameraX \`PreviewView\`를 통해 전면/후면 실시간 카메라 영상이 표출됩니다.
   - 전면 / 후면 카메라 실시간 스위칭 버튼 및 카메라 On/Off 제어 제공

2. **🎤 실시간 오디오 마이크 앰플리튜드 레벨 인디케이터**
   - \`AudioRecord\`를 연동하여 실시간 마이크 입력 볼륨 파동(Audio Wave Visualizer)을 시각적으로 측정 및 디스플레이
   - 런타임 동적 권한 요청(\`RequestMultiplePermissions\`) 팝업 및 허용 가이드 버튼 추가"

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
