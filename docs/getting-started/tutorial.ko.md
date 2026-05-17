# 튜토리얼 — 5분 실습

컨트롤러 → 서비스 → MyBatis 매퍼로 이어지는 완전한 페이지네이션 엔드포인트를 만듭니다. 끝나면 `GET /reports?page=0&size=20&sort=createdAt,desc` 가 Spring Data `Page` 형식의 JSON으로 응답합니다.

## 프로젝트 구조

```
src/main/java/com/example/report/
├── ReportController.java
├── ReportService.java
├── ReportMapper.java
└── Report.java
src/main/resources/
├── application.yml
└── mapper/
    └── ReportMapper.xml
```

## 1. 컨트롤러

```java title="ReportController.java"
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
        return PageResponse.from(reports.findAll(), pageable);
    }
}
```

`@AutoPaginate` 어노테이션이 하는 일:

- 요청에서 `Pageable` 읽기 (Spring MVC의 `PageableHandlerMethodArgumentResolver`가 `?page=`, `?size=`, `?sort=` 파라미터로 자동 채움)
- PageHelper 설정 → 다음 MyBatis 쿼리가 `LIMIT/OFFSET` 받음
- 정렬 파라미터의 SQL 인젝션 검증 (실패 시 HTTP 400)
- 매퍼의 `List`를 `PageResponse` 봉투로 래핑
- PageHelper의 내부 상태 정리 — 매퍼가 예외를 던져도 항상 실행

## 2. 서비스

```java title="ReportService.java"
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
        // 실제 코드에서는: 테넌트 필터링, 권한 검증, 도메인 규칙 처리
        return mapper.findAll();
    }
}
```

Aspect의 PageHelper 설정이 이 메서드 실행 *전에* 발생하므로, `mapper.findAll()` 호출이 자동으로 페이지네이션됩니다.

## 3. 매퍼 (인터페이스 + XML)

인터페이스는 메서드 시그니처만:

```java title="ReportMapper.java"
package com.example.report;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportMapper {
    List<Report> findAll();
}
```

XML이 SQL을 담당 — `LIMIT/OFFSET` 불필요, 런타임에 aspect가 주입:

```xml title="src/main/resources/mapper/ReportMapper.xml"
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

## 4. 설정

```yaml title="application.yml"
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/reports
    username: app
    password: ${DB_PASSWORD}

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true

pagehelper:
  helper-dialect: postgresql      # mysql, oracle 등 본인 DB

easy-paging:
  default-page-size: 20
  max-page-size: 500
```

## 5. 테스트

```bash
curl 'http://localhost:8080/reports?page=0&size=5&sort=createdAt,desc'
```

```json
{
  "content": [
    { "id": 137, "title": "Latest", "createdAt": "2026-05-15T..." },
    /* 4개 더 */
  ],
  "page": 0,
  "size": 5,
  "totalElements": 137,
  "totalPages": 28,
  "first": true,
  "last": false,
  "empty": false
}
```

## 방금 무슨 일이 일어났나

1. 컨트롤러가 `Pageable pageable`을 선언 → Spring MVC가 쿼리 파라미터에서 자동 바인딩
2. `@AutoPaginate`가 메서드 본문 *전에* 실행 → PageHelper 설정 + sort 파라미터 검증
3. 메서드 본문이 `reports.findAll()` → `mapper.findAll()` 호출 → PageHelper가 SQL 가로채서 `LIMIT 5 OFFSET 0 ORDER BY created_at desc` 추가
4. `PageResponse.from()`이 PageHelper `Page` 객체(런타임 타입)에서 메타데이터 추출 → 깔끔한 JSON 봉투로 패키징
5. Aspect의 `finally`가 PageHelper `ThreadLocal` 정리 → 같은 스레드의 다음 요청에 상태 누수 없음

## 다음 단계

- [Offset 페이지네이션 가이드](../guides/offset.md) — `@AutoPaginate` 옵션과 반환 타입 선택
- [Keyset 페이지네이션](../guides/keyset.md) — 무한 스트림 시계열 테이블용
- [커스텀 응답 형식](../guides/custom-response.md) — 회사 API 규약에 맞추기
- [정렬 & 페이지 번호](../guides/sorting.md) — 다중 정렬, NULL 처리, 0-based 컨벤션
