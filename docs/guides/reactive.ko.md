# Reactive (WebFlux + R2DBC) 지원

easy-paging은 두 가지 리액티브 시나리오를 지원합니다:

| 시나리오 | 필요한 것 | 보면 좋은 섹션 |
|---|---|---|
| WebFlux 앱에서 블로킹 MyBatis 호출 | 추가 의존성 없음 — `ReactivePagingSupport`는 core 스타터에 포함 | 아래 [MyBatis on Reactor](#mybatis-on-reactor) |
| WebFlux 앱이 **Spring Data R2DBC** 사용 (네이티브 논블로킹 SQL) | 별도 `easy-paging-spring-boot-starter-reactive` 아티팩트 추가 | 아래 [네이티브 R2DBC + WebFlux](#네이티브-r2dbc--webflux) |

두 시나리오는 함께 쓸 수 있음 — MyBatis와 R2DBC를 둘 다 쓰는 프로젝트는 양쪽 스타터를 추가하고 쿼리에 맞춰 헬퍼를 선택.

---

## MyBatis on Reactor

WebFlux/Reactor 앱에서 블로킹 MyBatis를 호출해야 하는 경우. core 스타터가 제공하는 `ReactivePagingSupport`는 `startPage → 매퍼 호출 → clearPage` 전체를 단일 `Mono.fromCallable` 안에 묶어 블로킹 IO 스케줄러에 올립니다. 덕분에 PageHelper의 `ThreadLocal`이 호출 전체에서 일관성을 유지합니다.

### 기본 사용

```java
@RestController
class ReportController {

    private final ReportService reports;

    ReportController(ReportService reports) { this.reports = reports; }

    @GetMapping("/reports")
    public Mono<PageResponse<Report>> list(Pageable pageable) {
        return reports.list(pageable);
    }
}
```

```java
@Service
class ReportService {

    private final ReportMapper mapper;

    ReportService(ReportMapper mapper) { this.mapper = mapper; }

    public Mono<PageResponse<Report>> list(Pageable pageable) {
        return ReactivePagingSupport.paginate(
            pageable,
            () -> mapper.findAll(),     // 블로킹 MyBatis 호출, 워커 스레드 위로 옮겨짐
            /* maxSize */ 100,
            /* count   */ true);
    }
}
```

매퍼와 XML은 offset 섹션과 동일 — `ReactivePagingSupport`는 호출 디스패치 방식만 바꿀 뿐, 쿼리 자체는 그대로.

### 커스텀 스케줄러

블로킹 작업은 기본 `Schedulers.boundedElastic()`. DB 전용 스케줄러를 쓰고 싶다면 전달:

```java
return ReactivePagingSupport.paginate(
    pageable,
    () -> mapper.findAll(),
    /* maxSize    */ 100,
    /* count      */ true,
    /* scheduler  */ databaseScheduler);
```

### 왜 `@AutoPaginate`를 직접 쓰지 않나요?

`@AutoPaginate`는 Spring AOP와 PageHelper의 `ThreadLocal`을 통해 동작합니다. Reactor의 스케줄러 모델 때문에 메서드를 호출하는 스레드가 매퍼를 실제 실행하는 스레드와 다를 수 있어 — PageHelper의 `ThreadLocal`이 엉뚱한 스레드에 설정될 수 있습니다.

`ReactivePagingSupport.paginate`는 pageable + 플래그를 호출 시점에 캡처하고, `boundedElastic`(또는 커스텀 스케줄러)에서 subscribe한 뒤, 그 스레드 안에서 PageHelper 설정 → 매퍼 호출 → 정리 전체를 수행해 이 문제를 해결합니다.

---

## 네이티브 R2DBC + WebFlux

**Spring Data R2DBC**를 쓰는 앱(요청 경로에 MyBatis 없음). 옵션 스타터 추가:

```kotlin title="build.gradle.kts"
dependencies {
    implementation("kr.devslab:easy-paging-spring-boot-starter:0.5.0")
    implementation("kr.devslab:easy-paging-spring-boot-starter-reactive:0.5.0")
}
```

(옵션 reactive 라인은 SB3 maintenance 라인에서도 `0.4.0`으로 게시 — 라인 선택은 [설치](../getting-started/installation.ko.md#릴리스-라인-선택) 참조.)

본인이 추가(일반적 Spring Data R2DBC 프로젝트와 동일): R2DBC 드라이버, `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`.

### Offset / limit 페이지네이션

`R2dbcOffsetPagingSupport.paginate(...)`는 페이지 행 쿼리와 총개수 쿼리 두 개의 R2DBC 호출을 단일 `Mono<PageResponse<T>>`로 묶습니다. MyBatis 쪽과 같은 응답 봉투 모양이라, 클라이언트는 백엔드와 무관하게 하나의 계약을 봅니다.

```java
@RestController
@RequestMapping("/users")
class UserController {

    private final R2dbcEntityTemplate template;

    UserController(R2dbcEntityTemplate template) { this.template = template; }

    @GetMapping
    public Mono<PageResponse<User>> list(Pageable pageable) {
        return R2dbcOffsetPagingSupport.paginate(
            template,
            User.class,
            Criteria.where("active").isTrue(),   // 추가 필터 (없으면 Criteria.empty())
            pageable);
    }
}
```

`Pageable`의 sort가 그대로 적용 — `PageRequest.of(0, 20, Sort.by("createdAt").descending())`로 정렬 제어. 같은 `Criteria`가 count 쿼리에도 적용되므로 `totalElements`가 사용자가 실제로 스크롤할 수 있는 행 수와 일치.

### Keyset (커서) 페이지네이션

`R2dbcKeysetSupport.paginate(...)`는 MyBatis 쪽 `KeysetPage.build`의 R2DBC 버전. 키 컬럼 메타데이터 + `KeysetRequest`를 넘기면 사전순(lexicographic) `WHERE`, 적절한 `ORDER BY`(역방향 스캔이면 뒤집힌), `size + 1` 쿼리, 그리고 `KeysetPage` 조립까지 헬퍼가 처리합니다.

```java
import kr.devslab.easypaging.r2dbc.R2dbcKeysetSupport;
import kr.devslab.easypaging.r2dbc.R2dbcKeysetSupport.KeyColumn;
import kr.devslab.easypaging.r2dbc.R2dbcKeysetSupport.SortDirection;

@RestController
@RequestMapping("/events")
class EventController {

    // 복합 키: 타임스탬프(비-고유) + id 타이브레이커
    private static final List<KeyColumn> KEYS = List.of(
        new KeyColumn("created_at", "createdAt", Instant.class, SortDirection.DESC),
        new KeyColumn("id",         "id",        Long.class,    SortDirection.DESC));

    private final R2dbcEntityTemplate template;
    private final CursorCodec codec;

    EventController(R2dbcEntityTemplate template, CursorCodec codec) {
        this.template = template;
        this.codec = codec;
    }

    @GetMapping
    @KeysetPaginate(keys = {"createdAt", "id"}, direction = "DESC", defaultSize = 50)
    public Mono<KeysetPage<Event>> stream(KeysetRequest request) {
        return R2dbcKeysetSupport.paginate(
            template,
            Event.class,
            Criteria.empty(),
            KEYS,
            request,
            e -> Map.of("createdAt", e.getCreatedAt(), "id", e.getId()),
            codec);
    }
}
```

`KeyColumn` 파라미터: `column`(DB 컬럼명), `cursorKey`(커서 JSON에서 쓰는 이름), `javaType`(JSON에서 String/Number로 복원된 커서 값을 다시 올바른 타입으로 강제 변환 — `Instant`, `Long`, `UUID` 등), `naturalDirection`(컬럼의 의도된 정렬 — `WHERE`와 `ORDER BY` 유도에 사용).

커서 토큰 자체에 스캔 방향이 인코딩되므로 클라이언트는 `?cursor=nextCursor` / `?cursor=prevCursor`만으로 충분 — 별도 `?direction=` 파라미터 불필요.

### 내장 타입 지원

`R2dbcKeysetSupport`가 자동으로 강제 변환하는 타입: `String`, primitive wrapper들 (`Long`, `Integer`, `Double` 등), `Instant`, `LocalDateTime`, `OffsetDateTime`, `LocalDate`, `UUID`. 그 외 타입은 헬퍼를 우회해서 `Criteria`를 직접 작성.

### WebFlux 인자 리졸버

`KeysetRequest` 파라미터는 WebFlux 컨트롤러에서 자동 해결 — servlet 측과 동일한 쿼리 파라미터(`?cursor=...&size=...&direction=...`). 별도 설정 없이 스타터의 auto-config가 WebFlux 클래스패스 발견 시 `ReactiveKeysetRequestArgumentResolver`를 등록.

### 커서 서명 (프로덕션)

servlet 측과 동일 — 프로덕션에서는 `easy-paging.keyset.cursor-secret` 설정. 시크릿 없으면 Base64만 적용되어 악의적 클라이언트가 커서 위조 가능. 자세한 내용은 [Keyset 가이드](keyset.md#커서-서명-프로덕션) 참조.

---

## 함께 보기

- [Offset 페이지네이션](offset.md) — 동기 `@AutoPaginate`
- [Keyset 페이지네이션](keyset.md) — 동기 `@KeysetPaginate` + 컨슈머 측 `findBefore` 패턴 (`R2dbcKeysetSupport`가 내부적으로 하는 일과 동일)
- [커스텀 응답 형식](custom-response.md) — `PageResponse` / `KeysetPage`를 본인 봉투로 래핑
