# Postman Tests

Ниже сценарий полного цикла работы сервера: регистрация, получение списка тестов, старт попытки, ответы на все 7 вопросов, результат и профиль.

## Environment

Создай Postman Environment, например `Interview Prep Local`, и добавь переменные:

```text
baseUrl = http://localhost:8080/api
token =
testId =
attemptId =
optionGetId =
optionHashSetId =
optionTreeSetId =
optionEntityId =
optionUriId =
optionMethodsId =
optionStatelessId =
```

Для защищенных запросов добавляй header:

```text
Authorization: Bearer {{token}}
```

## 1. Register

`POST {{baseUrl}}/auth/register`

Headers:

```text
Content-Type: application/json
```

Body:

```json
{
  "email": "student{{$randomInt}}@example.com",
  "username": "Student",
  "password": "secret123"
}
```

Tests:

```javascript
pm.test("register returns 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("token exists", function () {
  pm.expect(json.token).to.be.a("string").and.not.empty;
});

pm.test("user exists", function () {
  pm.expect(json.user.email).to.include("@example.com");
});

pm.environment.set("token", json.token);
```

## 2. Get Current User

`GET {{baseUrl}}/auth/me`

Headers:

```text
Authorization: Bearer {{token}}
```

Tests:

```javascript
pm.test("me returns 200", function () {
  pm.response.to.have.status(200);
});

pm.test("current user has id and email", function () {
  const json = pm.response.json();
  pm.expect(json.id).to.be.a("number");
  pm.expect(json.email).to.be.a("string");
});
```

## 3. Get Professions

`GET {{baseUrl}}/professions`

Tests:

```javascript
pm.test("professions returns 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();
const backend = json.find(item => item.title === "Backend Java Developer");

pm.test("backend profession exists", function () {
  pm.expect(backend).to.exist;
  pm.expect(backend.tests).to.have.length.above(0);
});

pm.environment.set("testId", backend.tests[0].id);
```

## 4. Get Test Details

`GET {{baseUrl}}/tests/{{testId}}`

Tests:

```javascript
pm.test("test details returns 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("test has 7 questions", function () {
  pm.expect(json.questions).to.have.length(7);
});

pm.test("questions do not expose correct answers", function () {
  const serialized = JSON.stringify(json);
  pm.expect(serialized).to.not.include("correctTextAnswer");
  pm.expect(serialized).to.not.include("\"correct\":true");
});
```

## 5. Start Attempt

`POST {{baseUrl}}/tests/{{testId}}/attempts`

Headers:

```text
Authorization: Bearer {{token}}
```

Tests:

```javascript
pm.test("start attempt returns 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("attempt and first question exist", function () {
  pm.expect(json.attemptId).to.be.a("number");
  pm.expect(json.question.position).to.equal(1);
  pm.expect(json.question.type).to.equal("SINGLE_CHOICE");
});

const getOption = json.question.options.find(option => option.text === "GET");
pm.environment.set("attemptId", json.attemptId);
pm.environment.set("optionGetId", getOption.id);
```

## 6. Answer Question 1: Single Choice

`POST {{baseUrl}}/attempts/{{attemptId}}/answer`

Headers:

```text
Authorization: Bearer {{token}}
Content-Type: application/json
```

Body:

```json
{
  "selectedOptionIds": [{{optionGetId}}]
}
```

Tests:

```javascript
pm.test("answer q1 returns 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("q1 is correct and generated explanation exists", function () {
  pm.expect(json.correct).to.equal(true);
  pm.expect(json.explanation).to.be.a("string").and.not.empty;
  pm.expect(json.readMoreUrl).to.be.a("string").and.not.empty;
  pm.expect(json.explanationGeneratedByAi).to.be.a("boolean");
});

pm.test("next question is q2 multiple choice", function () {
  pm.expect(json.nextQuestion.position).to.equal(2);
  pm.expect(json.nextQuestion.type).to.equal("MULTIPLE_CHOICE");
});

const hashSet = json.nextQuestion.options.find(option => option.text === "HashSet");
const treeSet = json.nextQuestion.options.find(option => option.text === "TreeSet");
pm.environment.set("optionHashSetId", hashSet.id);
pm.environment.set("optionTreeSetId", treeSet.id);
```

## 7. Answer Question 2: Multiple Choice

`POST {{baseUrl}}/attempts/{{attemptId}}/answer`

Body:

```json
{
  "selectedOptionIds": [{{optionHashSetId}}, {{optionTreeSetId}}]
}
```

Tests:

```javascript
pm.test("answer q2 returns 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("q2 is correct", function () {
  pm.expect(json.correct).to.equal(true);
  pm.expect(json.explanation).to.be.a("string").and.not.empty;
});

pm.test("next question is matching", function () {
  pm.expect(json.nextQuestion.position).to.equal(3);
  pm.expect(json.nextQuestion.type).to.equal("MATCHING");
  pm.expect(json.nextQuestion.matchLeftItems).to.include("SELECT");
  pm.expect(json.nextQuestion.matchRightItems).to.include("Получение данных");
});
```

## 8. Answer Question 3: Matching

`POST {{baseUrl}}/attempts/{{attemptId}}/answer`

Body:

```json
{
  "matches": {
    "SELECT": "Получение данных",
    "INSERT": "Добавление строки",
    "UPDATE": "Изменение строки",
    "DELETE": "Удаление строки"
  }
}
```

Tests:

```javascript
pm.test("answer q3 returns 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("q3 is correct", function () {
  pm.expect(json.correct).to.equal(true);
});

pm.test("next question is short text", function () {
  pm.expect(json.nextQuestion.position).to.equal(4);
  pm.expect(json.nextQuestion.type).to.equal("SHORT_TEXT");
});
```

## 9. Answer Question 4: Short Text

`POST {{baseUrl}}/attempts/{{attemptId}}/answer`

Body:

```json
{
  "textAnswer": "@RestController"
}
```

Tests:

```javascript
pm.test("answer q4 returns 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("q4 is correct", function () {
  pm.expect(json.correct).to.equal(true);
});

pm.test("next question is JPA single choice", function () {
  pm.expect(json.nextQuestion.position).to.equal(5);
  pm.expect(json.nextQuestion.type).to.equal("SINGLE_CHOICE");
});

const entity = json.nextQuestion.options.find(option => option.text === "Класс является сохраняемой JPA-сущностью");
pm.environment.set("optionEntityId", entity.id);
```

## 10. Answer Question 5: Single Choice

`POST {{baseUrl}}/attempts/{{attemptId}}/answer`

Body:

```json
{
  "selectedOptionIds": [{{optionEntityId}}]
}
```

Tests:

```javascript
pm.test("answer q5 returns 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("q5 is correct", function () {
  pm.expect(json.correct).to.equal(true);
});

pm.test("next question is REST multiple choice", function () {
  pm.expect(json.nextQuestion.position).to.equal(6);
  pm.expect(json.nextQuestion.type).to.equal("MULTIPLE_CHOICE");
});

pm.environment.set("optionUriId", json.nextQuestion.options.find(option => option.text === "Ресурсы имеют URI").id);
pm.environment.set("optionMethodsId", json.nextQuestion.options.find(option => option.text === "Используются стандартные HTTP-методы").id);
pm.environment.set("optionStatelessId", json.nextQuestion.options.find(option => option.text === "Каждый запрос должен содержать достаточно контекста для обработки").id);
```

## 11. Answer Question 6: Multiple Choice

`POST {{baseUrl}}/attempts/{{attemptId}}/answer`

Body:

```json
{
  "selectedOptionIds": [{{optionUriId}}, {{optionMethodsId}}, {{optionStatelessId}}]
}
```

Tests:

```javascript
pm.test("answer q6 returns 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("q6 is correct", function () {
  pm.expect(json.correct).to.equal(true);
});

pm.test("next question is final short text", function () {
  pm.expect(json.nextQuestion.position).to.equal(7);
  pm.expect(json.nextQuestion.type).to.equal("SHORT_TEXT");
});
```

## 12. Answer Question 7: Short Text And Finish

`POST {{baseUrl}}/attempts/{{attemptId}}/answer`

Body:

```json
{
  "textAnswer": "extends"
}
```

Tests:

```javascript
pm.test("answer q7 returns 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("q7 is correct", function () {
  pm.expect(json.correct).to.equal(true);
});

pm.test("attempt completed with perfect result", function () {
  pm.expect(json.nextQuestion).to.equal(null);
  pm.expect(json.result.correctAnswers).to.equal(7);
  pm.expect(json.result.totalQuestions).to.equal(7);
  pm.expect(json.result.weakTopics).to.have.length(0);
  pm.expect(json.result.recommendation).to.be.a("string").and.not.empty;
});
```

## 13. Get Result

`GET {{baseUrl}}/attempts/{{attemptId}}/result`

Headers:

```text
Authorization: Bearer {{token}}
```

Tests:

```javascript
pm.test("result returns 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("result is persisted", function () {
  pm.expect(json.correctAnswers).to.equal(7);
  pm.expect(json.totalQuestions).to.equal(7);
  pm.expect(json.completedAt).to.be.a("string");
});
```

## 14. Get Profile With Recent Attempts

`GET {{baseUrl}}/profile`

Headers:

```text
Authorization: Bearer {{token}}
```

Tests:

```javascript
pm.test("profile returns 200", function () {
  pm.response.to.have.status(200);
});

const json = pm.response.json();

pm.test("profile contains recent completed attempt", function () {
  pm.expect(json.user.id).to.be.a("number");
  pm.expect(json.recentAttempts).to.have.length.above(0);
  pm.expect(json.recentAttempts[0].attemptId).to.equal(Number(pm.environment.get("attemptId")));
});
```

## Negative Checks

Эти запросы лучше вынести в отдельную папку `Negative`.

### Request Without Token

`GET {{baseUrl}}/profile`

Без `Authorization`.

Tests:

```javascript
pm.test("profile without token returns 401", function () {
  pm.response.to.have.status(401);
});
```

### Login With Wrong Password

`POST {{baseUrl}}/auth/login`

Body:

```json
{
  "email": "missing@example.com",
  "password": "wrong-password"
}
```

Tests:

```javascript
pm.test("wrong login returns 401", function () {
  pm.response.to.have.status(401);
});
```

### Duplicate Answer

Повтори любой уже отвеченный запрос, например ответ на вопрос 7.

Tests:

```javascript
pm.test("duplicate or completed answer returns 400", function () {
  pm.response.to.have.status(400);
});
```

### Result Before Completion

Создай новую попытку и сразу вызови:

`GET {{baseUrl}}/attempts/{{attemptId}}/result`

Tests:

```javascript
pm.test("unfinished result returns 400", function () {
  pm.response.to.have.status(400);
});
```
