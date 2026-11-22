# Short Link Service — Kotlin / Spring Boot / Maven

## Project Structure
- `src/main/kotlin/com/example/shortlink/` — основной код
- `src/test/kotlin/labNN/` — тесты лаб (тег `@Tag("labNN")`)
- `docs/labs/` — постановки лаб
- `openspec/` — спеки, изменения, архив (создаётся участником)

## Build & Test Commands
- Сборка: `mvn clean package`
- Все тесты: `mvn test`
- Тесты конкретной лабы: `mvn test -Dtest.tags=lab01`
- Запуск: `mvn spring-boot:run`

## Code Standards
- Kotlin Coding Conventions, constructor injection (без field injection).
- DTO отдельно от доменной модели; entity не возвращать из контроллеров.
- Ошибки — через `@ControllerAdvice` с единым форматом `{"error": "..."}`.
- Логирование — SLF4J (`LoggerFactory.getLogger`), не `println`.

## Testing
- Интеграционные тесты: `@SpringBootTest` + `MockMvc`.
- Имена тестов: `should_<expected>_when_<condition>`.
- Перед коммитом: `mvn test -Dtest.tags=labNN` должен быть зелёным.

## Maven
- Версии зависимостей — в `pom.xml`, без «плавающих» версий.
- `test.tags` — свойство для фильтрации тестов по тегу лабы.

## OpenSpec
- Спеки и изменения — в ветке участника, не в `main`.
- `proposal` всегда содержит rollback plan и out of scope.