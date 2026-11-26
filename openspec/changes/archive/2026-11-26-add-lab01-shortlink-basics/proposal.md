# Proposal: add-lab01-shortlink-basics

## Why

Лаба 01 онбординга: в репозитории есть каркас Spring Boot-приложения
(`Application.kt`) и падающие тесты `src/test/kotlin/com/example/shortlink/lab01/`
(C1–C13, тег `lab01`). Реализации сервиса нет. Нужно реализовать минимальный
сервис коротких ссылок (создание + редирект, хранение in-memory) так, чтобы
тесты лабы стали зелёными — это критерий приёмки лабы
(`mvn test -Dtest.tags=lab01`).

Постановка: `docs/labs/lab01/lab01.md`.

## What Changes

- Добавлен REST-эндпоинт `POST /links` — создание короткой ссылки:
  валидация `longUrl`, генерация 6-символьного Base62-кода, ответ
  `201` c `shortCode`/`shortUrl` и header `Location: /{shortCode}`.
- Добавлен REST-эндпоинт `GET /{code}` — редирект: `302` + `Location: <longUrl>`
  для существующего кода, `404 {"error": "short code not found"}` во всех
  остальных случаях (не найден / не-Base62 символы / другой регистр).
- Добавлено единообразное оформление ошибок `{"error": "..."}` через
  `@ControllerAdvice`: ошибки валидации (`400`), невалидный JSON (`400`
  `malformed request body`), неподдерживаемый Content-Type (`415`).
- Добавлено in-memory хранилище `ConcurrentHashMap<String, Link>`.
- Существующие файлы не изменяются (включая `Application.kt` и тесты лабы).

## Capabilities

### New Capabilities

- `short-links`: создание короткой ссылки по `longUrl` (валидация, генерация
  Base62-кода, отсутствие дедупликации) и редирект по короткому коду
  (регистрозависимый lookup), с единым форматом ошибок `{"error": "..."}`.

### Modified Capabilities

<!-- Нет: это первая спека проекта (openspec/specs/ пуст). -->

## Impact

**Затронутые слои** (все три — новые, без изменения существующего кода):

- **controller**: новые `LinkController` (`POST /links`, `GET /{code}`) и
  `@ControllerAdvice` для единого формата ошибок; наружу только DTO.
- **service**: новый `LinkService` — валидация входа, генерация кода
  (Base62, до 3 попыток при коллизии), lookup для редиректа.
- **storage**: новое in-memory хранилище `LinkRepository`
  (`ConcurrentHashMap<String, Link>`, ключ — shortCode).

Прочее:

- Тесты: `src/test/kotlin/com/example/shortlink/lab01/` уже существуют,
  не изменяются — зелёные = приёмка.
- Зависимости pom.xml: новые **не требуются** (`spring-boot-starter-validation`,
  `jackson-module-kotlin` уже подключены).
- Доменная модель `Link(shortCode, longUrl)` не возвращается из контроллеров
  (только DTO).

**Out of scope** (прямо из постановки, «Вне scope лабы 01»):

- Дедупликация (один `longUrl` → один `shortCode`) — намеренно отсутствует;
  контракт изменится в lab02 (MODIFIED requirement).
- Кастомные (пользовательские) коды.
- Персистентность (H2/PostgreSQL) — хранилище очищается при рестарте.
- Статистика переходов.
- Вынос base URL (`http://localhost:8080`) в `application.yml`.
- Аутентификация, rate limiting.
- Health-check эндпоинт (в постановке lab01 отсутствует).

## Rollback Plan

Изменение состоит только из новых файлов (классы в
`src/main/kotlin/com/example/shortlink/`), без миграций и без изменения
существующего кода/конфигурации. Откат: revert коммитов ветки изменения —
проект возвращается к каркасу с падающими тестами lab01. Побочных эффектов
нет: состояние in-memory и исчезает при остановке процесса.
