pipeline {
  agent any
  options { timestamps(); disableConcurrentBuilds(); durabilityHint('PERFORMANCE_OPTIMIZED') }

  environment {
    DOCKER_HOST = 'tcp://host.docker.internal:2375'
    IMAGE_TAG   = 'ymk/auth-service:latest'
  }

  stages {
    stage('preflight') {
      steps {
        sh '''
          set -eu
          docker -H "$DOCKER_HOST" version
        '''
      }
    }

    stage('checkout') {
      steps {
        checkout scm
      }
    }

    stage('build jar') {
      steps {
        sh '''
          set -eu
          rm -f app.jar app-jar.tar
          tar -cf - . \
          | docker -H "$DOCKER_HOST" run --rm -i gradle:8.9-jdk21 bash -lc '
              set -e
              mkdir -p /app
              tar -C /app -xf -
              cd /app
              gradle clean bootJar -x test
              JAR=$(ls build/libs/*.jar | head -n1)
              exec tar -C "$(dirname "$JAR")" -cf - "$(basename "$JAR")"
            ' > app-jar.tar
          mkdir -p out
          tar -C out -xf app-jar.tar
          mv out/*.jar app.jar
          rm -rf out app-jar.tar
          ls -la app.jar
        '''
        stash name: 'app-jar', includes: 'app.jar'
      }
    }

    stage('build image') {
      steps {
        unstash 'app-jar'
        sh '''
          set -eu
          cat > Dockerfile.ci <<'EOF'
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
EOF
          docker -H "$DOCKER_HOST" build --no-cache -t "$IMAGE_TAG" -f Dockerfile.ci .
        '''
      }
    }

    stage('deploy') {
      steps {
        sh '''
          set -eu
          docker compose -f /workspace/ymk/docker-compose.yml up -d --force-recreate auth-service
        '''
      }
    }
  }
}
