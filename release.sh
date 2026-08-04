#!/bin/bash
set -e

REPO="eunho3023/Integrated-Portal"
TAG="v1.1.0"
TITLE="v1.1.0 - 날짜별 급식 조회, 정제된 학교 지도 검색, 통화 수신/스피커 제어 및 친구 관리"
NOTES="### 주요 업데이트 및 신규 기능

1. **🍱 날짜별 학교 급식 메뉴 조회**
   - 이전날/다음날/오늘날짜 이동 버튼을 통한 일자별 NEIS 급식 정보 상세 조회

2. **🗺️ 정제된 학교 위치 지도 검색**
   - 지도 앱 연동 시 교육청명, 특수기호, 괄호 등 불필요한 서식 자동 정제 후 순수 학교명으로 위치 탐색 정확도 향상

3. **📞 통화 수신/거절 및 스피커/수신기 전환**
   - 전화 걸기/받기/거절 제어 버튼 및 수신 테스트 모드 지원
   - 외장 스피커폰 ↔ 일반 전화용 스피커(수신기) 실시간 오디오 출력 전환 기능

4. **👥 친구 추가 / 삭제 및 수신자 빠른 발신**
   - 회원 목록 및 검색 카드에서 ⭐/➕ 버튼으로 간편한 친구 등록 및 삭제
   - 수신자 지정 시 즉시 음성/영상 통화 발신 연결"

echo "Creating GitHub release $TAG..."
RESPONSE=$(curl -s -X POST \
  -H "Authorization: token $GoogleAIStudioAutoSync" \
  -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/$REPO/releases \
  -d "$(jq -n \
    --arg tag "$TAG" \
    --arg name "$TITLE" \
    --arg body "$NOTES" \
    '{tag_name: $tag, name: $name, body: $body, draft: false, prerelease: false}')")

UPLOAD_URL=$(echo "$RESPONSE" | jq -r '.upload_url' | sed -e 's/{?name,label}/?name=app-debug.apk/')
HTML_URL=$(echo "$RESPONSE" | jq -r '.html_url')

echo "Release created: $HTML_URL"
echo "Uploading APK asset to $UPLOAD_URL..."

UPLOAD_RES=$(curl -s -X POST \
  -H "Authorization: token $GoogleAIStudioAutoSync" \
  -H "Content-Type: application/vnd.android.package-archive" \
  --data-binary "@app/build/outputs/apk/debug/app-debug.apk" \
  "$UPLOAD_URL")

ASSET_URL=$(echo "$UPLOAD_RES" | jq -r '.browser_download_url')
echo "APK Asset uploaded successfully: $ASSET_URL"

