# 기여 가이드

관심 가져주셔서 감사합니다! easy-paging은 작은 프로젝트라 기여 절차는 의도적으로 최소한입니다.

원본 기여자 가이드 (메인테이너 릴리즈 절차 포함)는 레포의 [`CONTRIBUTING.md`](https://github.com/devslab-kr/easy-paging-spring-boot-starter/blob/main/CONTRIBUTING.md)를 참조.

## 빠른 시작

```bash
git clone https://github.com/devslab-kr/easy-paging-spring-boot-starter.git
cd easy-paging-spring-boot-starter
./gradlew build
```

JDK 21+ 필요. Gradle wrapper가 나머지를 처리합니다.

## 기여 방법

1. **자명하지 않은 변경은 먼저 이슈 열기** — 변경이 라이브러리 스코프에 맞는지 시간 들이기 전에 확인할 수 있습니다.
2. **`main`에서 브랜치** 만들 때 설명적인 이름: `feature/keyset-reverse-direction`, `fix/sort-null-handling`, `docs/clarify-cursor-signing`.
3. **테스트부터 작성.** 특히 보안 관련 변경 (sort 검증, cursor codec 등) — `src/test/java/.../it/` 아래의 기존 테스트에서 통합 패턴 참조.
4. **로컬에서 `./gradlew build`** 실행 후 push. CI도 Linux에서 같은 명령을 실행하므로, 로컬 통과는 보통 CI 통과를 의미합니다.
5. **PR 열기** — 무엇이 *왜* 변경되었는지 설명. 이슈를 참조하세요.

## 코드 스타일

- 4 스페이스 들여쓰기, UTF-8, LF 줄바꿈 (`.editorconfig`로 강제됨)
- 컴파일러는 `-Werror`와 `-Xlint:all` (단, `classfile`, `processing`, `serial`은 제외) 옵션으로 실행. 로컬 빌드 통과 = CI 통과.
- 모든 public 타입과 자명하지 않은 public 메서드에 Javadoc. *무엇*이 아닌 *왜*를 설명 — *무엇*은 코드 자체에 있음.

## 테스트

- 순수 타입(`Sort`, `Cursor`, `PageResponse`, `KeysetPage`) 단위 테스트는 대상 클래스와 같은 위치 (`src/test/java/.../core` 또는 `support`).
- 통합 테스트 (Spring Boot + H2 + MyBatis + PageHelper) 는 `src/test/java/.../it` 아래, 단일 테스트 애플리케이션 (`TestApplication`) 공유.

## 이슈 리포트

[`.github/ISSUE_TEMPLATE/`](https://github.com/devslab-kr/easy-paging-spring-boot-starter/tree/main/.github/ISSUE_TEMPLATE) 아래 템플릿 사용. 다음 정보 포함:

- `./gradlew --version` 출력
- 사용 중인 Spring Boot 버전
- 최소 재현 (작은 public 레포가 이상적)

## 문서 기여

이 사이트는 [MkDocs Material](https://squidfunk.github.io/mkdocs-material/)로 빌드됩니다. 페이지는 `docs/`에 있고, 영문은 `.md`, 한글은 `.ko.md` 파일. 로컬 미리보기:

```bash
pip install mkdocs-material "mkdocs-static-i18n[material]"
mkdocs serve
```

`http://localhost:8000` 열기. CI가 `main` 푸시마다 [easy-paging.devslab.kr](https://easy-paging.devslab.kr)로 자동 배포합니다.
