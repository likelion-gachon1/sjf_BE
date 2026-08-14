// fork 권한 열어주세요

# MCM PORTAL Backend

MCM PORTAL은 사용자가 선택한 제품, 여행 무드, 여행 스타일에 따라 생성된 World를 체험하고, 촬영 결과를 QR로 저장·공유할 수 있는 서비스입니다.

이 저장소는 촬영 결과와 선택값을 저장하고 조회하는 Spring Boot 백엔드입니다.

## 주요 기능

- 촬영 결과 JPEG 이미지 업로드 및 로컬 저장
- 제품, 컬러, 무드, 여행 스타일, World 선택값 저장
- QR에 사용할 공유 URL 반환
- 촬영 결과 이미지 조회 및 다운로드
- 세션 24시간 만료 처리
- CORS 및 요청 예외 처리

## 기술 스택

- Java 17
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- H2 Database
- Gradle
- Lombok

## 실행 방법

### 프로젝트 실행

Windows:

```bash
gradlew.bat bootRun
```

Mac/Linux:

```bash
./gradlew bootRun
```

서버는 기본적으로 `8080` 포트에서 실행됩니다.

### 서버 상태 확인

```text
GET http://localhost:8080/api/health
```

응답:

```text
MCM PORTAL SERVER OK
```

## API

### 촬영 결과 저장

```text
POST /api/v1/sessions
Content-Type: multipart/form-data
```

요청 구성:

- `metadata`: `application/json`
- `image`: `image/jpeg`

`metadata` 예시:

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "consent": true,
  "productId": "stark_backpack_visetos",
  "colorwayKey": "pink",
  "mood": "calm",
  "journey": "culture",
  "worldId": "paris_dawn",
  "capturedAt": 1786737917203
}
```

성공 응답 예시:

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "productId": "stark_backpack_visetos",
  "colorwayKey": "pink",
  "mood": "calm",
  "journey": "culture",
  "worldId": "paris_dawn",
  "capturedAt": 1786737917203,
  "shareUrl": "http://localhost:3000/m/550e8400-e29b-41d4-a716-446655440000",
  "imageUrl": "http://localhost:8080/api/v1/sessions/550e8400-e29b-41d4-a716-446655440000/image",
  "downloadUrl": "http://localhost:8080/api/v1/sessions/550e8400-e29b-41d4-a716-446655440000/download",
  "expiresAt": "2026-08-15T20:05:19.183235600Z"
}
```

### 촬영 결과 조회

```text
GET /api/v1/sessions/{sessionId}
```

### 촬영 이미지 보기

```text
GET /api/v1/sessions/{sessionId}/image
```

### 촬영 이미지 다운로드

```text
GET /api/v1/sessions/{sessionId}/download
```

## 현재 저장 방식

개발 단계에서는 H2 Database와 로컬 파일 저장 방식을 사용합니다.

- DB: `./data`
- 이미지: `./uploads`
- 세션 보관 시간: 기본 24시간

배포 단계에서는 MySQL 등의 운영 DB와 Cloudinary 또는 AWS S3 같은 이미지 저장소로 교체할 예정입니다.

## 프론트 연동

프론트엔드는 촬영 완료 후 합성된 JPEG 이미지와 선택값을 `POST /api/v1/sessions`로 전송합니다.

응답으로 받은 `shareUrl`을 QR 코드로 생성하면 사용자가 휴대폰에서 결과 페이지에 접근할 수 있습니다.
