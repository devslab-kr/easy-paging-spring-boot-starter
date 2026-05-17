# Keyset / Cursor 페이지네이션 — `@KeysetPaginate`

무한 스트림 데이터 — 로그, 위치 추적, 감사 이력, 무한 스크롤 — 에서 `COUNT(*)`와 큰 `OFFSET`이 모두 부담스러운 경우.

## Offset과 Keyset 선택 기준

| 시나리오 | 권장 |
|---|---|
| 관리자 대시보드, 소·중 규모 테이블, "5페이지로 이동" UX | `@AutoPaginate` (offset) |
| 시계열 로그, IoT 이벤트, 감사 이력 | `@KeysetPaginate` |
| 무한 스크롤, 모바일 피드 | `@KeysetPaginate` |
| 1천만 행 이상이라 `COUNT(*)`가 느린 테이블 | `@KeysetPaginate` |

수십만 행까지는 offset이 잘 작동합니다. 그 이상이 되면 `OFFSET 1000000`는 매 페이지마다 처음 백만 행을 스캔하고 버리는 동작이 됨. Keyset은 마지막 본 값 기준 `WHERE` 절을 쓰므로 인덱스를 활용하고 `LIMIT` 행만큼만 읽고 멈춥니다.

## 기본 사용

```java
@RestController
@RequestMapping("/locations")
class LocationController {

    private final LocationService locations;

    LocationController(LocationService locations) { this.locations = locations; }

    @GetMapping
    @KeysetPaginate(
        keys        = {"time", "id"},   // 복합 키 — 시간 + id 동률 처리용
        direction   = "DESC",            // 최신순
        defaultSize = 50,
        maxSize     = 200
    )
    public KeysetPage<Location> stream(KeysetRequest req, @RequestParam UUID workerId) {
        return locations.stream(workerId, req);
    }
}
```

```java
@Service
class LocationService {

    private final LocationMapper mapper;
    private final CursorCodec codec;

    LocationService(LocationMapper mapper, CursorCodec codec) {
        this.mapper = mapper;
        this.codec = codec;
    }

    public KeysetPage<Location> stream(UUID workerId, KeysetRequest req) {
        // size + 1 행을 조회해서 다음 페이지 존재 여부 감지
        List<Location> rows = mapper.findAfter(
            workerId,
            req.keyAsInstant("time"),
            req.keyAsLong("id"),
            req.size() + 1);

        return KeysetPage.build(rows, req, r -> Map.of(
            "time", r.getTime(),
            "id",   r.getId()
        ), codec);
    }
}
```

매퍼는 keyset `WHERE` 절을 명시적으로 작성:

```xml
<select id="findAfter" resultType="com.example.location.Location">
    SELECT id, time, lat, lng
    FROM locations
    WHERE worker_id = #{workerId}
      AND (
          #{time} IS NULL
          OR time &lt; #{time}
          OR (time = #{time} AND id &lt; #{id})
      )
    ORDER BY time DESC, id DESC
    LIMIT #{limit}
</select>
```

`GET /locations?cursor=<토큰>&size=50` 요청에 대한 응답:

```json
{
  "content": [ /* 최대 50개의 행 */ ],
  "size": 50,
  "nextCursor": "eyJrIjp7InRpbWUiOi...",
  "prevCursor": null,
  "hasNext": true,
  "hasPrev": false
}
```

클라이언트는 다음 페이지 요청 시 `nextCursor`를 `?cursor=…`로 보냄. `OFFSET`도 `COUNT(*)`도 없음.

## 어노테이션 옵션

| 속성           | 기본값  | 의미                                                                     |
|----------------|---------|--------------------------------------------------------------------------|
| `keys`         | (필수)   | 커서 키 컬럼들, 순서대로. 보통 timestamp + ID 동률 처리용.               |
| `direction`    | `"DESC"`  | 키들의 기본 방향. 클라이언트가 `?direction=ASC`로 오버라이드 가능.       |
| `defaultSize`  | `20`     | 호출자가 `?size=`를 생략했을 때 사용.                                     |
| `maxSize`      | `100`    | 호출자가 요청 가능한 페이지 크기의 절대 상한.                              |

## 커서 서명 (운영 환경)

운영 환경에서는 반드시 `easy-paging.keyset.cursor-secret`을 설정. 시크릿 없으면 커서는 Base64 인코딩되긴 하지만 **인증되지 않음** — 악의적 클라이언트가 봐서는 안 되는 행(예: 다른 테넌트 키 위조)을 노리는 커서를 만들 수 있음. 시크릿이 있으면 모든 커서가 HMAC-SHA256으로 서명되고 위조 커서는 거부됩니다.

```yaml title="application.yml"
easy-paging:
  keyset:
    cursor-secret: ${EASY_PAGING_CURSOR_SECRET}   # 32바이트 이상 랜덤
    max-cursor-bytes: 2048
```

시크릿 생성:

```bash
openssl rand -base64 32
```

환경변수, secret manager, `application-prod.yml`(절대 git 커밋 금지) 등에 저장.

## "+1 행" 트릭

매퍼가 `size + 1` 행을 조회합니다. `KeysetPage.build` 헬퍼가 감지:

- **`size + 1`행 반환** → 다음 페이지 있음 → 여분 행 제거 → 마지막 표시 행의 키를 `nextCursor`로 인코딩
- **`<= size`행 반환** → 마지막 페이지 → `nextCursor`는 `null`, `hasNext`는 `false`

이 패턴은 "다음 페이지가 있는지" 알기 위한 두 번째 `COUNT(*)` 쿼리를 회피합니다.

## 복합 키 설명

타임스탬프처럼 unique하지 않은 컬럼으로 정렬할 때는 tiebreaker가 필요. 같은 `time`을 가진 두 행이 커서로는 모호하기 때문.

Keyset `WHERE`는 사전식 비교가 됩니다:

```sql
WHERE time < @last_time
   OR (time = @last_time AND id < @last_id)
```

첫 절은 "엄격히 더 이른 시간"을 처리. 둘째 절은 "같은 시간이지만 더 이른 id"를 처리. 합치면 페이지 간에 deterministic한 순서.

마지막 키로 `id`(또는 unique 보장된 컬럼)를 사용하세요.

## 함께 보기

- [커스텀 응답 형식](custom-response.md) — `KeysetPage`를 회사 봉투로 래핑
- [설정 레퍼런스](../reference/configuration.md) — `easy-paging.keyset.*` 옵션
