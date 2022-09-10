# Shop Inventory POS
[RUS](./README.RUS.md) [YouTube](https://youtu.be/SxvctngGpFo)  
Inventory web application with the function of payment by pos-acquiring. Suitable for small businesses with up to 1000 items.

Main functions:
- Adding employees with different roles
- Acceptance and release of goods only after a successful bank transaction
- Offline support for terminal
- Output of non-fiscal check documents with the function of subsequent printing on a printer
- Search by barcode / name, sorting of present commodity items or services
- Transfer of goods between stores
- Uploading the database of goods to an excel file
- Send check by email

## Installation and setup
Deployment requires Docker. Run via 'docker compose up'

## For developers
Java 17, Spring Boot 2.5, Postgres 13, Flyway 9

### Docker
Build an image with dependencies (and rebuild with pom changes) `docker build -f Dockerfile.deps . -t deps:latest`

Run all tests `docker-compose -f docker-compose.test.yml up`

Build and run the unit test container and then delete it  
```
docker build . -t unit_test --target unit_test  
docker run -it --rm --name unit_test unit_test
```

Build and run the integration test container with its subsequent removal  
```
docker build . -t integration_test --target integration_test  
docker run -it --rm --name integration_test integration_test
```
### Flyway
For migration, put the V_changes.sql file in the /src/main/resources/db/migration volume and start the container

Status of migrations  
```
docker exec -it flyway_container sh  
$flyway info
```

### Bugs in UI testing
**[SEVERE]: bind() failed: Cannot assign requested address (99)** - is OK  
**Error Unable to execute request: java.util.concurrent.TimeoutException** - floating bug, detailed in SeleniumHQ/Selenium #9528 issue

## What's next?
- Adding new banking protocols (UniPOS, Arcus)
- Elaboration of the possibility of cheques' fiscalization
