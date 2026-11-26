# Spec Delta

## Purpose

Сервис коротких ссылок: создание короткой ссылки для произвольного URL и
редирект по короткому коду. Хранение in-memory, время жизни — процесс.
Единый формат ошибок `{"error": "..."}` для всех неуспешных ответов.

## ADDED Requirements

### Requirement: Создание короткой ссылки

Система SHALL принимать `POST /links` c JSON-телом `{"longUrl": "..."}` и при
валидном `longUrl` возвращать `201 Created` с JSON-телом, содержащим
сгенерированный `shortCode` (ровно 6 символов из алфавита Base62
`[0-9a-zA-Z]`) и `shortUrl` вида `http://localhost:8080/{shortCode}`, а также
header `Location: /{shortCode}`. Каждый запрос SHALL создавать новый код
(дедупликация отсутствует).

#### Scenario: Валидный longUrl создаёт ссылку (C1)
- **GIVEN** запрос с `Content-Type: application/json` и телом `{"longUrl": "https://example.com/some/path"}`
- **WHEN** выполняется `POST /links`
- **THEN** ответ `201 Created`, тело содержит `shortCode`, совпадающий с `[0-9a-zA-Z]{6}`, и `shortUrl`, совпадающий с `http://localhost:8080/[0-9a-zA-Z]{6}`, header `Location: /{shortCode}`

#### Scenario: longUrl отсутствует, null, пустой или из пробелов (C2–C4)
- **GIVEN** запрос с телом `{}`, `{"longUrl": null}`, `{"longUrl": ""}` или `{"longUrl": "   "}`
- **WHEN** выполняется `POST /links`
- **THEN** ответ `400` с телом `{"error": "longUrl is required"}`

#### Scenario: longUrl с недопустимой схемой (C5)
- **GIVEN** запрос с телом `{"longUrl": "ftp://example.com"}`
- **WHEN** выполняется `POST /links`
- **THEN** ответ `400` с телом `{"error": "longUrl must start with http:// or https://"}`

#### Scenario: longUrl длиннее 2048 символов (C6)
- **GIVEN** запрос с `longUrl` длиной 2049 символов, начинающимся с `https://`
- **WHEN** выполняется `POST /links`
- **THEN** ответ `400` с телом `{"error": "longUrl must be at most 2048 characters"}`

#### Scenario: Повторный POST с тем же longUrl (C13)
- **GIVEN** запрос с телом `{"longUrl": "https://example.com/duplicate"}`, для которого ссылка уже создавалась
- **WHEN** запрос повторяется
- **THEN** оба ответа `201`, но значения `shortCode` различаются (дедупликации нет)

### Requirement: Редирект по короткому коду

Система SHALL принимать `GET /{code}` и для существующего кода возвращать
`302 Found` с header `Location: <longUrl>` и пустым телом. Поиск SHALL быть
регистрозависимым. Для несуществующего кода, кода с символами вне Base62 и
кода в другом регистре SHALL возвращаться `404` с телом
`{"error": "short code not found"}`.

#### Scenario: Существующий код редиректит (C9)
- **GIVEN** ссылка, созданная через `POST /links` для `https://example.com/target`
- **WHEN** выполняется `GET /{shortCode}`
- **THEN** ответ `302 Found`, header `Location` равен исходному `longUrl`, тело отсутствует

#### Scenario: Несуществующий код (C10)
- **WHEN** выполняется `GET /nonexistent` (код не создавался)
- **THEN** ответ `404` с телом `{"error": "short code not found"}`

#### Scenario: Код в другом регистре (C11)
- **GIVEN** созданный код, содержащий буквы
- **WHEN** выполняется `GET` с тем же кодом, но в инвертированном регистре
- **THEN** ответ `404` с телом `{"error": "short code not found"}` (поиск регистрозависимый: `aB3xY9` и `ab3xy9` — разные коды)

#### Scenario: Код с символами вне Base62 (C12)
- **WHEN** выполняется `GET /ab_cd` (содержит `_`)
- **THEN** ответ `404` с телом `{"error": "short code not found"}`

### Requirement: Единый формат ошибок

Все неуспешные HTTP-ответы сервиса SHALL возвращать JSON-тело вида
`{"error": "<сообщение>"}`, включая ошибки разбора запроса — невалидный JSON
и неподдерживаемый Content-Type.

#### Scenario: Тело не является валидным JSON (C7)
- **GIVEN** запрос с `Content-Type: application/json` и телом `{`
- **WHEN** выполняется `POST /links`
- **THEN** ответ `400` с телом `{"error": "malformed request body"}`

#### Scenario: Content-Type не application/json (C8)
- **GIVEN** запрос с `Content-Type: text/plain`
- **WHEN** выполняется `POST /links`
- **THEN** ответ `415` с телом `{"error": "unsupported media type"}`
