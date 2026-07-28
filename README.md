# Bank Card Management System

REST API для управления пользователями и банковскими картами. Приложение поддерживает выпуск и управление картами, просмотр собственных карт и баланса, заявки на блокировку, переводы между картами одного пользователя, JWT-аутентификацию и разграничение доступа по ролям.

Проект выполнен как тестовое задание на Java и Spring Boot. Клиентская часть не требуется: API можно проверять через Swagger UI, Postman или другой HTTP-клиент.

---

## Основные возможности

### Администратор (`ADMIN`)

Администратор может:

- создавать пользователей с ролями `USER` и `ADMIN`;
- получать список пользователей с пагинацией и сортировкой;
- получать пользователя по идентификатору;
- изменять и удалять пользователей;
- выпускать карту для выбранного пользователя;
- просматривать все карты;
- получать карту по идентификатору;
- блокировать, активировать и удалять карты;
- просматривать заявки на блокировку;
- фильтровать заявки по статусу;
- подтверждать или отклонять заявки на блокировку.

### Пользователь (`USER`)

Пользователь может:

- авторизоваться и получить JWT access token;
- просматривать только собственные карты;
- получать информацию о своей карте;
- просматривать баланс своей карты;
- создавать заявку на блокировку собственной карты;
- переводить деньги между двумя собственными картами.

---

## Реализованные требования безопасности

- stateless-аутентификация через JWT;
- пароли пользователей хранятся в виде BCrypt-хешей;
- доступ к операциям ограничен ролями `ADMIN` и `USER`;
- проверка владельца карты выполняется на сервисном уровне;
- номер карты хранится в зашифрованном виде;
- для шифрования используется отдельный ключ приложения и случайный IV;
- полный номер карты не возвращается через API;
- в ответах номер отображается в маскированном виде: `**** **** **** 1234`;
- секрет JWT и ключ шифрования передаются через переменные окружения;
- ошибки авторизации и запрета доступа возвращаются в REST-формате;
- CSRF отключён, поскольку API не использует серверные HTTP-сессии;
- CORS настроен для локальных frontend-адресов `http://localhost:3000` и `http://localhost:5173`.

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

## Архитектура приложения

Приложение разделено на слои. Контроллеры не обращаются напрямую к репозиториям и не содержат бизнес-логику.

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

Дополнительные компоненты отвечают за безопасность, обработку ошибок, валидацию, генерацию и защиту номеров карт.

### Структура проекта

```text
.
├── docs
│   └── openapi.yaml
├── src
│   ├── main
│   │   ├── java/com/example/bankcards
│   │   │   ├── config
│   │   │   │   ├── OpenApiConfig
│   │   │   │   └── SecurityConfig
│   │   │   ├── controller
│   │   │   │   ├── AuthController
│   │   │   │   ├── CardBlockRequestController
│   │   │   │   ├── CardController
│   │   │   │   ├── TransferController
│   │   │   │   └── UserController
│   │   │   ├── dto
│   │   │   │   ├── request
│   │   │   │   └── response
│   │   │   ├── entity
│   │   │   │   ├── enums
│   │   │   │   ├── Card
│   │   │   │   ├── CardBlockRequest
│   │   │   │   └── User
│   │   │   ├── exception
│   │   │   ├── repository
│   │   │   │   ├── CardBlockRequestRepository
│   │   │   │   ├── CardRepository
│   │   │   │   └── UserRepository
│   │   │   ├── security
│   │   │   │   ├── CustomUserDetailsService
│   │   │   │   ├── JwtAuthenticationFilter
│   │   │   │   ├── JwtService
│   │   │   │   ├── RestAccessDeniedHandler
│   │   │   │   ├── RestAuthenticationEntryPoint
│   │   │   │   └── UserPrincipal
│   │   │   ├── service
│   │   │   │   ├── AuthService
│   │   │   │   ├── CardBlockRequestService
│   │   │   │   ├── CardService
│   │   │   │   ├── TransferService
│   │   │   │   └── UserService
│   │   │   ├── util
│   │   │   │   ├── crypto
│   │   │   │   ├── CardNumberGenerator
│   │   │   │   └── CardNumberMasker
│   │   │   └── BankCardsApplication
│   │   └── resources
│   │       ├── db/migration
│   │       └── application.yml
│   └── test
│       └── java/com/example/bankcards
│           ├── controller
│           └── service
├── docker-compose.yml
├── pom.xml
└── README.md
```

### Назначение пакетов

| Пакет | Назначение |
|---|---|
| `config` | Конфигурация Spring Security, OpenAPI, CORS и необходимых Spring-бинов |
| `controller` | REST-эндпоинты, получение HTTP-параметров и возврат ответов |
| `dto.request` | Входные модели API и Jakarta Validation |
| `dto.response` | Выходные модели API и единый формат ошибок |
| `entity` | JPA-сущности и связи между таблицами |
| `entity.enums` | Роли и статусы предметной области |
| `exception` | Предметные исключения и глобальная обработка ошибок |
| `repository` | Доступ к данным через Spring Data JPA |
| `security` | JWT, загрузка пользователя, principal и REST-обработчики ошибок доступа |
| `service` | Бизнес-правила, проверки доступа и транзакционные операции |
| `util` | Генерация, маскирование и криптографическая обработка номеров карт |

---

## Модель данных

### `User`

Пользователь системы содержит:

- идентификатор;
- имя, отчество и фамилию;
- уникальный логин;
- BCrypt-хеш пароля;
- роль `ADMIN` или `USER`.

### `Card`

Банковская карта содержит:

- идентификатор;
- владельца;
- зашифрованный номер;
- IV, используемый при шифровании;
- последние четыре цифры для безопасного отображения;
- версию ключа шифрования;
- срок действия;
- статус;
- баланс;
- версию сущности для контроля конкурентных изменений.

Поддерживаемые статусы карты:

- `ACTIVE`;
- `BLOCKED`;
- `EXPIRED`.

Номер генерируется приложением, проверяется контрольной цифрой, шифруется перед сохранением и маскируется перед возвратом клиенту.

### `CardBlockRequest`

Заявка на блокировку содержит:

- карту;
- статус заявки;
- причину блокировки;
- дату создания;
- дату обработки;
- администратора, обработавшего заявку;
- комментарий администратора.

Поддерживаемые статусы заявки:

- `PENDING`;
- `APPROVED`;
- `REJECTED`.

### Перевод

Перевод выполняется между двумя картами одного авторизованного пользователя.

Перед изменением баланса сервис проверяет:

- что указаны разные карты;
- что обе карты существуют;
- что обе карты принадлежат текущему пользователю;
- что карты доступны для перевода;
- что сумма положительная;
- что на исходной карте достаточно средств.

Списание и зачисление выполняются в одной транзакции. При ошибке вся операция откатывается.

---

## Требования для запуска

Перед запуском должны быть установлены:

- JDK 17 или новее;
- Maven 3.9 или новее;
- Docker Desktop или Docker Engine;
- Docker Compose;
- Git.

Проверка установленных версий:

```bash
java -version
mvn -version
docker --version
docker compose version
```

---

## Переменные окружения

В корне проекта используется файл `.env` для локальной dev-среды.

Пример:

```dotenv
MYSQL_DATABASE=bank_card_management
MYSQL_USER=bank_user
MYSQL_PASSWORD=bank_password
MYSQL_ROOT_PASSWORD=root_password

DB_URL=jdbc:mysql://localhost:3306/bank_card_management
DB_USERNAME=bank_user
DB_PASSWORD=bank_password

JWT_SECRET=REPLACE_WITH_BASE64_SECRET
CARD_ENCRYPTION_KEY=REPLACE_WITH_BASE64_KEY
```

`JWT_SECRET` и `CARD_ENCRYPTION_KEY` должны содержать криптографически стойкие значения. Для локальной среды можно сгенерировать два разных Base64-значения длиной 32 байта:

```bash
openssl rand -base64 32
openssl rand -base64 32
```

Первое значение используется как `JWT_SECRET`, второе — как `CARD_ENCRYPTION_KEY`.

Настройки JWT по умолчанию:

- issuer: `bank-card-management`;
- время жизни access token: `3600000` мс, то есть 1 час.

> Не публикуйте настоящие production-секреты. В публичном репозитории рекомендуется хранить `.env.example`, а `.env` добавить в `.gitignore`.

---

## Запуск проекта

### 1. Клонировать репозиторий

```bash
git clone <repository-url>
cd bank_rest
```

### 2. Настроить окружение

Создайте `.env` в корне проекта и заполните его по примеру выше.

При запуске приложения из IntelliJ IDEA укажите переменные приложения в:

```text
Run → Edit Configurations → Environment variables
```

Минимально необходимы:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
CARD_ENCRYPTION_KEY
```

### 3. Запустить MySQL

В проекте Docker Compose используется для запуска MySQL. Spring Boot запускается отдельно.

```bash
docker compose up -d
```

Проверить состояние контейнера:

```bash
docker compose ps
```

Параметры локальной базы данных:

| Параметр | Значение |
|---|---|
| Host | `localhost` |
| Port | `3306` |
| Database | `bank_card_management` |
| Username | `bank_user` |
| Password | `bank_password` |

### 4. Запустить приложение

Через Maven:

```bash
mvn spring-boot:run
```

Или запустите класс:

```text
com.example.bankcards.BankCardsApplication
```

из IntelliJ IDEA.

Приложение доступно по адресу:

```text
http://localhost:8080
```

### 5. Применение миграций

Liquibase запускается автоматически вместе с приложением.

Главный changelog:

```text
src/main/resources/db/migration/db.changelog-master.yaml
```

Hibernate работает в режиме проверки схемы:

```text
ddl-auto: validate
```

Hibernate не создаёт таблицы самостоятельно. Структура базы должна соответствовать Liquibase-миграциям.

---

## Создание первого администратора

Управление пользователями защищено ролью `ADMIN`, поэтому для первой авторизации в пустой базе необходим начальный администратор.

Выполните SQL-запрос через MySQL-консоль или окно Database в IntelliJ IDEA:

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
    '$2y$10$JMtxrgiYJCGY6Mgx4DBHmepjah/5nlSuAjx5r/bAmHDvl.ex3LvlG'
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE login = 'admin'
);
```

Данные для локальной проверки:

```text
login: admin
password: Admin123!
```

Хеш в примере соответствует паролю `Admin123!` и предназначен только для локальной dev-среды.

---

## Авторизация

### Получение JWT

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

После успешной авторизации API возвращает access token.

Для защищённых запросов передавайте его в заголовке:

```http
Authorization: Bearer <access-token>
```

### Авторизация в Swagger UI

1. Выполните `POST /api/auth/login`.
2. Скопируйте полученный access token.
3. Нажмите кнопку **Authorize**.
4. Вставьте токен без префикса `Bearer`.
5. Нажмите **Authorize**.

Swagger UI самостоятельно сформирует заголовок `Authorization: Bearer ...`.

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

Зафиксированная спецификация находится в:

```text
docs/openapi.yaml
```

После изменения контроллеров или DTO спецификацию можно обновить командой:

```bash
curl http://localhost:8080/v3/api-docs.yaml -o docs/openapi.yaml
```

PowerShell:

```powershell
Invoke-WebRequest `
    -Uri "http://localhost:8080/v3/api-docs.yaml" `
    -OutFile ".\docs\openapi.yaml"
```

Swagger и OpenAPI открыты без JWT, но выполнение защищённых бизнес-операций требует авторизации.

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
| `GET` | `/api/admin/users` | ADMIN | Получение страницы пользователей |
| `GET` | `/api/admin/users/{userId}` | ADMIN | Получение пользователя |
| `PUT` | `/api/admin/users/{userId}` | ADMIN | Изменение пользователя |
| `DELETE` | `/api/admin/users/{userId}` | ADMIN | Удаление пользователя |

### Карты: операции администратора

| Метод | URL | Доступ | Назначение |
|---|---|---|---|
| `POST` | `/api/admin/users/{userId}/cards` | ADMIN | Выпуск карты пользователю |
| `GET` | `/api/admin/cards` | ADMIN | Получение страницы всех карт |
| `GET` | `/api/admin/cards/{cardId}` | ADMIN | Получение карты |
| `PATCH` | `/api/admin/cards/{cardId}/block` | ADMIN | Блокировка карты |
| `PATCH` | `/api/admin/cards/{cardId}/activate` | ADMIN | Активация карты |
| `DELETE` | `/api/admin/cards/{cardId}` | ADMIN | Удаление карты |

### Карты: операции пользователя

| Метод | URL | Доступ | Назначение |
|---|---|---|---|
| `GET` | `/api/cards` | USER, ADMIN | Получение собственных карт |
| `GET` | `/api/cards/{cardId}` | USER, ADMIN | Получение собственной карты |
| `GET` | `/api/cards/{cardId}/balance` | USER, ADMIN | Получение баланса собственной карты |

### Заявки на блокировку

| Метод | URL | Доступ | Назначение |
|---|---|---|---|
| `POST` | `/api/cards/{cardId}/block-requests` | USER | Создание заявки |
| `GET` | `/api/admin/block-requests` | ADMIN | Получение страницы заявок |
| `GET` | `/api/admin/block-requests/{requestId}` | ADMIN | Получение заявки |
| `PATCH` | `/api/admin/block-requests/{requestId}/approve` | ADMIN | Подтверждение заявки |
| `PATCH` | `/api/admin/block-requests/{requestId}/reject` | ADMIN | Отклонение заявки |

### Переводы

| Метод | URL | Доступ | Назначение |
|---|---|---|---|
| `POST` | `/api/transfers` | USER | Перевод между собственными картами |

Полное описание моделей запросов, ответов и кодов ошибок находится в Swagger UI и `docs/openapi.yaml`.

---

## Пагинация, сортировка и фильтрация

Страничные эндпоинты используют стандартные параметры Spring Data:

```http
GET /api/admin/users?page=0&size=10&sort=id,desc
```

Параметры:

- `page` — номер страницы, начиная с `0`;
- `size` — количество элементов на странице;
- `sort` — поле и направление сортировки.

Фильтрация заявок по статусу:

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

### Создание карты

```http
POST /api/admin/users/2/cards
Authorization: Bearer <admin-token>
```

Номер карты создаётся приложением автоматически.

### Создание заявки на блокировку

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

### Перевод между картами

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

Приложение использует единый `GlobalExceptionHandler` и DTO `ApiErrorResponse`.

Основные HTTP-статусы:

| Код | Значение |
|---|---|
| `400 Bad Request` | Ошибка валидации или некорректный запрос |
| `401 Unauthorized` | Отсутствует или недействителен JWT |
| `403 Forbidden` | Недостаточно прав или доступ к чужой карте |
| `404 Not Found` | Пользователь, карта или заявка не найдены |
| `409 Conflict` | Конфликт бизнес-состояния |
| `500 Internal Server Error` | Непредвиденная внутренняя ошибка |

К конфликтам бизнес-состояния относятся, например:

- недостаточно средств;
- карта заблокирована;
- срок действия карты истёк;
- заявка уже обработана;
- активная заявка на блокировку уже существует;
- логин уже занят.

---

## Тестирование

В проекте реализованы unit-тесты сервисного и контроллерного слоёв.

### Сервисные тесты

- `AuthServiceTest`
- `UserServiceTest`
- `CardServiceTest`
- `CardBlockRequestServiceTest`
- `TransferServiceTest`

Сервисные тесты проверяют бизнес-правила с использованием Mockito.

### Тесты контроллеров

- `AuthControllerTest`
- `UserControllerTest`
- `CardControllerTest`
- `CardBlockRequestControllerTest`
- `TransferControllerTest`

Контроллеры тестируются через MockMvc с изолированными моками сервисов.

Запуск всех тестов:

```bash
mvn clean test
```

Сборка проекта:

```bash
mvn clean package
```

---

## Остановка и очистка dev-среды

Остановить контейнеры:

```bash
docker compose down
```

Остановить контейнеры и удалить том с данными MySQL:

```bash
docker compose down -v
```

> Команда с `-v` полностью удаляет локальную базу данных. При следующем запуске Liquibase создаст схему заново.

---

## Краткий сценарий проверки

1. Запустить MySQL: `docker compose up -d`.
2. Запустить `BankCardsApplication`.
3. Убедиться, что Liquibase применил миграции.
4. Открыть Swagger UI.
5. Создать начального администратора, если база пустая.
6. Выполнить `POST /api/auth/login`.
7. Авторизоваться в Swagger через полученный JWT.
8. Создать пользователя с ролью `USER`.
9. Выпустить ему две карты.
10. Авторизоваться под пользователем.
11. Проверить просмотр карт и баланса.
12. Выполнить перевод между картами.
13. Создать заявку на блокировку.
14. Авторизоваться под администратором и обработать заявку.
15. Запустить `mvn clean test`.

---

## Особенности реализации

- приложение не использует серверные сессии;
- текущий пользователь определяется из JWT;
- Entity `User` не реализует `UserDetails`;
- для Spring Security используется отдельный `UserPrincipal`;
- бизнес-правила находятся в сервисах;
- операции изменения данных выполняются транзакционно;
- переводы не допускаются между картами разных пользователей;
- номер карты никогда не возвращается клиенту открытым текстом;
- схема базы управляется только Liquibase;
- Hibernate проверяет соответствие Entity и схемы базы;
- Swagger-аннотации расположены непосредственно в REST-контроллерах;
- OpenAPI-спецификация хранится в репозитории в формате YAML.
