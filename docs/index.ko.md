---
title: easy-paging — Spring Boot + MyBatis 어노테이션 페이지네이션
---

# easy-paging

> **Spring Boot + MyBatis를 위한 어노테이션 기반 페이지네이션.**
> Offset (PageHelper) 방식과 Keyset/Cursor 방식을 하나의 스타터로.

[:fontawesome-solid-rocket: 시작하기](getting-started/installation.md){ .md-button .md-button--primary }
[:fontawesome-brands-github: GitHub](https://github.com/devslab-kr/easy-paging-spring-boot-starter){ .md-button }
[:fontawesome-brands-java: Maven Central](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter){ .md-button }

---

## 한눈에 보기

컨트롤러 메서드에 어노테이션 하나만 붙이면 JSON 응답이 바로 나옵니다. Spring MVC 구조에서 컨트롤러는 서비스에 위임만 하고, 실제 로직은 서비스가 담당합니다:

```java
@RestController
class ReportController {

    private final ReportService reports;

    ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/reports")
    @AutoPaginate(maxSize = 50)                          // (1)!
    public PageResponse<Report> list(Pageable pageable) {
        return PageResponse.from(reports.findAll(), pageable);
    }
}
```

1.  Aspect가 PageHelper의 스레드별 상태를 설정하고, 정렬 파라미터를 검증하며, 페이지 크기를 클램핑하고, 매퍼 호출 후 정리까지 책임집니다.

`GET /reports?page=0&size=20&sort=createdAt,desc` 요청에 대한 응답:

```json
{
  "content": [ /* 20개의 행 */ ],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7,
  "first": true,
  "last": false,
  "empty": false
}
```

코드 곳곳에 `PageHelper.startPage(...)` 흩뿌릴 일 없고, `ThreadLocal` 정리 신경 쓸 일 없고, 컨트롤러마다 보일러플레이트 반복할 일 없습니다.

## 무엇을 제공하는가

<div class="grid cards" markdown>

-   :material-shield-check: **기본적으로 안전**

    `?sort=name;DROP TABLE` 같은 입력은 DB 도달 전 HTTP 400으로 거부. 페이지 크기는 엔드포인트별 + 전역 이중 클램핑.

-   :material-tag-arrow-up: **Spring Data 호환**

    응답 JSON이 Spring Data `Page`와 동일한 형태. `Pageable`을 이미 쓰고 있는 팀에 그대로 드롭인.

-   :material-cursor-pointer: **Offset & Keyset 둘 다**

    `@AutoPaginate`로 일반 리스트, `@KeysetPaginate`로 시계열·무한 스트림 — `OFFSET`/`COUNT(*)`가 부담스러운 테이블에 적합.

-   :material-format-list-numbered: **0-based, 일관성**

    요청·응답·내부 `Pageable` 전반에서 Spring Data 컨벤션. PageHelper의 1-based는 내부에서 자동 변환.

-   :material-cog-outline: **응답 형식 교체 가능**

    `PageResponseFactory`로 회사 표준 응답 래퍼로 교체하거나, 본인 타입에 정적 `from()` 메서드를 두는 패턴 모두 지원.

-   :material-lightning-bolt: **Virtual Threads 안전**

    예외 경로 포함, 매 요청 종료 시 내부 상태 자동 정리. `ThreadLocal` 누수 없음.

</div>

## 빠른 설치

```kotlin title="build.gradle.kts"
dependencies {
    implementation("kr.devslab:easy-paging-spring-boot-starter:0.2.0")
}
```

전체 사전 요구사항과 셋업은 [설치](getting-started/installation.md), 5분 실습은 [튜토리얼](getting-started/tutorial.md)로 이동하세요.

## 다음으로 어디로

-   :material-school: **easy-paging이 처음이신가요?** → [튜토리얼](getting-started/tutorial.md)
-   :material-book-open: **특정 기능을 찾으세요?** → [가이드](guides/offset.md)
-   :material-table: **모든 설정 옵션이 궁금하세요?** → [레퍼런스](reference/configuration.md)
-   :material-history: **버전 마이그레이션은?** → [변경 이력](changelog.md)
