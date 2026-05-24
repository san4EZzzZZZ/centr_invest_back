# Interview Prep Backend

Backend для сервиса подготовки к техническому собеседованию. Пользователь выбирает язык программирования, запускает тест по junior-роли на этом языке, отвечает на вопросы разных типов, получает пояснение после каждого ответа и итоговый разбор.

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

SMTP для отправки кодов:

```powershell
$env:MAIL_HOST="smtp.example.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="your_smtp_username"
$env:MAIL_PASSWORD="your_smtp_password"
$env:MAIL_FROM="no-reply@example.com"
```

Если `MAIL_HOST` не задан, сервер не падает: код подтверждения пишется в лог приложения. Это удобно для локальной разработки и Postman-проверок.

Аватарки хранятся локально:

```text
AVATARS_DIR=uploads/avatars
```

Таблицы пока создаются автоматически через Hibernate `ddl-auto: update`. Для более серьезной версии проекта следующим шагом лучше добавить миграции Flyway или Liquibase.

## Авторизация

При запуске создается тестовый админ:

```text
email: admin@example.com
password: admin123
role: ADMIN
```

Регистрация теперь двухшаговая: сначала backend отправляет код на почту, а пользователь создается только после подтверждения.

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

Ответ:

```json
{
  "expiresAt": "2026-06-05T10:10:00Z"
}
```

Подтверждение регистрации:

```http
POST /api/auth/register/confirm
Content-Type: application/json
```

```json
{
  "email": "student@example.com",
  "code": "123456"
}
```

После подтверждения backend создает пользователя и возвращает токен.

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
    "role": "USER",
    "avatarUrl": null
  }
}
```

Защищенные запросы отправляются с заголовком:

```http
Authorization: Bearer generated-token
```

Восстановление пароля:

```http
POST /api/auth/password/forgot
Content-Type: application/json
```

```json
{
  "email": "student@example.com"
}
```

Подтверждение нового пароля:

```http
POST /api/auth/password/reset
Content-Type: application/json
```

```json
{
  "email": "student@example.com",
  "code": "123456",
  "newPassword": "newSecret123"
}
```

## Каталог Языков И Тестов

Получить языки с тестами:

```http
GET /api/languages
```

Строгий список языков:

```text
Python
Java
C++
C#
SQL
PHP
JavaScript
GO
```

Поиск по части названия теста и языка:

```http
GET /api/languages?title=backend&language=java
```

Параметры необязательные, поиск регистронезависимый:

```text
title - часть названия теста
language - часть названия языка
```

Временная обратная совместимость: `GET /api/professions` и параметр `profession` пока тоже работают, но фронту лучше использовать `/api/languages` и `language`.

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

Data model note: the database table is still named `profession` for compatibility, but semantically it is now a strict language catalog. `interview_test` is a concrete test, and `question.test_id` points to the test that owns the question.

Публичная ручка не отдает правильные ответы. Она возвращает метаданные теста и пул вопросов именно этого теста для отображения.

## Новая Логика Тестов

Каждый тест хранит собственный пул вопросов. Язык нужен как категория каталога, но вопросы не являются общими для всех тестов одного языка.

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
$env:AI_CONNECT_TIMEOUT="5s"
$env:AI_READ_TIMEOUT="30s"
.\mvnw.cmd spring-boot:run
```

Ключ нельзя хардкодить в проект и нельзя коммитить. Если `AI_API_KEY` или `AI_MODEL` не заданы, пояснение, ссылка и итоговый AI-разбор вернут fallback, а `SHORT_TEXT` будет проверяться строгим сравнением. Если провайдер долго не отвечает, backend ждет не бесконечно, а до `AI_READ_TIMEOUT`, пишет предупреждение в лог и возвращает fallback.

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
  "languageId": 1,
  "languageTitle": "Java",
  "testTitle": "Java Backend: базовое собеседование",
  "testShortDescription": "Короткая проверка базы Java backend.",
  "testDescription": "7 вопросов разных типов.",
  "addedAt": "2026-05-23T10:30:00Z"
}
```

Настройки аккаунта:

```http
PATCH /api/profile/name
Authorization: Bearer token
Content-Type: application/json
```

```json
{
  "username": "New Name"
}
```

Смена email:

```http
POST /api/profile/email/change/request
Authorization: Bearer token
Content-Type: application/json
```

```json
{
  "newEmail": "new-email@example.com"
}
```

```http
POST /api/profile/email/change/confirm
Authorization: Bearer token
Content-Type: application/json
```

```json
{
  "newEmail": "new-email@example.com",
  "code": "123456"
}
```

Смена пароля из профиля:

```http
POST /api/profile/password/change/request
POST /api/profile/password/change/confirm
Authorization: Bearer token
```

Body для подтверждения:

```json
{
  "code": "123456",
  "newPassword": "newSecret123"
}
```

Аватарка:

```http
POST /api/profile/avatar
Authorization: Bearer token
Content-Type: multipart/form-data
```

Поле файла называется `file`. Поддерживаются `image/jpeg`, `image/png`, `image/webp`. По умолчанию файлы хранятся в `uploads/avatars`, путь можно поменять через `AVATARS_DIR`.

```http
DELETE /api/profile/avatar
GET /api/profile/avatar/{fileName}
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
GET /api/admin/tests?title=backend&language=java
Authorization: Bearer admin-token
```

Параметры `title` и `language` ищут по части строки без учета регистра.

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
  "languageId": 1,
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
export type CurrentUser = {
  id: number;
  email: string;
  username: string;
  role: "USER" | "ADMIN";
  avatarUrl: string | null;
};

export type TestSummary = {
  id: number;
  title: string;
  shortDescription: string;
  description: string;
  questionCount: number;
};

export type FavoriteTest = {
  testId: number;
  languageId: number;
  languageTitle: string;
  testTitle: string;
  testShortDescription: string;
  testDescription: string;
  addedAt: string;
};
```

Подробная API-шпаргалка лежит в `API.md`, сценарии ручной проверки в Postman - в `POSTMAN_TESTS.md`.
