# common-member-mange

## 도커 컴포즈 실행

```bash
docker compose up --build
```

애플리케이션: http://localhost:8080

## IntelliJ 로컬 개발 + MySQL만 실행

애플리케이션은 IntelliJ에서 직접 실행하고, MySQL만 도커 컴포즈로 올릴 경우 아래처럼 실행합니다.

```bash
docker compose up -d db
```

MySQL 접속 정보:

- Host: `localhost`
- Port: `13306`
- Database: `app_db`
- User: `app_user`
- Password: `app_password`

## MySQL 사용법

이 프로젝트는 Docker Compose로 MySQL(`mysql:8.0`)을 실행하며, SQL 파일로 DB를 초기화합니다.

### 기본 DB 설정

`docker-compose.yml`에 설정되어 있습니다.

- `MYSQL_DATABASE`: `app_db`
- `MYSQL_USER`: `app_user`
- `MYSQL_PASSWORD`: `app_password`
- `MYSQL_ROOT_PASSWORD`: `rootpassword`

### 초기화 SQL

초기화 스크립트는 `/docker-entrypoint-initdb.d/`에 마운트됩니다.

- `db/init/schema.sql`
- `db/init/data.sql`

MySQL은 **컨테이너 최초 생성 시**(`/var/lib/mysql`이 비어 있을 때)만 이 스크립트들을 실행합니다. 현재 컴포즈 구성은 영속 볼륨(`mysql_data`)을 사용하므로, `docker compose up`을 다시 실행해도 초기화 스크립트가 **재실행되지 않습니다**.

### 재초기화(Reset DB)

기존 데이터를 모두 제거하고 `schema.sql` / `data.sql`을 다시 실행하려면:

```bash
docker compose down -v
docker compose up --build
```

볼륨을 지우지 않고 SQL만 다시 적용하고 싶다면, 컨테이너 내부의 MySQL 클라이언트로 수동 실행할 수 있습니다.

### 포트

- MySQL: `localhost:13306`
- App: `localhost:8080`
