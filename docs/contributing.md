# Contributing

Thanks for your interest! easy-paging is small enough that the contribution flow is intentionally minimal.

For the canonical contributor guide (including the maintainer release procedure), see [`CONTRIBUTING.md`](https://github.com/devslab-kr/easy-paging-spring-boot-starter/blob/main/CONTRIBUTING.md) in the repository.

## Quick start

```bash
git clone https://github.com/devslab-kr/easy-paging-spring-boot-starter.git
cd easy-paging-spring-boot-starter
./gradlew build
```

Requires JDK 21+. The Gradle wrapper handles everything else.

## How to contribute

1. **Open an issue first** for non-trivial changes — it lets us check whether the change fits the library's scope before you spend time on it.
2. **Branch from `main`** with a descriptive name: `feature/keyset-reverse-direction`, `fix/sort-null-handling`, `docs/clarify-cursor-signing`.
3. **Write the test first.** Especially for security-touching changes (sort validation, cursor codec, etc.) — see existing tests under `src/test/java/.../it/` for the integration patterns we use.
4. **Run `./gradlew build`** locally before pushing. CI runs the same command on Linux; passing locally usually means passing in CI.
5. **Open a PR** describing what changed and *why*. Reference the issue.

## Code style

- 4-space indent, UTF-8, LF line endings (`.editorconfig` enforces this).
- Compiler runs with `-Werror` and `-Xlint:all` (minus `classfile`, `processing`, `serial`). Passing local builds means CI agrees.
- Javadoc on every public type and non-trivial public method. Explain *why*, not *what* — the *what* is in the code.

## Tests

- Unit tests for pure types (`Sort`, `Cursor`, `PageResponse`, `KeysetPage`) live alongside their target under `src/test/java/.../core` or `support`.
- Integration tests (Spring Boot + H2 + MyBatis + PageHelper) live under `src/test/java/.../it` and share a single test application (`TestApplication`).

## Reporting issues

Use the templates under [`.github/ISSUE_TEMPLATE/`](https://github.com/devslab-kr/easy-paging-spring-boot-starter/tree/main/.github/ISSUE_TEMPLATE) and include:

- Output of `./gradlew --version`
- Your Spring Boot version
- A minimal reproduction (a small public repo is ideal)

## Documentation contributions

This site is built with [MkDocs Material](https://squidfunk.github.io/mkdocs-material/). Pages live in `docs/`, with `.md` for English and `.ko.md` for Korean. To preview locally:

```bash
pip install mkdocs-material "mkdocs-static-i18n[material]"
mkdocs serve
```

Then open `http://localhost:8000`. CI auto-deploys to [easy-paging.devslab.kr](https://easy-paging.devslab.kr) on every push to `main`.
