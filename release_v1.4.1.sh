#!/bin/bash
set -e

REPO="eunho3023/Integrated-Portal"
TAG="v1.4.1"
TITLE="v1.4.1 - Firebase Realtime Database (https://integrated-portal-ea306-default-rtdb.firebaseio.com) 회원 연동 최적화"
NOTES="### 🔥 주요 업데이트 내역

1. **🔥 Firebase Realtime Database 회원 실시간 동기화 완료**
   - \`https://integrated-portal-ea306-default-rtdb.firebaseio.com/users\` 노드 실시간 수신 리스너 등록
   - 앱 내 가입된 모든 회원을 실시간으로 자동 연동하여 연락처, 회원 검색, 1:1 톡 및 통화 대상에 동기화

2. **📇 연락처 및 회원 연동 안정화**
   - Firebase Realtime Database에 등록된 회원의 ID, 이름, 전화번호, 역할, 학년/반 정보를 앱 전체에 실시간 적용"

GH_EXE="/tmp/gh_2.45.0_linux_amd64/bin/gh"

echo "Creating GitHub release $TAG..."
GH_TOKEN="$GoogleAIStudioAutoSync" $GH_EXE release create "$TAG" "app/build/outputs/apk/debug/app-debug.apk#app-debug.apk" \
  --title "$TITLE" \
  --notes "$NOTES" \
  -R "$REPO"

GH_TOKEN="$GoogleAIStudioAutoSync" $GH_EXE release edit "$TAG" --draft=false -R "$REPO"

echo "Release $TAG created and published successfully!"
