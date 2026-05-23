FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package dependency:copy-dependencies

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/classes ./classes
COPY --from=build /app/target/dependency ./dependency

RUN mkdir -p /app/uploads/images

ENV DB_URL="jdbc:mysql://mysql:3306/auction_db?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Ho_Chi_Minh"
ENV DB_USER="root"
ENV DB_PASSWORD="123456"
ENV SERVER_PORT="5050"
ENV JAVA_OPTS="-Dauction.public.host=localhost -Dauction.media.dir=/app/uploads/images"

EXPOSE 5050 8081

VOLUME ["/app/uploads"]

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -cp '/app/classes:/app/dependency/*' com.auction.network.server.Server ${SERVER_PORT}"]
