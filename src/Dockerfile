FROM openjdk:11-jdk-alpine as base
RUN apk add --no-cache maven
WORKDIR /usr/src/app
COPY . .

FROM base as test
CMD mvn test

FROM base as build
RUN mvn package

FROM openjdk:11-jre-alpine3.8 as production
WORKDIR /usr/src/app
COPY --from=build /usr/src/app/target/DBtest-1.0.jar .
COPY --from=build /usr/src/app/upos_base .
RUN mkdir -p /usersUpos
RUN chmod -R 777 /usersUpos
RUN adduser -S appuser
USER appuser