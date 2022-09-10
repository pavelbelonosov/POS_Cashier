# Shop Inventory POS
[ENG](./README.md) [YouTube](https://youtu.be/SxvctngGpFo)  
Товароучетное веб-приложение с функцией оплаты по пос-эквайрингу. Подоходит для малых предприятий с количеством товарных позиций до 1000 единиц.

Основные функции:
- Добавление сотрудников с разными ролями
- Прием и отпуск товара только при успешной банковской транзакции
- Поддержка автономного режима для терминала
- Вывод нефискальных чековых документов с функцией последующей печати на принтере
- Поиск по штрих-коду/наименованию, сортировка присутвующих товарных позиций или услуг
- Перенос товаров между магазинами
- Выгрузка базы товаров в эксель файл
- Отправка чека на электронную почту

## Установка и настройка
Для развертывания требуется Docker. Запуск через `docker compose up`

## Для разработчиков
Java 17, Spring Boot 2.5, Postgres 13, Flyway 9

### Docker
Билдим образ с зависимостями(и пересобираем при изменениях pom) `docker build -f Dockerfile.deps . -t deps:latest`

Прогон всех тестов `docker-compose -f docker-compose.test.yml up`

Билд и запуск контейнера юнит тестов с последующим его удалением
```
docker build . -t unit_test --target unit_test
docker run  -it --rm --name unit_test  unit_test
```

Билд и запуск контейнера интеграционных тестов с последующим его удалением
```
docker build . -t integration_test --target integration_test   
docker run  -it --rm --name integration_test  integration_test
```

### Flyway
Для миграции подкладываем файл `V_changes.sql` в вольюм `/src/main/resources/db/migration` и перезапускаем контейнер

Статус миграций
```
docker exec -it flyway_container sh
$ flyway info
```

### Баги при ui тестировании
**[SEVERE]: bind() failed: Cannot assign requested address (99)** - это норма (малышева.jpg)
**Error Unable to execute request: java.util.concurrent.TimeoutException** - плавающий глюк, подробно описан в SeleniumHQ/Selenium #9528 issue

## Что дальше?
- Добавление новых банковских протоколов (УниПОС, Аркус)
- Проработка возможности фискализации чеков