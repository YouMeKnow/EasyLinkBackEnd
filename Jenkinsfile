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
    COMPOSE_IMG = 'docker/compose:1.29.2'
  }

  stages {
    stage('preflight') {
      steps {
        sh '''
          set -eu

          echo "[preflight] DOCKER_HOST=${DOCKER_HOST:-<unset>}"
          docker -H "$DOCKER_HOST" version

          echo "[preflight] compose availability on docker-host"
          if docker -H "$DOCKER_HOST" compose version >/dev/null 2>&1; then
            echo "[preflight] docker compose (v2 plugin) OK"
          else
            echo "[preflight] docker compose plugin not found; will use ${COMPOSE_IMG}"
            docker -H "$DOCKER_HOST" run --rm "$COMPOSE_IMG" version
          fi

          echo "[preflight] required files on docker-host"
          docker -H "$DOCKER_HOST" run --rm -v /workspace/ymk:/w busybox sh -lc "ls -la /w/docker-compose.yml"
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

          CID=$(docker -H "$DOCKER_HOST" create gradle:8.9-jdk21 bash -lc "sleep infinity")
          echo "[jar] temp container: $CID"
          docker -H "$DOCKER_HOST" start "$CID" >/dev/null

          docker -H "$DOCKER_HOST" cp . "$CID:/app"

          docker -H "$DOCKER_HOST" exec "$CID" bash -lc '
            set -e
            cd /app
            gradle clean bootJar -x test
            echo "[gradle] built jars:"
            ls -la build/libs
          '

          JAR_IN=$(docker -H "$DOCKER_HOST" exec "$CID" bash -lc 'ls -1 /app/build/libs/*.jar | head -n 1')
          echo "[jar] jar in container: $JAR_IN"
          docker -H "$DOCKER_HOST" cp "$CID:$JAR_IN" ./app.jar
          ls -la app.jar

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

    stage('prepare secrets') {
      steps {
        withCredentials([file(credentialsId: 'jwt-private-pem', variable: 'JWT_PEM')]) {
          sh '''
            set -eu

            echo "[secrets] ensure folder exists on docker-host"
            docker -H "$DOCKER_HOST" run --rm \
              -v /workspace/ymk/secrets:/secrets \
              busybox sh -lc "mkdir -p /secrets && chmod 700 /secrets || true"

            echo "[secrets] upload private.pem to docker-host"
            cat "$JWT_PEM" | docker -H "$DOCKER_HOST" run --rm -i \
              -v /workspace/ymk/secrets:/secrets \
              busybox sh -lc "cat > /secrets/private.pem && chmod 400 /secrets/private.pem || true"

            echo "[secrets] verify on docker-host"
            docker -H "$DOCKER_HOST" run --rm -v /workspace/ymk/secrets:/s \
              busybox sh -lc "ls -la /s && test -f /s/private.pem"
          '''
        }
      }
    }

    stage('deploy auth-service') {
      steps {
        sh '''
          set -eu

          echo "[deploy] bring up auth-service on docker-host"

          if docker -H "$DOCKER_HOST" compose version >/dev/null 2>&1; then
            echo "[deploy] using docker compose (v2 plugin)"
            docker -H "$DOCKER_HOST" compose -f /workspace/ymk/docker-compose.yml up -d --force-recreate auth-service
          else
            echo "[deploy] using ${COMPOSE_IMG} container"
            docker -H "$DOCKER_HOST" run --rm \
              -v /workspace/ymk:/work \
              -w /work \
              "$COMPOSE_IMG" \
              -f /work/docker-compose.yml up -d --force-recreate auth-service
          fi
        '''
      }
    }

    stage('check secret in container') {
      steps {
        sh '''
          set -eu
          echo "[check] /run/secrets inside auth-service"
          docker -H "$DOCKER_HOST" exec auth-service sh -lc "ls -la /run/secrets && test -f /run/secrets/jwt_private_key && echo OK"
        '''
      }
    }
  }

  post {
    success { echo 'Backend deploy successful!' }
    failure { echo 'Backend deploy failed!' }
  }
}
