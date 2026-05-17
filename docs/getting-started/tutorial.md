# Tutorial — 5-minute walkthrough

This walks through a complete paginated endpoint from controller → service → MyBatis mapper. By the end you'll have a working `GET /reports?page=0&size=20&sort=createdAt,desc` returning a JSON response shaped like Spring Data `Page`.

## Project layout

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

## 1. Controller

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

The `@AutoPaginate` annotation does the work:

- Reads `Pageable` from the request (Spring MVC's `PageableHandlerMethodArgumentResolver` populates it from `?page=`, `?size=`, `?sort=`)
- Sets up PageHelper so the next MyBatis query gets `LIMIT/OFFSET`
- Validates the sort parameter against SQL injection (rejects with HTTP 400)
- Wraps the mapper's `List` into a `PageResponse` envelope
- Cleans up PageHelper's internal state — even if the mapper throws

## 2. Service

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
        // In real code: tenant filtering, authorization, domain rules here.
        return mapper.findAll();
    }
}
```

The aspect's PageHelper setup happens *before* this method runs, so `mapper.findAll()` is automatically paginated.

## 3. Mapper (interface + XML)

The interface is just a method signature:

```java title="ReportMapper.java"
package com.example.report;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportMapper {
    List<Report> findAll();
}
```

The XML carries the SQL — no `LIMIT/OFFSET` needed, the aspect injects them at runtime:

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

## 4. Configuration

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
  helper-dialect: postgresql      # or mysql, oracle, etc.

easy-paging:
  default-page-size: 20
  max-page-size: 500
```

## 5. Try it

```bash
curl 'http://localhost:8080/reports?page=0&size=5&sort=createdAt,desc'
```

```json
{
  "content": [
    { "id": 137, "title": "Latest", "createdAt": "2026-05-15T..." },
    /* 4 more */
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

## What just happened

1. The controller declared `Pageable pageable`. Spring MVC auto-bound it from query parameters.
2. `@AutoPaginate` ran *before* the method body — it set up PageHelper for the next query and validated the sort parameter.
3. The method body called `reports.findAll()` → `mapper.findAll()`. PageHelper intercepted the SQL and added `LIMIT 5 OFFSET 0 ORDER BY created_at desc`.
4. `PageResponse.from()` extracted the pagination metadata from PageHelper's `Page` (which is the runtime type of `List<Report>`) and packaged it into a clean JSON envelope.
5. The aspect's `finally` cleared PageHelper's `ThreadLocal` — preventing state from leaking to the next request on this thread.

## Next steps

- [Offset pagination guide](../guides/offset.md) — `@AutoPaginate` options and return-type choices
- [Keyset pagination](../guides/keyset.md) — for unbounded time-series tables
- [Custom response format](../guides/custom-response.md) — match your company's API conventions
- [Sorting & page numbering](../guides/sorting.md) — multi-column sort, null handling, 0-based convention
