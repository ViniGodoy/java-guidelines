# Java Guidelines

Coding rules for **new** Java services on **Java 25 (LTS)** and **Spring Boot 4.1** (Web MVC, Validation,
Data JPA, Security), written as a `CLAUDE.md`, plus a sample project that proves the rules and the
toolchain behind them actually work.

## Objective

Java and Spring changed a lot between Java 17/Spring Boot 3 and Java 25/Spring Boot 4: records,
sealed types and pattern matching replaced several patterns; Lombok, `RestTemplate`, `@MockBean`,
Spring's own `@Nullable` and the `.and()` security DSL became redundant, deprecated or removed; JSpecify
became Spring's null-safety standard. Most code on the internet, and most of what AI coding assistants
learned from, still reflects the older practices.

This project pins down **one current, strict way** to write a Spring Boot service, so that:

- every rule is backed by official documentation (OpenJDK, Oracle, Spring, Hibernate, JetBrains,
  Google, and the tools' own docs or source), listed at the end of `CLAUDE.md`;
- nothing is relaxed for legacy code: the rules target greenfield services only;
- every rule a tool can check is checked, and the build fails on any violation;
- the claims that matter were verified by running them, not just by reading about them.

## Why `CLAUDE.md` matters to Java programmers

`CLAUDE.md` is the file Claude Code reads automatically at the start of every session in a
repository. Whatever it says becomes the instructions the assistant follows while writing code there.
That makes it relevant to you in two ways:

1. **It steers AI-written code toward current Java.** Without it, an assistant tends to produce what
   was common in its training data: Lombok `@Data` entities, field injection, `Optional` fields,
   `RestTemplate`, `throws Exception` on security config, H2 in tests. With it, the assistant
   writes records, constructor injection, JSpecify nullability, `RestClient`, Testcontainers, and
   explains deviations instead of silently making them.
2. **It is a readable team standard on its own.** The file is plain Markdown organised by topic
   (baseline, modern Java, null handling and `Optional`, Lombok, Spring, JPA, validation, web,
   security, testing, style, static analysis). Each rule says what to do, what not to do, and why.
   It answers recurring review questions once, for example:
   - where `Optional` is allowed (return types only) and where it is not (fields, parameters,
     DTOs, controller parameters);
   - why Lombok is not used at all on Java 25;
   - which layer owns which constraint (request DTO, entity, domain, Flyway migration), and what
     Hibernate's `ddl-auto=validate` does and does not catch;
   - which Error Prone, NullAway, PMD, SpotBugs and ArchUnit rules enforce all of the above.

Because the same file drives the assistant and documents the standard, human-written and
AI-written code are held to the same rules, and the build enforces them for both.

## What's in this folder

| Path | What it is |
|---|---|
| `CLAUDE.md` | The rules: 13 sections plus sources. |
| `java25-sample/` | Reference Spring Boot 4.1.1 service (Maven) that follows the rules and wires the full toolchain: google-java-format via Spotless, Error Prone + NullAway (JSpecify mode) with `-Werror`, PMD, SpotBugs, ArchUnit, Testcontainers. |

## How it was verified

The sample builds green with `mvnw verify` on JDK 25 (Corretto 25.0.4.1), running all 22 tests,
including PostgreSQL 18 via Testcontainers. On top of a clean build:

- each quality gate was shown to **fail** the build on a deliberately planted violation (NullAway,
  Error Prone, Spotless, PMD, SpotBugs, ArchUnit);
- Hibernate's startup schema checks were shown to catch a renamed index, a renamed unique key and an
  index on the wrong column, and to **miss** column-length and nullability drift, which is why the
  rules require repository tests for those.

Running it surfaced real issues that are now part of the rules, e.g. google-java-format 1.37.0 is
incompatible with Spotless (pinned to 1.36.1), Spring Security 7.1 no longer needs
`throws Exception`, and Mockito must be loaded as a Java agent on JDK 25.

## How to use it

- **In a new service:** copy `CLAUDE.md` to the repository root (Claude Code picks it up
  automatically), and start from `java25-sample/`'s `pom.xml`, `.mvn/jvm.config` and `config/`.
- **To run the sample:** JDK 25 and Docker are required.

  ```bash
  cd java25-sample
  ./mvnw verify
  ```

  Without Docker the three database tests are skipped and everything else still runs.

## Keeping it current

The rules were verified on 2026-10-13 against the latest releases at that time. Revisit them when:

- a new Spring Boot minor ships (4.2 is expected in November 2026);
- google-java-format releases the Spotless fix (then drop the 1.36.1 pin);
- the next Java LTS arrives (Java 29, September 2027).

Each tool version in `CLAUDE.md` section 13 should be bumped to the latest release, and the sample
rebuilt to confirm the toolchain still holds.
