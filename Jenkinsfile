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

          echo "[preflight] compose tool (container)"
          docker -H "$DOCKER_HOST" run --rm "$COMPOSE_IMG" version

          # Detect how Windows C:\\ymk is exposed to Linux containers on this Docker Desktop
          CAND1="/run/desktop/mnt/host/c/ymk"
          CAND2="/host_mnt/c/ymk"

          if docker -H "$DOCKER_HOST" run --rm -v "$CAND1:/w" busybox sh -lc "test -f /w/docker-compose.yml"; then
            COMPOSE_ROOT="$CAND1"
          elif docker -H "$DOCKER_HOST" run --rm -v "$CAND2:/w" busybox sh -lc "test -f /w/docker-compose.yml"; then
            COMPOSE_ROOT="$CAND2"
          else
            echo "[error] cannot find docker-compose.yml under $CAND1 or $CAND2"
            echo "[hint] on Windows host it is C:\\ymk\\docker-compose.yml"
            exit 1
          fi

          echo "[preflight] COMPOSE_ROOT=$COMPOSE_ROOT"
          docker -H "$DOCKER_HOST" run --rm -v "$COMPOSE_ROOT:/w" busybox ls -la /w | head -n 60

          # Save for later stages
          echo "COMPOSE_ROOT=$COMPOSE_ROOT" > .compose_root.env
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

    stage('prepare secrets (to C:\\ymk\\secrets)') {
      steps {
        withCredentials([file(credentialsId: 'jwt-private-pem', variable: 'JWT_PEM')]) {
          sh '''
            set -eu
            . ./.compose_root.env

            echo "[secrets] target compose root: $COMPOSE_ROOT"
            docker -H "$DOCKER_HOST" run --rm -v "$COMPOSE_ROOT:/w" busybox sh -lc "mkdir -p /w/secrets && chmod 700 /w/secrets || true"

            echo "[secrets] uploading private.pem"
            cat "$JWT_PEM" | docker -H "$DOCKER_HOST" run --rm -i -v "$COMPOSE_ROOT:/w" busybox sh -lc "cat > /w/secrets/private.pem && chmod 400 /w/secrets/private.pem || true"

            echo "[secrets] verify"
            docker -H "$DOCKER_HOST" run --rm -v "$COMPOSE_ROOT:/w" busybox sh -lc "ls -la /w/secrets && test -f /w/secrets/private.pem"
          '''
        }
      }
    }

    stage('deploy auth-service (from C:\\ymk)') {
      steps {
        sh '''
          set -eu
          . ./.compose_root.env

          echo "[deploy] COMPOSE_ROOT=$COMPOSE_ROOT"
          docker -H "$DOCKER_HOST" run --rm \
            -v "$COMPOSE_ROOT:/work" \
            -w /work \
            "$COMPOSE_IMG" \
            -f /work/docker-compose.yml up -d --force-recreate auth-service
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
