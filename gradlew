#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCAL_GRADLE_DIR="$ROOT_DIR/.gradle-local"
DIST_DIR="$LOCAL_GRADLE_DIR/dist"

DEFAULT_TASKS=(":mobile:assembleDebug" ":wear:assembleDebug")

find_gradle_zip() {
  local candidates=(
    "$ROOT_DIR/gradle-8.10.2-all.zip"
    "$ROOT_DIR/gradle-9.0-all.zip"
    "$ROOT_DIR/gradle/distributions/gradle-8.10.2-all.zip"
    "$ROOT_DIR/gradle/distributions/gradle-9.0-all.zip"
    "$HOME/Downloads/gradle-8.10.2-all.zip"
    "$HOME/Downloads/gradle-9.0-all.zip"
  )

  for file in "${candidates[@]}"; do
    if [[ -f "$file" ]]; then
      echo "$file"
      return 0
    fi
  done

  return 1
}

resolve_gradle_bin() {
  if [[ -n "${GRADLE_HOME:-}" && -x "$GRADLE_HOME/bin/gradle" ]]; then
    echo "$GRADLE_HOME/bin/gradle"
    return 0
  fi

  if command -v gradle >/dev/null 2>&1; then
    command -v gradle
    return 0
  fi

  mkdir -p "$DIST_DIR"

  local zip_file
  if ! zip_file="$(find_gradle_zip)"; then
    cat >&2 <<'EOF'
Gradle을 찾지 못했습니다.

아래 중 하나를 해주세요.

1) 권장: gradle-8.10.2-all.zip 파일을 프로젝트 루트 또는 ~/Downloads 에 넣기
   예: GestureSync/gradle-8.10.2-all.zip
   예: ~/Downloads/gradle-8.10.2-all.zip

2) 가지고 있는 gradle-9.0-all.zip 파일을 프로젝트 루트 또는 ~/Downloads 에 넣기
   예: GestureSync/gradle-9.0-all.zip
   예: ~/Downloads/gradle-9.0-all.zip

3) GRADLE_HOME 설정
   export GRADLE_HOME=/path/to/gradle

4) Homebrew 사용 가능하면 설치
   brew install gradle
EOF
    exit 1
  fi

  local base_name
  base_name="$(basename "$zip_file" .zip)"
  local extracted_dir="$DIST_DIR/$base_name"

  if [[ ! -x "$extracted_dir/bin/gradle" ]]; then
    echo "Using local Gradle zip: $zip_file"
    rm -rf "$extracted_dir"
    unzip -q "$zip_file" -d "$DIST_DIR"
  fi

  if [[ ! -x "$extracted_dir/bin/gradle" ]]; then
    echo "Gradle 실행 파일을 찾지 못했습니다: $extracted_dir/bin/gradle" >&2
    exit 1
  fi

  echo "$extracted_dir/bin/gradle"
}

GRADLE_BIN="$(resolve_gradle_bin)"

cd "$ROOT_DIR"

if [[ $# -eq 0 ]]; then
  exec "$GRADLE_BIN" "${DEFAULT_TASKS[@]}" --stacktrace
else
  exec "$GRADLE_BIN" "$@"
fi
