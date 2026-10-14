# Java / Spring Boot rules

Rules for new Java services built on Spring Boot with Web MVC, Validation, Data JPA and Security.
Verified against official documentation on 2026-10-13 (sources at the end). These rules are for
greenfield code only: every rule is mandatory, there are no legacy exceptions, and nothing is
relaxed for compatibility with older code or older versions.

## 1. Baseline

- **Java 25** (current LTS, GA Sept 2025; next LTS is Java 29 in Sept 2027). Set
  `<java.version>25</java.version>` / `java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }`.
- **Spring Boot 4.1.x** (latest GA line; 4.1.1 at time of writing). Spring Boot has no OSS LTS:
  stay on the latest GA minor and latest patch. Boot 4.1 brings Spring Framework 7, Spring
  Security 7.1, Spring Data 2026.0, Hibernate ORM 7.4, Hibernate Validator 9.1, Jakarta EE 11,
  Jackson 3.
- Never pin versions that Spring Boot's dependency management already manages. Only add a
  version for dependencies Boot does not manage.
- Do not enable preview features (`--enable-preview`) in production code.
- Use `jakarta.*` packages only. `javax.persistence` / `javax.validation` / `javax.servlet`
  must never appear.

## 2. Starters (Spring Boot 4 modular names)

Use the Boot 4 starter names; the old ones are gone or deprecated:

| Purpose | Starter | Test starter |
|---|---|---|
| Web MVC | `spring-boot-starter-webmvc` (not `-web`) | `spring-boot-starter-webmvc-test` |
| JPA | `spring-boot-starter-data-jpa` | `spring-boot-starter-data-jpa-test` |
| Validation | `spring-boot-starter-validation` | `spring-boot-starter-validation-test` |
| Security | `spring-boot-starter-security` | `spring-boot-starter-security-test` |
| OAuth2 resource server | `spring-boot-starter-security-oauth2-resource-server` | |
| Migrations | `spring-boot-starter-flyway` + `org.flywaydb:flyway-database-postgresql` | `spring-boot-starter-flyway-test` |

- Test starters pull in `spring-boot-starter-test` transitively; don't list it separately.
- Never use `spring-boot-starter-classic` / `-test-classic` (they exist only to ease migrations).
- Don't add `spring-boot-starter-aspectj` unless you actually write aspects.

## 3. Modern Java: use these

All of the following are final (non-preview) in Java 25.

- **Records** for DTOs, request/response bodies, value objects, projections, events and
  `@ConfigurationProperties`. Validate invariants in the compact constructor.
- **Sealed interfaces + records** for closed hierarchies (results, commands, domain states).
- **Pattern matching**: `instanceof` patterns, `switch` patterns and record deconstruction
  patterns instead of casts, `if/else instanceof` chains and calling record accessors after a
  type test. Use a `when` guard instead of an `if` as the only statement in a `case` body.
- **`switch` over `if/else` chains** that compare one variable against several values.
- **Exhaustive `switch` over an enum or sealed type has no `default`**, so the compiler flags new
  cases (PMD `ExhaustiveSwitchHasDefault`, Error Prone `UnnecessaryDefaultInEnumSwitch`).
- **Arrow-form `switch`** only (expressions and statements). Never colon-form `case X:` blocks.
- **Unnamed variables `_`** for unused lambda parameters, catch parameters and pattern components.
- **Text blocks** for multi-line SQL/JPQL/JSON in code and tests.
- **Sequenced collections**: `list.getFirst()`, `getLast()`, `removeLast()`, `reversed()` instead
  of index arithmetic or reversing a copy.
- **`Math.clamp`** instead of nested `Math.min`/`Math.max`.
- **`EnumSet` / `EnumMap`** for enum keys.
- **Explicit charset and locale**: `StandardCharsets.UTF_8` on every byte/char conversion,
  `Locale.ROOT` on `toUpperCase`/`toLowerCase`/`String.format` of non-user-facing text. Never
  rely on platform defaults.
- **Time comes from an injected `Clock`** bean (`Instant.now(clock)`), never `Instant.now()` /
  `LocalDate.now()` without one; makes time testable and the zone explicit.
- **`Stream.toList()`** instead of `collect(Collectors.toList())` (result is unmodifiable, which
  is what you want). Stream gatherers (`Stream.gather`) when a custom intermediate op is needed.
- **Immutable collection factories**: `List.of`, `Set.of`, `Map.of`, `List.copyOf` for
  defensive copies.
- **Flexible constructor bodies**: validate/compute arguments *before* `super(...)`/`this(...)`
  instead of static helper methods.
- **`java.time`** only (`Instant` for timestamps, `LocalDate` for dates, `OffsetDateTime` at API
  boundaries). Never `Date`, `Calendar`, `SimpleDateFormat`, `java.sql.Timestamp` in domain code.
- **Virtual threads** are on: `spring.threads.virtual.enabled=true` and
  `spring.main.keep-alive=true` (virtual threads are daemon threads; keep-alive stops the JVM
  exiting when only they remain). Don't configure thread-pool sizes; they have no effect.
- **Scoped values** (`ScopedValue`) instead of `ThreadLocal` for new per-request context you
  own (framework context such as `SecurityContextHolder` stays as is).

### `var`

Follow the OpenJDK LVTI style guide: use `var` when the initializer makes the type obvious
(`var users = new ArrayList<User>()`, `var order = orderRepository.getReferenceById(id)`) and the
scope is small. Use the explicit type when the initializer doesn't reveal it, for numeric
literals, and with diamond/generic methods that would infer `Object`. Never on fields,
parameters or return types (not allowed anyway).

### Outdated practices (never do them)

- Replacing `synchronized` with `ReentrantLock` "for virtual threads": since JDK 24 (JEP 491)
  `synchronized` no longer pins virtual threads. Choose the lock for semantics, not pinning.
  Pinning still happens when blocking inside a class initializer or in Java code called back from
  native code; watch `jdk.VirtualThreadPinned` JFR events.
- `Optional.get()`: use `orElseThrow()` (Java 10+), which reads as what it does.
- `Collections.unmodifiableList(new ArrayList<>(x))`: use `List.copyOf(x)`.
- Module import declarations (`import module java.base;`) in production code: Google style
  forbids them; keep explicit single-type imports.
- Pooling platform threads for blocking I/O when virtual threads are enabled.

## 4. Null handling and `Optional`

### Null-safety: JSpecify (Spring Framework 7 standard)

- Every package has a `package-info.java` with `@NullMarked`
  (`org.jspecify.annotations.NullMarked`). Inside it, everything is non-null by default.
- Mark the exceptions with `org.jspecify.annotations.@Nullable`, placed on the type use, right
  before the type: `private @Nullable String nickname;`, `public @Nullable User find(...)`,
  `List<@Nullable String>`, `Object @Nullable []` (nullable array).
- Never use the deprecated `org.springframework.lang.Nullable/NonNull/NonNullApi/NonNullFields`,
  `javax.annotation.*` (JSR-305), or any other nullability annotation set.
- Nullability annotations aren't inherited: repeat `@Nullable` on overriding methods when it
  applies.
- Enforced at compile time by NullAway as an error, in full JSpecify mode (section 13). Suppress
  only with a targeted `@SuppressWarnings("NullAway")` on the smallest element, plus a comment
  giving the reason.
- JPA entities need no suppression for their `protected` no-arg constructor: NullAway's built-in
  Jakarta Persistence handler (0.13.6+) knows the provider initializes those fields.

### `Optional` rules

The JDK defines `Optional` as "primarily intended for use as a method return type where there is
a clear need to represent 'no result'". Therefore:

- **Do** return `Optional<T>` from methods whose "not found" is a normal outcome
  (`findByEmail`, lookups, parsing).
- **Do** return `T` (non-null) when absence is a bug or must become an error: throw a domain
  exception (e.g. `orElseThrow(() -> new UserNotFoundException(id))`) at the boundary that knows
  what absence means.
- **Don't** use `Optional` for fields, method/constructor parameters, record components, entity
  attributes, collection elements, map values, or `@ConfigurationProperties`. Use `@Nullable`
  instead. Basis: the JDK API note (return type only); JetBrains' `OptionalUsedAsFieldOrParameterType`
  inspection (flags fields/parameters; `Optional` is not `Serializable`); Spring Boot docs (not
  recommended in `@ConfigurationProperties`); Brian Goetz, who designed it ("almost never use it as
  a field of something or a method parameter"). Collection elements, map values and entity
  attributes follow from the same reasoning; no single official source lists them.
- **Don't** return `Optional<List<T>>` / `Optional<Stream<T>>`: return an empty collection or
  stream. Never return `null` for collections.
- **Never** return `null` from a method declared to return `Optional`; an `Optional` variable is
  never `null`.
- **Don't** call `get()`; don't do `isPresent()` + `get()`. Use `map`, `flatMap`, `filter`,
  `orElse` (constant/cheap default), `orElseGet` (computed default), `orElseThrow`,
  `ifPresentOrElse`, `or`, `stream()`.
- **Don't** compare with `==` against `Optional.empty()`, synchronize on it, or rely on identity
  (it's a value-based class). Use `isEmpty()`/`isPresent()`.
- **Don't** create `Optional` just to chain a null check (`Optional.ofNullable(x).ifPresent(...)`
  in place of `if (x != null)`). Plain `if` is clearer.
- Use `OptionalInt`/`OptionalLong`/`OptionalDouble` for primitives, never `Optional<Integer>`.
- Spring Data repositories: use `Optional<T> findBy...` for single results that may be missing
  (`findById` already returns it); collection-returning methods never return `null`.

#### `Optional` in controllers and web DTOs

- **Controller parameters**: Spring MVC accepts `Optional<T>` for `@RequestParam`, `@PathVariable`,
  `@RequestHeader`, but don't use it. Use `@RequestParam(defaultValue = "...")` when there's a
  sensible default, otherwise `@RequestParam(required = false) @Nullable Integer page`. This keeps
  one nullability convention (JSpecify, checked by NullAway) across the codebase.
- **Request DTOs** (records bound from JSON): no `Optional` components; use `@Nullable`.
  `Optional` doesn't distinguish "field absent" from "field null" reliably (Jackson 3 has a reported
  issue deserializing a missing `Optional` property as `null`, which breaks the "never null" rule).
  If PATCH semantics need absent vs. null, design for it explicitly (JSON Merge Patch / a dedicated
  type), not with `Optional`.
- **Response DTOs**: no `Optional` components. `null` (or omitting the property via
  `@JsonInclude(NON_NULL)`) is what the client sees either way, so `Optional` adds nothing.
- **Controller return values**: resolve the `Optional` in the service or controller with
  `orElseThrow(() -> new XNotFoundException(id))` and let the exception handler turn it into a
  404 Problem Details response. Don't use `ResponseEntity.of(optional)`: it returns a bare 404
  without a Problem Details body.

## 5. Lombok policy

**Lombok is not used.** Don't add the dependency.

Why: everything it was used for is covered by the language or is unsafe on our main mutable type.

| Lombok feature | Use instead |
|---|---|
| `@Value`, `@Data`, `@AllArgsConstructor`, `@EqualsAndHashCode`, `@ToString` on value types | Records |
| `@Builder` | Records with a small static factory per meaningful construction; a hand-written builder only when a type genuinely has many optional components |
| `val` / `var` | Java `var` |
| `@RequiredArgsConstructor` on Spring beans | A plain constructor (Spring Boot's documented style) |
| `@Slf4j` | `private static final Logger log = LoggerFactory.getLogger(Foo.class);` |
| `@Getter` / `@Setter` / `@NoArgsConstructor` on entities | Hand-written accessors and a `protected` no-arg constructor. Entities expose intention-revealing methods (`order.cancel()`), not a setter per field, so there is little to generate |
| `@NonNull` | JSpecify (`@NullMarked` / `@Nullable`) checked by NullAway |
| `@SneakyThrows`, `@Cleanup`, `@Synchronized`, `@Delegate`, `@UtilityClass`, `lombok.experimental.*` | Explicit Java (wrap checked exceptions, try-with-resources, `synchronized`, delegation by hand, a final class with a private constructor) |

Lombok's `@Data`/`@EqualsAndHashCode`/`@ToString` are actively harmful on JPA entities (JetBrains
JPA Buddy guidance: they break with proxies/collections and trigger lazy loading), Lombok needs an
upgrade for every new JDK (JDK 25 support arrived in 1.18.40), and generated code is invisible to
reviewers and to static analysis that runs on source.

## 6. Spring core

- **Constructor injection only**, dependencies in `private final` fields. Exactly one
  constructor, with no `@Autowired` (Spring Boot docs). No field or setter injection.
- `@RestController` (never `@Controller` + `@ResponseBody`) and composed mappings (`@GetMapping`,
  `@PostMapping`, ...); never a bare `@RequestMapping` on a handler method.
- Proxy-based annotations (`@Transactional`, `@Async`, `@Cacheable`, `@PreAuthorize`) go on
  `public` methods and are never reached through `this.` calls inside the same class; the proxy
  is bypassed. Split the method into another bean instead.
- `@Transactional` rolls back on unchecked exceptions only; since we don't throw checked
  exceptions from services (section 12) the default is correct. Never catch-and-swallow inside a
  transaction to "keep going".
- Keep beans package-private when nothing outside the package needs them; `@Configuration` and
  `@Bean` methods don't need to be `public`.
- Main class in the root package; rely on `@SpringBootApplication` component scanning. Never
  explicit `@ComponentScan`/`@EntityScan`/`@EnableJpaRepositories`.
- **Configuration**: typed `@ConfigurationProperties` records, registered with
  `@ConfigurationPropertiesScan`, validated with `@Validated` + constraint annotations. No
  `@ConstructorBinding` (implied for a single constructor). Never `@Value("${...}")` for
  configuration. Configuration properties classes don't inject other beans.
- **HTTP clients**: `RestClient` (via the auto-configured `RestClient.Builder`) or declarative
  `@HttpExchange` interfaces (HTTP Service Clients). Never `RestTemplate` (deprecated in Spring
  Framework 7 in favor of `RestClient`). No `WebClient` (we don't write reactive code).
- **JSON**: Jackson 3. Databind is `tools.jackson.*` (annotations stay in
  `com.fasterxml.jackson.annotation`). Customize via `JsonMapperBuilderCustomizer` or a
  `JsonMapper` bean, never an `ObjectMapper` bean. Use `@JacksonComponent` / `@JacksonMixin`
  (not `@JsonComponent` / `@JsonMixin`).
- Profiles for environment differences only; no business logic switched on profile names.
- Log through SLF4J with parameterized messages
  (`log.info("Created order {}", id)`), never string concatenation; never log secrets or PII.

## 7. JPA / Hibernate

### Entities

- Entities are **classes, never records** (JPA requires non-final, mutable, with a no-arg
  constructor). Records are fine for `@Embeddable` types, composite ids and projections.
- Entity class and its persistent methods are not `final`. Provide a `protected` no-arg
  constructor plus a meaningful public constructor/factory that enforces invariants.
- **Never expose entities in the web API.** Map to/from record DTOs in the service or controller
  layer.
- Use surrogate keys (`@Id @GeneratedValue`), `Long` with `GenerationType.SEQUENCE` (or
  `IDENTITY` on databases without sequences), or `UUID` (`@GeneratedValue` / `@UuidGenerator`)
  when ids are exposed or generated outside the DB.
- Every aggregate root has a `@Version` attribute (optimistic locking).
- Nullability and length on entities follow "Constraints: one owner per layer" below.
- Enums: `@Enumerated(EnumType.STRING)`, never `ORDINAL`.

### Constraints: one owner per layer

Each layer states a constraint once, in that layer's own mechanism. Same fact in two layers is
fine (they're different contracts); the same fact twice in one layer is not.

| Layer | Purpose | States constraints with |
|---|---|---|
| Request DTO (record) | Reject bad input with a 400 and field errors | Jakarta Validation, full set (`@NotBlank`, `@Email`, `@Size`, `@Pattern`, ...) |
| Entity | Last check before SQL; catches bugs, not user errors | Jakarta Validation, structural only (`@NotNull`, `@NotBlank`, `@Size(max)`, `@Digits`, `@Min`/`@Max`) |
| Domain methods | Business invariants (state transitions, cross-field rules) | Constructor/method checks throwing domain exceptions |
| Migration (Flyway) | The real guarantee | `NOT NULL`, `varchar(n)`, `numeric(p,s)`, `UNIQUE`, FKs, `CHECK` |

Why entities use Jakarta Validation and not `@Column(nullable/length)`:

- With Hibernate Validator on the classpath, Hibernate ORM validates entity constraints
  automatically before every insert and update (`BeanValidationEventListener`; deletes are not
  validated by default). Jakarta Persistence 3.2 requires this to be enabled.
- In that setup `hibernate.check_nullability` defaults to off, so `@Column(nullable = false)` is
  **not** checked at runtime. It only feeds generated DDL, which we don't use (Flyway owns the
  schema). `@NotNull` is checked.
- Hibernate translates `@NotNull`, `@Size(max)`, `@Digits`, `@Min`/`@Max` into the same mapping
  metadata `@Column` would carry, so writing both is pure duplication.

Rules:

- On entity attributes, write `@NotNull` / `@Size(max = N)`, not `@Column(nullable = false,
  length = N)`. Keep `@Column` for what validation can't say: `name`, `precision`/`scale` when not
  using `@Digits`, `updatable = false`, `insertable = false`. Never `columnDefinition`.
- Required associations: `@ManyToOne(fetch = LAZY, optional = false)` (JPA mapping semantics),
  no extra `@NotNull`.
- No input-format constraints on entities (`@Email`, `@Pattern` for user input): those belong to
  the request DTO.
- Uniqueness is never a Bean Validation concern: it's a `UNIQUE` constraint in the migration, plus
  a service-level check if you need a friendly error. Mirror every unique constraint in the
  mapping with an explicit name matching the migration, so schema validation can check it:
  `@Table(uniqueConstraints = @UniqueConstraint(name = "uk_customer_email", columnNames = "email"))`.
  Never `@Column(unique = true)` or an unnamed `@UniqueConstraint`: Hibernate generates a name
  that won't exist in the database and startup fails.
- An entity constraint violation surfaces at flush as `jakarta.validation.ConstraintViolationException`
  and means the input or domain layer missed something. Fix the earlier layer; don't map it to a
  user-facing 400.
- Share limits through constants so layers can't drift:
  `@Size(max = Customer.NAME_MAX_LENGTH)` on both the entity and the request DTO, and the same
  value in the migration (`varchar(100)`).
- `ddl-auto=validate` does **not** catch length, precision/scale or nullability drift. In
  Hibernate 7.4 it checks that tables and columns exist, that each column's base type matches
  (arguments stripped: `varchar(50)` vs `varchar(100)` passes), and that sequences exist with the
  right increment. Index and unique-key checks exist since 7.3 but are off by default; we turn
  both on (see "Transactions and configuration"). Cover critical lengths and
  nullability with a repository test against Testcontainers. Verified in `java25-sample` on
  PostgreSQL 18: a `varchar(50)` column under `@Size(max = 100)`, and a nullable column under
  `@NotNull`, both start up without error.
- Nullability must agree in three places: JSpecify (no `@Nullable` ⇔ required), `@NotNull` on the
  entity, `NOT NULL` in the migration.
- Keep Hibernate's defaults: don't set `jakarta.persistence.validation.mode` or
  `hibernate.check_nullability`.

### `equals` / `hashCode`

Follow Hibernate's guidance: base them on a natural/business key when one exists (matching a DB
unique constraint); otherwise compare the id and return a constant hash code. Use `instanceof`
(proxies are subclasses) and accessors, not field access, on the other object:

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Customer other)) return false;
    return getId() != null && getId().equals(other.getId());
}

@Override
public int hashCode() {
    return Customer.class.hashCode();
}
```

Never include mutable fields or lazy associations in `equals`/`hashCode`/`toString`.

### Associations and fetching

- **Every association is `LAZY`.** Write `@ManyToOne(fetch = FetchType.LAZY)` and
  `@OneToOne(fetch = FetchType.LAZY)` explicitly; JPA's default for them is EAGER, which Hibernate
  calls "a very unfortunate misfeature". Never `FetchType.EAGER` on collections.
- Load what a use case needs explicitly: `JOIN FETCH` in JPQL, `@EntityGraph` on repository
  methods, or DTO projections. Watch for N+1 in tests (enable SQL logging or a query counter).
- Prefer `Set` or ordered `List` with `@OrderBy` for `@OneToMany`; keep bidirectional
  associations in sync with helper methods (`addItem`/`removeItem`) on the owning side.
- Every `@OneToMany` has `mappedBy` (bidirectional) or `@JoinColumn` (unidirectional); never the
  default join-table mapping.
- Don't call `EntityManager.merge()` on managed entities; if you do merge a detached one, use the
  returned instance, not the argument.
- Prefer unidirectional `@ManyToOne` over `@OneToMany` when the collection isn't navigated.
- Avoid `@ManyToMany` with business data on the link; model the link as an entity.
- `CascadeType.ALL`/`orphanRemoval` only from aggregate root to its owned children.

### Repositories and queries

- Extend `JpaRepository<T, ID>` (or `ListCrudRepository`). Don't annotate repository interfaces
  with `@Repository`.
- Derived queries for simple cases; `@Query` (JPQL in a text block) when the name gets long.
  Never build JPQL/SQL with string concatenation of input; use parameters.
- Read-only screens: **record projections** (Spring Data recommends records for DTO projections),
  with a single constructor.
- Paginate any query that can grow: `Pageable` + `Page`/`Slice`. Never return unbounded lists
  from API endpoints.
- `getReferenceById(id)` when you only need a reference to set an association.

### Transactions and configuration

- `@Transactional` on service methods (the use-case boundary), not on controllers or
  repositories. `@Transactional(readOnly = true)` for read-only service methods.
- `spring.jpa.open-in-view=false`, always. Fix `LazyInitializationException` with fetch joins,
  entity graphs or projections, never by re-enabling OSIV or switching to EAGER.
- Schema is owned by Flyway: `spring.jpa.hibernate.ddl-auto=validate` in every environment,
  including tests. Never `none`, `update`, `create` or `create-drop`.
- Validate unique constraints and indexes at startup (Hibernate 7.3+, both default `none`):

  ```properties
  spring.jpa.properties.hibernate.tooling.schema.unique_key_validation=all
  spring.jpa.properties.hibernate.tooling.schema.index_validation=all
  ```

  Each mapped unique key / index is looked up **by name**, then its columns are compared in
  order; startup fails with "Missing unique constraint" / "Missing index" or a mismatch error
  (verified in `java25-sample` on PostgreSQL 18: a renamed index, a renamed unique key and an
  index on the wrong column each fail startup).
  `all` rather than `named`, because `named` silently skips names starting with `UK` / `IDX`
  (Hibernate's generated prefixes). This only works with explicit names that match the migration:
  - Unique constraints: see "Constraints: one owner per layer".
  - Indexes: mirror the migration's indexes in the mapping with the same name, e.g.
    `@Table(indexes = @Index(name = "idx_order_customer_id", columnList = "customer_id"))`.
    Never an unnamed `@Index`. Only mapped indexes are checked, so add every index the
    service's queries depend on.
- Don't call external systems (HTTP, Kafka, email) inside a DB transaction; publish after
  commit (`@TransactionalEventListener(phase = AFTER_COMMIT)`) or use an outbox.

## 8. Validation (Jakarta Validation 3.1 / Hibernate Validator 9)

- Put constraints on request **records**: `record CreateUserRequest(@NotBlank @Email String email, @Size(max = 100) String name) {}`.
- Controllers: `@Valid @RequestBody`. Put constraints like `@Positive`, `@NotBlank` directly on
  `@PathVariable` / `@RequestParam` parameters; Spring MVC validates them natively (since
  Framework 6.1).
- **Don't put class-level `@Validated` on controllers.** It routes validation through an AOP proxy
  and bypasses MVC's built-in method validation (Spring docs say to remove it).
- Services/other beans that validate method arguments: class-level `@Validated` plus constraints
  on parameters.
- Handle both `MethodArgumentNotValidException` and `HandlerMethodValidationException`.
- Validation is for input shape. Business rules (uniqueness, state transitions) live in the
  domain/service and throw domain exceptions.
- Entities also carry structural constraints; see "Constraints: one owner per layer" (section 7).
- Custom constraints: annotation + `ConstraintValidator`; keep validators stateless.

## 9. Web layer and errors

- Controllers are thin: map DTO → call service → map result. No repositories or business logic in
  controllers.
- Return records or `ResponseEntity<Record>`; use correct status codes (201 + `Location` on create,
  204 on empty success).
- Paged endpoints return `PagedModel<T>` (`new PagedModel<>(page)`), never a raw Spring Data
  `Page`, whose JSON shape is not a stable contract.
- Errors use **RFC 9457 Problem Details** (`ProblemDetail`) from exactly one
  `@RestControllerAdvice` that extends `ResponseEntityExceptionHandler` (which renders Spring MVC's
  own exceptions as problem details) and maps every domain exception. Never leak stack traces,
  SQL or entity internals.
- API versioning, when needed, uses Framework 7's built-in support (`spring.mvc.apiversion.*`),
  not hand-rolled path/header parsing.

## 10. Security (Spring Security 7)

- Configure with a `SecurityFilterChain` `@Bean`. `WebSecurityConfigurerAdapter`,
  `authorizeRequests()`, `.and()` chaining and `HttpSecurity#apply(...)` are removed: use the
  **lambda DSL**, `authorizeHttpRequests(...)`, and `with(...)` for custom DSLs.
- No `throws Exception` on the bean method: in Spring Security 7.1 `build()` and the DSL methods
  no longer declare checked exceptions (SpotBugs flags the leftover clause as
  `THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION`).

```java
@Bean
SecurityFilterChain api(HttpSecurity http) {
    return http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health/**").permitAll()
                    .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .build();
}
```

- Request matching uses `PathPatternRequestMatcher` (the default). Don't use
  `AntPathRequestMatcher` / `MvcRequestMatcher` (removed). Patterns are absolute paths without the
  context root.
- Deny by default: end with `.anyRequest().authenticated()` (or `denyAll()`); list public endpoints
  explicitly.
- Method security: `@EnableMethodSecurity` + `@PreAuthorize`. Never `@EnableGlobalMethodSecurity`.
- Passwords (if stored locally at all): `PasswordEncoderFactories.createDelegatingPasswordEncoder()`.
  Never `NoOpPasswordEncoder`, never plain hashing.
- CSRF: keep it on for browser/session-based apps; disable only for stateless token-authenticated
  APIs, and say so in a comment.
- Secrets come from the environment/secret store, never from committed `application*.yml`.
- Test with `spring-boot-starter-security-test`: `@WithMockUser`, `jwt()` / `oidcLogin()` request
  post-processors.

## 11. Testing

- JUnit Jupiter + AssertJ (versions managed by Boot). One behavior per test; names describe it.
- Unit tests for domain/services without Spring. Use `@ExtendWith(MockitoExtension.class)` for
  plain Mockito mocks.
- Slices: `@WebMvcTest` for controllers (with `MockMvcTester` for AssertJ-style assertions),
  `@DataJpaTest` for repositories. `@SpringBootTest` only for real integration tests.
- `@MockitoBean` / `@MockitoSpyBean` (Spring Framework). `@MockBean`/`@SpyBean` are removed.
- Load Mockito as a Java agent in Surefire (`maven-dependency-plugin:properties` +
  `<argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>`, with an empty
  `<argLine/>` property). On JDK 25 Mockito otherwise warns that self-attaching "will no longer
  work in future releases of the JDK".
- `@SpringBootTest` doesn't set up MockMvc: add `@AutoConfigureMockMvc` when needed. HTTP-level
  integration tests use `RestTestClient` (`@AutoConfigureRestTestClient`); never
  `TestRestTemplate`.
- Databases in tests: **Testcontainers 2** with `@ServiceConnection`, same engine as production.
  No H2 standing in for PostgreSQL.
- Test validation failures, authorization rules (401/403) and N+1-prone queries explicitly.

## 12. Code style

- **Google Java Style, formatted by google-java-format** (section 13). Never hand-format, never
  reformat to another style. No wildcard or module imports. `@Override` always. Braces always.
- Acronyms as words in names: `HttpClient`, `UserId`, `parseXml` (not `HTTPClient`).
- Caught exceptions are never ignored; an empty catch needs a comment explaining why. A caught
  `InterruptedException` restores the flag (`Thread.currentThread().interrupt()`) before
  rethrowing as unchecked.
- Throw unchecked, domain-specific exceptions; never throw or catch `Exception`,
  `RuntimeException` or `Throwable` generically. Wrap checked exceptions at the boundary, always
  passing the cause.
- Favor immutability: `final` fields, records, unmodifiable collections. Mutable state only in
  entities. Never return a mix of mutable and immutable collections from one method.
- Declare variables, parameters and return types by interface (`List`, `Map`), not implementation.
- No `System.out` / `System.err` / `printStackTrace` / `java.util.logging`; SLF4J only.
- No deprecated APIs (ours or the platform's).
- Documentation comments use Markdown (`///`, JEP 467, Java 23+), which Google style accepts and
  google-java-format formats. Javadoc on public APIs that aren't self-explanatory; comments explain
  *why*, not *what*.

## 13. Static analysis and formatting (mandatory, build fails on any finding)

Every rule above that a tool can check is checked by one; the build (local and CI) fails on any
violation. Use the latest release of each tool; versions below are the latest as of 2026-10-13.

Reference implementation: `java25-sample/` next to this file (Spring Boot 4.1.1, Maven). It
builds green with `mvnw verify` on JDK 25 (Corretto 25.0.4.1), and each gate was confirmed to fail
the build on a planted violation. Copy its `pom.xml`, `.mvn/jvm.config` and `config/` when
starting a service.

| Tool | Version | Role |
|---|---|---|
| google-java-format via Spotless | gjf **1.36.1**; Spotless Maven 3.10.4 / Gradle 8.10.4 | Formatting. CI runs `spotlessCheck`; developers run `spotlessApply`. No JVM flags needed: Spotless opens the `jdk.compiler` packages gjf needs at runtime (`ModuleHelper`; on JDK 25 this prints a `sun.misc.Unsafe` deprecation warning, which is harmless). Pin gjf to 1.36.1: 1.37.0 fails every file in Spotless with `InvocationTargetException` (diffplug/spotless#3126, fixed in gjf but unreleased as of 2026-10-13). |
| Error Prone | 2.50.0 (runs on JDK 21+) | Compile-time bug patterns and modern-idiom checks |
| NullAway | 0.14.2 (+ `org.jspecify:jspecify:1.0.0`) | Compile-time null-safety |
| PMD | 7.28.0 (Java 25 since 7.16.0) | Design and modernisation rules Error Prone lacks |
| SpotBugs | 4.10.5 (Maven plugin 4.10.5.0, Gradle `com.github.spotbugs` 6.5.13) | Bytecode analysis: resource leaks, concurrency, null flow |
| ArchUnit | 1.5.1 (`archunit-junit6`; Boot 4.1.1 manages JUnit Jupiter 6.0.3) | Architecture rules as unit tests |

Not used: **Checkstyle** (layout is the formatter's job; naming/import/switch checks are covered
by Error Prone, PMD and the compiler), **find-sec-bugs** (no release since 2025-06, no Java 25
or SpotBugs 4.10 support stated), **OpenRewrite** (its recipes target migrations, which greenfield
code doesn't need), **Lombok** (section 5).

### javac + Error Prone + NullAway

- javac: `-Xlint:all,-processing,-classfile -Werror`. Error Prone runs inside javac and reports
  through it, so `-Werror` ("terminates compilation when warnings occur") makes every Error Prone
  warning fail the build. Two lint categories are excluded because they report on dependencies,
  not on our code:
  - `processing`: "No processor claimed any of these annotations".
  - `classfile`: e.g. "Cannot find annotation method 'value()' in type
    'kotlin.jvm.internal.SourceDebugExtension'" when javac reads Kotlin-compiled jars on the
    Boot 4.1 classpath.
- List every annotation processor explicitly (Error Prone, NullAway,
  `spring-boot-configuration-processor`, `hibernate-processor`): configuring a processor path
  disables classpath discovery, and JDK 23+ doesn't discover them anyway.
- Maven needs the `--add-exports`/`--add-opens` lines for `jdk.compiler` in `.mvn/jvm.config`
  (see errorprone.info/docs/installation); the Gradle plugin `net.ltgt.errorprone` (5.1.1) adds
  them itself.
- Flags go **inline, space-separated, in the single `-Xplugin:ErrorProne ...` argument** (shown one
  per line below for readability). Don't use an Error Prone argument file
  (`-Xplugin:ErrorProne @file`) with Maven's in-process compiler: maven-compiler-plugin 3.15.0
  fails with "The parameter file cannot reference another parameter file".
- `-XepExcludedPaths` must match both path separators (`[\\/]`), or generated sources are not
  excluded on Windows.

```text
-XepDisableWarningsInGeneratedCode
-XepExcludedPaths:.*[\\/](generated-sources|generated-test-sources|generated)[\\/].*
# NullAway: error, every package @NullMarked, full JSpecify semantics
-Xep:NullAway:ERROR
-XepOpt:NullAway:OnlyNullMarked=true
-XepOpt:NullAway:JSpecifyMode=true
-XepOpt:NullAway:CustomContractAnnotations=org.springframework.lang.Contract
-XepOpt:NullAway:CheckOptionalEmptiness=true
# Off by default in Error Prone; on for us
-Xep:UnnecessaryDefaultInEnumSwitch:WARN
-Xep:FieldCanBeFinal:WARN
-Xep:FieldCanBeLocal:WARN
-Xep:UnusedException:WARN
-Xep:InterruptedExceptionSwallowed:WARN
-Xep:SystemOut:WARN
-Xep:AnnotationPosition:WARN
-Xep:PreferJavaTimeOverload:WARN
-Xep:DefaultLocale:WARN
-Xep:UnnecessaryOptionalGet:WARN
-Xep:UnnecessaryAnonymousClass:WARN
-Xep:UnnecessaryBoxedVariable:WARN
-Xep:ConstantPatternCompile:WARN
-Xep:YodaCondition:WARN
-Xep:UseCorrectAssertInTests:ERROR
```

  Already on by default and relied on (don't disable): `StatementSwitchToExpressionSwitch`,
  `IfChainToSwitch`, `PatternMatchingInstanceof`, `UnnamedVariable`, `TraditionalSwitchExpression`,
  `MissingCasesInEnumSwitch`, `StringConcatToTextBlock`, `JavaUtilDate`, `JdkObsolete`,
  `JavaTimeDefaultTimeZone`, `DefaultCharset`, `StringCaseLocaleUsage`, `MixedMutabilityReturnType`,
  `EqualsGetClass`, `OptionalNotPresent`, `NullOptional`, `NullableOptional`, `UnusedVariable`,
  `UnusedMethod`, `MissingOverride`, `ReturnValueIgnored`, `CheckReturnValue`.
- `JSpecifyMode` is documented by NullAway and Spring as "still under development" and may
  report false positives. We accept that for the stricter generics/array checking; suppress a
  confirmed false positive per the null-safety rule in section 4, never by turning the mode off.
- Never `-XepAllDisabledChecksAsWarnings` (pulls in Google-internal and contradictory checks) and
  never `-XepAllErrorsAsWarnings`.

### PMD

Run a curated ruleset (not whole categories), failing on any violation (Gradle:
`ignoreFailures = false`, `ruleSets = []` with `ruleSetFiles`):

- bestpractices: `UseEnumCollections`, `LooseCoupling`, `SimplifiableTestAssertion`,
  `UnusedAssignment`, `UseTryWithResources`, `UseStandardCharsets`, `RelianceOnDefaultCharset`,
  `PrimitiveWrapperInstantiation`, `ForLoopCanBeForeach`, `DoubleBraceInitialization`,
  `ExhaustiveSwitchHasDefault`, `NonExhaustiveSwitch`, `UnnecessaryVarargsArrayCreation`,
  `UnnecessaryWarningSuppression`, `UseCollectionIsEmpty`, `ReplaceVectorWithList`,
  `ReplaceHashtableWithMap`, `ReplaceEnumerationWithIterator`
- codestyle: `UseDiamondOperator`, `LambdaCanBeMethodReference`, `UnnecessaryModifier`,
  `UnnecessaryFullyQualifiedName`, `VariableCanBeInlined`, `UnnecessaryBoxing`
- design: `InstantiableUtilityClass`, `CognitiveComplexity`, `NPathComplexity`, `GodClass`
- errorprone: `ReplaceJavaUtilDate`, `ReplaceJavaUtilCalendar`, `UseLocaleWithCaseConversions`,
  `UnsupportedJdkApiUsage`, `ReturnEmptyCollectionRatherThanNull`, `ImplicitSwitchFallThrough`

Excluded on purpose: `UseExplicitTypes` (contradicts our `var` rule), `AvoidSynchronizedAtMethodLevel`
and `AvoidSynchronizedStatement` (pinning rationale obsolete since JEP 491; see section 3), the
deprecated `UseUtilityClass` (renamed `InstantiableUtilityClass` in 7.27.0).

PMD runs on tests too (`includeTests`). The ArchUnit test class trips `InstantiableUtilityClass`
(all members are static `@ArchTest` fields, but JUnit instantiates it); suppress it there with
`@SuppressWarnings("PMD.InstantiableUtilityClass")` and a comment saying why.

### SpotBugs

- Effort `max`, threshold `low`, build fails on any bug.
- Exclude only `EI_EXPOSE_REP` and `EI_EXPOSE_REP2`, via the filter file, because they misfire on
  records and Spring beans even with defensive copies (open SpotBugs issue #3022). Our immutability
  rules (records, `List.copyOf`) cover what they guard against.
- Also exclude `UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD` on classes matching `~.*_` only: the
  Jakarta Persistence static metamodel generated by `hibernate-processor` (`Customer_`) has public
  attribute fields that stay unused until a criteria query references them.
- Any other suppression is a `@SuppressFBWarnings(value = "...", justification = "...")` on the
  smallest element.

### ArchUnit

One `ArchitectureTest` per service, run with the unit tests:

- `GeneralCodingRules`: `NO_CLASSES_SHOULD_USE_FIELD_INJECTION`,
  `NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING`, `NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS`,
  `NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS`, `NO_CLASSES_SHOULD_USE_JODATIME`,
  `OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED`, `DEPRECATED_API_SHOULD_NOT_BE_USED`.
- Project rules: controllers don't depend on repositories; `@Entity` types aren't referenced from
  the web package; nothing depends on `lombok..`, `org.springframework.lang..` nullability
  annotations, `javax..` (except `javax.sql`/`javax.crypto` and other JDK-only `javax` packages),
  or `org.springframework.web.client.RestTemplate`.

### IDE

IntelliJ IDEA with the "Java language level migration aids" inspections for Java 14–25 enabled
(e.g. `EnhancedSwitchMigration`, `TextBlockMigration`, `ClassCanBeRecord`,
`PatternVariableCanBeUsed`, `SequencedCollectionMethodCanBeUsed`, `DeconstructionCanBeUsed`,
`MathClampMigration`, `MarkdownDocumentationCommentsMigration`) plus
`OptionalUsedAsFieldOrParameterType`. If SonarQube is available, use the Sonar way profile and
also activate the off-by-default `S3553` (Optional parameters), `S9404` (Optional wrapping
collections), `S5194` (switch expressions) and `S3306` (constructor injection).

## Sources

- Oracle Java SE Support Roadmap: https://www.oracle.com/java/technologies/java-se-support-roadmap.html
- JDK 25 `Optional` API docs: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Optional.html
- OpenJDK `var` style guide: https://openjdk.org/projects/amber/guides/lvti-style-guide
- Google Java Style Guide: https://google.github.io/styleguide/javaguide.html
- Spring Boot system requirements: https://docs.spring.io/spring-boot/system-requirements.html
- Spring Boot 4.0 migration guide: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- Spring Boot 4.0 / 4.1 release notes: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes , https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.1-Release-Notes
- Spring Boot dependency injection: https://docs.spring.io/spring-boot/reference/using/spring-beans-and-dependency-injection.html
- Spring Boot externalized configuration: https://docs.spring.io/spring-boot/reference/features/external-config.html
- Spring Boot virtual threads: https://docs.spring.io/spring-boot/reference/features/spring-application.html
- Spring Framework null-safety (JSpecify): https://docs.spring.io/spring-framework/reference/core/null-safety.html
- Spring MVC validation: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html
- Spring Framework REST clients: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html
- Spring Data JPA null handling: https://docs.spring.io/spring-data/jpa/reference/repositories/null-handling.html
- Spring Data JPA projections: https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html
- Spring Security 7 migration (configuration, web): https://docs.spring.io/spring-security/reference/6.5/migration-7/configuration.html , https://docs.spring.io/spring-security/reference/6.5/migration-7/web.html
- Hibernate ORM introduction: https://docs.hibernate.org/orm/7.0/introduction/html_single/Hibernate_Introduction.html
- JetBrains IntelliJ IDEA, JPA Buddy entity designer (Lombok on entities): https://www.jetbrains.com/help/idea/2026.2/jpa-buddy-entity-designer.html
- Lombok changelog: https://projectlombok.org/changelog
- JEP 491 (synchronized without pinning, JDK 24): https://openjdk.org/jeps/491
- JDK 23 release notes (JDK-8321314, implicit annotation processing off): https://www.oracle.com/java/technologies/javase/23-relnote-issues.html
- JetBrains inspection OptionalUsedAsFieldOrParameterType: https://www.jetbrains.com/help/inspectopedia/OptionalUsedAsFieldOrParameterType.html
- Spring MVC `@RequestParam`: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestparam.html
- Spring Framework MockMvc AssertJ (`MockMvcTester`): https://docs.spring.io/spring-framework/reference/testing/mockmvc/assertj.html
- Spring Boot testing (`MockMvcTester`, `RestTestClient` auto-config): https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html
- Spring Boot servlet web (Problem Details property): https://docs.spring.io/spring-boot/reference/web/servlet.html
- Hibernate Validator 9 reference, §2.3.1 (DDL impact) and §11.1 (ORM integration): https://docs.hibernate.org/validator/9.0/reference/en-US/html_single/
- Hibernate ORM `ValidationSettings` (`check_nullability` default): https://docs.hibernate.org/orm/7.0/javadocs/org/hibernate/cfg/ValidationSettings.html
- Hibernate ORM 7.4 schema validator source (`AbstractSchemaValidator`, `ColumnDefinitions`, `ConstraintValidationType`): https://github.com/hibernate/hibernate-orm/tree/7.4/hibernate-core/src/main/java/org/hibernate/tool/schema/internal
- Error Prone installation, flags, bug patterns, releases: https://errorprone.info/docs/installation , https://errorprone.info/docs/flags , https://errorprone.info/bugpatterns , https://github.com/google/error-prone/releases
- Gradle Error Prone plugin: https://github.com/tbroyer/gradle-errorprone-plugin
- NullAway (configuration, JSpecify support, changelog): https://github.com/uber/NullAway/wiki/Configuration , https://github.com/uber/NullAway/wiki/JSpecify-Support , https://github.com/uber/NullAway/blob/master/CHANGELOG.md
- PMD Java rules and Java language support: https://docs.pmd-code.org/latest/pmd_rules_java.html , https://docs.pmd-code.org/latest/pmd_languages_java.html
- SpotBugs releases and EI_EXPOSE_REP issue: https://github.com/spotbugs/spotbugs/releases , https://github.com/spotbugs/spotbugs/issues/3022
- find-sec-bugs releases: https://github.com/find-sec-bugs/find-sec-bugs/releases
- Checkstyle Google style coverage: https://checkstyle.org/google-style.html
- google-java-format: https://github.com/google/google-java-format
- Spotless × google-java-format 1.37.0 incompatibility: https://github.com/diffplug/spotless/issues/3126
- Spotless: https://github.com/diffplug/spotless
- ArchUnit (`GeneralCodingRules`, releases): https://github.com/TNG/ArchUnit
- IntelliJ "Java language level migration aids": https://www.jetbrains.com/help/inspectopedia/Java-Java-language-level-migration-aids.html
- javac `-Werror` / `-Xlint`: https://docs.oracle.com/en/java/javase/25/docs/specs/man/javac.html
- Spotless `ModuleHelper` (opens `jdk.compiler` for gjf): https://github.com/diffplug/spotless/blob/main/lib/src/main/java/com/diffplug/spotless/java/ModuleHelper.java
- Spring Boot 4.1.1 managed versions (`junitJupiterVersion=6.0.3`): https://github.com/spring-projects/spring-boot/blob/v4.1.1/gradle.properties
- Sonar Java rules and Sonar way profile (analyzer source): https://github.com/SonarSource/sonar-java
- OpenRewrite migration recipes: https://docs.openrewrite.org/recipes/java/migrate/upgradetojava25
- Jackson Java 8 modules merged into databind in 3.0: https://github.com/FasterXML/jackson-modules-java8
