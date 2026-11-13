FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests


FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr \
    && rm -rf /var/lib/apt/lists/*

# Supabase publishes this private root CA for verified database TLS.
COPY certs/supabase-root-2021.crt /opt/carepath/supabase-root-2021.crt
RUN keytool -importcert -noprompt -cacerts -storepass changeit \
    -alias carepath-supabase-root-2021 -file /opt/carepath/supabase-root-2021.crt

WORKDIR /app

COPY --from=build /app/target/carepath-backend-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

# Leave room for the isolated extraction JVM and native OCR on 512 MB instances.
ENTRYPOINT ["java", "-Xmx192m", "-XX:+UseSerialGC", "-jar", "app.jar"]