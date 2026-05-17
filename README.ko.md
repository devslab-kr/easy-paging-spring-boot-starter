# easy-paging-spring-boot-starter

[English](README.md) · **한국어**

> Spring Boot + MyBatis를 위한 어노테이션 기반 페이지네이션 스타터.
> Offset 방식과 Keyset/Cursor 방식을 하나로 제공합니다.

[![Maven Central](https://img.shields.io/maven-central/v/kr.devslab/easy-paging-spring-boot-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/kr.devslab/easy-paging-spring-boot-starter)
[![CI](https://github.com/devslab-kr/easy-paging-spring-boot-starter/actions/workflows/ci.yml/badge.svg)](https://github.com/devslab-kr/easy-paging-spring-boot-starter/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/devslab-kr/easy-paging-spring-boot-starter/branch/main/graph/badge.svg)](https://codecov.io/gh/devslab-kr/easy-paging-spring-boot-starter)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

📖 **[문서 보기 → easy-paging.devslab.kr](https://easy-paging.devslab.kr/ko/)**

## 한눈에 보기

컨트롤러 메서드에 어노테이션 하나만 붙이면 JSON 응답이 바로 나옵니다. Spring MVC 구조에서 컨트롤러는 서비스에 위임만 하고, 실제 로직은 서비스가 담당합니다:

```java
// Controller
@RestController
class ReportController {

    private final ReportService reports;

    ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/reports")
    @AutoPaginate(maxSize = 50)
    public PageResponse<Report> list(Pageable pageable) {
        return PageResponse.from(reports.findAll(), pageable);
    }
}

// Service
@Service
class ReportService {

    private final ReportMapper mapper;

    ReportService(ReportMapper mapper) {
        this.mapper = mapper;
    }

    public List<Report> findAll() {
        return mapper.findAll();   // 페이지네이션은 컨트롤러 레벨의 aspect가 주입함
    }
}
```

(임포트는 생략 — 다음 섹션에서 import 포함한 전체 파일과 MyBatis 매퍼까지 함께 보여줍니다.)

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

### 라이브러리 없이 직접 작성하면

같은 엔드포인트를 PageHelper만으로 직접 구현하면 대략 이런 모양이 됩니다:

```java
@GetMapping("/reports")
public Map<String, Object> list(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(required = false) String sort
) {
    // 1. 페이지 크기 검증 — 안 하면 ?size=999999로 DoS 공격당함
    if (size <= 0 || size > 100) size = 20;

    // 2. ?sort 파라미터 파싱 + SQL 인젝션 차단
    //    (?sort=name;DROP TABLE users 같은 입력을 그냥 두면 DB까지 도달)
    String orderBy = parseAndValidateSort(sort);   // 별도 어딘가 ~30줄짜리 메서드

    // 3. PageHelper의 스레드별 스택에 페이지 정보 push
    PageHelper.startPage(page + 1, size);          // 0-인덱스 아니라 1-인덱스
    if (!orderBy.isEmpty()) {
        PageHelper.orderBy(orderBy);
    }

    try {
        // 4. 쿼리 실행 — PageHelper가 SQL을 가로채서 LIMIT/OFFSET 주입
        PageInfo<Report> info = new PageInfo<>(reportMapper.findAll());

        // 5. 응답 JSON을 직접 조립
        return Map.of(
            "content",       info.getList(),
            "page",          page,
            "size",          size,
            "totalElements", info.getTotal(),
            "totalPages",    info.getPages(),
            "first",         info.isIsFirstPage(),
            "last",          info.isIsLastPage()
        );
    } finally {
        // 6. 매우 중요: 스레드별 상태를 정리. 빠뜨리면 같은 스레드(또는
        //    Virtual Thread carrier)에서 처리되는 다음 요청이 이전의
        //    페이지네이션 설정을 그대로 물려받아, 페이지네이션이 필요 없는
        //    쿼리까지 잘못 페이지네이션됨.
        PageHelper.clearPage();
    }
}
```

이 스타터는 위 6단계를 섹션 첫머리의 4줄짜리 컨트롤러로 압축합니다. 1·2·5·6번이 통째로 사라지고, 3번은 어노테이션 한 줄로 대체되며, 4번은 평범한 매퍼 호출 그대로 유지됩니다.

## 무엇을 제공하는가

- **Spring Data 호환 JSON** 응답을 기본 제공 (회사 표준 래퍼가 따로 있다면 [응답 형식 커스터마이징](#커스텀-응답-형식) 가능)
- **0-based 페이지 번호** — Spring Data 컨벤션 (`?page=0`이 첫 페이지). PageHelper의 1-based 인덱싱은 내부에서 자동 변환됨
- **안전한 `?sort=…`** — sort 파라미터는 DB에 도달하기 전 검증되어 인젝션 시도를 HTTP 400으로 거부
- **페이지 크기 클램핑** — 엔드포인트별 + 전역 상한 이중 적용. 클라이언트가 `?size=999999`로 요청해도 막힘
- **합리적인 기본값** — 기본 페이지 크기, 최대 크기, 범위 밖 페이지 처리 모두 설정 가능
- **Keyset(커서) 페이지네이션** — 시계열이나 무한 스트림 테이블처럼 `OFFSET`과 `COUNT(*)`가 부담스러운 경우 ([자세히](#keyset--cursor-페이지네이션--keysetpaginate))
- **WebFlux/Reactor 지원** — `Schedulers.boundedElastic()` 위에서 블로킹 MyBatis 호출 ([자세히](#reactive-webflux-지원))
- **Virtual Threads 안전** — 매 요청 종료 시 내부 상태가 자동 정리됨

## 설치

```kotlin
// build.gradle.kts
dependencies {
    implementation("kr.devslab:easy-paging-spring-boot-starter:0.2.0")
}
```

여러분이 추가:
- Spring Boot 3.3+ / Java 21+
- `mybatis-spring-boot-starter` (3.x 모두 지원) — `DataSource`, `SqlSessionFactory`, `@MapperScan` 자동 설정
- JDBC 드라이버

스타터가 자동으로 가져옴: `spring-boot-starter-aop`, `spring-data-commons`, `pagehelper-spring-boot-starter`. **Spring Data JPA는 필요 없습니다** — 가벼운 `spring-data-commons` (`Pageable`, `Page`, `Sort` 제공)만 transitively 따라옵니다.

> `mybatis-spring-boot-starter`를 왜 transitive로 안 가져오는지? 거의 모든 MyBatis 프로젝트가 이미 명시적으로 선언하고 있고, 우리 스타터에서 특정 버전을 강제하면 충돌이 발생합니다. `spring-boot-starter-web` / `webflux`나 JDBC 드라이버를 우리가 가져오지 않는 것과 같은 이유 — 본인 프로젝트가 이미 쓰고 있는 걸 그대로 활용하라는 의도입니다.

## Offset 페이지네이션 — `@AutoPaginate`

가장 일반적인 페이지네이션. 총 개수가 필요하고 데이터가 `LIMIT/OFFSET`으로 무난히 처리되는 리스트 화면에 적합합니다.

전체 레이어드 구현:

```java
// src/main/java/com/example/report/ReportController.java
package com.example.report;

import kr.devslab.easypaging.annotation.AutoPaginate;
import kr.devslab.easypaging.core.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
class ReportController {

    private final ReportService reports;

    ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping
    @AutoPaginate(maxSize = 50)
    public PageResponse<Report> list(Pageable pageable) {
        // 메서드 본문이 실행되기 전에 aspect가 PageHelper.startPage(...)를 호출하므로,
        // reports.findAll() 안에서 일어나는 매퍼 호출이 자동으로 페이지네이션됩니다.
        return PageResponse.from(reports.findAll(), pageable);
    }
}
```

```java
// src/main/java/com/example/report/ReportService.java
package com.example.report;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
class ReportService {

    private final ReportMapper mapper;

    ReportService(ReportMapper mapper) {
        this.mapper = mapper;
    }

    public List<Report> findAll() {
        // 실제 서비스에서는 권한 검증, 테넌트 필터링, 도메인 규칙 등을 처리한 다음
        // 마지막에 매퍼를 호출합니다.
        return mapper.findAll();
    }
}
```

MyBatis 매퍼는 페이지네이션 로직 없는 평범한 `List` 쿼리로 둡니다 — `LIMIT/OFFSET` 직접 쓸 필요 없습니다. 국내 엔터프라이즈에서 표준인 XML 매핑 방식 기준으로, 인터페이스는 메서드 시그니처만:

```java
// src/main/java/com/example/report/ReportMapper.java
package com.example.report;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportMapper {
    List<Report> findAll();   // 런타임에 aspect가 페이지네이션을 주입
}
```

```xml
<!-- src/main/resources/mapper/ReportMapper.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.report.ReportMapper">

    <select id="findAll" resultType="com.example.report.Report">
        SELECT id, title, created_at AS createdAt
        FROM reports
    </select>

</mapper>
```

`application.yml`에서 XML 위치 지정:

```yaml
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
```

`@AutoPaginate` aspect가 컨트롤러 호출을 가로채서 PageHelper의 스레드별 상태를 설정하면, 직후의 MyBatis 쿼리가 자동으로 `LIMIT/OFFSET`과 `ORDER BY`를 받아 실행됩니다. XML은 깔끔한 상태로 유지됩니다.

### 어노테이션 옵션

| 속성          | 기본값  | 의미                                                                          |
|---------------|---------|-------------------------------------------------------------------------------|
| `count`       | `true`  | `totalElements`/`totalPages`를 위한 `COUNT(*)` 쿼리 실행 여부. 시계열·로그 테이블에서는 비활성화 권장. |
| `maxSize`     | `100`   | 호출자가 요청 가능한 페이지 크기의 절대 상한.                                  |
| `reasonable`  | `true`  | `true`일 때, 범위 밖 페이지 번호를 자동으로 보정 (빈 결과 대신).               |

### 모든 옵션을 함께 사용

```java
// Controller
@GetMapping("/audit-events")
@AutoPaginate(
    count       = false,    // 감사 로그는 1억 행 이상 — COUNT(*) 부담이 너무 큼
    maxSize     = 200,      // 데이터 내보내기용 사용자에게는 큰 페이지 허용
    reasonable  = false     // 엄격 모드: page > totalPages이면 빈 결과 반환
)
public PageResponse<AuditEvent> events(Pageable pageable) {
    return PageResponse.from(auditEvents.findAll(), pageable);
}

// Service
@Service
class AuditEventService {
    private final AuditEventMapper mapper;
    // ... 생성자 생략

    public List<AuditEvent> findAll() {
        return mapper.findAll();
    }
}
```

### 반환 타입 선택

| 선언된 반환 타입          | 동작                                                                              |
|---------------------------|-----------------------------------------------------------------------------------|
| `PageResponse<T>`         | 페이지네이션 메타데이터를 포함한 응답 래퍼. **REST 엔드포인트에 권장.**            |
| `Object`                  | 응답 래퍼 — [커스텀 응답 형식](#커스텀-응답-형식)을 등록했다면 그쪽으로 라우팅됨. 기본 응답 형식을 바꿔 쓰는 경우 사용. |
| `List<T>`                 | 평범한 리스트 (슬라이스·정렬은 적용되지만 메타데이터·총개수 없음).                  |

### 페이지 번호

페이지 번호는 **요청·응답·`Pageable` 코드 전반에서 0-based** — Spring Data 컨벤션을 그대로 따릅니다. PageHelper는 내부적으로 1-based지만, aspect가 자동 변환해주므로 매퍼 SQL이나 나머지 코드는 항상 0-based만 마주합니다.

```
GET /reports?page=0&size=20  →  첫 페이지
GET /reports?page=1&size=20  →  두 번째 페이지
```

클라이언트에 1-based 페이지 번호를 노출하고 싶다면 (일부 팀에서 선호) `easy-paging.one-indexed-pages: true` 설정 — `?page=1`이 첫 페이지가 되고 응답의 `page` 필드도 `1`부터 시작합니다. Keyset 엔드포인트는 영향 없음.

### 정렬

Pageable이 Spring Data의 표준 정렬 문법을 자동으로 인식합니다. 다중 컬럼 정렬도 그대로 지원됩니다:

```
GET /reports?page=0&size=20&sort=createdAt,desc&sort=name,asc
```

Aspect는 이를 `ORDER BY created_at desc, name asc`로 변환해서 PageHelper에 전달합니다. 컬럼명은 `[A-Za-z_][A-Za-z0-9_.]*` 패턴으로 검증되어, 세미콜론·괄호·공백이 포함된 입력은 `IllegalArgumentException`으로 거부됩니다. 즉 `?sort=…;DROP TABLE…` 같은 공격은 DB에 도달조차 못 합니다.

NULL 값 처리 순서를 정하려면 프로그래밍 방식으로 설정하세요:

```java
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

Pageable pageable = PageRequest.of(0, 20, Sort.by(
    Sort.Order.desc("createdAt").with(Sort.NullHandling.NULLS_LAST),
    Sort.Order.asc("name")
));
// 변환 결과: ORDER BY created_at desc nulls last, name asc
```

## Keyset / Cursor 페이지네이션 — `@KeysetPaginate`

무한 스트림 데이터 — 로그, 위치 추적, 감사 이력 등 — 에서 `COUNT(*)`와 큰 `OFFSET`이 모두 부담스러운 경우에 사용합니다.

```java
// src/main/java/com/example/location/LocationController.java
package com.example.location;

import java.util.UUID;
import kr.devslab.easypaging.annotation.KeysetPaginate;
import kr.devslab.easypaging.core.KeysetPage;
import kr.devslab.easypaging.core.KeysetRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/locations")
class LocationController {

    private final LocationService locations;

    LocationController(LocationService locations) {
        this.locations = locations;
    }

    @GetMapping
    @KeysetPaginate(
        keys        = {"time", "id"},   // 복합 키 — 시간 + id 동률 처리용
        direction   = "DESC",            // 최신순
        defaultSize = 50,
        maxSize     = 200
    )
    public KeysetPage<Location> stream(KeysetRequest req, @RequestParam UUID workerId) {
        // KeysetRequest는 argument resolver가 ?cursor=…&size=…&direction=… 에서 채워줍니다
        // (기본값은 위 @KeysetPaginate 어노테이션 참조). 컨트롤러는 서비스에 위임만 합니다.
        return locations.stream(workerId, req);
    }
}
```

```java
// src/main/java/com/example/location/LocationService.java
package com.example.location;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.devslab.easypaging.core.CursorCodec;
import kr.devslab.easypaging.core.KeysetPage;
import kr.devslab.easypaging.core.KeysetRequest;
import org.springframework.stereotype.Service;

@Service
class LocationService {

    private final LocationMapper mapper;
    private final CursorCodec codec;

    LocationService(LocationMapper mapper, CursorCodec codec) {
        this.mapper = mapper;
        this.codec = codec;
    }

    public KeysetPage<Location> stream(UUID workerId, KeysetRequest req) {
        // size + 1행을 조회해서 다음 페이지 존재 여부 판단
        List<Location> rows = mapper.findAfter(
            workerId,
            req.keyAsInstant("time"),
            req.keyAsLong("id"),
            req.size() + 1);

        // keyExtractor 람다는 마지막 행의 어느 필드를 다음 커서로 인코딩할지 헬퍼에게 알려줍니다.
        return KeysetPage.build(rows, req, r -> Map.of(
            "time", r.getTime(),
            "id",   r.getId()
        ), codec);
    }
}
```

대응되는 MyBatis 매퍼는 keyset `WHERE` 절을 명시적으로 작성합니다 — 커서 값이 파라미터로 들어옵니다:

```java
// src/main/java/com/example/location/LocationMapper.java
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LocationMapper {
    List<Location> findAfter(
        @Param("workerId") UUID workerId,
        @Param("time")     Instant time,    // 첫 페이지에서는 null
        @Param("id")       Long id,         // 첫 페이지에서는 null
        @Param("limit")    int limit);
}
```

```xml
<!-- src/main/resources/mapper/LocationMapper.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.location.LocationMapper">

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

</mapper>
```

> **XML 이스케이프 주의**: MyBatis XML 안에서 `<`는 반드시 `&lt;`로 작성해야 합니다 — raw `<`를 쓰면 XML 파싱에 실패합니다. (`<select>` 본문 전체를 `<![CDATA[ ... ]]>`로 감싸는 팀도 많습니다.)

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

클라이언트는 다음 페이지를 요청할 때 `nextCursor`를 `?cursor=…`로 다시 보내면 됩니다. `OFFSET`도 `COUNT(*)`도 없습니다.

### 커서 서명 (운영 환경 필수)

운영 환경에서는 반드시 `easy-paging.keyset.cursor-secret`을 설정하세요. 시크릿이 없으면 커서는 Base64로 인코딩만 될 뿐 **인증되지 않습니다** — 악의적인 클라이언트가 위조한 커서로 봐서는 안 되는 행을 노출시킬 수 있습니다 (예: 멀티테넌트에서 다른 테넌트의 키 위조). 시크릿이 설정되면 모든 커서는 HMAC-SHA256으로 서명되고 위조된 커서는 거부됩니다.

## Reactive (WebFlux) 지원

WebFlux/Reactor 앱에서 블로킹 MyBatis를 호출해야 하는 경우. 레이어 구조는 Offset 섹션과 동일하고, 반환 타입이 `Mono<...>`로 바뀌고 서비스가 `ReactivePagingSupport`를 호출한다는 점만 다릅니다:

```java
// src/main/java/com/example/report/ReportController.java
package com.example.report;

import kr.devslab.easypaging.core.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
class ReportController {

    private final ReportService reports;

    ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/reports")
    public Mono<PageResponse<Report>> list(Pageable pageable) {
        return reports.list(pageable);
    }
}
```

```java
// src/main/java/com/example/report/ReportService.java
package com.example.report;

import kr.devslab.easypaging.core.PageResponse;
import kr.devslab.easypaging.reactive.ReactivePagingSupport;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
class ReportService {

    private final ReportMapper mapper;

    ReportService(ReportMapper mapper) {
        this.mapper = mapper;
    }

    public Mono<PageResponse<Report>> list(Pageable pageable) {
        return ReactivePagingSupport.paginate(
            pageable,
            () -> mapper.findAll(),     // 블로킹 MyBatis 호출, 워커 스레드 위로 옮겨짐
            /* maxSize */ 100,
            /* count   */ true);
    }
}
```

매퍼와 XML은 Offset 섹션과 동일합니다 — `ReactivePagingSupport`는 호출이 디스패치되는 방식만 바꿀 뿐, 쿼리 자체는 그대로입니다.

블로킹 작업은 기본적으로 `Schedulers.boundedElastic()` 위에서 실행됩니다. 별도의 DB 전용 스케줄러가 있다면 직접 전달할 수 있습니다:

```java
import reactor.core.scheduler.Scheduler;

return ReactivePagingSupport.paginate(
    pageable,
    () -> mapper.findAll(),
    /* maxSize    */ 100,
    /* count      */ true,
    /* scheduler  */ databaseScheduler);
```

## 커스텀 응답 형식

권장하는 패턴은 **본인의 응답 타입을 정의하고 정적 팩토리 메서드를 두는 것**입니다 — 기본 제공되는 `PageResponse.from()`과 똑같은 패턴이에요. Aspect는 PageHelper 처리만 책임지고, 응답 형태는 본인의 타입이 직접 소유합니다:

```java
// 회사 표준 페이지네이션 응답 — 타입 안전하고, 모든 페이지네이션 엔드포인트에서 재사용
public record CompanyPage<T>(
        boolean ok,
        List<T> data,
        PageMeta meta) {

    /** 매퍼 결과 + Pageable로부터 CompanyPage 생성. */
    public static <T> CompanyPage<T> from(List<T> list, Pageable pageable) {
        // 스타터의 메타데이터 추출 로직을 재사용한 다음, 본인 타입에 맞게 매핑.
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

컨트롤러에서 그대로 사용 — `Object` 반환 불필요, 특별한 어노테이션 불필요:

```java
@RestController
class ReportController {

    private final ReportService reports;

    ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/reports")
    @AutoPaginate(maxSize = 50)
    public CompanyPage<Report> list(Pageable pageable) {
        return CompanyPage.from(reports.findAll(), pageable);
    }
}
```

완전한 타입 안전성을 얻고, JSON 형태는 `CompanyPage`가 직렬화되는 그대로이며, aspect는 여전히 PageHelper 생명주기만 책임집니다. 스타터는 본인의 타입에 대해 아무것도 알 필요가 없습니다.

### 대안: 중앙화된 팩토리 빈

모든 컨트롤러가 `CompanyPage.from(...)`을 명시적으로 호출하지 않고 **응답 형식이 자동 적용**되기를 원한다면, `PageResponseFactory` 빈을 등록하고 컨트롤러 반환 타입을 `Object`로 선언하면 됩니다:

```java
import java.util.List;
import kr.devslab.easypaging.spi.PageResponseFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

선택 가이드:

| | 커스텀 타입 + `from()` (권장) | `Object` + 팩토리 빈 |
|---|---|---|
| 타입 안전성 | 완전 — 반환 타입이 `CompanyPage<Report>` | 없음 — 반환 타입이 `Object` |
| DRY | 컨트롤러마다 `.from(...)` 호출 | 팩토리 한 번만 정의 |
| 테스트 모킹 | 단순 — 순수 정적 메서드 | 팩토리 빈을 컨텍스트에 띄워야 함 |
| 적합한 경우 | 응답 형태가 1~2개일 때 | 모든 엔드포인트가 동일한 형태를 강제할 때 |

두 패턴은 공존 가능 — 엔드포인트별로 선택하세요. 팩토리는 **aspect가 직접 응답을 만들 때만** 동작합니다 (컨트롤러가 `List` 또는 `Object`를 반환한 경우). `PageResponse<T>`나 `CompanyPage<T>` 같은 명시적 값을 반환했다면 가공 없이 그대로 응답됩니다.

## 설정

```yaml
easy-paging:
  enabled: true                # 마스터 스위치
  default-page-size: 20        # 호출자가 ?size를 생략했을 때 사용
  max-page-size: 500           # 전역 절대 상한 (@AutoPaginate maxSize가 더 크더라도 절대 초과 못 함)
  auto-wrap-list: true         # false로 두면 PageResponse 자동 래핑을 전역 비활성화
  keyset:
    cursor-secret: ${EASY_PAGING_CURSOR_SECRET:}   # HMAC 시크릿; 비어 있으면 서명 없음 (개발용만)
    max-cursor-bytes: 2048                          # 디코딩 후 커서 페이로드 크기 상한 (DoS 방지)
```

## 라이선스

[Apache License 2.0](LICENSE). 버그 리포트와 PR 환영합니다 — [CONTRIBUTING.md](CONTRIBUTING.md)를 참조하세요.
