# GestureSync

GestureSync는 갤럭시 워치의 스와이프/탭 제스처를 갤럭시 폰으로 전달해 폰 화면에 실제 스와이프/탭 제스처를 실행하는 Android + Wear OS 연동 앱입니다.

현재 버전은 MVP입니다.

- `mobile`: 갤럭시 폰용 앱
- `wear`: 갤럭시 워치용 앱
- `shared`: 폰/워치가 공유하는 메시지 경로와 명령 상수

## 핵심 동작

```text
Galaxy Watch 앱 실행
  -> Wear OS MessageClient로 폰 앱 실행 요청
  -> Phone 앱이 실행되거나 알림으로 실행 유도
  -> Watch에서 스와이프/탭
  -> Phone의 AccessibilityService가 실제 제스처 실행
```

## 지원 기능

| 워치 동작 | 폰 동작 |
|---|---|
| 위로 스와이프 | 폰 화면 위로 스와이프 |
| 아래로 스와이프 | 폰 화면 아래로 스와이프 |
| 왼쪽 스와이프 | 폰 화면 왼쪽 스와이프 |
| 오른쪽 스와이프 | 폰 화면 오른쪽 스와이프 |
| 탭 | 폰 화면 중앙 탭 |

## 기술 구조

```text
GestureSync/
  shared/
    GestureSyncContract.kt

  mobile/
    MainActivity.kt
    RemoteGestureAccessibilityService.kt
    GestureWearListenerService.kt

  wear/
    MainActivity.kt
```

### mobile

폰 앱은 두 가지 일을 합니다.

1. `GestureWearListenerService`가 워치에서 보낸 메시지를 수신합니다.
2. `RemoteGestureAccessibilityService`가 `dispatchGesture()`로 실제 화면 제스처를 실행합니다.

접근성 서비스는 사용자가 직접 켜야 합니다.

```text
설정 > 접근성 > 설치된 앱 > GestureSync Remote Control > 사용
```

### wear

워치 앱은 화면 전체를 간단한 터치패드처럼 사용합니다.

앱 실행 시 자동으로 폰 앱 실행 명령을 보냅니다.

```kotlin
sendCommand(GestureSyncContract.PATH_OPEN_PHONE_APP)
```

그 후 워치 화면에서 스와이프/탭하면 폰으로 명령을 전송합니다.

## 빌드 방법

### GitHub Actions에서 빌드

이 저장소에는 `.github/workflows/android-build.yml`이 포함되어 있습니다.

1. GitHub 저장소로 이동
2. `Actions` 탭 선택
3. `Android APK Build` 워크플로우 선택
4. `Run workflow` 실행
5. 빌드 완료 후 `Artifacts`에서 `GestureSync-debug-apks` 다운로드

다운로드하면 다음 APK가 들어 있습니다.

```text
mobile-debug.apk
wear-debug.apk
```

### 로컬에서 빌드

Android Studio 또는 Gradle이 설치된 환경에서 실행합니다.

```bash
git clone https://github.com/awepp-jiappa/GestureSync.git
cd GestureSync
gradle :mobile:assembleDebug :wear:assembleDebug
```

빌드 결과:

```text
mobile/build/outputs/apk/debug/mobile-debug.apk
wear/build/outputs/apk/debug/wear-debug.apk
```

Gradle Wrapper는 아직 포함하지 않았습니다. Android Studio에서 프로젝트를 열면 Gradle 설정을 자동으로 구성할 수 있습니다.

## 설치 방법

### 1. 폰 앱 설치

폰에서 USB 디버깅을 켠 뒤 실행합니다.

```bash
adb install -r mobile/build/outputs/apk/debug/mobile-debug.apk
```

### 2. 워치 앱 설치

워치에서 개발자 옵션과 ADB 디버깅을 켭니다.

Galaxy Watch에서 보통 다음 순서로 켭니다.

```text
설정 > 워치 정보 > 소프트웨어 정보 > 소프트웨어 버전 여러 번 탭
설정 > 개발자 옵션 > ADB 디버깅 ON
설정 > 개발자 옵션 > Wi-Fi 디버깅 ON
```

워치와 PC가 같은 Wi-Fi에 연결된 상태에서 워치 IP를 확인한 뒤:

```bash
adb connect WATCH_IP:5555
adb install -r wear/build/outputs/apk/debug/wear-debug.apk
```

예:

```bash
adb connect 192.168.0.25:5555
adb install -r wear/build/outputs/apk/debug/wear-debug.apk
```

## 사용 방법

1. 갤럭시 폰에 `mobile-debug.apk` 설치
2. 갤럭시 워치에 `wear-debug.apk` 설치
3. 폰에서 GestureSync 실행
4. `접근성 설정 열기` 선택
5. `GestureSync Remote Control` 접근성 서비스 ON
6. 워치에서 GestureSync 실행
7. 워치 앱이 폰 앱 실행 명령을 보냄
8. YouTube Shorts, TikTok, 브라우저, 웹툰 등 원하는 앱을 폰에서 열기
9. 워치 화면에서 스와이프/탭

## 현재 제약사항

- 폰 화면이 켜져 있고 잠금 해제된 상태에서 사용하는 것을 전제로 합니다.
- Android의 백그라운드 Activity 실행 제한 때문에 워치 앱 실행 시 폰 앱이 항상 전면으로 뜨지 않을 수 있습니다. 이 경우 폰에 알림을 띄워 실행을 유도합니다.
- 앱별로 필요한 스와이프 거리와 시간이 다를 수 있습니다.
- Play Store 배포 시 접근성 API 사용 목적을 명확히 설명해야 합니다.

## 다음 개발 후보

- 스와이프 거리/시간 설정 화면
- 앱별 프리셋: Shorts, TikTok, 웹툰, 브라우저
- 뒤로가기/홈/최근 앱 명령
- 볼륨 제어
- 워치 베젤 또는 회전 입력 지원
- 연결 상태 표시
- 폰 앱에서 워치 연결 테스트
