# Interview Prep API

Base URL: `http://localhost:8080/api`

## Auth

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
