FROM deps:latest as base
ARG DIR
WORKDIR ${DIR}
COPY . .

FROM base as unit_test
CMD mvn -Dtest=com.app.bank_acquiring.unit.** test

FROM base as integration_test
CMD mvn -Dtest=com.app.bank_acquiring.integration.** test

FROM base as build
RUN mvn -Dmaven.test.skip package

FROM eclipse-temurin:17-jre-alpine as production
ARG APP_NAME
ARG DIR
WORKDIR ${DIR}
COPY --from=build /usr/src/app/target/${APP_NAME} .
COPY --from=build /usr/src/app/upos_base ./upos_base
RUN mkdir -p /usersUpos && chmod -R 777 /usersUpos && \
    apk add --no-cache tzdata
ENV TZ=Europe/Moscow
#upos port to connect POS-terminal
EXPOSE 8888
#port for smtp mail
EXPOSE 587
#RUN adduser -S appuser
#USER appuser