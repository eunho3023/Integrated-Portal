#!/bin/bash
set -e

REPO="eunho3023/Integrated-Portal"
TAG="v1.4.0"
TITLE="v1.4.0 - 통화/문자 탭 내 📇 연락처(회원 검색 및 추가/삭제) 전면 개편"
NOTES="### 📇 주요 업데이트 내역

1. **📇 연락처 (회원 검색 및 추가/삭제) 센터 전면 도입**
   - 상단에 연락처 섹션을 배치하여 내 가입 회원 전체 검색(이름, 전화번호, 학년/반) 지원
   - 검색된 회원을 ⭐ 연락처 추가 / ❌ 삭제할 수 있는 관리 기능 제공
   - 연락처로 등록된 회원 목록을 상단에 고정 표시하며 💬 문자, 📞 통화를 바로 시작하는 단축 버튼 연결

2. **🧹 기존 임시 가상 구성원 연결 제거 및 회원 연동 정제**
   - 가짜 예시 구성원(임시 데이터)을 완전 삭제하고 실제 가입된 전체 회원 데이터와 연동
   - 통화 및 1:1 대화 대상 선택 시 실제 회원 정보(displayName, 학년/반/번호, 역할) 기반으로 동작하도록 최적화"

GH_EXE="/tmp/gh_2.45.0_linux_amd64/bin/gh"

echo "Creating GitHub release $TAG..."
GH_TOKEN="$GoogleAIStudioAutoSync" $GH_EXE release create "$TAG" "app/build/outputs/apk/debug/app-debug.apk#app-debug.apk" \
  --title "$TITLE" \
  --notes "$NOTES" \
  -R "$REPO"

GH_TOKEN="$GoogleAIStudioAutoSync" $GH_EXE release edit "$TAG" --draft=false -R "$REPO"

echo "Release $TAG created and published successfully!"
