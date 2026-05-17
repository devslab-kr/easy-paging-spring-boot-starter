# Reactive (WebFlux) 지원

WebFlux/Reactor 앱에서 블로킹 MyBatis를 호출해야 하는 경우. 스타터가 제공하는 `ReactivePagingSupport`는 `startPage → 매퍼 호출 → clearPage` 전체를 단일 `Mono.fromCallable` 블록 안에 묶어 블로킹 IO 스케줄러에 올립니다. 덕분에 PageHelper의 `ThreadLocal`이 호출 전체에서 일관성을 유지합니다.

## 기본 사용

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

## 커스텀 스케줄러

블로킹 작업은 기본적으로 `Schedulers.boundedElastic()` 위에서 실행. 별도의 DB 전용 스케줄러(예: connection pool 크기와 맞춘 스레드 풀)가 있다면 직접 전달:

```java
import reactor.core.scheduler.Scheduler;

return ReactivePagingSupport.paginate(
    pageable,
    () -> mapper.findAll(),
    /* maxSize    */ 100,
    /* count      */ true,
    /* scheduler  */ databaseScheduler);
```

## 왜 `@AutoPaginate`를 직접 쓰지 않나요?

`@AutoPaginate`는 Spring AOP와 PageHelper의 `ThreadLocal`을 통해 동작합니다. Reactor의 스케줄러 모델 때문에 메서드를 호출하는 스레드가 매퍼를 실제로 실행하는 스레드와 다를 수 있고 — PageHelper의 `ThreadLocal`이 엉뚱한 스레드에 설정될 수 있음.

`ReactivePagingSupport.paginate`는 이를 해결:

1. 호출 시점에 pageable, maxSize, count 플래그를 캡처
2. `boundedElastic` (또는 커스텀 스케줄러)에서 subscribe
3. 구독된 스레드 안에서: PageHelper 설정 → 매퍼 호출 → 응답 래핑 → PageHelper 정리 — 모두 같은 스레드

이로써 상류 스케줄러 호핑과 무관하게, 이 연산이 PageHelper ThreadLocal 관점에서 원자적이 됩니다.

## 함께 보기

- [Offset 페이지네이션](offset.md) — 동기 `@AutoPaginate`
- [커스텀 응답 형식](custom-response.md) — `PageResponse`를 본인 봉투로 래핑
