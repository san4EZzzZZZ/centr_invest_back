# Interview Prep Backend

Backend для сервиса подготовки к техническому собеседованию. Пользователь выбирает профессию, запускает тест, отвечает на вопросы разных типов, получает пояснение после каждого ответа и итоговый разбор.

## Быстрый Старт

Требования:

```text
Java 21
Docker Desktop или локально установленный PostgreSQL
Maven Wrapper уже есть в проекте
```

Поднять PostgreSQL через Docker Compose:

```powershell
docker compose up -d
```

Если PostgreSQL установлен локально без Docker, создай базу и пользователя вручную:

```sql
CREATE USER interview_prep WITH PASSWORD 'interview_prep';
CREATE DATABASE interview_prep OWNER interview_prep;
```

На Windows с PostgreSQL 16 это можно сделать так:

```powershell
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -c "CREATE USER interview_prep WITH PASSWORD 'interview_prep';"
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -c "CREATE DATABASE interview_prep OWNER interview_prep;"
```

Если пользователь уже существует, но пароль не подходит:

```powershell
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -c "ALTER USER interview_prep WITH PASSWORD 'interview_prep';"
```

По умолчанию backend подключается к базе:

```text
url: jdbc:postgresql://localhost:5432/interview_prep
username: interview_prep
password: interview_prep
```

Запуск backend:

```powershell
.\mvnw.cmd spring-boot:run
```

Если порт `8080` занят, можно запустить на другом порту:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

Проверка тестов backend:

```powershell
.\mvnw.cmd test
```

Base URL:

```text
http://localhost:8080/api
```

Основная база проекта теперь PostgreSQL. H2 оставлен только для тестового профиля `test`, чтобы `.\mvnw.cmd test` не требовал запущенного PostgreSQL.

Если хочешь подключиться к другой PostgreSQL-базе, можно переопределить переменные окружения:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/interview_prep"
$env:DB_USERNAME="interview_prep"
$env:DB_PASSWORD="interview_prep"
.\mvnw.cmd spring-boot:run
```

Таблицы пока создаются автоматически через Hibernate `ddl-auto: update`. Для более серьезной версии проекта следующим шагом лучше добавить миграции Flyway или Liquibase.

## Авторизация

При запуске создается тестовый админ:

```text
email: admin@example.com
password: admin123
role: ADMIN
```

Регистрация:

```http
POST /api/auth/register
Content-Type: application/json
```

```json
{
  "email": "student@example.com",
  "username": "Student",
  "password": "secret123"
}
```

Логин:

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "student@example.com",
  "password": "secret123"
}
```

Ответ:

```json
{
  "token": "generated-token",
  "expiresAt": "2026-06-05T10:00:00Z",
  "user": {
    "id": 1,
    "email": "student@example.com",
    "username": "Student",
    "role": "USER"
  }
}
```

Защищенные запросы отправляются с заголовком:

```http
Authorization: Bearer generated-token
```

## Каталог Профессий И Тестов

Получить профессии с тестами:

```http
GET /api/professions
```

Поиск по части названия теста и профессии:

```http
GET /api/professions?title=java&profession=backend
```

Параметры необязательные, поиск регистронезависимый:

```text
title - часть названия теста
profession - часть названия профессии
```

Пример теста в ответе:

```json
{
  "id": 1,
  "title": "Java Backend: базовое собеседование",
  "shortDescription": "Короткая проверка базы Java backend.",
  "description": "7 вопросов разных типов.",
  "questionCount": 7
}
```

Получить детали теста:

```http
GET /api/tests/{testId}
```

Current implementation: this endpoint returns metadata and the question pool of this exact test. Correct answers are still hidden from public clients.

Data model note: `profession` is a catalog grouping, `interview_test` is a concrete test, and `question.test_id` points to the test that owns the question. Questions are not shared automatically between tests of the same profession.

Публичная ручка не отдает правильные ответы. Она возвращает метаданные теста и пул вопросов именно этого теста для отображения.

## Новая Логика Тестов

Каждый тест хранит собственный пул вопросов. Профессия нужна как категория каталога, но вопросы не являются общими для всех тестов профессии.

При старте попытки backend выбирает только из пула выбранного теста:

```text
до 2 SINGLE_CHOICE
до 2 MULTIPLE_CHOICE
до 1 MATCHING
до 2 SHORT_TEXT
```

Если вопросов какого-то типа меньше, backend берет все доступные вопросы этого типа из выбранного теста. Выбранный набор фиксируется внутри попытки, поэтому уже начатая попытка не меняется, даже если админ потом отредактирует пул.

Типы вопросов:

```text
SINGLE_CHOICE
MULTIPLE_CHOICE
MATCHING
SHORT_TEXT
```

## Прохождение Теста

Старт попытки:

```http
POST /api/tests/{testId}/attempts
Authorization: Bearer token
```

Текущее состояние попытки:

```http
GET /api/attempts/{attemptId}
Authorization: Bearer token
```

Ответить на вопрос:

```http
POST /api/attempts/{attemptId}/answer
Authorization: Bearer token
Content-Type: application/json
```

Single choice и multiple choice:

```json
{
  "selectedOptionIds": [1, 2]
}
```

Matching:

```json
{
  "matches": {
    "SELECT": "Получение данных",
    "INSERT": "Добавление строки"
  }
}
```

Short text:

```json
{
  "textAnswer": "extends"
}
```

Ответ backend содержит `correct`, сгенерированные `explanation` и `readMoreUrl`, флаг `explanationGeneratedByAi`, информацию об AI-проверке короткого ответа, `nextQuestion` или итоговый `result` с `aiReview`.

Для `SHORT_TEXT`, если строгая проверка не совпала, backend может дополнительно спросить AI-модель: совпадает ли ответ пользователя по смыслу с эталонным ответом автора вопроса. Это помогает засчитывать ответы с небольшой опечаткой, другой формулировкой или другим языком, если смысл корректный.

Фрагмент ответа после `POST /api/attempts/{attemptId}/answer`:

```json
{
  "correct": true,
  "explanation": "Для наследования класса используется extends.",
  "readMoreUrl": "https://docs.oracle.com/javase/tutorial/java/IandI/subclasses.html",
  "explanationGeneratedByAi": true,
  "checkedByAi": true,
  "aiConfidence": 0.91,
  "nextQuestion": null,
  "result": null
}
```

Итог:

```http
GET /api/attempts/{attemptId}/result
Authorization: Bearer token
```

AI-разбор и AI-проверка коротких ответов:

```http
GET /api/attempts/{attemptId}/ai-review
Authorization: Bearer token
```

AI-функции используют OpenAI-compatible API. Сейчас по умолчанию настроен NVIDIA endpoint:

```powershell
$env:AI_BASE_URL="https://integrate.api.nvidia.com/v1"
$env:AI_API_KEY="your_api_key"
$env:AI_MODEL="meta/llama-3.1-70b-instruct"
.\mvnw.cmd spring-boot:run
```

Ключ нельзя хардкодить в проект и нельзя коммитить. Если `AI_API_KEY` или `AI_MODEL` не заданы, пояснение, ссылка и итоговый AI-разбор вернут fallback, а `SHORT_TEXT` будет проверяться строгим сравнением.

Дополнительные настройки:

```powershell
$env:AI_ANSWER_CHECK_ENABLED="true"
$env:AI_ANSWER_CHECK_MIN_CONFIDENCE="0.75"
```

`AI_ANSWER_CHECK_MIN_CONFIDENCE` задает минимальную уверенность модели, при которой ответ будет засчитан как правильный.

## Профиль И Избранное

Профиль:

```http
GET /api/profile
Authorization: Bearer token
```

В ответе есть пользователь, последние завершенные попытки и избранные тесты.

Избранное:

```http
GET /api/profile/favorites
POST /api/profile/favorites/tests/{testId}
DELETE /api/profile/favorites/tests/{testId}
Authorization: Bearer token
```

Элемент избранного содержит:

```json
{
  "testId": 1,
  "professionId": 1,
  "professionTitle": "Backend Java Developer",
  "testTitle": "Java Backend: базовое собеседование",
  "testShortDescription": "Короткая проверка базы Java backend.",
  "testDescription": "7 вопросов разных типов.",
  "addedAt": "2026-05-23T10:30:00Z"
}
```

## Админка

Все админские ручки требуют `Authorization: Bearer admin-token`, а пользователь должен иметь роль `ADMIN`.

Без токена будет `401`, с обычным user-токеном будет `403`.

Список тестов:

```http
GET /api/admin/tests
Authorization: Bearer admin-token
```

Поиск в админке:

```http
GET /api/admin/tests?title=java&profession=backend
Authorization: Bearer admin-token
```

Параметры `title` и `profession` ищут по части строки без учета регистра.

Детали теста с правильными ответами:

```http
GET /api/admin/tests/{testId}
Authorization: Bearer admin-token
```

Создать тест:

```http
POST /api/admin/tests
Authorization: Bearer admin-token
Content-Type: application/json
```

Редактировать тест:

```http
PUT /api/admin/tests/{testId}
Authorization: Bearer admin-token
Content-Type: application/json
```

Удалить тест:

```http
DELETE /api/admin/tests/{testId}
Authorization: Bearer admin-token
```

Body для создания и редактирования:

```json
{
  "professionId": 1,
  "title": "Java Backend: новый тест",
  "shortDescription": "Короткая проверка Java, HTTP и SQL.",
  "description": "Тест для проверки базовых знаний.",
  "questions": [
    {
      "type": "SINGLE_CHOICE",
      "topic": "HTTP",
      "prompt": "Какой метод используют для чтения ресурса?",
      "correctTextAnswer": null,
      "options": [
        {
          "text": "GET",
          "correct": true
        },
        {
          "text": "POST",
          "correct": false
        }
      ],
      "matchPairs": []
    }
  ]
}
```

`shortDescription` обязателен для создания и редактирования теста.

Current admin behavior: questions from `POST /api/admin/tests` and `PUT /api/admin/tests/{testId}` are saved into the pool of that exact test. `PUT` fully replaces only this test's pool and deletes attempts for this test.

Правила валидации вопросов:

```text
SINGLE_CHOICE - минимум 2 options и ровно 1 correct=true
MULTIPLE_CHOICE - минимум 2 options и минимум 1 correct=true
MATCHING - нужен непустой matchPairs
SHORT_TEXT - нужен correctTextAnswer
```

Вопросы из `POST`/`PUT` попадают в пул конкретного теста. При `PUT` заменяется пул только редактируемого теста, а старые попытки этого теста удаляются, чтобы ответы не расходились с новой версией пула.

## Ошибки

Ошибки возвращаются в формате `ProblemDetail`.

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authentication required"
}
```

Частые статусы:

```text
400 Bad Request - неверный body или нарушение правил прохождения
401 Unauthorized - нет токена или неверный логин/пароль
403 Forbidden - пользователь не админ для /api/admin
404 Not Found - сущность не найдена
```

## TypeScript Модели

```ts
export type TestSummary = {
  id: number;
  title: string;
  shortDescription: string;
  description: string;
  questionCount: number;
};

export type FavoriteTest = {
  testId: number;
  professionId: number;
  professionTitle: string;
  testTitle: string;
  testShortDescription: string;
  testDescription: string;
  addedAt: string;
};
```

Подробная API-шпаргалка лежит в `API.md`, сценарии ручной проверки в Postman - в `POSTMAN_TESTS.md`.
