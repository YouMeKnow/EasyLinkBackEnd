pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    durabilityHint('PERFORMANCE_OPTIMIZED')
  }

  environment {
    DOCKER_HOST = 'tcp://host.docker.internal:2375'
    IMAGE_TAG   = 'ymk/auth-service:latest'
  }

  stages {
    stage('preflight') {
      steps {
        sh '''
          set -eu
          echo "[preflight] DOCKER_HOST=${DOCKER_HOST:-<unset>}"
          docker -H "$DOCKER_HOST" version

          echo "[preflight] compose availability"
          (docker compose version || true)
          (docker-compose --version || true)

          echo "[preflight] /workspace/ymk listing"
          ls -la /workspace || true
          ls -la /workspace/ymk || true
          ls -la /workspace/ymk/docker-compose.yml || true
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

          rm -f app.jar

          # Create a container we can copy files in/out of
          CID=$(docker -H "$DOCKER_HOST" create gradle:8.9-jdk21 bash -lc "sleep infinity")
          echo "[jar] temp container: $CID"

          # Start container
          docker -H "$DOCKER_HOST" start "$CID" >/dev/null

          # Copy project sources into container
          docker -H "$DOCKER_HOST" cp . "$CID:/app"

          # Build jar inside container
          docker -H "$DOCKER_HOST" exec "$CID" bash -lc '
            set -e
            cd /app
            gradle clean bootJar -x test
            echo "[gradle] built jars:"
            ls -la build/libs
          '

          # Find jar path in container and copy it out
          JAR_IN=$(docker -H "$DOCKER_HOST" exec "$CID" bash -lc 'ls -1 /app/build/libs/*.jar | head -n 1')
          echo "[jar] jar in container: $JAR_IN"
          docker -H "$DOCKER_HOST" cp "$CID:$JAR_IN" ./app.jar
          ls -la app.jar

          # Cleanup
          docker -H "$DOCKER_HOST" rm -f "$CID" >/dev/null
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
          docker -H "$DOCKER_HOST" image inspect "$IMAGE_TAG" --format "id={{.Id}} created={{.Created}} tags={{.RepoTags}}"
        '''
      }
    }

    stage('deploy') {
      steps {
        sh '''
          set -e

          if docker compose version >/dev/null 2>&1; then
            DC="docker compose"
          elif command -v docker-compose >/dev/null 2>&1; then
            DC="docker-compose"
          else
            echo "[error] docker compose not available in this agent"
            exit 1
          fi

          DOCKER_HOST="$DOCKER_HOST" $DC -f /workspace/ymk/docker-compose.yml up -d --force-recreate auth-service
        '''
      }
    }
  }

  post {
    success { echo 'Backend deploy successful!' }
    failure { echo 'Backend deploy failed!' }
  }
}
