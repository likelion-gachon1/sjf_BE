# MCM PORTAL — 백엔드 (`sjf_BE`)

> 멋사 대학 14기 해커톤 · 가천미인 · Challenge 02 :: 인터랙티브 리테일
> Live ▸ https://mcm-portal.duckdns.org
> Repos ▸ 프론트 sjf_track · 백엔드 sjf_BE · 배경생성 sjf_ai

MCM PORTAL 부스에서 촬영한 합성 결과와 고객이 고른 선택값(제품·컬러웨이·무드·여행 스타일·World)을
저장하고, QR로 이어지는 모바일 결과 페이지의 주소를 만들어 돌려주는 Spring Boot 백엔드입니다.
부스 프론트가 합성 JPEG과 선택값을 한 번에 올리면, 이 서버가 세션 하나로 묶어 보관하고
`shareUrl`(모바일 페이지) · `imageUrl` · `downloadUrl` 을 응답으로 돌려줍니다. 저장된 세션은
24시간 뒤 만료되며, 이후 조회·이미지 요청은 모두 만료 응답으로 처리됩니다.

## 아키텍처

부스에서 촬영이 끝난 순간부터 방문객 휴대폰에서 사진을 여는 순간까지의 요청 흐름입니다.

```
[부스 프론트] 촬영·합성(JPEG) + 선택값(metadata)
      │
      │  multipart/form-data (metadata JSON + image JPEG)
      ▼
POST /api/v1/sessions ──▶ 동의·형식 검증 ─▶ 이미지 로컬 저장(./uploads)
                                            + 세션 저장(H2, ./data)
      │
      │  201 Created { sessionId, shareUrl, imageUrl, downloadUrl, expiresAt, ... }
      ▼
[부스 프론트] shareUrl 로 QR 코드 생성
      │
      │  방문객이 QR 스캔
      ▼
[모바일] GET /api/v1/sessions/{sessionId}          선택값 + 이미지 URL 조회
         GET /api/v1/sessions/{sessionId}/image     사진 보기 (inline)
         GET /api/v1/sessions/{sessionId}/download   사진 저장 (attachment)
```

`shareUrl`(`{FRONTEND_BASE_URL}/m/{sessionId}`)과 `imageUrl`·`downloadUrl`
(`{PUBLIC_API_BASE_URL}/api/v1/sessions/{sessionId}/...`)의 host 는 환경 변수로 갈립니다.
로컬은 `localhost:3000` / `localhost:8080`, 배포는 nginx 리버스 프록시 뒤에서 같은
도메인(`mcm-portal.duckdns.org`)을 가리킵니다.

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| 언어 | Java 17 (Gradle toolchain) |
| 프레임워크 | Spring Boot (`org.springframework.boot` 4.1.0) |
| 웹 | Spring Web MVC (`spring-boot-starter-webmvc`) |
| 검증 | Spring Validation (`spring-boot-starter-validation`) |
| 영속성 | Spring Data JPA (`spring-boot-starter-data-jpa`) |
| DB | H2 (개발·현재 운영), PostgreSQL 드라이버 동봉(운영 전환 대비) |
| 빌드 | Gradle (`gradlew`) |
| 보일러플레이트 | Lombok |

빌드 파일 기준으로 H2 는 `runtimeOnly`, PostgreSQL 드라이버(`org.postgresql:postgresql`)도
`runtimeOnly` 로 함께 들어 있어 datasource URL 만 바꾸면 운영 DB로 전환할 수 있는 상태입니다.
현재 실제 구동은 H2 파일 모드입니다.

## 도메인 모델

핵심 엔티티는 `PortalSession` 하나입니다. `@Table(name = "portal_sessions")`, PK 는 프론트가
발급한 `sessionId`(문자열)이며, enum 필드는 모두 `@Enumerated(EnumType.STRING)` 으로
문자열 컬럼에 저장됩니다.

| 필드 | 타입 | 컬럼 | 설명 |
| --- | --- | --- | --- |
| `sessionId` | String | `session_id` (PK, len 64) | 프론트가 발급한 세션 ID(UUID 형식) |
| `consent` | boolean | `consent` | 이미지 활용 동의 여부. 저장은 항상 `true`(미동의는 400으로 차단) |
| `productId` | String | `product_id` (len 100) | 선택한 제품 ID |
| `colorwayKey` | ColorwayKey (enum) | `colorway_key` | 제품 컬러웨이 |
| `mood` | Mood (enum) | `mood` | AI가 판정한 의상 무드 |
| `journey` | Journey (enum) | `journey` | 선택한 여행 스타일 |
| `worldId` | WorldId (enum) | `world_id` | 프론트가 결정한 World |
| `imageFilename` | String | `image_filename` | 저장된 이미지 파일명(`{sessionId}.jpg`) |
| `capturedAt` | Instant | `captured_at` | 촬영 시각(요청의 epoch millis) |
| `createdAt` | Instant | `created_at` | 서버 저장 시각 |
| `expiresAt` | Instant | `expires_at` | 만료 시각(`createdAt + TTL`) |

enum 허용값은 코드에 정의된 그대로입니다. 요청에서는 소문자로 오고, 응답에서도 소문자로
내려갑니다(서버 내부에서만 대문자 enum).

| enum | 값 |
| --- | --- |
| `ColorwayKey` | `pink`, `beige` |
| `Mood` | `light`, `calm`, `bold` |
| `Journey` | `explore`, `culture`, `relax` |
| `WorldId` | `paris_dawn`, `monaco_night`, `seoul_neon`, `milano_terrace`, `tokyo_mirage`, `ibiza_sunset`, `newyork_attitude`, `santorini_breeze` |

`WorldId` enum 은 8종을 모두 정의하지만, 실제 체험에서 프론트가 활성화하는 World 는 4종
(`paris_dawn`·`milano_terrace`·`newyork_attitude`·`seoul_neon`)입니다. 백엔드는 8종을 모두
유효값으로 받아들입니다.

## API 명세

Base URL(로컬): `http://localhost:8080` · 인증 없음(MVP) · 상세 규격은
[`docs/API_SPEC.md`](docs/API_SPEC.md) 참고.

| 메서드 | 경로 | 설명 | 성공 응답 |
| --- | --- | --- | --- |
| `GET` | `/api/health` | 헬스체크 | `200` · `MCM PORTAL SERVER OK` (text) |
| `POST` | `/api/v1/sessions` | 촬영 결과 저장(멀티파트) | `201 Created` · `SessionResponse` |
| `GET` | `/api/v1/sessions/{sessionId}` | 세션 조회 | `200` · `SessionResponse` |
| `GET` | `/api/v1/sessions/{sessionId}/image` | 이미지 보기(inline) | `200` · `image/jpeg` |
| `GET` | `/api/v1/sessions/{sessionId}/download` | 이미지 다운로드(attachment) | `200` · `image/jpeg` |
| `GET` | `/api/products` | 제품 목록(하드코딩 2종) | `200` · `ProductResponse[]` |
| `POST` | `/api/worlds/recommend` | World 추천(규칙 기반) | `200` · `WorldRecommendResponse` |

이미지 응답에는 `Cache-Control: no-store` 가 걸립니다(만료·교체 시 캐시 잔존 방지).

> `/api/products` 와 `/api/worlds/recommend` 는 서버에 구현돼 있지만, 현재 프론트는 제품
> 데이터와 World 결정을 자체 config 로 처리합니다. 두 엔드포인트의 응답값은 코드에 하드코딩된
> 예시 데이터입니다.

### `POST /api/v1/sessions` 요청

`multipart/form-data` 의 두 파트로 구성됩니다.

| 파트 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `metadata` | application/json | O | 선택값과 세션 정보 |
| `image` | image/jpeg | O | 촬영·합성 완료된 JPEG |

`metadata` 필드 검증 규칙(요청 DTO 기준):

| 필드 | 타입 | 제약 |
| --- | --- | --- |
| `sessionId` | string | 필수 · `^[A-Za-z0-9-]{16,64}$` |
| `consent` | boolean | 필수 · 반드시 `true` |
| `productId` | string | 필수 · 최대 100자 |
| `colorwayKey` | string | 필수 · enum 허용값 |
| `mood` | string | 필수 · enum 허용값 |
| `journey` | string | 필수 · enum 허용값 |
| `worldId` | string | 필수 · enum 허용값 |
| `capturedAt` | number | 필수 · 양수(epoch millis) |

요청 예시(`metadata` 파트):

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

성공 응답(`201 Created`, `SessionResponse`):

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

이미 존재하는 `sessionId` 로 다시 저장 요청이 오면 새로 만들지 않고 기존 세션을 그대로
반환합니다(만료됐으면 `410`). 같은 촬영이 재전송돼도 중복 저장·이미지 재작성이 일어나지
않도록 하려는 의도입니다.

## 저장 방식과 설계 근거

| 대상 | 현재 방식 | 위치 |
| --- | --- | --- |
| 세션 메타데이터 | H2 파일 DB | `./data/sjf` (`DB_CLOSE_ON_EXIT=FALSE`) |
| 촬영 이미지 | 로컬 파일 | `./uploads/{sessionId}.jpg` |
| 세션 수명 | 저장 후 24시간(`SESSION_TTL_HOURS`, 기본 24) | `expiresAt` 컬럼 |

이미지를 DB에 넣지 않고 파일로 두는 이유는, 조회·다운로드 요청이 곧 정적 파일 전송이라
DB를 거칠 이유가 없고, 파일명을 `{sessionId}.jpg` 로 고정하면 세션과 파일이 1:1로 묶여
경로 추정·정리가 단순해지기 때문입니다. 저장 경로는 `storageRoot` 밖으로 벗어나지 못하도록
정규화 검사(`resolveSafePath`)를 거치고, JPEG 은 확장자·Content-Type 뿐 아니라 파일 앞
2바이트 매직넘버(`FF D8`)까지 확인합니다.

**왜 지금은 이렇게** — 해커톤 기간의 단일 서버·짧은 세션 수명(24시간)에서는 H2 파일 + 로컬
디스크가 가장 적은 설정으로 동작합니다. 외부 DB·스토리지 계정이나 마이그레이션 없이 서버
한 대에서 곧바로 뜹니다. 세션도 부스 체험이라는 성격상 하루 지나면 의미가 없어 만료로 정리하면
충분합니다.

**운영 전환 시** — DB는 이미 PostgreSQL 드라이버가 들어 있어 `DB_URL`·계정만 바꾸면 넘어가고
(JPA `ddl-auto: update`), 이미지는 로컬 디스크 대신 오브젝트 스토리지(S3 등)로 옮기면서
`FileStorageService` 의 저장·로드 지점을 교체하면 됩니다. 세션 만료는 지금은 조회 시점에
`expiresAt` 을 비교해 판정할 뿐 실제 레코드·파일 삭제는 하지 않으므로, 운영에서는 만료분을
정리하는 스케줄러가 추가로 필요합니다.

## 에러 처리

모든 오류는 `@RestControllerAdvice` 에서 아래 형식으로 통일됩니다. 프론트는 이 상태 코드로
화면 문구를 분기합니다.

```json
{
  "status": 410,
  "error": "Gone",
  "message": "만료된 체험 결과입니다.",
  "timestamp": "2026-08-16T20:05:19.183235600Z"
}
```

| 상태 코드 | 의미 | 발생 지점 |
| --- | --- | --- |
| `400 Bad Request` | 필수값 누락·잘못된 enum·동의 미체크·metadata JSON 형식 오류·JPEG 아님 | 검증 실패, `PortalApiException`, `HttpMessageNotReadableException` |
| `404 Not Found` | 존재하지 않는 세션 또는 이미지 | 세션·파일 조회 실패 |
| `410 Gone` | 24시간이 지나 만료된 세션 | `expiresAt` 경과 |
| `413 Payload Too Large` | 이미지 10MB 초과 | multipart 제한 초과·용량 검사 |
| `500 Internal Server Error` | 이미지 저장 등 서버 오류 | I/O 실패 등 |

프론트는 이 코드들을 그대로 받아 "링크가 만료되었어요"(410) / "사진을 찾을 수 없어요"(404) 등으로
안내하므로, 상태 코드의 의미가 프론트-백 공통 계약입니다.

## CORS · 환경 변수

CORS 는 `WebConfig` 에서 `/api/**` 에만 적용되며, 허용 오리진은 `portal.allowed-origins`
(`ALLOWED_ORIGINS`) 값을 콤마로 분리해 구성합니다. 허용 메서드는 `GET`·`POST`·`OPTIONS`
입니다.

주요 설정 키(`application.yml` 기준, `키(환경변수:기본값)`):

| 설정 키 | 환경 변수 | 기본값 | 용도 |
| --- | --- | --- | --- |
| `spring.datasource.url` | `DB_URL` | `jdbc:h2:file:./data/sjf;DB_CLOSE_ON_EXIT=FALSE` | DB 접속 URL |
| `spring.datasource.username` | `DB_USERNAME` | `sa` | DB 사용자 |
| `spring.datasource.password` | `DB_PASSWORD` | (빈 값) | DB 비밀번호 |
| `spring.h2.console.enabled` | `H2_CONSOLE_ENABLED` | `true` | H2 콘솔(`/h2-console`) |
| `server.port` | `PORT` | `8080` | 서버 포트 |
| `portal.storage-path` | `STORAGE_PATH` | `./uploads` | 이미지 저장 경로 |
| `portal.session-ttl-hours` | `SESSION_TTL_HOURS` | `24` | 세션 만료 시간(시간) |
| `portal.frontend-base-url` | `FRONTEND_BASE_URL` | `http://localhost:3000` | `shareUrl` host |
| `portal.public-api-base-url` | `PUBLIC_API_BASE_URL` | `http://localhost:8080` | `imageUrl`·`downloadUrl` host |
| `portal.allowed-origins` | `ALLOWED_ORIGINS` | `http://localhost:3000` | CORS 허용 오리진(콤마 구분) |

multipart 업로드 상한은 파일 10MB · 요청 11MB 로 설정돼 있습니다.

## 배포

가비아 클라우드 서버 1대에 Docker Compose 로 백엔드·프론트·nginx 를 함께 올립니다. 배포
스크립트는 이 레포의 [`deploy/`](deploy/) 에 있습니다.

| 파일 | 역할 |
| --- | --- |
| `deploy/setup-server.sh` | 서버 최초 배포(HTTP) — Docker 설치·레포 clone·빌드·실행 |
| `deploy/https-deploy.sh` | DuckDNS + Let's Encrypt HTTPS 배포 |

nginx 는 `/` → 프론트(3000), `/api/` → 백엔드(8080)로 프록시하며, 같은 출처가 되므로 배포
환경에서는 CORS 문제가 발생하지 않습니다.

**HTTPS 가 필수인 이유** — 프론트의 카메라 촬영(`getUserMedia`)은 보안 컨텍스트(HTTPS 또는
localhost)에서만 동작합니다. HTTP·IP 로 접속하면 브라우저가 카메라를 차단하므로, 부스에서
쓰려면 HTTPS 배포까지 마쳐야 합니다.

## 실행

```bash
# Mac / Linux
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

기본 포트는 `8080` 입니다. 상태 확인:

```
GET http://localhost:8080/api/health   →   MCM PORTAL SERVER OK
```

로컬에서 프론트(3000)·백(8080)을 함께 띄우면 기본 환경 변수 그대로 연동됩니다. 단, 휴대폰에서
QR 을 스캔하려면 `shareUrl` 의 host(`FRONTEND_BASE_URL`)를 실제 접근 가능한 주소로 바꿔야
합니다(휴대폰의 `localhost` 는 자기 자신을 가리킴).

## 알려진 한계 · TODO

- 만료 세션·이미지의 실제 삭제 로직이 없습니다. 조회 시점에 `expiresAt` 으로 판정만 하므로,
  운영에서는 만료분을 정리하는 스케줄러가 필요합니다.
- 인증이 없습니다(MVP). `sessionId` 를 알면 누구나 이미지를 조회할 수 있으므로, 공개 서비스로
  전환한다면 접근 제어가 필요합니다.
- 이미지가 로컬 디스크에 저장돼 서버가 교체되면 함께 사라집니다(운영 전환 시 오브젝트
  스토리지로 이관).
- `/api/products` · `/api/worlds/recommend` 응답이 코드 하드코딩 예시값이며, 현재 프론트는
  이 엔드포인트를 사용하지 않습니다.
