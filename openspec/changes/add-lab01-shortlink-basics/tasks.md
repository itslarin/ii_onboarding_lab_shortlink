# Tasks

## 1. Доменная модель и хранилище (storage)

- [x] 1.1 Создать `Link.kt` (data class: `shortCode: String`, `longUrl: String`) и `LinkRepository.kt` (`ConcurrentHashMap<String, Link>`; методы `save`/`findByCode`) с constructor injection-совместимым API. Проверить: компиляция `mvn compile` проходит. Commit.
- [x] 1.2 Создать `ShortCodeGenerator.kt`: `SecureRandom`, алфавит Base62 `[0-9a-zA-Z]`, ровно 6 символов. Проверить: unit-вызов возвращает код по маске `[0-9a-zA-Z]{6}`. Commit.

## 2. Сервис (service)

- [x] 2.1 Создать DTO `CreateLinkRequest` (`longUrl: String?`, без аннотаций — валидация в сервисе, см. design D2) и `LinkResponse` (`shortCode`, `shortUrl`). Проверить: компиляция проходит, DTO не ссылаются на `Link`. Commit.
- [x] 2.2 Создать `LinkService.kt`: валидация `longUrl` (ручная цепочка в порядке контракта, дословные сообщения через `InvalidLongUrlException`); `create(longUrl)` — генерация кода с до 3 попытками при коллизии, сохранение, `LinkResponse` с `shortUrl = http://localhost:8080/{code}`; `resolve(code)` — lookup, `null` при отсутствии. Конфликт после 3 попыток → исключение → 500. Проверить: `mvn compile`. Commit.

## 3. Контроллер и единый формат ошибок (controller)

- [x] 3.1 Создать `LinkController.kt`: `POST /links` (`@Valid @RequestBody` → `201` + header `Location: /{shortCode}` + `LinkResponse`) и `GET /{code}` (`302` + `Location: <longUrl>`; `null` → `404 {"error": "short code not found"}`). Проверить: `mvn compile`. Commit.
- [x] 3.2 Создать `GlobalExceptionHandler.kt` (`@ControllerAdvice`): `InvalidLongUrlException` → `400` с сообщением нарушения; `HttpMessageNotReadableException` → `400 {"error": "malformed request body"}`; `HttpMediaTypeNotSupportedException` → `415 {"error": "unsupported media type"}`; коллизия кода → `500 {"error": ...}`; fallback для единого формата. Проверить: `mvn compile`. Commit.

## 4. Верификация приёмки

- [x] 4.1 Запустить `mvn test -Dtest.tags=lab01` — все 13 сценариев C1–C13 зелёные (LinkControllerTest + RedirectControllerTest). Commit (если были правки) или пометка «тесты зелёные без правок».
- [ ] 4.2 Прогнать полный набор `mvn test` — лаб нет, кроме lab01, зелёных тестов достаточно; убедиться, что ничего вне lab01 не затронуто (git diff: только новые файлы в `src/main/kotlin/com/example/shortlink/`). Commit.

## Workflow follow-up

- Заполнить `NOTES.md` в корне ветки (5–10 строк: что сделали, где застрял, что было неочевидно).
- Archive через `/opsx-archive` после ревью: спека переедет в `openspec/specs/short-links/`.
