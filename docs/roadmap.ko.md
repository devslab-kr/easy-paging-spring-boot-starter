# 로드맵

easy-paging의 향후 계획을 정리한 살아있는 문서입니다. 항목은 *느슨한* 약속 — 기대치는 잡되, 사용자 피드백에 따라 유연하게 조정됩니다. 항목을 위/아래로 옮기고 싶다면 이슈를 열어주세요.

정식 버전은 [`ROADMAP.md`](https://github.com/devslab-kr/easy-paging-spring-boot-starter/blob/main/ROADMAP.md)에 있습니다. 출시 완료된 기능은 [변경 이력](changelog.md)을 참고하세요.

---

## 다음 마이너 — v0.3.0

기존 표면에 깔끔하게 들어가는 작은 기능들.

### Keyset 역방향

현재 비활성화된 `KeysetPage.prevCursor` 필드 활성화. 매퍼 쪽에 거울 형태의 `findBefore` 쿼리가 필요하고, 코덱이 커서 토큰에 스캔 방향을 기록해야 합니다 (이미 `Cursor.Direction`에 와이어링되어 있고 노출만 안 한 상태).

**사용 사례**: 진정한 양방향 무한 스크롤 — "위로 더 불러오기" + "아래로 더 불러오기". 모바일 피드에 거의 필수.

### Spring Boot 베이스라인 업그레이드

Spring Boot BOM을 3.3.5 → 릴리즈 시점의 LTS (3.5 / 3.7) 로 bump. 의존성 그래프를 최신으로 유지하고 deprecation 이슈를 조기에 잡아냄.

소비자 레벨에서는 하위 호환 — Spring Boot 3.3+를 쓰는 사용자는 그대로 동작, 우리 내부 베이스라인만 이동.

---

## 중기 — v0.4.x

자체 마이너 버전을 받을 만한 큰 기능.

### 네이티브 WebFlux + R2DBC 지원

지금의 WebFlux 지원은 `ReactivePagingSupport.paginate(...)` — 블로킹 MyBatis를 `Schedulers.boundedElastic()`에 올리는 정적 헬퍼. MyBatis 사용자에겐 충분하지만, 실제 리액티브 DB까지는 못 닿음.

v0.4가 추가할 1급 지원:

- Spring Data R2DBC 리포지토리가 반환하는 `Mono<Page<T>>` / `Flux<T>`
- WebFlux 용 `KeysetRequest` 인자 리졸버
- `@AutoPaginate`의 인식되는 반환 타입에 `Mono<PageResponse<T>>` 추가

코어 JAR을 작게 유지하기 위해 별도 옵션 스타터 (`easy-paging-spring-boot-starter-reactive`) 로 배포할 가능성 높음.

---

## v1.0 — API 동결

v0.x가 안정되고 실제 사용자 피드백이 쌓이면. v1.0은 SemVer 하에 API 안정성을 약속 — 깨는 변경은 메이저 bump에서만.

### 멀티테넌트 컨텍스트 인지

Spring Security / 테넌트 리졸버와 연동하여 테넌트별 기본값 적용:

```yaml
easy-paging:
  tenants:
    tenant-a:
      max-page-size: 100
    tenant-b:
      max-page-size: 1000
```

- 테넌트별 커서 시크릿 로테이션
- 테넌트별 rate limit 훅 (강제는 아니고 카운터)

**직접적 동기**: row-level 멀티테넌시를 쓰는 SaaS가 고객 플랜별로 export 크기를 제한하고 싶을 때 (전역이 아니라).

### 다른 페이지네이션 엔진 어댑터

현재 엔진은 PageHelper로 하드와이어. v1.0은 엔진 경계를 추상화하여 같은 `@AutoPaginate` 어노테이션으로 다음을 구동:

| 엔진 | 대상 |
|---|---|
| **JPA Specification** | 이미 Spring Data JPA를 쓰는 프로젝트 |
| **jOOQ** | 타입-안전 SQL 선호자 |
| **순수 JDBC** + `NamedParameterJdbcTemplate` | 최소 의존성 셋업 |

`@AutoPaginate` 어노테이션은 그대로 유지; 소비자가 `easy-paging.engine: pagehelper | jpa | jooq | jdbc` 로 엔진 선택.

---

## 검토 중 — 미확정

레이더에 올라가 있는 것들. 강한 의견이 있거나, 이 중 하나가 본인의 채택을 막고 있다면 이슈 열어주세요.

| 아이디어 | 매력 | 망설임 |
|---|---|---|
| 커서 만료 / TTL | 도난 커서 피해 제한 | 복잡도 증가, 대부분 팀은 체감 못 함 |
| 요청별 커서 시크릿 로테이션 | 고보안 도메인 위한 방어 1겹 더 | 니치; 현재 HMAC도 충분히 강력 |
| GraphQL Relay 스타일 connection | 널리 쓰이는 spec 정렬 | 소비자에 GraphQL 스택 필요 |
| 스트리밍 export (chunked `Transfer-Encoding`) | "1M 행 export" 사용 사례 해결 | 페이지네이션과 다른 문제; 별도 라이브러리에 속할 수도 |
| 내장 메트릭 (Micrometer) | 페이지네이션 레이턴시, 쿼리 카운트 관측 | 사용자가 직접 추가하기 쉬움 |
| Spring Native / GraalVM 네이티브 지원 | 클라우드 네이티브 배포 스토리 | PageHelper의 reflection 패턴 검증 필요 |

---

## 범위 외

명시적으로 검토 후 안 하기로 결정한 것들.

- **NoSQL 페이지네이션 어댑터** (Mongo, DynamoDB, Elasticsearch). PageHelper/JPA 정신 모델로 잘 옮겨지지 않는 고유 패턴 보유. 전용 라이브러리가 더 적합.
- **프론트엔드 페이지네이션 컴포넌트**. 서버사이드 라이브러리. 알맞은 클라이언트 라이브러리와 페어링.
- **PageHelper가 이미 지원하는 것 외의 SQL 방언 커스텀 지원**. PageHelper가 흔한 DB (MySQL, PostgreSQL, Oracle, SQL Server, H2, DB2, MariaDB 등) 다 커버; 여러분 DB가 거기 없다면 추가는 PageHelper 쪽에서.

---

## 로드맵에 영향 주는 방법

- **투표**: 관련 GitHub 이슈에 :material-thumb-up:, 없으면 새로 등록.
- **사용 사례 > 의견**: *왜* 필요한지 구체적 시나리오로 알려주세요. 우선순위 판단이 쉬워집니다.
- **PR 환영** — "다음 마이너" 또는 "검토 중" 항목은 PR 환영, 다만 API 같이 그릴 수 있게 코멘트 먼저.
