# Database Schema

```mermaid
erDiagram
    USERS {
        long id PK
        string email UK
        string username
        string password_hash
        string role
        datetime created_at
    }

    AUTH_SESSION {
        long id PK
        string token UK
        long user_id FK
        datetime expires_at
    }

    PROFESSION {
        long id PK
        string title UK
        string description
    }

    INTERVIEW_TEST {
        long id PK
        long profession_id FK
        string title
        string description
    }

    QUESTION {
        long id PK
        long profession_id FK
        int position
        string type
        string topic
        text prompt
        string correct_text_answer
        text explanation
        string read_more_url
    }

    QUESTION_OPTION {
        long id PK
        long question_id FK
        text text
        boolean correct
    }

    MATCH_PAIR {
        long id PK
        long question_id FK
        string left_label
        string right_label
    }

    TEST_ATTEMPT {
        long id PK
        long user_id FK
        long test_id FK
        int current_position
        int total_questions
        int correct_answers
        string status
        datetime started_at
        datetime completed_at
    }

    ATTEMPT_QUESTION {
        long id PK
        long attempt_id FK
        long question_id FK
        int position
    }

    ATTEMPT_ANSWER {
        long id PK
        long attempt_id FK
        long question_id FK
        boolean correct
        text submitted_answer
        datetime answered_at
    }

    FAVORITE_TEST {
        long id PK
        long user_id FK
        long test_id FK
        datetime created_at
    }

    USERS ||--o{ AUTH_SESSION : "has sessions"
    USERS ||--o{ TEST_ATTEMPT : "starts"
    USERS ||--o{ FAVORITE_TEST : "favorites"

    PROFESSION ||--o{ INTERVIEW_TEST : "contains tests"
    PROFESSION ||--o{ QUESTION : "owns question pool"

    INTERVIEW_TEST ||--o{ TEST_ATTEMPT : "is attempted in"
    INTERVIEW_TEST ||--o{ FAVORITE_TEST : "can be favorite"

    QUESTION ||--o{ QUESTION_OPTION : "has choice options"
    QUESTION ||--o{ MATCH_PAIR : "has matching pairs"
    QUESTION ||--o{ ATTEMPT_QUESTION : "selected for attempt"
    QUESTION ||--o{ ATTEMPT_ANSWER : "is answered in"

    TEST_ATTEMPT ||--o{ ATTEMPT_QUESTION : "fixed question set"
    TEST_ATTEMPT ||--o{ ATTEMPT_ANSWER : "stores answers"
```

## Notes

`QUESTION` belongs to a profession question pool, not directly to a test.

When a user starts an attempt, the backend selects questions from the profession pool and stores the concrete selected set in `ATTEMPT_QUESTION`.

Current composition rule:

```text
up to 2 SINGLE_CHOICE
up to 2 MULTIPLE_CHOICE
up to 1 MATCHING
up to 2 SHORT_TEXT
```

If the pool has fewer questions of a type, the backend takes all available questions of that type.

Correct answer storage depends on question type:

```text
SINGLE_CHOICE   -> QUESTION_OPTION.correct
MULTIPLE_CHOICE -> QUESTION_OPTION.correct
MATCHING        -> MATCH_PAIR.left_label + MATCH_PAIR.right_label
SHORT_TEXT      -> QUESTION.correct_text_answer
```

Public endpoints do not expose correct answers. Correct flags and text answers are returned only from admin endpoints.
