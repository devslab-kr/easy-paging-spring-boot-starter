# 커스텀 응답 형식

기본 `PageResponse<T>` 봉투는 Spring Data `Page`와 동일한 형태로 클라이언트 호환성을 보장합니다. 팀에 다른 표준 — `{ ok, data, meta }`, `{ status, payload }`, 무엇이든 — 이 있다면 두 가지 패턴을 사용할 수 있습니다.

## 패턴 1 — 커스텀 타입 + 정적 `from()` (권장)

본인의 응답 타입을 정의하고 정적 팩토리 메서드를 두세요 — 내장된 `PageResponse.from()`과 같은 패턴. Aspect는 PageHelper 처리만 책임지고, 응답 형태는 본인 타입이 직접 소유:

```java
// 회사 표준 페이지네이션 응답 — 타입 안전하고, 모든 페이지네이션 엔드포인트에서 재사용
public record CompanyPage<T>(
        boolean ok,
        List<T> data,
        PageMeta meta) {

    public static <T> CompanyPage<T> from(List<T> list, Pageable pageable) {
        // 스타터의 메타데이터 추출 로직 재사용 후, 본인 형태에 맞게 매핑
        PageResponse<T> p = PageResponse.from(list, pageable);
        return new CompanyPage<>(
            true,
            p.content(),
            new PageMeta(p.page(), p.size(), p.totalElements(), p.totalPages())
        );
    }
}

public record PageMeta(int page, int size, long total, int pages) {}
```

컨트롤러에서 그대로 사용 — `Object` 반환 불필요, 특별한 어노테이션 변경 불필요:

```java
@RestController
class ReportController {

    private final ReportService reports;

    ReportController(ReportService reports) { this.reports = reports; }

    @GetMapping("/reports")
    @AutoPaginate(maxSize = 50)
    public CompanyPage<Report> list(Pageable pageable) {
        return CompanyPage.from(reports.findAll(), pageable);
    }
}
```

완전한 타입 안전성, JSON 형태는 `CompanyPage`가 직렬화되는 그대로, aspect는 여전히 PageHelper 생명주기만 책임. 스타터는 본인 타입에 대해 아무것도 알 필요 없음.

## 패턴 2 — 중앙화된 팩토리 빈

모든 컨트롤러가 `CompanyPage.from(...)`을 명시적으로 호출하지 않고 **응답 형식이 자동 적용**되기를 원한다면, `PageResponseFactory` 빈을 등록하고 컨트롤러 반환 타입을 `Object`로 선언:

```java
@Configuration
class PagingConfig {

    @Bean
    PageResponseFactory companyEnvelope() {
        return (content, pageable, totalElements, totalPages) ->
            new CompanyPage<>(
                true,
                content,
                new PageMeta(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    totalElements,
                    totalPages));
    }
}
```

```java
@GetMapping("/reports")
@AutoPaginate(maxSize = 50)
public Object list(Pageable pageable) {
    return reports.findAll();   // aspect가 List를 팩토리에 라우팅
}
```

## 비교

|                            | 커스텀 타입 + `from()` (권장) | `Object` + 팩토리 빈 |
|----------------------------|------------------------------|---------------------|
| 타입 안전성                | 완전 — 반환 타입이 `CompanyPage<Report>` | 없음 — 반환 타입이 `Object` |
| DRY                        | 컨트롤러마다 `.from(...)` 호출 | 팩토리 한 번만 정의 |
| 테스트 모킹                | 단순 — 순수 정적 메서드 | 팩토리 빈을 컨텍스트에 띄워야 함 |
| 적합한 경우                | 응답 형태가 1~2개일 때 | 모든 엔드포인트가 동일한 형태를 강제할 때 |

두 패턴은 공존 가능 — 엔드포인트별로 선택. 팩토리는 **aspect가 직접 응답을 만들 때만** 동작 (컨트롤러가 `List` 또는 `Object`를 반환한 경우). 본인이 직접 만든 `PageResponse<T>`나 `CompanyPage<T>`는 가공 없이 그대로 응답됩니다.

## 클래스 기반 팩토리 구현

의존성(로거, metrics 등)이 필요하거나 단위 테스트로 격리하기 쉽게 하려면, SPI를 클래스로 구현:

```java
@Component
class CompanyEnvelope implements PageResponseFactory {

    @Override
    public Object create(List<?> content, Pageable pageable, long totalElements, int totalPages) {
        return new CompanyPage<>(
            true,
            content,
            new PageMeta(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                totalElements,
                totalPages));
    }
}
```

람다 형식과 기능적으로 동일 — 테스트/와이어링 선호도에 따라 선택.

## 함께 보기

- [Offset 페이지네이션](offset.md) — `@AutoPaginate` 반환 타입 결정
- [설정 레퍼런스](../reference/configuration.md) — `easy-paging.auto-wrap-list`로 전역 래핑 비활성화
