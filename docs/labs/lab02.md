# Лаба 02: Дедупликация коротких ссылок

## Цель

Изменить контракт `POST /links`: если `longUrl` уже сокращали, вернуть
**существующий** `shortCode`, а не создавать новый. Это новое состояние
системы поверх реализации лабы 01.

## Критерий приёмки

```bash
mvn test -Dtest.tags=lab02
```

Тесты лежат в src/test/kotlin/lab02/ и изначально падают — это
ожидаемо. Их «позеленение» и есть результат лабы.

## Контракт POST /links (новое состояние)

|Ситуация	|Статус	|Body |
|---|---|---|
|longUrl |новый (ещё не сокращали)	|201 Created	{"shortCode": "...", "shortUrl": "..."}|
|longUrl |уже есть в хранилище	|200 OK	{"shortCode": "<тот же>", "shortUrl": "<тот же>"}|
|Невалидный longUrl	|400	|{"error": "..."}|
|Неверный Content-Type	|415	|{"error": "unsupported media type"}|

### Точное совпадение: сравнение longUrl — побайтовое (String.equals),
без нормализации (регистр домена, завершающие слеши, query-параметры не
трогаем).

### Порядок проверок: сначала валидация longUrl, затем поиск в хранилище.
Невалидный URL никогда не попадает в хранилище и не участвует в
дедупликации.

## Что остаётся неизменным
- Валидация longUrl: not null, not blank, ≤ 2048, начинается с http://
или https://.
- GET /{code} и его поведение (редирект, 404 для ненайденных кодов).
- Формат ошибок: единый {"error": "..."}.

## Тестовые сценарии (C1–C15)

| # | Тип | Сценарий | Ожидание |
|---|---|---|---|
| C1 | happy | Первый `POST /links` с новым `longUrl` | `201 Created`, `shortCode` (6 символов Base62), `shortUrl` |
| C2 | happy | Повторный `POST /links` с тем же `longUrl` | `200 OK`, `shortCode` и `shortUrl` **совпадают** с ответом на первый POST |
| C3 | edge | Два разных `longUrl` → два разных `shortCode` | Оба `201`, коды различаются |
| C4 | validation | `longUrl` отсутствует в JSON | `400`, `"longUrl is required"` |
| C5 | validation | `longUrl = ""` | `400`, `"longUrl is required"` |
| C6 | validation | `longUrl = "   "` | `400`, `"longUrl is required"` |
| C7 | validation | `longUrl = "ftp://example.com"` | `400`, `"longUrl must start with http:// or https://"` |
| C8 | boundary | `longUrl` длиной 2049 символов | `400`, `"longUrl must be at most 2048 characters"` |
| C9 | validation | Тело — невалидный JSON (`{`) | `400`, `"malformed request body"` |
| C10 | validation | `Content-Type: text/plain` | `415`, `"unsupported media type"` |
| C11 | validation | Повторный `POST` с **невалидным** `longUrl` (например, `ftp://…`) | Оба раза `400`; дедупликация не срабатывает, потому что валидация идёт первой |
| C12 | happy | `GET /{code}` для существующего кода | `302 Found`, `Location` = исходный `longUrl` |
| C13 | not-found | `GET /{code}` для несуществующего кода | `404`, `"short code not found"` |
| C14 | edge | `GET /{code}` с кодом в другом регистре | `404`, `"short code not found"` |
| C15 | edge | `GET /{code}` с не-Base62 символом (`ab_cd`) | `404`, `"short code not found"` |

**Распределение по типам:** happy — 3 (C1, C2, C12); validation — 7 (C4, C5, C6, C7, C9, C10, C11); boundary — 1 (C8); not-found — 1 (C13); edge — 3 (C3, C14, C15). Итого 15.

## Вне scope лабы 02
- Нормализация URL (регистр домена, слеши, query-параметры).
- Персистентность (H2/PostgreSQL).
- Статистика переходов.
- Кастомные коды.
- Аутентификация, rate limiting.

## С чего начать
- Прочитайте постановку.
- Запустите mvn test -Dtest.tags=lab02 — убедитесь, что C1–C4 падают.
- Сформулируйте изменение через OpenSpec: /opsx:propose lab02-deduplication. В proposal.md явно укажите, что это MODIFIED для требования
«создание короткой ссылки», и приведите полный новый текст требования.
- Реализуйте дедупликацию, ориентируясь на тесты.
- Завершите изменение: /opsx:archive.
