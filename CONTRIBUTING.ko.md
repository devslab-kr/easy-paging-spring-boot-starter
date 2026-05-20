# 기여 가이드

[English](CONTRIBUTING.md) · **한국어**

관심 가져주셔서 감사합니다. 작은 프로젝트라 기여 절차는 의도적으로 최소한으로 유지합니다.

## 빌드와 테스트

```bash
./gradlew build
```

JDK 21 이상이 필요합니다. Gradle wrapper를 사용하므로 별도의 Gradle 설치는 필요 없습니다.

## 코드 스타일

- 4 스페이스 들여쓰기, UTF-8, LF 줄바꿈 (`.editorconfig`로 강제됨)
- 컴파일러는 `-Werror`와 `-Xlint:all` (단, `classfile`, `processing`, `serial`은 제외) 옵션으로 실행됩니다. 로컬에서 빌드가 통과되면 CI도 통과합니다.
- 모든 public 타입과 자명하지 않은 public 메서드에는 Javadoc 작성. **무엇** 을 하는지가 아니라 **왜** 그렇게 했는지를 설명하세요 — 무엇은 코드 자체에 이미 있습니다.

## 테스트

테스트는 두 레이어로 분리되어 있습니다:

1. **Fast** — H2 in-memory, 기본 실행.
   ```bash
   ./gradlew test          # core + reactive, 약 30초, Docker 불필요
   ```
   순수 타입(`Sort`, `Cursor`, `PageResponse`, `KeysetPage`)에 대한 단위 테스트는 대상 클래스와 같은 위치(`src/test/java/.../core` 또는 `support`).
   통합 테스트(core는 Spring Boot + H2 + MyBatis + PageHelper, reactive는 Spring Boot + r2dbc-h2)는 `src/test/java/.../it` 아래에 두며 단일 테스트 애플리케이션(`TestApplication` / `ReactiveTestApplication`) 공유.

2. **Dialect-compat** — Testcontainers로 실제 PostgreSQL + MySQL 컨테이너 띄워서 검증, 명시 실행.
   ```bash
   ./gradlew testDialect   # Docker daemon 필요, 약 2분
   ```
   `@Tag("dialect-compat")`로 태깅되어 있고 `src/test/java/.../it/dialect/`에 위치. fast `test` 태스크는 이 태그를 제외하므로 일상 개발은 빠른 상태 유지. CI는 둘을 별도 job으로 모두 실행.

### Windows 로컬 개발에서 Docker 셋업

dialect 테스트는 Testcontainers가 접속할 수 있는 Docker daemon이 필요합니다. 최근 Windows Docker Desktop이 기본 named pipe(`\\.\pipe\docker_engine`)에 "400 + redirect" 응답을 보내는데 Testcontainers ≤ 1.21.x가 이 redirect를 따라가지 않습니다. 두 가지 해결책:

- **[Testcontainers Desktop](https://testcontainers.com/desktop/)** 실행 — 공식 companion 도구로 Docker 접속을 깔끔하게 프록시. 권장.
- 또는 Docker Desktop 설정에서 TCP daemon 노출 (*Settings → General → Expose daemon on tcp://localhost:2375*) 후 `~/.testcontainers.properties`에 `docker.host=tcp://localhost:2375` 설정. 보안상 신뢰된 로컬 환경에서만.

Linux + Docker (CI의 Ubuntu runner 포함)는 표준 Unix 소켓 `/var/run/docker.sock`이 추가 설정 없이 동작. macOS는 Docker Desktop이 자동으로 named pipe `unix:///var/run/docker.sock` 생성.

## 이슈 리포트

`.github/ISSUE_TEMPLATE/` 아래의 템플릿을 사용해주세요. 다음 정보를 포함해주시면 좋습니다:
- `./gradlew --version` 출력
- 사용 중인 Spring Boot 버전

---

## 메인테이너용: 릴리즈 절차

> 이 섹션은 프로젝트 메인테이너에게만 해당됩니다. 사용자는 이 절차를 알 필요 없이 그냥 `implementation("kr.devslab:easy-paging-spring-boot-starter:…")` 한 줄로 끝납니다.

### 1회성 초기 설정

1. **Maven Central 네임스페이스 클레임**
   <https://central.sonatype.com/> 에서 `kr.devslab` 네임스페이스를 추가하고, `devslab.kr` 도메인에 DNS TXT 레코드 추가하여 소유권 검증. 보통 몇 시간 내 승인됩니다.

2. **Sonatype User Token 발급**
   `central.sonatype.com → Account → Generate User Token`. 사용자명/비밀번호 안전한 곳에 저장.

3. **릴리즈 서명 키 생성**
   Maven Central은 모든 아티팩트에 GPG 서명을 요구합니다:
   ```bash
   gpg --full-generate-key                      # RSA 4096, 만료 없음
   gpg --list-secret-keys --keyid-format=long   # KEY_ID(긴 형식) 복사
   gpg --armor --export-secret-keys KEY_ID      # SIGNING_KEY 시크릿 값
   gpg --keyserver hkps://keys.openpgp.org \
       --send-keys KEY_ID                        # 공개키를 keyserver에 publish
   ```

4. **GitHub Secrets 등록**
   레포 Settings → Secrets and variables → Actions에서 다음 5개 시크릿 등록:
   - `MAVEN_CENTRAL_USERNAME` — Sonatype 토큰 사용자명
   - `MAVEN_CENTRAL_PASSWORD` — Sonatype 토큰 비밀번호
   - `SIGNING_KEY` — 3번에서 export한 ASCII-armored 개인키
   - `SIGNING_KEY_ID` — GPG 키 지문의 마지막 8자리
   - `SIGNING_KEY_PASSWORD` — GPG 키 패스프레이즈

### 매 릴리즈마다

1. `gradle.properties`의 `VERSION` 업데이트 (또는 워크플로우에 `-PVERSION=...` 전달). 안정 릴리즈는 `-SNAPSHOT` 접미사 제거.
2. `CHANGELOG.md`에 새 버전 항목 추가.
3. 커밋 후 태그: `git tag v0.1.0 && git push --tags`
4. `release.yml` 워크플로우가 자동으로 빌드 → 테스트 → 서명 → Maven Central publish → GitHub Release 생성까지 진행.

publish 단계에서 실패하면 워크플로우 로그를 먼저 확인하세요 — 가장 흔한 원인은 GPG 서명 누락 또는 만료입니다. 서명 외 파이프라인을 먼저 검증하고 싶으면 `RELEASE_SIGNING_ENABLED=false`로 다시 실행해볼 수 있습니다.
