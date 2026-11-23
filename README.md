# Short Link Service — учебный проект онбординга

Демо-проект для онбординга по OpenCode + OpenSpec. Здесь вы проходите
полный цикл Spec-Driven Development: формулируете изменение, реализуете
его, архивируете спеку. Каждая лаба — отдельная задача, которая
добавляется в `main` администратором.

## Что уже есть в `main`

- Каркас Spring Boot приложения: `Application.kt`, `HealthController` (`GET /health`).
- Настройки OpenCode (`opencode.json`) и правила для AI (`AGENTS.md`).
- Инициализированный OpenSpec (`openspec/config.yaml`).
- Постановки лаб в `docs/labs/`.

Чего **нет** в `main`: реализации лаб и спек. Всё это вы создаёте сами,
в своей ветке.

## Как проходить лабы

### 1. Создайте свою ветку

```bash
git checkout -b onboarding/<ваше-имя>
```

Ветка создаётся от main. Не мержьте в main — это учебный
репозиторий, main обновляет только администратор.

### 2. Прочитайте постановку лабы
Постановки лежат в docs/labs/. Например, для первой лабы:
docs/labs/lab01.md.

Там — контракты API, ограничения и корнер-кейсы. Это ваш вход в OpenSpec:
на основе постановки вы сформулируете proposal.

### 3. Запустите тесты лабы
Тесты уже лежат в src/test/kotlin/lab01/ и изначально падают —
это ожидаемо. Их «позеленение» и есть критерий приёмки лабы.

```bash
# Только тесты текущей лабы
mvn test -Dtest.tags=lab01
```
** ИЛИ **
В pom.xml установите значение тэга <test.tags> соответствующей лабы, после чего запустите сборку и тесты

### 4. Пройдите цикл OpenSpec
propose — сформулируйте изменение: /opsx:propose (или
соответствующая команда OpenSpec). Результат — папка
openspec/changes/<change-name>/ с proposal.md, specs/, tasks.md.

apply — реализуйте код по задачам из tasks.md. Ориентир —
зелёные тесты вашей лабы.

archive — завершите изменение: /opsx:archive. Спека переезжает
в openspec/specs/, изменение — в openspec/archive/.

### 5. Заполните NOTES.md
В корне ветки создайте NOTES.md (5–10 строк):

что сделали;

где застряли;

что показалось неочевидным в OpenCode/OpenSpec;

что улучшили бы в постановке лабы.

Это не «сдача», а обратная связь для улучшения онбординга.

### 6. Переход к следующей лабе
Когда администратор добавит лабу 2 в main:

bash
git checkout main
git pull
git checkout onboarding/<ваше-имя>
git merge main
После этого в вашей ветке появятся docs/labs/lab02-*.md и тесты с
тегом lab02. Приступайте к следующей лабе тем же циклом.

# Правила
Не мержьте в main. Только pull из main.

Не коммитьте .env. Ключи храните в переменных окружения
(см. onboarding-hub/03-opencode-config-examples/secrets.md).

Спеки — в вашей ветке. openspec/changes/, openspec/specs/,
openspec/archive/ администратор не трогает.

# Сборка и запуск
```bash
# Сборка
mvn clean package

# Запуск локально
mvn spring-boot:run

# Проверка health
curl http://localhost:8080/health
```

# Ссылки
Онбординг-хаб: ii_onboarding_hub

OpenSpec: https://openspec.dev/

OpenCode: https://opencode.ai/docs/
