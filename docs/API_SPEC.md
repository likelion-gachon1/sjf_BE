# MCM PORTAL API 명세서

## 기본 정보

- Base URL (로컬): `http://localhost:8080`
- API Prefix: `/api/v1`
- 인증: 없음 (MVP 단계)
- 데이터 형식: JSON
- 이미지 업로드: `multipart/form-data`

---

## 1. 촬영 결과 저장

사용자가 선택한 제품, 무드, 여행 스타일, World 정보와 합성 완료된 JPEG 이미지를 저장합니다.

### Request

```text
POST /api/v1/sessions
Content-Type: multipart/form-data
```

### Form Data

| Key | Type | 필수 | 설명 |
| --- | --- | --- | --- |
| `metadata` | application/json | O | 사용자 선택값과 세션 정보 |
| `image` | image/jpeg | O | 촬영 및 합성 완료된 JPEG 이미지 |

### metadata 예시

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

### metadata 필드

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `sessionId` | string | O | 프론트에서 생성한 UUID |
| `consent` | boolean | O | 촬영 이미지 활용 동의 여부. 반드시 `true` |
| `productId` | string | O | 선택한 제품 ID |
| `colorwayKey` | string | O | 선택한 제품 컬러 |
| `mood` | string | O | 선택한 여행 무드 |
| `journey` | string | O | 선택한 여행 스타일 |
| `worldId` | string | O | 프론트가 결정한 World ID |
| `capturedAt` | number | O | 촬영 시각. `Date.now()` 값 사용 |

### 허용값

#### colorwayKey

```text
pink
black
```

#### mood

```text
light
calm
bold
```

#### journey

```text
explore
culture
relax
```

#### worldId

```text
paris_dawn
monaco_night
seoul_neon
milano_terrace
tokyo_mirage
ibiza_sunset
newyork_attitude
santorini_breeze
```

### 성공 응답

```text
HTTP 201 Created
```

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

### 응답 필드

| 필드 | 설명 |
| --- | --- |
| `shareUrl` | QR 코드에 넣을 모바일 결과 페이지 주소 |
| `imageUrl` | 저장된 촬영 결과 이미지 조회 주소 |
| `downloadUrl` | 저장된 촬영 결과 이미지 다운로드 주소 |
| `expiresAt` | 세션 만료 시각 |

### 프론트 연동 방식

프론트는 합성 완료 후 `FormData`를 만들어 전송합니다.

```text
1. 선택값과 sessionId를 metadata JSON으로 생성
2. 캡처한 JPEG 이미지를 image로 추가
3. POST /api/v1/sessions 요청
4. 응답의 shareUrl을 QR 코드로 생성
```

---

## 2. 촬영 결과 정보 조회

특정 세션의 선택값과 이미지 URL을 조회합니다.

### Request

```text
GET /api/v1/sessions/{sessionId}
```

### 예시

```text
GET /api/v1/sessions/550e8400-e29b-41d4-a716-446655440000
```

### 성공 응답

```text
HTTP 200 OK
```

응답 형식은 촬영 결과 저장 API의 성공 응답과 동일합니다.

---

## 3. 촬영 이미지 보기

저장된 JPEG 이미지를 브라우저에서 조회합니다.

### Request

```text
GET /api/v1/sessions/{sessionId}/image
```

### 성공 응답

```text
HTTP 200 OK
Content-Type: image/jpeg
```

---

## 4. 촬영 이미지 다운로드

저장된 JPEG 이미지를 다운로드합니다.

### Request

```text
GET /api/v1/sessions/{sessionId}/download
```

### 성공 응답

```text
HTTP 200 OK
Content-Type: image/jpeg
Content-Disposition: attachment
```

---

## 5. 오류 응답

모든 오류는 다음 형식으로 반환됩니다.

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "촬영 이미지 활용 동의가 필요합니다.",
  "timestamp": "2026-08-15T20:05:19.183235600Z"
}
```

| 상태 코드 | 상황 |
| --- | --- |
| `400 Bad Request` | 필수값 누락, 잘못된 enum 값, 동의 미체크, JSON 형식 오류, JPEG가 아닌 이미지 |
| `404 Not Found` | 존재하지 않는 세션 또는 이미지 |
| `410 Gone` | 24시간이 지나 만료된 세션 |
| `413 Payload Too Large` | 이미지 파일이 10MB 초과 |
| `500 Internal Server Error` | 서버 또는 이미지 저장 중 오류 |

---

## 6. 이미지 제한

| 항목 | 제한 |
| --- | --- |
| 파일 형식 | JPEG / JPG |
| 최대 파일 크기 | 10MB |
| 보관 시간 | 기본 24시간 |

---

## 7. 로컬 개발 환경 주의사항

로컬에서는 `shareUrl`이 다음처럼 반환됩니다.

```text
http://localhost:3000/m/{sessionId}
```

하지만 실제 휴대폰으로 QR을 스캔하면 휴대폰의 `localhost`를 의미하므로 접속할 수 없습니다.

휴대폰 테스트 또는 배포 단계에서는 다음 값을 실제 PC IP 또는 배포 URL로 변경해야 합니다.

```text
FRONTEND_BASE_URL
PUBLIC_API_BASE_URL
ALLOWED_ORIGINS
```
