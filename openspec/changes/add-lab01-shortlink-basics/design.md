# Design

## Context

Каркас: только `Application.kt` (`@SpringBootApplication`). Зависимости для
решения уже в pom: `spring-boot-starter-web`, `spring-boot-starter-validation`,
`jackson-module-kotlin`. Тесты C1–C13 существуют и задают контракт дословно
(статусы + точные тексты ошибок). Спецификация поведения — в delta spec
`specs/short-links/spec.md`; постановка — `docs/labs/lab01/lab01.md`.

## Goals / Non-Goals

**Goals:**

- Прямолинейная реализация контракта C1–C13 без «улучшений» сверх него.
- Демонстрация конвенций проекта: 3 слоя, constructor injection, DTO ≠ entity,
  `@ControllerAdvice` с единым `{"error": "..."}`.

**Non-Goals:**

- Персистентность, конфигурируемый base URL, дедупликация, health-check
  (см. proposal — Out of scope).
- Продакшн-уровень устойчивости: ретраи коллизий фиксированы постановкой
  (3 попытки → 500), распределённость не рассматривается.

## Decisions

**D1. Три слоя: controller → service → storage.**

`LinkController` + `GlobalExceptionHandler` / `LinkService` / `LinkRepository`.
Альтернатива — держать `ConcurrentHashMap` прямо в сервисе (два слоя). Выбрано
три: постановка и правило proposal «Identify affected layers
(controller / service / storage)» используют именно эту номенклатуру; для
учебного demo это делает границы слоёв наблюдаемыми. Репозиторий — конкретный
класс с `ConcurrentHashMap<String, Link>` (без интерфейса: одна реализация,
YAGNI).

**D2. Валидация `longUrl` — Bean Validation на DTO.**

`CreateLinkRequest(val longUrl: String?)` с аннотациями
`@NotBlank(message = "longUrl is required")` (закрывает C2–C4: единый текст
сделал null-vs-blank неразличимыми в контракте), `@Pattern(regexp = "^https?://.*")`
(C5), `@Size(max = 2048)` (C6); сообщения задаются в аннотациях дословно.
Контроллер принимает `@Valid @RequestBody`. Хендлер
`MethodArgumentNotValidException` в advice возвращает сообщение первой
field-ошибки. Альтернатива — ручная цепочка проверок в сервисе: давала бы
гарантированный порядок сообщений, но при объединённом тексте C2–C4
перекрывающиеся нарушения (не тестируются) не влияют на контракт; аннотации —
идиоматичный Spring и меньше кода, starter-validation уже в pom.

**D3. Ошибки парсинга и Content-Type — через `@ControllerAdvice`.**

`HttpMessageNotReadableException` → `400 "malformed request body"` (C7),
`HttpMediaTypeNotSupportedException` → `415 "unsupported media type"` (C8).
Аннотациями валидации это не закрывается — advice нужен в любом случае.
Плюс fallback-хендлер, чтобы любой необработанный ошибочный ответ также
возвращал `{"error": "..."}`.

**D4. Редирект без валидации кода.**

`GET /{code}` просто ищет код в хранилище: null (не найден / вне Base62 /
другой регистр) → `404 "short code not found"`; найден → `302` + `Location`.
Три ошибки контракта (C10–C12) схлопываются в lookup. `404` возвращается
напрямую из контроллера (нормальный исход, не исключение).

**D5. Генерация кода — `SecureRandom` по алфавиту Base62.**

Ровно 6 символов `[0-9a-zA-Z]`; при коллизии в хранилище — до 3 попыток,
затем `IllegalStateException` → advice → `500` (постановка: «в тестах не
проверяем»). `SecureRandom` вместо `Random`: предсказуемость кодов ссылок —
реальная уязвимость перебора, и замена тривиальна.

**D6. Base URL и `Location` — захардкожены.**

Константа `http://localhost:8080` для `shortUrl`; header `Location: /{shortCode}`
— относительный путь (требование постановки). Вынос в конфиг — lab02
(MODIFIED requirement), не делаем «заранее правильно».

## Поток создания ссылки (POST /links)

```
Client                 LinkController            LinkService            LinkRepository
  |  POST /links            |                        |                       |
  |  {"longUrl": "..."}     |                        |                       |
  |------------------------>| @Valid @RequestBody    |                       |
  |                         |  (415/400 -> advice)   |                       |
  |                         |----------------------->| create(longUrl)       |
  |                         |                        |-- генерация кода ---->|  [до 3 попыток]
  |                         |                        |<-- коллизия? ---------|
  |                         |                        |-- save(code, Link) -->|
  |                         |<-- LinkResponse -------|                       |
  |<-- 201 + Location ------|  {shortCode, shortUrl} |                       |
  |    /{shortCode}         |                        |                       |
```

## Risks / Trade-offs

- [Порядок field-ошибок Bean Validation не гарантирован спецификацией] →
  перекрытие нарушений не тестируется (C2–C6 взаимоисключающие); при
  необходимости детерминизма — ручная валидация (D2, альтернатива).
- [In-memory хранилище теряет данные при рестарте] → осознанное ограничение
  постановки; персистентность — отдельная лаба.
- [Гонка при параллельном `putIfAbsent`-подобном сценарии] → для lab01
  достаточно; атомарность не входит в контракт (C13 требует лишь различных
  кодов, что даёт случайная генерация).
- [Хардкод base URL ломает запуск на другом порту] → зафиксировано в
  постановке; исправляется в lab02 выносом в конфиг.

## Migration Plan

Развёртывание не требуется (учебный локальный запуск `mvn spring-boot:run`).
Откат — revert ветки (см. proposal — Rollback Plan).

## Open Questions

Нет: развилка «2 или 3 слоя» и стратегия валидации решены (D1, D2).
