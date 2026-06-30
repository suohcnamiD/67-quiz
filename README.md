# 67-quiz

## Deploying
The workflow builds and publishes two Docker Images: one for the backend and one for the frontend. You can use them to deploy the app.
Compose:
```
services:
  db:
    image: mariadb:latest
    environment:
      MYSQL_ROOT_PASSWORD: example
      MYSQL_DATABASE: main
      MYSQL_USER: user
      MYSQL_PASSWORD: password
    healthcheck:
      test: ["CMD", "healthcheck.sh", "--connect", "--innodb_initialized"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 30s
    volumes:
      - sixseven_data:/var/lib/mysql
  backend:
    image: ghcr.io/suohcnamid/67-quiz:latest # for the latest release
    environment:
      - DATABASE_URL=jdbc:mariadb://db:3306/main
      - DATABASE_USERNAME=user
      - DATABASE_PASSWORD=password
    ports:
      - "127.0.0.1:10005:8080"
    depends_on:
      db:
        condition: service_healthy
  frontend:
    image: ghcr.io/suohcnamid/67-quiz-frontend:latest # for the latest release
    ports:
      - "127.0.0.1:10004:80"
    depends_on:
      - backend

volumes:
  sixseven_data:
```

To deploy dev builds, see available tags in the [packages](https://github.com/suohcnamiD?tab=packages&repo_name=67-quiz). The tags for backend and frontend are the same.

## Intro
Good day well-respected sirs, this is a repo for 67 Quiz.

It consists of two root folders:
- ./backend
- ./frontend

### Backend
The backend is a bootstrapped Spring Boot project.
It was created from a template that has:
- Basic Web MVC
- Authentication with Spring Security and DB-based sessions
- Authorization (Roles) with Spring Security
- Mapstruct for mappings between entities and use-case-specific DTOs
- Liquibase for DB schema migration
- An OpenAPI spec generator

### Frontend
The frontend is a Vue app.

### Workflow
The repo has a workflow that runs a check to validate the API usage in frontend:
1. Generate the OpenAPI spec from the backend
2. Run **orval** to generate the API client for TypeScript
3. Run vue-tsc to check for type errors in the frontend

It's triggered upon pushing into any branch because it doesn't change anything.


## Guidelines

### Contributing
No pushing into main!!! This is enforced via a protection rule - create pull requests instead.
To use the REST API, use the generated API client in `./frontend/src/api` instead of making HTTP calls directly.
To modify the database, add new Liquibase changelogs in `./backend/src/main/resources/db/changelog` and add them to the main changelog.

### Project structure
#### Backend
The classes are organized per-feature, not per-function.

Do NOT do this:
```
src
└── main
    └── java
        └── com
            └── example
                ├── controller
                ├── service
                ├── repository
                └── model
```

Do this instead:
```
src
└── main
    └── java
        └── com
            └── example
                ├── feature1
                │   ├── controller
                │   ├── service
                │   ├── repository
                │   └── model
                └── feature2
                    ├── controller
                    ├── service
                    ├── repository
                    └── model
```

This makes the project maintainable & scalable.
Thanks!

