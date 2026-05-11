# Contributing

**English** · [한국어](CONTRIBUTING.ko.md)

Thanks for your interest. This project is small enough that the contribution
flow is intentionally minimal.

## Build and test

```bash
./gradlew build
```

Requires JDK 21+. The build uses the Gradle wrapper, so no separate Gradle
install is needed.

## Code style

- 4-space indent, UTF-8, LF line endings (enforced by `.editorconfig`).
- The compiler runs with `-Werror` and `-Xlint:all` (minus `classfile`,
  `processing`, `serial`). A passing local build means CI will agree.
- Javadoc on every public type and non-trivial public method. Explain *why* —
  the *what* is in the code.

## Tests

- Unit tests for pure types (`Sort`, `Cursor`, `PageResponse`, `KeysetPage`)
  live alongside their target under `src/test/java/.../core` or `support`.
- Integration tests (Spring Boot + H2 + MyBatis + PageHelper) live under
  `src/test/java/.../it` and share a single test application
  (`TestApplication`).

## Reporting issues

Please use the templates under `.github/ISSUE_TEMPLATE/` and include the
output of `./gradlew --version` plus your Spring Boot version.

---

## Maintainer: cutting a release

> This section is only relevant to project maintainers. End users don't need
> any of this — they just `implementation("kr.devslab:easy-paging-spring-boot-starter:…")`.

### One-time setup

1. **Claim the Maven Central namespace.** At <https://central.sonatype.com/>,
   add `kr.devslab` as a namespace and follow the DNS TXT verification flow
   against `devslab.kr`. Approval typically lands in a few hours.
2. **Generate a Sonatype user token.** `central.sonatype.com → Account →
   Generate User Token`. Save the username and password.
3. **Create a release signing key.** Maven Central requires a GPG signature on
   every artifact:
   ```bash
   gpg --full-generate-key                      # RSA 4096, no expiry
   gpg --list-secret-keys --keyid-format=long   # copy the long KEY_ID
   gpg --armor --export-secret-keys KEY_ID      # the SIGNING_KEY secret value
   gpg --keyserver hkps://keys.openpgp.org \
       --send-keys KEY_ID                        # publish the public half
   ```
4. **Register GitHub secrets** on this repository:
   - `MAVEN_CENTRAL_USERNAME` — Sonatype user token username
   - `MAVEN_CENTRAL_PASSWORD` — Sonatype user token password
   - `SIGNING_KEY` — ASCII-armored private key from step 3
   - `SIGNING_KEY_ID` — last 8 chars of the GPG key fingerprint
   - `SIGNING_KEY_PASSWORD` — passphrase for the GPG key

### Every release

1. Update `VERSION` in `gradle.properties` (or pass `-PVERSION=...` to the
   workflow). Drop the `-SNAPSHOT` suffix for a stable release.
2. Update `CHANGELOG.md` with the new entry.
3. Commit and tag: `git tag v0.1.0 && git push --tags`.
4. The `release.yml` workflow builds, tests, signs, publishes to Maven
   Central, and creates a GitHub Release.

If the publish step fails, inspect the workflow logs — the most common cause
is a missing or expired GPG signature; re-run with `RELEASE_SIGNING_ENABLED=false`
to verify the rest of the pipeline before fixing the signing setup.
