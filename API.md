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

Optional case-insensitive substring search:

```text
GET /professions?title=java&profession=backend
```

`title` filters by part of the test title. `profession` filters by part of the profession title.

`GET /tests/{testId}`

Returns full test metadata and questions without correct answers.

Important: tests are now generated from a profession question pool. A test is not a fixed list of questions. When a user starts an attempt, the backend selects questions from the test profession pool:

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

Starts a new attempt, composes a concrete question set from the profession pool, stores it for this attempt, and returns the first question.

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

`GET /attempts/{attemptId}/ai-review`

Returns a final personalized review based on the user's answers. Available after completion.

If `OPENAI_API_KEY` is configured, the backend asks the model to generate a concise Russian review. If the key is missing or the provider call fails, the backend returns a deterministic fallback review from stored explanations and resource URLs.

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
OPENAI_API_KEY=your_api_key
OPENAI_MODEL=gpt-4.1-mini
```

## Profile

`GET /profile`

Returns the current user, 5 latest completed attempts, and favorite tests.

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

Returns all tests with profession info and question counts.

Optional case-insensitive substring search:

```text
GET /admin/tests?title=java&profession=backend
```

`title` searches by part of the test title. `profession` searches by part of the profession title.

`GET /admin/tests/{testId}`

Returns test details with correct answers. This endpoint is admin-only because it exposes option `correct` flags and `correctTextAnswer`.

`POST /admin/tests`

Creates a test with questions.

The questions from this request are added to the selected profession question pool. Attempts generated for tests of that profession will use this pool.

`PUT /admin/tests/{testId}`

Fully replaces test metadata and questions. Existing attempts for this test are deleted because old answers would no longer match the new question set.

In the current prototype, update also replaces the profession question pool provided in the request.

`DELETE /admin/tests/{testId}`

Deletes test, questions, options, matching pairs, attempts, and answers.

Create/update request example:

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
