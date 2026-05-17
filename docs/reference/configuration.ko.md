# 설정 레퍼런스

모든 속성은 `application.yml` / `application.properties`의 `easy-paging` prefix 아래에 있습니다. 아래는 기본값 — 다른 값이 필요한 것만 설정하면 됩니다.

## 전체 속성 트리

```yaml title="application.yml"
easy-paging:
  enabled: true                  # 마스터 스위치; false면 스타터 전체 비활성화
  default-page-size: 20          # 호출자가 ?size= 생략했을 때 사용
  max-page-size: 500             # 전역 절대 상한 (@AutoPaginate maxSize가 더 커도 절대 초과 X)
  auto-wrap-list: true           # false면 PageResponse 자동 래핑을 전역 비활성화
  keyset:
    cursor-secret: ""            # 커서 서명용 HMAC-SHA256 시크릿; 빈 값 = 서명 없음 (개발용만)
    max-cursor-bytes: 2048       # 디코딩된 커서 페이로드 크기 상한 (DoS 방지)
```

## 속성 세부 사항

### `easy-paging.enabled`

| | |
|---|---|
| **타입** | `boolean` |
| **기본값** | `true` |
| **효과** | 스타터 전체의 마스터 스위치 |

`false`면 자동 설정이 스킵됩니다. `@AutoPaginate`와 `@KeysetPaginate`가 no-op이 됨 (메서드는 실행되지만 PageHelper 설정도, 요청 인자 해결도 일어나지 않음).

테스트 중 페이지네이션을 비활성화하거나, MyBatis가 초기화되지 않은 환경에 유용.

### `easy-paging.default-page-size`

| | |
|---|---|
| **타입** | `int` |
| **기본값** | `20` |
| **효과** | 요청이 `?size=`를 생략했을 때 fallback 페이지 크기 |

Spring MVC의 `PageableHandlerMethodArgumentResolver`도 자체 기본값(20)이 있음. 이 속성은 해결된 `Pageable`이 양수가 아닌 `pageSize`를 보고할 때 aspect가 참조.

### `easy-paging.max-page-size`

| | |
|---|---|
| **타입** | `int` |
| **기본값** | `500` |
| **효과** | 페이지 크기의 절대 상한 (어노테이션별 `maxSize`와 무관) |

방어의 한 겹 더로 사용. 컨트롤러가 `@AutoPaginate(maxSize = 10000)`를 선언해도, 실제 페이지 크기는 `min(annotation.maxSize, easy-paging.max-page-size)`로 클램핑됨.

전체 애플리케이션의 DoS 방어를 강화하려면 더 낮게 설정.

### `easy-paging.auto-wrap-list`

| | |
|---|---|
| **타입** | `boolean` |
| **기본값** | `true` |
| **효과** | Aspect가 `List` 반환을 `PageResponse` 봉투로 래핑할지 여부 |

`false`면, `PageResponse`나 `Object` 반환 타입을 선언하고 실제로 `List`를 반환하는 메서드에서 리스트가 래핑 없이 그대로 반환됨. Aspect는 여전히 `PageHelper.startPage(...)`를 호출 — 래핑 단계만 스킵.

응답 래핑을 수동으로 처리하고 싶을 때 유용 (예: `ResponseBodyAdvice` 사용).

### `easy-paging.keyset.cursor-secret`

| | |
|---|---|
| **타입** | `String` |
| **기본값** | `""` (빈 값) |
| **효과** | Keyset 커서를 위한 HMAC-SHA256 서명 키 |

비어있지 않으면, 모든 커서가 서명되고 위조 시도는 HTTP 400으로 거부됨. 비어있으면 커서는 Base64 인코딩되지만 인증되지 않음 — 악의적 클라이언트가 봐서는 안 되는 행을 노리는 커서를 만들 수 있음.

**운영 환경 셋업**: 32바이트 이상의 랜덤 시크릿 생성, 환경변수로 저장, 절대 git에 커밋 금지:

```bash
openssl rand -base64 32
```

```yaml
easy-paging:
  keyset:
    cursor-secret: ${EASY_PAGING_CURSOR_SECRET}
```

### `easy-paging.keyset.max-cursor-bytes`

| | |
|---|---|
| **타입** | `int` |
| **기본값** | `2048` |
| **효과** | 디코딩된 커서 페이로드의 최대 크기 (바이트) |

이보다 큰 커서는 HTTP 400으로 거부됨. 공격자가 거대한 커서 토큰을 제출하는 메모리 증폭 공격을 방지. 커서 키가 정말 크게 필요한 경우(드묾)에만 상향 조정.

## IDE 자동완성

스타터에는 configuration metadata가 포함되어 있어, IntelliJ IDEA (및 Spring Boot 지원 IDE)에서 다음을 제공:

- `easy-paging.*` 키 자동완성
- 인라인 문서
- 기본값 힌트
- 알 수 없는 키에 대한 검증 경고

`application.yml`에 `easy-paging.`을 입력하면 위 모든 키가 제안됩니다.
