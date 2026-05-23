# Interview Prep Backend

Backend для сервиса подготовки к техническому собеседованию. Пользователь выбирает профессию, запускает тест, отвечает на вопросы разных типов, получает пояснение после каждого ответа и итоговый разбор.

## Быстрый Старт

Требования:

```text
Java 21
Maven Wrapper уже есть в проекте
```

Запуск:

```powershell
.\mvnw.cmd spring-boot:run
```

Тесты:

```powershell
.\mvnw.cmd test
```

Base URL:

```text
http://localhost:8080/api
```

H2 Console:

```text
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:interview_prep
```

Сейчас проект использует H2 in-memory. Это удобно для показа и прототипа: база создается при запуске, демо-данные сидируются автоматически. Минус: после остановки приложения данные пропадают. Для командной разработки и более похожего на production окружения лучше перейти на PostgreSQL, оставив H2 для быстрых тестов.

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

Публичная ручка не отдает правильные ответы. Она возвращает метаданные теста и пул вопросов профессии для отображения.

## Новая Логика Тестов

Тест больше не хранит жесткий фиксированный список вопросов. Тест связан с профессией, а вопросы лежат в общем пуле этой профессии.

При старте попытки backend выбирает из пула:

```text
до 2 SINGLE_CHOICE
до 2 MULTIPLE_CHOICE
до 1 MATCHING
до 2 SHORT_TEXT
```

Если вопросов какого-то типа меньше, backend берет все доступные вопросы этого типа. Выбранный набор фиксируется внутри попытки, поэтому уже начатая попытка не меняется, даже если админ потом отредактирует пул.

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

Ответ backend содержит `correct`, `explanation`, `readMoreUrl`, `nextQuestion` или итоговый `result`.

Итог:

```http
GET /api/attempts/{attemptId}/result
Authorization: Bearer token
```

AI-разбор:

```http
GET /api/attempts/{attemptId}/ai-review
Authorization: Bearer token
```

Если задан `OPENAI_API_KEY`, backend генерирует персональный разбор. Если ключа нет, возвращается fallback-разбор по сохраненным пояснениям и ссылкам.

```powershell
$env:OPENAI_API_KEY="your_api_key"
$env:OPENAI_MODEL="gpt-4.1-mini"
.\mvnw.cmd spring-boot:run
```

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
      "explanation": "GET используют для получения ресурса без изменения состояния сервера.",
      "readMoreUrl": "https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/GET",
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
    },
    {
      "type": "SHORT_TEXT",
      "topic": "Java Core",
      "prompt": "Ключевое слово для наследования класса?",
      "correctTextAnswer": "extends",
      "explanation": "Для наследования класса используется extends.",
      "readMoreUrl": "https://docs.oracle.com/javase/tutorial/java/IandI/subclasses.html",
      "options": [],
      "matchPairs": []
    }
  ]
}
```

`shortDescription` обязателен для создания и редактирования теста.

Правила валидации вопросов:

```text
SINGLE_CHOICE - минимум 2 options и ровно 1 correct=true
MULTIPLE_CHOICE - минимум 2 options и минимум 1 correct=true
MATCHING - нужен непустой matchPairs
SHORT_TEXT - нужен correctTextAnswer
```

В текущем прототипе вопросы из `POST`/`PUT` попадают в пул вопросов профессии. При `PUT` пул указанной профессии заменяется вопросами из запроса, а старые попытки по затронутой профессии удаляются, чтобы ответы не расходились с новой версией пула.

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
