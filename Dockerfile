FROM deps:latest as base
WORKDIR /usr/src/app
COPY . .

FROM base as unit_test
CMD mvn -Dtest=com.app.bank_acquiring.unit.** test

FROM base as integration_test
CMD mvn -Dtest=com.app.bank_acquiring.integration.** test

FROM base as build
ARG JAVA_APP_VERSION
ENV JAVA_APP_VERSION=${JAVA_APP_VERSION}
RUN mvn -Dmaven.test.skip package

FROM eclipse-temurin:17-jre-alpine as production
WORKDIR /usr/src/app
ARG JAVA_APP_VERSION
ARG EMAIL_ROBOT_NAME
ARG EMAIL_ROBOT_PASS
ENV JAVA_APP_VERSION=${JAVA_APP_VERSION}
ENV EMAIL_ROBOT_NAME=${EMAIL_ROBOT_NAME}
ENV EMAIL_ROBOT_PASS=${EMAIL_ROBOT_PASS}
COPY --from=build /usr/src/app/target/Shop_Inventory_POS-${JAVA_APP_VERSION}.jar .
COPY --from=build /usr/src/app/upos_base ./upos_base
RUN mkdir -p /usersUpos && chmod -R 777 /usersUpos && \
    apk add --no-cache tzdata
ENV TZ=Europe/Moscow
EXPOSE 8080
#upos port to connect POS-terminal
EXPOSE 8888
#port for smtp mail
EXPOSE 587
CMD java -jar *.jar