FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . .
RUN javac -cp "lib/*" server/src/*.java
EXPOSE 5000
CMD ["java", "-cp", "lib/*:server/src", "BioWatcherCentralServer", "5000"]