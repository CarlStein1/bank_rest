# Bank Card Management System

REST API для управления пользователями, банковскими картами, заявками на блокировку и переводами между собственными картами.

Проект выполнен как тестовое задание на Java и Spring Boot. Отдельный frontend не требуется: API можно проверять через Swagger UI, Postman или другой HTTP-клиент.

---

## Возможности системы

### Администратор (`ADMIN`)

- создание пользователей с ролями `USER` и `ADMIN`;
- просмотр пользователей с пагинацией и сортировкой;
- получение, изменение и удаление пользователя;
- выпуск карты для выбранного пользователя;
- просмотр всех карт с пагинацией;
- получение карты по идентификатору;
- блокировка, активация и удаление карт;
- просмотр заявок на блокировку;
- фильтрация заявок по статусу;
- подтверждение и отклонение заявок.

### Пользователь (`USER`)

- авторизация и получение JWT access token;
- просмотр собственных карт с пагинацией;
- поиск карты по последним четырём цифрам номера;
- фильтрация карт по статусу;
- получение собственной карты и её баланса;
- создание заявки на блокировку;
- перевод денег между двумя собственными картами.

---

## Ключевые особенности

- stateless-аутентификация через JWT;
- разграничение доступа по ролям `ADMIN` и `USER`;
- BCrypt-хеширование паролей;
- проверка владельца карты на сервисном уровне;
- шифрование номера карты через `AES/GCM/NoPadding`;
- отдельный случайный IV для каждой операции шифрования;
- хранение версии ключа шифрования;
- возврат только маскированного номера: `**** **** **** 1234`;
- пагинация через Spring Data и стабильное представление `PagedModel`;
- поиск собственных карт по `lastFour`;
- фильтрация карт и заявок по статусу;
- транзакционные переводы;
- optimistic locking для изменения баланса;
- Liquibase как единственный источник структуры БД;
- единый формат ошибок `ApiErrorResponse`;
- отдельные предметные исключения для бизнес-конфликтов;
- технические ошибки шифрования обрабатываются как `500 Internal Server Error`;
- OpenAPI-аннотации описывают успешные и ошибочные ответы.

---

## Технологический стек

- Java 17+
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Hibernate
- Jakarta Validation
- JWT
- MySQL 8.4
- Liquibase
- Docker Compose
- Maven
- Swagger UI / OpenAPI
- JUnit 5
- Mockito
- MockMvc
- Lombok

---

## Архитектура

```text
HTTP request
    |
    v
Controller
    |
    v
Service
    |
    v
Repository
    |
    v
MySQL
```

Контроллеры не обращаются напрямую к репозиториям и не содержат бизнес-логику.

### Структура проекта

```text
.
├── docs
│   └── openapi.yaml
├── src
│   ├── main
│   │   ├── java/com/example/bankcards
│   │   │   ├── config
│   │   │   ├── controller
│   │   │   ├── dto
│   │   │   │   ├── request
│   │   │   │   └── response
│   │   │   ├── entity
│   │   │   │   └── enums
│   │   │   ├── exception
│   │   │   ├── repository
│   │   │   ├── security
│   │   │   ├── service
│   │   │   ├── util
│   │   │   │   └── crypto
│   │   │   └── BankCardsApplication
│   │   └── resources
│   │       ├── db/migration
│   │       └── application.yml
│   └── test
│       └── java/com/example/bankcards
│           ├── controller
│           ├── entity
│           └── service
├── .env.example
├── docker-compose.yml
├── pom.xml
└── README.md
```

### Назначение пакетов

| Пакет | Назначение |
|---|---|
| `config` | Настройка Spring Security, OpenAPI, CORS и Spring-бинов |
| `controller` | REST-эндпоинты и работа с HTTP-запросами |
| `dto.request` | Входные модели и Jakarta Validation |
| `dto.response` | Выходные модели и единый формат ошибок |
| `entity` | JPA-сущности и методы изменения состояния |
| `entity.enums` | Роли и статусы предметной области |
| `exception` | Предметные исключения и глобальная обработка ошибок |
| `repository` | Доступ к данным через Spring Data JPA |
| `security` | JWT, principal и обработчики `401`/`403` |
| `service` | Бизнес-правила, проверки доступа и транзакции |
| `util` | Генерация, маскирование и шифрование номеров карт |

---

## Модель данных

### `User`

Содержит идентификатор, ФИО, уникальный логин, BCrypt-хеш пароля и роль `ADMIN` или `USER`.

### `Card`

Содержит:

- владельца;
- зашифрованный номер;
- IV для AES-GCM;
- последние четыре цифры;
- версию ключа шифрования;
- срок действия;
- статус;
- баланс;
- версию сущности для optimistic locking.

Статусы карты:

- `ACTIVE`;
- `BLOCKED`;
- `EXPIRED`.

Номер генерируется приложением, проходит проверку контрольной цифры, шифруется перед сохранением и возвращается только в маскированном виде.

### `CardBlockRequest`

Содержит карту, статус, причину, дату создания, дату обработки, администратора и комментарий администратора.

Статусы заявки:

- `PENDING`;
- `APPROVED`;
- `REJECTED`.

### Перевод

Перевод выполняется только между двумя картами одного авторизованного пользователя.

Проверяется:

- что выбраны разные карты;
- что обе карты существуют;
- что обе карты принадлежат текущему пользователю;
- что карты активны и не просрочены;
- что сумма положительная;
- что средств достаточно.

Списание и зачисление выполняются в одной транзакции.

---

## Безопасность и шифрование

### JWT

После входа клиент получает access token и передаёт его в заголовке:

```http
Authorization: Bearer <access-token>
```

### Шифрование номера карты

Используется:

```text
AES/GCM/NoPadding
```

Для каждой операции шифрования создаётся новый 12-байтовый IV. Полный номер карты не возвращается через REST API.

Ключ передаётся через:

```text
CARD_ENCRYPTION_KEY
```

Рекомендуется использовать 32 случайных байта в Base64. JWT-секрет и ключ шифрования должны быть разными.

---

## Конфигурация

Основной файл:

```text
src/main/resources/application.yml
```

Актуальная структура настроек безопасности:

```yaml
app:
  security:
    jwt:
      secret: ${JWT_SECRET}
      access-token-expiration-ms: ${JWT_ACCESS_TOKEN_EXPIRATION_MS:3600000}
      issuer: ${JWT_ISSUER:bank-card-management}

    card-encryption:
      key: ${CARD_ENCRYPTION_KEY}
      key-version: ${CARD_ENCRYPTION_KEY_VERSION:1}
```

`CardNumberCrypto` использует те же свойства:

```java
@Value("${app.security.card-encryption.key}")
String encodedKey,

@Value("${app.security.card-encryption.key-version:1}")
short keyVersion
```

---

## Требования для запуска

- JDK 17+
- Maven 3.9+
- Docker Desktop или Docker Engine
- Docker Compose
- Git

Проверка:

```bash
java -version
mvn -version
docker --version
docker compose version
```

---

## Переменные окружения

Создайте `.env` на основе `.env.example`:

```dotenv
SERVER_PORT=8080

MYSQL_ROOT_PASSWORD=root_password
MYSQL_DATABASE=bank_card_management
MYSQL_USER=bank_user
MYSQL_PASSWORD=bank_password

DB_URL=jdbc:mysql://localhost:3306/bank_card_management
DB_USERNAME=bank_user
DB_PASSWORD=bank_password

JWT_SECRET=REPLACE_WITH_BASE64_SECRET
JWT_ACCESS_TOKEN_EXPIRATION_MS=3600000
JWT_ISSUER=bank-card-management

CARD_ENCRYPTION_KEY=REPLACE_WITH_BASE64_AES_KEY
CARD_ENCRYPTION_KEY_VERSION=1
```

Генерация ключа AES-256:

```bash
openssl rand -base64 32
```

Генерация отдельного JWT-секрета:

```bash
openssl rand -base64 64
```

PowerShell для AES-ключа:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

> `.env` не должен попадать в Git. В репозитории хранится только `.env.example`.

### Важно

Docker Compose автоматически читает `.env` из корня проекта. Spring Boot, запущенный отдельно через IntelliJ IDEA или Maven, не обязан автоматически загружать этот файл.

В IntelliJ IDEA укажите переменные здесь:

```text
Run → Edit Configurations → BankCardsApplication → Environment variables
```

Минимально необходимы:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
CARD_ENCRYPTION_KEY
```

---

## Запуск проекта

### 1. Клонировать репозиторий

```bash
git clone https://github.com/CarlStein1/bank_rest.git
cd bank_rest
```

### 2. Создать `.env`

```bash
cp .env.example .env
```

PowerShell:

```powershell
Copy-Item .env.example .env
```

### 3. Запустить MySQL

```bash
docker compose up -d
```

Проверить контейнер:

```bash
docker compose ps
```

### 4. Запустить приложение

```bash
mvn spring-boot:run
```

Либо запустить класс:

```text
com.example.bankcards.BankCardsApplication
```

Адрес приложения:

```text
http://localhost:8080
```

### 5. Миграции

Liquibase запускается автоматически.

Главный changelog:

```text
src/main/resources/db/migration/db.changelog-master.yaml
```

Hibernate настроен на проверку схемы:

```yaml
spring:
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
```

---

## Создание первого администратора

Для первой авторизации в пустой базе можно вручную добавить локального администратора:

```sql
INSERT INTO users (
    first_name,
    middle_name,
    last_name,
    role,
    login,
    password_hash
)
SELECT
    'System',
    NULL,
    'Administrator',
    'ADMIN',
    'admin',
    '$2a$10$zfRS82m69L4VIHr6lhp5ue1D8X80TavnQg0Mm4F97uY9klLBL0U32'
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE login = 'admin'
);
```

```text
login: admin
password: Admin123!
```

Этот пользователь предназначен только для локальной dev-среды.

---

## Авторизация

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "login": "admin",
  "password": "Admin123!"
}
```

В Swagger UI:

1. выполнить `POST /api/auth/login`;
2. скопировать access token;
3. нажать **Authorize**;
4. вставить токен без префикса `Bearer`.

---

## Документация API

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

OpenAPI YAML:

```text
http://localhost:8080/v3/api-docs.yaml
```

Зафиксированная спецификация:

```text
docs/openapi.yaml
```

Обновление в PowerShell:

```powershell
Invoke-WebRequest `
    -Uri "http://localhost:8080/v3/api-docs.yaml" `
    -OutFile ".\docs\openapi.yaml"
```

Ошибочные ответы должны ссылаться на:

```yaml
$ref: '#/components/schemas/ApiErrorResponse'
```

---

## Основные эндпоинты

### Авторизация

| Метод | URL | Доступ | Назначение |
|---|---|---|---|
| `POST` | `/api/auth/login` | Public | Получение JWT |

### Пользователи

| Метод | URL | Доступ | Назначение |
|---|---|---|---|
| `POST` | `/api/admin/users` | ADMIN | Создание пользователя |
| `GET` | `/api/admin/users` | ADMIN | Страница пользователей |
| `GET` | `/api/admin/users/{userId}` | ADMIN | Получение пользователя |
| `PUT` | `/api/admin/users/{userId}` | ADMIN | Изменение пользователя |
| `DELETE` | `/api/admin/users/{userId}` | ADMIN | Удаление пользователя |

### Карты администратора

| Метод | URL | Доступ | Назначение |
|---|---|---|---|
| `POST` | `/api/admin/users/{userId}/cards` | ADMIN | Выпуск карты |
| `GET` | `/api/admin/cards` | ADMIN | Страница всех карт |
| `GET` | `/api/admin/cards/{cardId}` | ADMIN | Получение карты |
| `PATCH` | `/api/admin/cards/{cardId}/block` | ADMIN | Блокировка карты |
| `PATCH` | `/api/admin/cards/{cardId}/activate` | ADMIN | Активация карты |
| `DELETE` | `/api/admin/cards/{cardId}` | ADMIN | Удаление карты |

### Карты пользователя

| Метод | URL | Доступ | Назначение |
|---|---|---|---|
| `GET` | `/api/cards` | USER, ADMIN | Просмотр, поиск и фильтрация собственных карт |
| `GET` | `/api/cards/{cardId}` | USER, ADMIN | Получение собственной карты |
| `GET` | `/api/cards/{cardId}/balance` | USER, ADMIN | Получение баланса |

### Заявки на блокировку

| Метод | URL | Доступ | Назначение |
|---|---|---|---|
| `POST` | `/api/cards/{cardId}/block-requests` | USER | Создание заявки |
| `GET` | `/api/admin/block-requests` | ADMIN | Просмотр и фильтрация заявок |
| `GET` | `/api/admin/block-requests/{requestId}` | ADMIN | Получение заявки |
| `PATCH` | `/api/admin/block-requests/{requestId}/approve` | ADMIN | Подтверждение заявки |
| `PATCH` | `/api/admin/block-requests/{requestId}/reject` | ADMIN | Отклонение заявки |

### Переводы

| Метод | URL | Доступ | Назначение |
|---|---|---|---|
| `POST` | `/api/transfers` | USER | Перевод между собственными картами |

---

## Пагинация, поиск и фильтрация

Страничные эндпоинты используют:

- `page` — номер страницы с `0`;
- `size` — количество элементов;
- `sort` — поле и направление сортировки.

Пример:

```http
GET /api/admin/users?page=0&size=10&sort=id,desc
```

Ответ формируется через `PagedModel`:

```json
{
  "content": [],
  "page": {
    "size": 10,
    "number": 0,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

### Собственные карты

Без фильтров:

```http
GET /api/cards?page=0&size=10
```

По последним четырём цифрам:

```http
GET /api/cards?lastFour=3456&page=0&size=10
```

По статусу:

```http
GET /api/cards?status=ACTIVE&page=0&size=10
```

Одновременно:

```http
GET /api/cards?lastFour=3456&status=ACTIVE&page=0&size=10&sort=id,desc
```

`lastFour` должен состоять ровно из четырёх цифр.

Допустимые статусы:

```text
ACTIVE
BLOCKED
EXPIRED
```

### Заявки

```http
GET /api/admin/block-requests?status=PENDING&page=0&size=10
```

---

## Примеры запросов

### Создание пользователя

```http
POST /api/admin/users
Authorization: Bearer <admin-token>
Content-Type: application/json
```

```json
{
  "firstName": "Иван",
  "middleName": "Иванович",
  "lastName": "Иванов",
  "role": "USER",
  "login": "ivanov",
  "password": "Password123!"
}
```

### Выпуск карты

```http
POST /api/admin/users/2/cards
Authorization: Bearer <admin-token>
```

### Заявка на блокировку

```http
POST /api/cards/1/block-requests
Authorization: Bearer <user-token>
Content-Type: application/json
```

```json
{
  "reason": "Карта потеряна"
}
```

### Перевод

```http
POST /api/transfers
Authorization: Bearer <user-token>
Content-Type: application/json
```

```json
{
  "fromCardId": 1,
  "toCardId": 2,
  "amount": 500.00
}
```

---

## Обработка ошибок

Единый формат:

```json
{
  "timestamp": "2026-07-28T19:34:52.426",
  "status": 404,
  "error": "Not Found",
  "message": "Карта с идентификатором 15 не найдена",
  "path": "/api/cards/15"
}
```

| Код | Назначение |
|---|---|
| `400 Bad Request` | Ошибка валидации, JSON, параметра или enum |
| `401 Unauthorized` | Отсутствует или недействителен JWT |
| `403 Forbidden` | Недостаточно прав или доступ к чужому ресурсу |
| `404 Not Found` | Объект не найден |
| `405 Method Not Allowed` | HTTP-метод не поддерживается |
| `409 Conflict` | Конфликт бизнес-состояния |
| `415 Unsupported Media Type` | Неподдерживаемый `Content-Type` |
| `500 Internal Server Error` | Непредвиденная техническая ошибка |

К бизнес-конфликтам относятся:

- карта уже заблокирована;
- карта уже активна;
- карта просрочена;
- карта недоступна для перевода;
- недостаточно средств;
- заявка уже обработана;
- активная заявка уже существует;
- логин уже занят.

Для них используются предметные исключения. Общий `IllegalStateException` не применяется как универсальное бизнес-исключение. Технические ошибки шифрования и расшифрования приводят к `500`.

---

## Тестирование

Сервисные тесты:

- `AuthServiceTest`
- `UserServiceTest`
- `CardServiceTest`
- `CardBlockRequestServiceTest`
- `TransferServiceTest`

Контроллерные тесты:

- `AuthControllerTest`
- `UserControllerTest`
- `CardControllerTest`
- `CardBlockRequestControllerTest`
- `TransferControllerTest`

Также присутствуют предметные тесты состояний карт и заявок.

Запуск:

```bash
mvn clean test
```

Сборка:

```bash
mvn clean package
```

---

## Финальная проверка

1. `docker compose up -d`
2. Запустить `BankCardsApplication`.
3. Проверить применение Liquibase.
4. Открыть Swagger UI.
5. Авторизоваться как `ADMIN`.
6. Создать пользователя `USER`.
7. Выпустить две карты.
8. Проверить пагинацию карт.
9. Проверить `lastFour` и `status`.
10. Проверить баланс и перевод.
11. Создать и обработать заявку на блокировку.
12. Проверить ответы `400`, `401`, `403`, `404` и `409`.
13. Обновить `docs/openapi.yaml`.
14. Выполнить:

```bash
mvn clean test
mvn clean package
```

---

## Остановка и очистка

```bash
docker compose down
```

Удаление контейнеров и тома MySQL:

```bash
docker compose down -v
```

> `-v` полностью удаляет локальную базу данных.

### Смена ключа шифрования

Текущая реализация использует один активный ключ и проверяет его версию. После замены `CARD_ENCRYPTION_KEY` старые номера карт нельзя будет расшифровать новым ключом.

Для локальной среды можно пересоздать базу:

```bash
docker compose down -v
docker compose up -d
```

Для production-среды необходим механизм ротации ключей.

---

## Дополнительные замечания

- приложение не использует серверные HTTP-сессии;
- текущий пользователь определяется из JWT;
- Entity не возвращаются напрямую через REST API;
- DTO запросов валидируются через Jakarta Validation;
- страницы возвращаются через `PagedModel`;
- полный номер карты не возвращается клиенту;
- схема базы управляется Liquibase;
- Hibernate работает в режиме `validate`;
- ошибки в Swagger описаны через `ApiErrorResponse`;
- OpenAPI-спецификация хранится в `docs/openapi.yaml`.
