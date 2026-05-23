# API 레퍼런스 (Javadoc)

매 릴리즈는 core 아티팩트와 옵션 reactive 아티팩트 둘 다에 대한 `javadoc.jar`를 게시합니다. 두 가지 방법으로 읽을 수 있습니다.

## 온라인 (javadoc.io)

[javadoc.io](https://javadoc.io/)는 Maven Central의 모든 아티팩트에 대한 Javadoc을 자동으로 미러링합니다 — 별도 설정 불필요.

**Core (`easy-paging-spring-boot-starter`):**

- 최신: <https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest>
- 특정 버전: `https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/<버전>` — 예: [4.0.0](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/4.0.0) (Spring Boot 4) 또는 [3.0.0](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/3.0.0) (SB3 maintenance)

**Reactive (`easy-paging-spring-boot-starter-reactive`, 0.4.0부터):**

- 최신: <https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter-reactive/latest>
- 특정 버전: `https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter-reactive/<버전>` — 예: [4.0.0](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter-reactive/4.0.0) (Spring Boot 4) 또는 [3.0.0](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter-reactive/3.0.0) (SB3 maintenance)

새 버전을 처음 방문할 때는 javadoc.io가 Maven Central에서 jar를 가져오느라 약간 느리지만, 이후 방문부터는 캐시되어 빠릅니다.

## IDE 안에서

의존성을 추가하면 IntelliJ IDEA, Eclipse, VS Code (Java 확장) 가 Maven Central에서 `javadoc.jar`를 자동으로 가져옵니다. `@AutoPaginate`, `@KeysetPaginate`, `PageResponse`, `KeysetPage` 같은 타입 위에 마우스를 올리면 Javadoc 툴팁이 보입니다.

IDE에서 안 보이면 의존성 새로고침:

=== "Gradle"

    ```bash
    ./gradlew dependencies --refresh-dependencies
    ```

=== "Maven"

    ```bash
    mvn dependency:resolve -Dclassifier=javadoc
    ```

## 주요 진입점

API를 읽기 시작할 때 좋은 지도:

| 클래스 | 역할 |
|---|---|
| [`@AutoPaginate`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/annotation/AutoPaginate.html) | Offset 페이지네이션용 컨트롤러 어노테이션 |
| [`@KeysetPaginate`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/annotation/KeysetPaginate.html) | 커서 페이지네이션용 컨트롤러 어노테이션 |
| [`PageResponse`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/core/PageResponse.html) | Offset 응답 봉투 |
| [`KeysetPage`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/core/KeysetPage.html) | Keyset 응답 봉투 |
| [`KeysetRequest`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/core/KeysetRequest.html) | Keyset 요청에서 해결된 커서 + 페이지 크기 |
| [`CursorCodec`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/core/CursorCodec.html) | Keyset 커서 인코딩/디코딩 (HMAC-SHA256 서명) |
| [`PageResponseFactory`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/spi/PageResponseFactory.html) | 기본 응답 봉투를 교체하는 SPI |
| [`ReactivePagingSupport`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/reactive/ReactivePagingSupport.html) | 블로킹 MyBatis 호출용 Reactor 헬퍼 (core 아티팩트) |
| [`EasyPagingProperties`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter/latest/kr/devslab/easypaging/autoconfigure/EasyPagingProperties.html) | 모든 `easy-paging.*` 설정 키 |

옵션 reactive 아티팩트 (`easy-paging-spring-boot-starter-reactive`):

| 클래스 | 역할 |
|---|---|
| [`R2dbcOffsetPagingSupport`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter-reactive/latest/kr/devslab/easypaging/r2dbc/R2dbcOffsetPagingSupport.html) | R2DBC offset/limit 페이지네이션 → `Mono<PageResponse<T>>` |
| [`R2dbcKeysetSupport`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter-reactive/latest/kr/devslab/easypaging/r2dbc/R2dbcKeysetSupport.html) | R2DBC keyset/커서 페이지네이션 — 사전순(lexicographic) `WHERE` 빌더 + 헬퍼 |
| [`ReactiveKeysetRequestArgumentResolver`](https://javadoc.io/doc/kr.devslab/easy-paging-spring-boot-starter-reactive/latest/kr/devslab/easypaging/webflux/ReactiveKeysetRequestArgumentResolver.html) | WebFlux `KeysetRequest` 인자 리졸버 (자동 등록) |
