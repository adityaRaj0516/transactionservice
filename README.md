# 💳 Fintech Transaction Management System

A backend service for handling money transfers with **transactional integrity, concurrency safety, and idempotent request handling**.

Built using **Java, Spring Boot, Redis, and MySQL**.

---

## 🚀 Key Capabilities

* **Atomic Transfers** → debit and credit executed within a single transaction
* **Concurrency Safe** → prevents race conditions using row-level locking
* **Idempotent APIs** → reduces duplicate execution under retries
* **Failure Aware** → maintains consistent state under partial failures

---

## ⚙️ System Architecture

```id="arch1"
                ┌────────────────────┐
                │    Controller      │
                └─────────┬──────────┘
                          ↓
                ┌────────────────────┐
                │ TransferService    │
                └─────────┬──────────┘
                          ↓
                ┌────────────────────┐
                │ TransferOrchestrator│ (@Transactional)
                └───────┬────────────┘
                        ↓
        ┌──────────────────────────────┐
        │ AccountService | Idempotency │
        └─────────┬─────────┬─────────┘
                  ↓         ↓
            ┌────────┐  ┌────────┐
            │ MySQL  │  │ Redis  │
            └────────┘  └────────┘
```

---

## 🔁 Transfer Flow

1. Validate request + idempotency key
2. Validate payload consistency (request hash)
3. Check idempotency state (Redis)
4. Acquire distributed lock (best-effort)
5. Lock accounts using `SELECT FOR UPDATE` (ordered)
6. Perform debit + credit
7. Persist transaction + transfer record
8. Commit transaction
9. Cache success response (Redis)

---

## 🔐 Concurrency Control

* **Pessimistic locking (`SELECT FOR UPDATE`)**
* Deterministic ordering:

```id="lock1"
lock(min(accountId)) → lock(max(accountId))
```

Prevents:

* race conditions
* double-spending
* deadlocks

---

## 🔁 Idempotency Design

Each request uses an **Idempotency-Key**.

| Scenario                     | Behavior                        |
| ---------------------------- | ------------------------------- |
| First request                | Executes                        |
| Same key + same payload      | Returns cached response (Redis) |
| Same key + processing        | Rejected                        |
| Same key + different payload | Rejected                        |

---

## 🧠 Hybrid Safety Model

### Redis (Fast Path)

* Request hashing
* Distributed locking
* Response caching

### Database (Consistency Layer)

* Transaction ensures atomic debit/credit
* Persisted transfer record reflects final state

> Note: Redis is primary for idempotency. DB fallback is limited in this phase.

---

## ⚠️ Failure Modes & Handling

### 1. Redis Unavailable

* Request still executes
* Idempotency guarantees may degrade
* DB ensures no partial transaction

---

### 2. Concurrent Requests (Same Account)

* DB row locks serialize access
* Prevents double-spend

---

### 3. Concurrent Requests (Same Idempotency Key)

* One request proceeds
* Others rejected or replay cached response

---

### 4. Business Failure (e.g., insufficient balance)

* Transaction rollback
* No partial state written

---

### 5. DB Failure Mid-Transaction

* Full rollback via `@Transactional`
* System remains consistent

---

## 🧪 Testing Strategy

### 1. Concurrency Testing

* Simulated multiple parallel transfers on same account
* Verified:

  * no negative balances
  * no duplicate deductions

---

### 2. Idempotency Testing

* Replayed same request with identical key
* Verified:

  * single execution
  * consistent response

---

### 3. Failure Scenarios

* Simulated:

  * insufficient balance
  * Redis unavailable
* Verified:

  * rollback correctness
  * system stability

---

### 4. Edge Cases

* same source & target
* invalid payload
* repeated requests

---

## 🛠️ Tech Stack

* Java, Spring Boot
* MySQL (JPA/Hibernate)
* Redis
* Maven

---

## ▶️ Prerequisites

Ensure the following are running:

* MySQL (configured in `application.properties`)
* Redis (default: localhost:6379)

---

## ▶️ Run Locally

```bash id="run1"
git clone https://github.com/adityaRaj0516/transactionservice.git
cd transactionservice
./mvnw spring-boot:run
```

---

## 📌 API

### POST `/transactions/transfer`

Header:

```id="api1"
Idempotency-Key: <unique-key>
```

Body:

```json id="api2"
{
  "sourceId": 1,
  "targetId": 2,
  "amount": 100
}
```

---

## ✅ Guarantees (Phase 0)

* No partial transactions (atomicity ensured)
* Safe under concurrent requests (DB locking)
* Retry-safe for most scenarios
* Consistent state under failures

---

## 👨‍💻 Author

Aditya Raj
Backend Engineering (Java | Spring Boot)
