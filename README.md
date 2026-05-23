<<<<<<< HEAD
# centr_invest_back
=======
# Interview Prep Backend

Backend для сервиса подготовки к техническому собеседованию в формате: пользователь выбирает профессию, проходит тест, отвечает на вопросы разных типов, получает пояснение после каждого ответа и итоговый результат.

Документ ориентирован на frontend-разработчика: как запустить сервер, какие ручки дергать, как хранить токен, какие JSON-структуры ожидать и отправлять.

## Быстрый Старт

Требования:

```text
Java 21
Maven Wrapper уже есть в проекте
```

Запуск backend:

```powershell
.\mvnw.cmd spring-boot:run
```

Проверка тестов backend:

```powershell
.\mvnw.cmd test
```

API будет доступен по адресу:

```text
http://localhost:8080/api
```

H2 Console:

```text
http://localhost:8080/h2-console
```

JDBC URL для H2:

```text
jdbc:h2:mem:interview_prep
```

Сейчас проект использует H2 in-memory базу:

```text
jdbc:h2:mem:interview_prep
```

Это удобно для прототипа: ничего не нужно устанавливать, база создается при запуске, демо-данные сидируются автоматически. Минус: данные исчезают после остановки приложения.

Для более серьезной разработки лучше перейти на PostgreSQL. PostgreSQL даст постоянное хранение данных, нормальную работу в команде, реальные индексы, миграции через Flyway/Liquibase и окружение ближе к production. Практичный путь: оставить H2 для быстрых тестов, а для локальной разработки добавить профиль `postgres`.

Опционально для AI-разбора результата можно задать переменные окружения:

```powershell
$env:OPENAI_API_KEY="your_api_key"
$env:OPENAI_MODEL="gpt-4.1-mini"
.\mvnw.cmd spring-boot:run
```

Если `OPENAI_API_KEY` не задан, endpoint AI-разбора все равно работает, но возвращает fallback-разбор на основе сохраненных пояснений и ссылок у вопросов.

## CORS

Backend уже разрешает запросы с популярных frontend dev-серверов:

```text
http://localhost:3000
http://localhost:5173
```

То есть React/Vite-клиент с `localhost:5173` должен подключаться без дополнительной настройки CORS.

## Базовая Схема Работы Клиента

1. Пользователь регистрируется или логинится.
2. Backend возвращает `token`.
3. Frontend сохраняет токен, например в `localStorage`.
4. Frontend получает список профессий и тестов.
5. Пользователь выбирает тест.
6. Frontend стартует попытку прохождения.
7. Backend собирает конкретный набор вопросов из пула профессии и возвращает первый вопрос.
8. Пользователь отвечает.
9. Backend возвращает `correct`, `explanation`, `readMoreUrl` и следующий вопрос.
10. После последнего ответа backend возвращает итоговый `result`.
11. В профиле можно получить последние завершенные попытки.

## Авторизация

При старте проекта автоматически создается тестовый администратор:

```text
email: admin@example.com
password: admin123
role: ADMIN
```

Через этот аккаунт можно заходить в админскую часть и создавать, редактировать, удалять тесты.

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

Ответ регистрации и логина:

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

Все защищенные запросы отправляй с заголовком:

```http
Authorization: Bearer generated-token
```

Проверка текущего пользователя:

```http
GET /api/auth/me
Authorization: Bearer generated-token
```

Выход:

```http
POST /api/auth/logout
Authorization: Bearer generated-token
```

На frontend обычно достаточно удалить токен из `localStorage` после успешного logout.

## Пример API-Клиента

Минимальный вариант на TypeScript:

```ts
const API_URL = "http://localhost:8080/api";

export function getToken() {
  return localStorage.getItem("token");
}

export function setToken(token: string) {
  localStorage.setItem("token", token);
}

export function clearToken() {
  localStorage.removeItem("token");
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken();
  const headers = new Headers(options.headers);

  headers.set("Content-Type", "application/json");

  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers
  });

  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(error?.detail || `HTTP ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}
```

Пример логина:

```ts
type AuthResponse = {
  token: string;
  expiresAt: string;
  user: {
    id: number;
    email: string;
    username: string;
  };
};

const auth = await apiFetch<AuthResponse>("/auth/login", {
  method: "POST",
  body: JSON.stringify({
    email,
    password
  })
});

setToken(auth.token);
```

## Профессии И Тесты

Получить список профессий:

```http
GET /api/professions
```

Пример ответа:

```json
[
  {
    "id": 1,
    "title": "Backend Java Developer",
    "description": "Базовая проверка знаний Java, HTTP, SQL, Spring и REST API для junior backend-разработчика.",
    "tests": [
      {
        "id": 1,
        "title": "Java Backend: базовое собеседование",
        "description": "7 вопросов разных типов: один ответ, несколько ответов, соответствие и короткий текст.",
        "questionCount": 7
      }
    ]
  }
]
```

Получить детали теста:

```http
GET /api/tests/{testId}
```

Важно: backend не отдает правильные ответы в `GET /tests/{testId}`. Клиент получает только данные, нужные для отображения вопросов.

Важно по новой логике: тест больше не является жестко заданным списком вопросов. Тест связан с профессией, а вопросы лежат в пуле профессии. Когда пользователь начинает попытку, backend выбирает вопросы из пула:

```text
до 2 SINGLE_CHOICE
до 2 MULTIPLE_CHOICE
до 1 MATCHING
до 2 SHORT_TEXT
```

Если админ добавил меньше вопросов какого-то типа, backend берет все доступные вопросы этого типа. Например, если в пуле есть только один `MATCHING`, он попадет в попытку; если нет ни одного, этот тип просто не попадет в попытку.

Выбранный набор фиксируется в таблице попытки. Поэтому если админ позже изменит пул вопросов, уже начатая попытка не поменяет свои вопросы.

## Типы Вопросов

Backend использует 4 типа вопросов:

```text
SINGLE_CHOICE
MULTIPLE_CHOICE
MATCHING
SHORT_TEXT
```

### SINGLE_CHOICE

Пользователь выбирает один вариант.

Пример вопроса:

```json
{
  "id": 1,
  "position": 1,
  "type": "SINGLE_CHOICE",
  "topic": "HTTP",
  "prompt": "Какой HTTP-метод обычно используют для получения ресурса без изменения состояния сервера?",
  "options": [
    {
      "id": 1,
      "text": "GET"
    },
    {
      "id": 2,
      "text": "POST"
    }
  ],
  "matchLeftItems": [],
  "matchRightItems": []
}
```

Что рисовать на frontend:

```text
radio group
```

Что отправлять:

```json
{
  "selectedOptionIds": [1]
}
```

Да, даже для одного выбранного варианта отправляется массив. Это сделано, чтобы single choice и multiple choice обрабатывались единообразно.

### MULTIPLE_CHOICE

Пользователь выбирает несколько вариантов.

Что рисовать на frontend:

```text
checkbox group
```

Что отправлять:

```json
{
  "selectedOptionIds": [4, 5, 8]
}
```

Frontend не должен знать, какие варианты правильные. Он просто отправляет id тех options, которые выбрал пользователь.

### MATCHING

Пользователь сопоставляет элементы из левой колонки с элементами из правой.

Пример вопроса:

```json
{
  "id": 3,
  "position": 3,
  "type": "MATCHING",
  "topic": "SQL",
  "prompt": "Сопоставь SQL-операцию с ее назначением.",
  "options": [],
  "matchLeftItems": ["SELECT", "INSERT", "UPDATE", "DELETE"],
  "matchRightItems": ["Добавление строки", "Изменение строки", "Получение данных", "Удаление строки"]
}
```

Что рисовать на frontend:

```text
для каждого matchLeftItems элемент select/dropdown с вариантами из matchRightItems
```

Что отправлять:

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

### SHORT_TEXT

Пользователь вводит короткий текст.

Что рисовать на frontend:

```text
input или textarea
```

Что отправлять:

```json
{
  "textAnswer": "extends"
}
```

Backend нормализует регистр и пробелы, поэтому `extends`, ` Extends ` и `EXTENDS` будут считаться одинаковыми.

## Прохождение Теста

Старт попытки:

```http
POST /api/tests/{testId}/attempts
Authorization: Bearer generated-token
```

Пример ответа:

```json
{
  "attemptId": 1,
  "question": {
    "id": 1,
    "position": 1,
    "type": "SINGLE_CHOICE",
    "topic": "HTTP",
    "prompt": "Какой HTTP-метод обычно используют для получения ресурса без изменения состояния сервера?",
    "options": [
      {
        "id": 1,
        "text": "GET"
      }
    ],
    "matchLeftItems": [],
    "matchRightItems": []
  }
}
```

Получить текущее состояние попытки:

```http
GET /api/attempts/{attemptId}
Authorization: Bearer generated-token
```

Пример ответа:

```json
{
  "attemptId": 1,
  "status": "IN_PROGRESS",
  "currentPosition": 2,
  "totalQuestions": 7,
  "question": {
    "id": 2,
    "position": 2,
    "type": "MULTIPLE_CHOICE",
    "topic": "Java Collections",
    "prompt": "Какие коллекции в Java обычно гарантируют уникальность элементов?",
    "options": [],
    "matchLeftItems": [],
    "matchRightItems": []
  }
}
```

Отправить ответ:

```http
POST /api/attempts/{attemptId}/answer
Authorization: Bearer generated-token
Content-Type: application/json
```

Пример ответа backend после обычного вопроса:

```json
{
  "correct": true,
  "explanation": "GET предназначен для чтения ресурса. Он должен быть безопасным: сам запрос не должен менять состояние сервера.",
  "readMoreUrl": "https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/GET",
  "nextQuestion": {
    "id": 2,
    "position": 2,
    "type": "MULTIPLE_CHOICE",
    "topic": "Java Collections",
    "prompt": "Какие коллекции в Java обычно гарантируют уникальность элементов?",
    "options": [],
    "matchLeftItems": [],
    "matchRightItems": []
  },
  "result": null
}
```

Frontend-поведение:

1. Пользователь нажал "Ответить".
2. Клиент отправил ответ.
3. Backend вернул `correct`, `explanation`, `readMoreUrl`.
4. UI показывает, правильно или нет, и пояснение.
5. Кнопка "Далее" берет `nextQuestion` из ответа и показывает его.

После последнего вопроса `nextQuestion` будет `null`, а `result` будет заполнен.

Пример финального ответа:

```json
{
  "correct": true,
  "explanation": "Класс наследует другой класс с помощью extends. Для реализации интерфейса используется implements.",
  "readMoreUrl": "https://docs.oracle.com/javase/tutorial/java/IandI/subclasses.html",
  "nextQuestion": null,
  "result": {
    "attemptId": 1,
    "testTitle": "Java Backend: базовое собеседование",
    "correctAnswers": 7,
    "totalQuestions": 7,
    "weakTopics": [],
    "recommendation": "Отличный результат. Можно переходить к задачам уровня junior+ и практическим проектам.",
    "completedAt": "2026-05-22T10:30:00Z"
  }
}
```

Получить результат отдельно:

```http
GET /api/attempts/{attemptId}/result
Authorization: Bearer generated-token
```

Работает только после завершения попытки.

## AI-Разбор После Теста

После завершения попытки frontend может запросить персональный разбор:

```http
GET /api/attempts/{attemptId}/ai-review
Authorization: Bearer generated-token
```

Ручка доступна только владельцу попытки и только после завершения теста.

Backend берет:

```text
ответы пользователя
ошибочные вопросы
темы вопросов
пояснения
readMoreUrl по каждому вопросу
```

Если настроен `OPENAI_API_KEY`, backend отправляет эти данные модели и просит сделать финальный разбор на русском языке. Важно: модель получает уже подготовленный список ссылок и не должна придумывать URL. Если ключ не настроен или вызов модели не удался, backend вернет fallback-разбор.

Пример ответа:

```json
{
  "attemptId": 1,
  "generatedByAi": true,
  "summary": "Краткий итог по попытке, слабым темам и дальнейшим действиям.",
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
  "nextStep": "Выбери одну слабую тему, прочитай ресурс из списка и затем пройди похожий тест повторно."
}
```

На этом этапе backend уже зафиксировал конкретные вопросы попытки. Frontend не должен сам выбирать вопросы из списка теста.

Рекомендованный UX:

```text
1. Сначала показать обычный result.
2. Затем загрузить /ai-review.
3. Пока идет запрос, показать skeleton или текст "Готовим персональный разбор".
4. Если generatedByAi=false, можно все равно показывать результат как обычную рекомендацию.
```

## Профиль Пользователя

```http
GET /api/profile
Authorization: Bearer generated-token
```

Пример ответа:

```json
{
  "user": {
    "id": 1,
    "email": "student@example.com",
    "username": "Student"
  },
  "recentAttempts": [
    {
      "attemptId": 1,
      "professionTitle": "Backend Java Developer",
      "testTitle": "Java Backend: базовое собеседование",
      "correctAnswers": 7,
      "totalQuestions": 7,
      "completedAt": "2026-05-22T10:30:00Z"
    }
  ],
  "favoriteTests": [
    {
      "testId": 1,
      "professionId": 1,
      "professionTitle": "Backend Java Developer",
      "testTitle": "Java Backend: базовое собеседование",
      "testDescription": "7 вопросов разных типов.",
      "addedAt": "2026-05-23T10:30:00Z"
    }
  ]
}
```

На frontend это можно использовать для блоков "Недавние тесты" и "Избранные тесты" в профиле.

### Избранные Тесты

Получить избранные тесты:

```http
GET /api/profile/favorites
Authorization: Bearer generated-token
```

Добавить тест в избранное:

```http
POST /api/profile/favorites/tests/{testId}
Authorization: Bearer generated-token
```

Удалить тест из избранного:

```http
DELETE /api/profile/favorites/tests/{testId}
Authorization: Bearer generated-token
```

Рекомендованный UX:

```text
на карточке теста показывать кнопку/иконку "В избранное"
если тест уже в избранном, повторный POST просто вернет существующую запись
после DELETE можно убрать тест из списка избранного без перезагрузки страницы
```

## Админская Часть

Админские ручки находятся под:

```text
/api/admin
```

Они требуют обычный `Bearer`-токен, но пользователь должен иметь роль `ADMIN`.

Если токена нет, backend вернет `401`.
Если токен есть, но роль не `ADMIN`, backend вернет `403`.

### Логин Админа

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "email": "admin@example.com",
  "password": "admin123"
}
```

Ответ будет таким же, как у обычного логина, но `user.role` будет `ADMIN`.

### Получить Все Тесты Для Админки

```http
GET /api/admin/tests
Authorization: Bearer admin-token
```

Пример ответа:

```json
[
  {
    "id": 1,
    "professionId": 1,
    "professionTitle": "Backend Java Developer",
    "title": "Java Backend: базовое собеседование",
    "description": "7 вопросов разных типов.",
    "questionCount": 7
  }
]
```

### Получить Тест С Правильными Ответами

```http
GET /api/admin/tests/{testId}
Authorization: Bearer admin-token
```

Эта ручка отличается от публичной `GET /api/tests/{testId}`: она отдает правильные ответы, поэтому доступна только админу.

Пример фрагмента ответа:

```json
{
  "id": 1,
  "professionId": 1,
  "professionTitle": "Backend Java Developer",
  "title": "Java Backend: базовое собеседование",
  "description": "7 вопросов разных типов.",
  "questions": [
    {
      "id": 1,
      "position": 1,
      "type": "SINGLE_CHOICE",
      "topic": "HTTP",
      "prompt": "Какой HTTP-метод используют для получения ресурса?",
      "correctTextAnswer": null,
      "explanation": "GET предназначен для чтения ресурса.",
      "readMoreUrl": "https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/GET",
      "options": [
        {
          "id": 1,
          "text": "GET",
          "correct": true
        }
      ],
      "matchPairs": []
    }
  ]
}
```

### Создать Тест

```http
POST /api/admin/tests
Authorization: Bearer admin-token
Content-Type: application/json
```

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
      "type": "MULTIPLE_CHOICE",
      "topic": "REST",
      "prompt": "Какие признаки относятся к REST API?",
      "correctTextAnswer": null,
      "explanation": "REST обычно строится вокруг ресурсов и стандартных HTTP-методов.",
      "readMoreUrl": "https://restfulapi.net/",
      "options": [
        {
          "text": "Ресурсы имеют URI",
          "correct": true
        },
        {
          "text": "Используются стандартные HTTP-методы",
          "correct": true
        },
        {
          "text": "Сервер хранит состояние UI каждого клиента",
          "correct": false
        }
      ],
      "matchPairs": []
    },
    {
      "type": "MATCHING",
      "topic": "SQL",
      "prompt": "Сопоставь SQL-операцию с назначением.",
      "correctTextAnswer": null,
      "explanation": "SELECT читает данные, INSERT добавляет строки.",
      "readMoreUrl": "https://www.postgresql.org/docs/current/tutorial-sql.html",
      "options": [],
      "matchPairs": [
        {
          "leftLabel": "SELECT",
          "rightLabel": "Получение данных"
        },
        {
          "leftLabel": "INSERT",
          "rightLabel": "Добавление строки"
        }
      ]
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

Правила валидации:

```text
SINGLE_CHOICE - минимум 2 options и ровно 1 correct=true
MULTIPLE_CHOICE - минимум 2 options и минимум 1 correct=true
MATCHING - нужен непустой matchPairs
SHORT_TEXT - нужен correctTextAnswer
```

Важное изменение: вопросы из этого запроса попадают в пул вопросов профессии. При старте попытки backend выбирает из пула нужное количество вопросов каждого типа. Сам тест хранит профессию, название и описание, а не фиксированный список вопросов для каждого прохождения.

### Редактировать Тест

```http
PUT /api/admin/tests/{testId}
Authorization: Bearer admin-token
Content-Type: application/json
```

Body такой же, как у создания.

Важно: сейчас `PUT` полностью заменяет пул вопросов профессии, переданный в body. Старые попытки по затронутой профессии удаляются, потому что их ответы могут не соответствовать новой версии пула.

### Удалить Тест

```http
DELETE /api/admin/tests/{testId}
Authorization: Bearer admin-token
```

Удаляются:

```text
test
questions
options
matching pairs
attempts
attempt answers
```

## Ошибки

Backend возвращает ошибки в формате `ProblemDetail`.

Пример:

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
400 Bad Request - неверный body, попытка уже завершена, повторный ответ
401 Unauthorized - нет токена или неверный логин/пароль
404 Not Found - тест или попытка не найдены
```

## Рекомендуемые Frontend-Роуты

Это не требование backend, просто удобная схема для клиента:

```text
/login
/register
/profile
/professions
/tests/:testId
/attempts/:attemptId
/attempts/:attemptId/result
```

## TypeScript-Модели

Можно положить рядом с API-клиентом:

```ts
export type QuestionType =
  | "SINGLE_CHOICE"
  | "MULTIPLE_CHOICE"
  | "MATCHING"
  | "SHORT_TEXT";

export type User = {
  id: number;
  email: string;
  username: string;
  role: "USER" | "ADMIN";
};

export type AuthResponse = {
  token: string;
  expiresAt: string;
  user: User;
};

export type Option = {
  id: number;
  text: string;
};

export type Question = {
  id: number;
  position: number;
  type: QuestionType;
  topic: string;
  prompt: string;
  options: Option[];
  matchLeftItems: string[];
  matchRightItems: string[];
};

export type Profession = {
  id: number;
  title: string;
  description: string;
  tests: TestSummary[];
};

export type TestSummary = {
  id: number;
  title: string;
  description: string;
  questionCount: number;
};

export type AttemptState = {
  attemptId: number;
  status: "IN_PROGRESS" | "COMPLETED";
  currentPosition: number;
  totalQuestions: number;
  question: Question | null;
};

export type AnswerRequest = {
  selectedOptionIds?: number[];
  matches?: Record<string, string>;
  textAnswer?: string;
};

export type Result = {
  attemptId: number;
  testTitle: string;
  correctAnswers: number;
  totalQuestions: number;
  weakTopics: string[];
  recommendation: string;
  completedAt: string;
};

export type AnswerResponse = {
  correct: boolean;
  explanation: string;
  readMoreUrl: string;
  nextQuestion: Question | null;
  result: Result | null;
};

export type FavoriteTest = {
  testId: number;
  professionId: number;
  professionTitle: string;
  testTitle: string;
  testDescription: string;
  addedAt: string;
};

export type AiTopicReview = {
  topic: string;
  diagnosis: string;
  recommendation: string;
};

export type AiResource = {
  title: string;
  url: string;
  reason: string;
};

export type AiReview = {
  attemptId: number;
  generatedByAi: boolean;
  summary: string;
  topics: AiTopicReview[];
  resources: AiResource[];
  nextStep: string;
};
```

## Полезные Документы

Подробная краткая справка по API:

```text
API.md
```

Готовый сценарий проверок в Postman:

```text
POSTMAN_TESTS.md
```
>>>>>>> dev
