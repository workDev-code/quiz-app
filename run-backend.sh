#!/bin/bash
cd /home/killer/development/projects/quiz-app/backend
exec env SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/quizdb SPRING_DATASOURCE_USERNAME=quiz SPRING_DATASOURCE_PASSWORD=quiz SPRING_PROFILES_ACTIVE=dev java -jar target/quiz-backend.jar
