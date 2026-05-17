# 정렬 & 페이지 번호

이 페이지는 첫 사용자가 헷갈리기 쉬운 두 가지 — `?sort=` 쿼리 파라미터 문법과 0-based 페이지 번호 컨벤션 — 을 다룹니다.

## 페이지 번호

페이지 번호는 **요청·응답·`Pageable` 코드 전반에서 0-based** — Spring Data 컨벤션을 그대로 따릅니다. PageHelper는 내부적으로 1-based지만 aspect가 자동 변환해주므로 매퍼 SQL이나 나머지 코드는 항상 0-based만 마주합니다.

```
GET /reports?page=0&size=20  →  첫 페이지
GET /reports?page=1&size=20  →  두 번째 페이지
```

JSON 응답에서:

```json
{
  "page": 0,        ← 첫 페이지
  "totalPages": 7,
  "first": true,
  "last": false
}
```

!!! note "클라이언트에 1-based 페이지 번호 노출"
    클라이언트에 1-based 페이지 번호를 노출하는 설정 옵션 (일부 팀에서 선호) 은 v0.2.0에 예정되어 있습니다. 그때까지는 클라이언트가 0-based를 사용해야 합니다.

## 정렬 — 기본

`Pageable`이 Spring Data 표준 정렬 문법을 인식합니다. 단일 컬럼:

```
GET /reports?sort=createdAt,desc
```

`ORDER BY created_at desc`로 변환됩니다.

## 다중 컬럼 정렬

`sort` 파라미터를 여러 번 전달:

```
GET /reports?page=0&size=20&sort=createdAt,desc&sort=name,asc
```

Aspect가 이를 `ORDER BY created_at desc, name asc`로 변환하여 PageHelper에 전달.

## NULL 처리

명시적인 null 정렬은 프로그래밍 방식으로 설정 (URL 문법으로 표현 불가):

```java
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

Pageable pageable = PageRequest.of(0, 20, Sort.by(
    Sort.Order.desc("createdAt").with(Sort.NullHandling.NULLS_LAST),
    Sort.Order.asc("name")
));
// 변환 결과: ORDER BY created_at desc nulls last, name asc
```

지정하지 않으면 DB의 기본 null 정렬을 따릅니다.

## SQL 인젝션 방어

컬럼명은 `[A-Za-z_][A-Za-z0-9_.]*` 패턴으로 검증됩니다 — 세미콜론, 괄호, 공백, 기타 특수문자는 DB에 도달 전 HTTP 400으로 거부:

```
GET /reports?sort=name;DROP%20TABLE%20users  →  400 Bad Request
GET /reports?sort=(SELECT 1)                 →  400 Bad Request
GET /reports?sort=name'OR'1'='1              →  400 Bad Request
```

이 검증은 SQL이 만들어지기 전 aspect 레이어에서 발생합니다. 잘못된 속성은 PageHelper나 DB에 절대 도달하지 않음.

!!! warning "매퍼 컬럼명은 JPA 스타일이어야 함"
    Aspect가 검증된 속성명을 그대로 `ORDER BY`에 넣기 때문에, 속성명은 `SELECT` 절의 컬럼명(또는 별칭)과 매치되어야 합니다. snake_case 컬럼을 camelCase Java 필드로 매핑하는 경우 `mybatis.configuration.map-underscore-to-camel-case: true`를 활성화하고 `?sort=`에 **camelCase** 속성명을 사용하세요.

## 정렬 제한

특정 컬럼만 정렬 허용하려면 (안정적 API 계약을 위해 권장) 컨트롤러 레이어에서 필터링:

```java
private static final Set<String> ALLOWED_SORT = Set.of("createdAt", "id", "name");

@GetMapping("/reports")
@AutoPaginate
public PageResponse<Report> list(Pageable pageable) {
    Sort filtered = Sort.by(pageable.getSort().stream()
        .filter(o -> ALLOWED_SORT.contains(o.getProperty()))
        .toList());
    Pageable safe = PageRequest.of(
        pageable.getPageNumber(),
        pageable.getPageSize(),
        filtered);
    return PageResponse.from(reports.findAll(), safe);
}
```

Aspect의 문법 검증 위에 방어를 한 겹 더 — *어떤 문자*가 허용되는지가 아니라 *어떤 컬럼*이 정렬 가능한지를 제한.

## 함께 보기

- [설정 레퍼런스](../reference/configuration.md) — 전역 정렬 기본값
- [Offset 페이지네이션](offset.md) — `Pageable`이 aspect로 흘러 들어가는 지점
