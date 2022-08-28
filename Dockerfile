FROM openjdk:17-jdk-alpine as base
RUN apk add --no-cache maven
WORKDIR /usr/src/app
COPY . .

FROM base as test
CMD mvn test

FROM base as build
RUN mvn -Dmaven.test.skip package

FROM eclipse-temurin:17-jre-alpine as production
WORKDIR /usr/src/app
COPY --from=build /usr/src/app/target/Shop_Inventory_POS-1.0.jar .
COPY --from=build /usr/src/app/upos_base ./upos_base
RUN mkdir -p /usersUpos
RUN chmod -R 777 /usersUpos
RUN adduser -S appuser
USER appuser