# Offset 페이지네이션 — `@AutoPaginate`

기본 전략. 총 개수가 필요하고 데이터가 `LIMIT/OFFSET`으로 무난히 처리되는 일반적인 리스트 화면에 적합합니다.

## 기본 사용

```java
@RestController
@RequestMapping("/reports")
class ReportController {

    private final ReportService reports;

    ReportController(ReportService reports) { this.reports = reports; }

    @GetMapping
    @AutoPaginate(maxSize = 50)
    public PageResponse<Report> list(Pageable pageable) {
        return PageResponse.from(reports.findAll(), pageable);
    }
}
```

Aspect는 메서드 인자에서 `Pageable`을 찾아내고, 다음 쿼리를 위해 PageHelper를 설정하고, `?sort=` 파라미터를 검증하고, `?size=`를 클램핑한 다음 종료 시 정리합니다. 완전한 서비스/매퍼 코드는 [튜토리얼](../getting-started/tutorial.md) 참조.

## 어노테이션 옵션

| 속성          | 기본값  | 의미                                                                          |
|---------------|---------|-------------------------------------------------------------------------------|
| `count`       | `true`  | `totalElements`/`totalPages`를 위한 `COUNT(*)` 쿼리 실행 여부. 시계열·로그 테이블에서는 비활성화 권장. |
| `maxSize`     | `100`   | 호출자가 요청 가능한 페이지 크기의 절대 상한.                                  |
| `reasonable`  | `true`  | `true`일 때, 범위 밖 페이지 번호를 자동으로 보정 (빈 결과 대신).               |

### 모든 옵션을 함께 사용

```java
@GetMapping("/audit-events")
@AutoPaginate(
    count       = false,   // 감사 로그는 1억 행 이상 — COUNT(*) 부담이 너무 큼
    maxSize     = 200,     // 데이터 내보내기용 사용자에게는 큰 페이지 허용
    reasonable  = false    // 엄격 모드: page > totalPages면 빈 결과 반환
)
public PageResponse<AuditEvent> events(Pageable pageable) {
    return PageResponse.from(auditEvents.findAll(), pageable);
}
```

## 반환 타입 선택

Aspect는 선언된 반환 타입을 보고 동작을 조정합니다:

| 선언된 반환 타입          | 동작                                                                              |
|---------------------------|-----------------------------------------------------------------------------------|
| `PageResponse<T>`         | 페이지네이션 메타데이터를 포함한 응답 래퍼. **REST 엔드포인트에 권장.**            |
| `Object`                  | 응답 래퍼 — [커스텀 팩토리](custom-response.md) 등록되어 있으면 그쪽으로 라우팅됨. |
| `List<T>`                 | 평범한 리스트 (슬라이스·정렬은 적용되지만 메타데이터·총개수 없음).                  |

!!! note "왜 항상 래핑하지 않나요?"
    서비스 메서드를 REST 엔드포인트와 내부 호출자가 둘 다 사용한다면, 내부 호출자는 JSON 봉투가 필요 없을 수 있습니다. `List<T>` 반환은 양쪽에서 사용 가능하고, `PageResponse<T>` 반환은 HTTP 의도를 명시합니다.

## 내부 동작

1. **`Pageable` 해결** — 메서드 인자에서 추출 (Spring MVC가 `?page=`, `?size=`, `?sort=` 파라미터로 자동 채움)
2. **페이지 크기 클램핑** — 어노테이션의 `maxSize`와 전역 설정 `easy-paging.max-page-size` 중 작은 값
3. **`Sort` 변환** — SQL-safe `ORDER BY`로 변환 ([정렬](sorting.md) 참조)
4. **`PageHelper.startPage(...)` 호출** — 다음 MyBatis 쿼리가 페이지네이션됨
5. **메서드 본문 실행** — 매퍼 호출이 `Page<T>` 반환 (이건 `List<T>`이기도 함)
6. **반환 타입에 따라 래핑** (위 표 참조)
7. **`finally`에서 PageHelper 상태 정리** — 어떤 예외가 발생해도 항상

## 흔한 패턴

### 필터링 후 페이지네이션

서비스는 권한 검증, 테넌트 필터링, 도메인 규칙을 처리하는 자연스러운 위치입니다. 매퍼는 순수 쿼리로 유지:

```java
@Service
class ReportService {
    public List<Report> findAll() {
        var tenantId = currentTenant();      // 본인의 테넌트 리졸버
        return mapper.findByTenant(tenantId);
    }
}
```

Aspect의 PageHelper 설정은 **다음에 실행되는** 매퍼 호출에 적용됩니다 — 그래서 서비스 내 필터링 로직도 자동으로 페이지네이션됩니다.

### 여러 매퍼 결과 조합

PageHelper의 `startPage`는 단일 쿼리에 적용됩니다. 서비스가 여러 매퍼 결과를 조합한다면, 페이지를 이끄는 하나만 페이지네이션됩니다:

```java
@AutoPaginate
public PageResponse<EnrichedReport> list(Pageable pageable) {
    List<Report> page = reportMapper.findAll();           // ← 페이지네이션됨
    Map<Long, Author> authors = authorMapper.findAll();   // ← 전체 조회됨
    List<EnrichedReport> enriched = page.stream()
        .map(r -> new EnrichedReport(r, authors.get(r.authorId())))
        .toList();
    return PageResponse.from(enriched, pageable);
}
```

잠깐 — `PageResponse.from(enriched, pageable)`는 PageHelper 메타데이터를 갖지 못합니다. `enriched`가 새 `ArrayList`이지 `Page`가 아니기 때문. 다음 패턴을 사용하세요:

```java
List<Report> page = reportMapper.findAll();
PageResponse<Report> base = PageResponse.from(page, pageable);
List<EnrichedReport> enriched = base.content().stream()
    .map(...).toList();
return new PageResponse<>(enriched, base.page(), base.size(),
    base.totalElements(), base.totalPages(),
    base.first(), base.last(), enriched.isEmpty());
```

## 함께 보기

- [정렬 & 페이지 번호](sorting.md) — `?sort=` 문법, SQL 인젝션 방어
- [커스텀 응답 형식](custom-response.md) — 기본 봉투 대체
- [설정 레퍼런스](../reference/configuration.md) — 전역 옵션
