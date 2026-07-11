---
name: java
description: Functional Java coding style — Optional composition, streams, records, immutability, custom exceptions, and minimal try scope. Use when writing or reviewing Java code, adding new classes, or discussing Java architecture patterns.
---

# Java Conventions

These conventions apply to all Java code in this project. Consult them before writing or reviewing any Java.

## Optional

Use `Optional` as a **return type only** — never as a parameter type or field type. A parameter typed as `Optional` is a signal that the caller should be doing validation before the call; fix the call site instead.

Always consume `Optional` monadically. Never call `.isPresent()` then `.get()` — that is an explicit null check wearing a costume.

```java
// banned
if (result.isPresent()) {
    process(result.get());
}

// required
result.map(this::process).orElse(defaultValue);
result.ifPresent(this::process);
result.flatMap(this::lookup).orElseThrow(NotFoundException::new);
```

Prefer `.map()`, `.flatMap()`, `.filter()`, `.orElse()`, `.orElseGet()`, `.orElseThrow()`, `.ifPresent()`.

## Null Checks

Prefer `Objects.nonNull()` and `Objects.isNull()` over explicit `x != null` / `x == null`. The primary use case is as a method reference in streams at boundaries with APIs that don't use `Optional`:

```java
// at a boundary with a non-Optional API
List<Odds> filtered = rawResults.stream()
    .filter(Objects::nonNull)
    .collect(toList());
```

Inside business logic, if you find yourself doing null checks, prefer restructuring to use `Optional` on the return type of the upstream method instead.

## Streams

Use streams for all data transformation work. Never use indexed or enhanced for-loops for transforming or filtering collections.

```java
// banned
List<GameEvent> result = new ArrayList<>();
for (RawEvent raw : events) {
    if (raw.isValid()) {
        result.add(transform(raw));
    }
}

// required
List<GameEvent> result = events.stream()
    .filter(RawEvent::isValid)
    .map(this::transform)
    .collect(toList());
```

**Side-effecting iteration inside a stream is an architectural antipattern.** If you find yourself calling `stream().forEach(kafka::send)` or similar, the design is wrong — stop and fix the design, not the syntax. Streams are for transformations; side effects belong at a defined boundary, not inside a pipeline.

## Records

Use `record` as the default for all data-carrying types. Records are immutable, concise, and eliminate boilerplate. Prefer them unless a framework explicitly prevents it (e.g., JPA entities that require a no-arg constructor and mutable fields).

```java
// preferred
record OddsSnapshot(String gameId, double homeOdds, double awayOdds, Instant capturedAt) {}

// only when forced by a framework
class MutableEntity { ... }
```

Immutability is the default. When you need to "modify" a record, return a new instance. If you find yourself wanting mutable state in a data type, question the design first.

## Custom Exceptions

Use custom unchecked exceptions (extending `RuntimeException`) for all domain-specific failures. Never throw raw `RuntimeException`, `IllegalArgumentException`, or `IllegalStateException` from business logic — name the failure explicitly.

One exception type per distinct failure domain:

```java
public class OddsFetchException extends RuntimeException {
    public OddsFetchException(String message) { super(message); }
    public OddsFetchException(String message, Throwable cause) { super(message, cause); }
}

public class GameEventParseException extends RuntimeException {
    public GameEventParseException(String message, Throwable cause) { super(message, cause); }
}
```

Custom exceptions are unchecked because checked exceptions cannot propagate cleanly through stream lambdas and force callers to handle errors they cannot meaningfully recover from at the call site.

## Try Blocks — Minimal Scope

A try block should contain only the operation that can actually fail — not the setup code leading to it. If setup code is inside the try, you cannot tell which line threw.

```java
// banned — setup is not risky
try {
    String url = buildUrl(gameId);
    Map<String, String> headers = buildHeaders();
    Response response = client.fetch(url, headers);
} catch (OddsFetchException e) { ... }

// required — only the risky call
String url = buildUrl(gameId);
Map<String, String> headers = buildHeaders();
try {
    Response response = client.fetch(url, headers);
} catch (OddsFetchException e) { ... }
```

If multiple operations can each fail independently, give each its own try block with its own catch. Do not batch unrelated risky calls under one catch.

## Functional Interfaces Over Static Methods

Never put behavior in `static` methods on utility classes. Static methods cannot be injected, swapped, or tested in isolation.

Express behavior as `Function`, `BiFunction`, `Supplier`, or `Consumer` — implemented as classes, wired as Spring `@Bean`s, and received via constructor injection.

```java
// banned — static utility, hidden coupling, not injectable
public final class Normalizer {
    public static List<Event> normalize(Feed feed, int index) { ... }
}

// required — implements BiFunction, wired as a @Bean
public final class Normalizer implements BiFunction<Feed, Integer, List<Event>> {
    @Override
    public List<Event> apply(Feed feed, Integer index) { ... }
}

// in Spring config
@Bean
public BiFunction<Feed, Integer, List<Event>> normalizer() {
    return new Normalizer();
}

// in caller — constructor-injected, swappable in tests
public class GamePoller {
    private final BiFunction<Feed, Integer, List<Event>> normalizer;
}
```

Pick the right interface by role:

| Interface | Use when |
|---|---|
| `Function<A, B>` | transform one value to another |
| `BiFunction<A, B, C>` | combine two inputs into a result |
| `Supplier<T>` | produce a value lazily (no input) |
| `Consumer<T>` | side-effecting action on a value (e.g. send, log) |

`static` is only permitted on record factory methods (e.g. `Result.success(...)`). All other `static` usage is banned.
