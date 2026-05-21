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

`POST /auth/login`

```json
{
  "email": "student@example.com",
  "password": "secret123"
}
```

Both endpoints return:

```json
{
  "token": "bearer-token",
  "expiresAt": "2026-06-04T20:30:00Z",
  "user": {
    "id": 1,
    "email": "student@example.com",
    "username": "Student"
  }
}
```

Send protected requests with:

```text
Authorization: Bearer bearer-token
```

## Content

`GET /professions`

Returns professions and their tests.

`GET /tests/{testId}`

Returns full test metadata and questions without correct answers.

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

Starts a new attempt and returns the first question.

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

Answer response includes `correct`, `explanation`, `readMoreUrl`, and either `nextQuestion` or final `result`.

`GET /attempts/{attemptId}/result`

Returns final score, weak topics, and recommendation. Available after completion.

## Profile

`GET /profile`

Returns the current user and 5 latest completed attempts.

## Admin Tests

All admin endpoints require:

```text
Authorization: Bearer admin-token
```

The token must belong to a user with `role = ADMIN`.

`GET /admin/tests`

Returns all tests with profession info and question counts.

`GET /admin/tests/{testId}`

Returns test details with correct answers. This endpoint is admin-only because it exposes option `correct` flags and `correctTextAnswer`.

`POST /admin/tests`

Creates a test with questions.

`PUT /admin/tests/{testId}`

Fully replaces test metadata and questions. Existing attempts for this test are deleted because old answers would no longer match the new question set.

`DELETE /admin/tests/{testId}`

Deletes test, questions, options, matching pairs, attempts, and answers.

Create/update request example:

```json
{
  "professionId": 1,
  "title": "Java Backend: новый тест",
  "description": "Тест для проверки базовых знаний.",
  "questions": [
    {
      "type": "SINGLE_CHOICE",
      "topic": "HTTP",
      "prompt": "Какой метод используют для чтения ресурса?",
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
