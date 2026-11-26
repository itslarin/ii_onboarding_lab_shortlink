# Лаба 01: Minimal starting functionality — создание и редирект

## Цель

Реализовать сервис коротких ссылок с двумя операциями: создание короткой
ссылки и редирект по ней. Хранение — in-memory. Аутентификации,
дедупликации и статистики нет.

## Критерий приёмки

Все тесты с тегом `lab01` зелёные:
В pom.xml установите в `<test.tags>` значение lab01
или
```bash
mvn test -Dtest.tags=lab01
```

Тесты лежат в src/test/kotlin/com/example/shortlink/lab01/ и изначально падают — это
ожидаемо. Их «позеленение» и есть результат лабы.

## Доменная модель

### Link

|Поле	|Тип	|Описание |
|---|---|---|
|shortCode	|String	|6 символов Base62 ([0-9a-zA-Z]), генерируется сервисом |
|longUrl	|String	|Валидированный URL, ≤ 2048 символов |

### Хранилище: ConcurrentHashMap<String, Link>, ключ — shortCode.
Живёт в памяти процесса, при рестарте очищается.

### Генерация кода: случайная строка длиной 6 из алфавита Base62
(62^6 ≈ 5.6·10^10 комбинаций). При коллизии — до 3 повторных попыток,
затем 500 Internal Server Error (в тестах не проверяем).

## API-контракты

### POST /links — создание короткой ссылки

#### Request

Content-Type: application/json
Body:
```json
{ "longUrl": "https://example.com/some/path" }
```

#### Success — 201 Created

Header: ```Location: /{shortCode} ``` \
Content-Type: ```application/json``` \
Body: 
```json
{
  "shortCode": "aB3xY9",
  "shortUrl": "http://localhost:8080/aB3xY9"
}
```

#### Ошибки

|Условие	|Статус	|Body |
|---|---|---|
|longUrl отсутствует или null или пустой или только пробелы	|400	|{"error": "longUrl is required"} |
|longUrl не начинается с http:// или https://	|400	|{"error": "longUrl must start with http:// or https://"} |
|longUrl длиннее 2048 символов	|400	|{"error": "longUrl must be at most 2048 characters"} |
|Тело не является валидным JSON	|400	|{"error": "malformed request body"} |
|Content-Type не application/json	|415	|{"error": "unsupported media type"} |

### GET /{code} — редирект

#### Success — 302 Found

Header: ```Location: <longUrl>``` \
Body: ```отсутствует``` 

#### Ошибки

|Условие	|Статус	|Body | Комментарий |
|---|---|---|---|
|Код не найден в хранилище	|404	|{"error": "short code not found"} | |
|Код содержит символы вне Base62	|404	|{"error": "short code not found"} | |
|Код передан в другом регистре, чем сохранён	|404	|{"error": "short code not found"} |Поиск регистрозависимый (aB3xY9 и ab3xy9 — разные коды) |

## Ограничения

|Параметр	|Правило |
|---|---|
|longUrl	|not null, not blank, ≤ 2048, начинается с http:// или https:// |
|shortCode	|ровно 6 символов, [0-9a-zA-Z] |
|Base URL для shortUrl	|жёстко http://localhost:8080 (вынос в конфиг — во второй лабе) |
|Формат ошибок |единый {"error": "..."}, включая ошибки парсинга JSON и отсутствующие поля |

## Тестовые сценарии (C1–C13)

`C` = Case (тестовый сценарий). Сюда входят и основные сценарии (happy path),
и корнер-кейсы (ошибки, границы, регистр). Номер используется в
`@DisplayName` тестов для однозначной связи постановки и кода.

| # | Тип | Сценарий | Ожидание |
|---|---|---|---|
| C1 | happy | Валидный `longUrl` | `201`, `shortCode` (6 символов Base62) и `shortUrl` |
| C2 | validation | `longUrl` отсутствует в JSON | `400` |
| C3 | validation | `longUrl = ""` | `400` |
| C4 | validation | `longUrl = "   "` | `400` |
| C5 | validation | `longUrl = "ftp://example.com"` | `400` |
| C6 | boundary | `longUrl` длиной 2049 символов | `400` |
| C7 | validation | Тело невалидный JSON (`{`) | `400` |
| C8 | validation | `Content-Type: text/plain` | `415` |
| C9 | happy | `GET /{code}` для существующего кода | `302`, `Location` = исходный `longUrl` |
| C10 | not-found | `GET /{code}` для несуществующего кода | `404` |
| C11 | edge | `GET /{code}` с кодом в другом регистре | `404` |
| C12 | edge | `GET /{code}` с не-Base62 символом | `404` |
| C13 | edge | Повторный `POST /links` с тем же `longUrl` | `201`, `shortCode` **отличается** (дедупликации нет) |

Распределение по типам
|Тип	|Кол-во	|Сценарии |
|---|---|---|
|happy	    |2	|C1, C9 |
|validation	|6	|C2, C3, C4, C5, C7, C8 |
|boundary	  |1	|C6 |
|not-found	|1	|C10 |
|edge	      |3	|C11, C12, C13 |


## Вне scope лабы 01

Дедупликация: один longUrl → один shortCode
Кастомные коды
Персистентность (H2/PostgreSQL)
Статистика переходов
Вынос base URL в application.yml
Аутентификация, rate limiting

Дедупликация отсутствует намеренно: во второй лабе контракт
меняется (MODIFIED requirement) — это часть демонстрации работы
OpenSpec с изменениями.

## С чего начать

Прочитайте постановку.

В pom.xml установите в `<test.tags>` значение lab01
или
Запустите mvn test -Dtest.tags=lab01 — убедитесь, что тесты падают.

Сформулируйте изменение через OpenSpec: /opsx:propose.

Реализуйте по задачам из tasks.md, ориентируясь на тесты.

Завершите изменение: /opsx:archive.
