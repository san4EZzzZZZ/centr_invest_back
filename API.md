# Interview Prep API

Base URL: `http://localhost:8080/api`

## Auth

Seeded admin account:

```text
email: admin@example.com
password: admin123
role: ADMIN
```

`POST /auth/register`

```json
{
  "email": "student@example.com",
  "username": "Student",
  "password": "secret123"
}
```

Creates a pending registration and sends a confirmation code to email. The user is not created yet.

Response:

```json
{
  "expiresAt": "2026-06-04T20:40:00Z"
}
```

`POST /auth/register/confirm`

```json
{
  "email": "student@example.com",
  "code": "123456"
}
```

Creates the user and returns an auth session.

`POST /auth/login`

```json
{
  "email": "student@example.com",
  "password": "secret123"
}
```

Login and registration confirmation return:

```json
{
  "token": "bearer-token",
  "expiresAt": "2026-06-04T20:30:00Z",
  "user": {
    "id": 1,
    "email": "student@example.com",
    "username": "Student",
    "role": "USER",
    "avatarUrl": null
  }
}
```

Send protected requests with:

```text
Authorization: Bearer bearer-token
```

`POST /auth/password/forgot`

```json
{
  "email": "student@example.com"
}
```

Sends a password reset code if the user exists.

`POST /auth/password/reset`

```json
{
  "email": "student@example.com",
  "code": "123456",
  "newPassword": "newSecret123"
}
```

Updates password and deletes existing sessions for this user.

## Content

`GET /languages`

Returns strict language categories and tests inside each category.

Supported languages are fixed:

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

Optional case-insensitive substring search:

```text
GET /languages?title=backend&language=java
```

`title` filters by part of the test title. `language` filters by part of the language title.

Temporary compatibility alias: `GET /professions` and query parameter `profession` still work, but frontend should use `/languages` and `language`.

`GET /tests/{testId}`

Returns full test metadata and questions without correct answers.

Important: each test owns its own question pool. A language is only a catalog category for tests. When a user starts an attempt, the backend selects questions from the selected test pool:

```text
up to 2 SINGLE_CHOICE
up to 2 MULTIPLE_CHOICE
up to 1 MATCHING
up to 2 SHORT_TEXT
```

If the pool does not contain enough questions of a required type, the backend takes all available questions of that type.

Question types:

```text
SINGLE_CHOICE
MULTIPLE_CHOICE
MATCHING
SHORT_TEXT
```

For `MATCHING`, the question contains `matchLeftItems` and `matchRightItems`; submit a map from left item to chosen right item.

## Attempts

`POST /tests/{testId}/attempts`

Starts a new attempt, composes a concrete question set from the selected test pool, stores it for this attempt, and returns the first question.

`GET /attempts/{attemptId}`

Returns current attempt state and current question.

`POST /attempts/{attemptId}/answer`

Single or multiple choice:

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

Answer response includes `correct`, generated `explanation`, generated `readMoreUrl`, `explanationGeneratedByAi`, AI short-text check fields, and either `nextQuestion` or final `result`.

For `SHORT_TEXT`, the backend first uses strict normalized comparison. If it does not match and AI is configured, the backend asks the model whether the submitted answer is semantically equivalent to the expected answer.

Example answer response fragment:

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

`GET /attempts/{attemptId}/result`

Returns final score, weak topics, recommendation, and generated `aiReview`. Available after completion.

`GET /attempts/{attemptId}/ai-review`

Returns a final personalized review based on the user's answers. Available after completion.

If `AI_API_KEY` and `AI_MODEL` are configured, the backend asks the model to generate a concise Russian review and resource links from the user's answers. If the key is missing or the provider call fails, the backend returns a deterministic fallback review and search links.

Example response:

```json
{
  "attemptId": 1,
  "generatedByAi": false,
  "summary": "Есть пробелы в темах: REST, SQL. Ниже собраны рекомендации и ссылки для повторения.",
  "topics": [
    {
      "topic": "REST",
      "diagnosis": "Ошибки или точки для повторения связаны с вопросами по этой теме.",
      "recommendation": "Разбери пояснение к вопросу и прочитай ресурс из блока resources."
    }
  ],
  "resources": [
    {
      "title": "REST",
      "url": "https://restfulapi.net/",
      "reason": "Материал поможет повторить тему REST"
    }
  ],
  "nextStep": "Начни с первой слабой темы, прочитай материал по ссылке и затем повтори вопросы по этой теме."
}
```

Environment variables:

```text
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=your_smtp_username
MAIL_PASSWORD=your_smtp_password
MAIL_FROM=no-reply@example.com
MAIL_CODE_TTL=10m
AVATARS_DIR=uploads/avatars
AI_BASE_URL=https://integrate.api.nvidia.com/v1
AI_API_KEY=your_api_key
AI_MODEL=meta/llama-3.1-70b-instruct
AI_CONNECT_TIMEOUT=5s
AI_READ_TIMEOUT=30s
AI_ANSWER_CHECK_ENABLED=true
AI_ANSWER_CHECK_MIN_CONFIDENCE=0.75
```

## Profile

`GET /profile`

Returns the current user, 5 latest completed attempts, and favorite tests.

Current user includes `avatarUrl`:

```json
{
  "id": 1,
  "email": "student@example.com",
  "username": "Student",
  "role": "USER",
  "avatarUrl": "/api/profile/avatar/uuid.png"
}
```

`PATCH /profile/name`

```json
{
  "username": "New Name"
}
```

Updates username without email confirmation.

`POST /profile/email/change/request`

```json
{
  "newEmail": "new-email@example.com"
}
```

Sends a confirmation code to the new email.

`POST /profile/email/change/confirm`

```json
{
  "newEmail": "new-email@example.com",
  "code": "123456"
}
```

Updates account email.

`POST /profile/password/change/request`

Sends a password change code to the current account email.

`POST /profile/password/change/confirm`

```json
{
  "code": "123456",
  "newPassword": "newSecret123"
}
```

Updates password and deletes existing sessions.

`POST /profile/avatar`

Multipart form-data:

```text
file = avatar image
```

Accepted types: `image/jpeg`, `image/png`, `image/webp`. Files are stored locally in `uploads/avatars` by default.

`DELETE /profile/avatar`

Deletes current avatar.

`GET /profile/avatar/{fileName}`

Returns avatar image by `avatarUrl`.

`GET /profile/favorites`

Returns only favorite tests.

`POST /profile/favorites/tests/{testId}`

Adds a test to favorites.

`DELETE /profile/favorites/tests/{testId}`

Removes a test from favorites.

## Admin Tests

All admin endpoints require:

```text
Authorization: Bearer admin-token
```

The token must belong to a user with `role = ADMIN`.

`GET /admin/tests`

Returns all tests with language info and question counts.

Optional case-insensitive substring search:

```text
GET /admin/tests?title=backend&language=java
```

`title` searches by part of the test title. `language` searches by part of the language title.

`GET /admin/tests/{testId}`

Returns test details with correct answers. This endpoint is admin-only because it exposes option `correct` flags and `correctTextAnswer`.

`POST /admin/tests`

Creates a test with questions.

The questions from this request are saved into the created test's own question pool.

`PUT /admin/tests/{testId}`

Fully replaces test metadata and questions. Existing attempts for this test are deleted because old answers would no longer match the new question set.

Update replaces only this test's question pool. Other tests of the same language are not changed.

`DELETE /admin/tests/{testId}`

Deletes test, questions, options, matching pairs, attempts, and answers.

Create/update request example:

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
      "options": [],
      "matchPairs": []
    }
  ]
}
```
