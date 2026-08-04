#!/bin/bash
set -e

REPO="eunho3023/Integrated-Portal"
TAG="v1.2.0"
TITLE="v1.2.0 - 학년/반/번호 정보 수정 및 WebSockets/WebRTC 회원 통화 연동"
NOTES="### 🚀 신규 기능 및 주요 업데이트

1. **🏫 정보 수정 내 학년 / 반 / 번호 편집 지원**
   - 프로필 수정(내 정보 수정) 대화상자에서 사용자의 **학년**, **반**, **번호** 정보 변경 가능
   - 입력값 자동 정제 및 로컬 데이터베이스 / 사용자 세션 실시간 즉시 반영

2. **📞 WebSockets & WebRTC 기반 회원간 통화 / 문자 지원**
   - 가입 회원 간 WebSockets SDP 시그널링 통신 및 WebRTC P2P 고음질 음성/HD 영상 통화 연동
   - 실시간 수신/발신 상태 표시 및 스피커폰/수신기 오디오 전환 기능 지원"

GH_EXE="/tmp/gh_2.45.0_linux_amd64/bin/gh"

echo "Creating GitHub release $TAG..."
GH_TOKEN="$GoogleAIStudioAutoSync" $GH_EXE release create "$TAG" "app/build/outputs/apk/debug/app-debug.apk#app-debug.apk" \
  --title "$TITLE" \
  --notes "$NOTES" \
  -R "$REPO"

echo "Release $TAG created and APK uploaded successfully!"
