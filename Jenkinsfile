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

    // Change if your Dockerfile is not in repo root
    DOCKERFILE_PATH = 'Dockerfile'
  }

  stages {

    stage('preflight') {
      steps {
        sh '''
          set -eu
          echo "[preflight] DOCKER_HOST=$DOCKER_HOST"
          docker -H "$DOCKER_HOST" version

          echo "[preflight] compose tool (host plugin)"
          docker -H "$DOCKER_HOST" compose version

          # Detect host path mount for Windows Desktop
          CAND1=/run/desktop/mnt/host/c/ymk
          CAND2=/host_mnt/c/ymk

          if docker -H "$DOCKER_HOST" run --rm -v "$CAND1:/w" busybox sh -lc 'test -f /w/docker-compose.yml'; then
            COMPOSE_ROOT="$CAND1"
          elif docker -H "$DOCKER_HOST" run --rm -v "$CAND2:/w" busybox sh -lc 'test -f /w/docker-compose.yml'; then
            COMPOSE_ROOT="$CAND2"
          else
            echo "[preflight] ERROR: cannot find docker-compose.yml in $CAND1 or $CAND2"
            exit 2
          fi

          echo "[preflight] COMPOSE_ROOT=$COMPOSE_ROOT"
          printf "COMPOSE_ROOT=%s" "$COMPOSE_ROOT" > .compose_root.env

          docker -H "$DOCKER_HOST" run --rm -v "$COMPOSE_ROOT:/w" busybox sh -lc 'ls -la /w | head -n 80'
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

          CID=$(docker -H "$DOCKER_HOST" create gradle:8.9-jdk21 bash -lc 'sleep infinity')
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
        stash name: 'appjar', includes: 'app.jar'
      }
    }

    stage('build image') {
      steps {
        unstash 'appjar'
        sh '''
          set -eu
          test -f app.jar

          echo "[image] building $IMAGE_TAG using $DOCKERFILE_PATH"
          docker -H "$DOCKER_HOST" build \
            -t "$IMAGE_TAG" \
            -f "$DOCKERFILE_PATH" \
            .

          echo "[image] built:"
          docker -H "$DOCKER_HOST" image ls --format '{{.Repository}}:{{.Tag}}  {{.ID}}  {{.Size}}' | grep -E '^ymk/auth-service:latest' || true
        '''
      }
    }

    stage('prepare secrets (to C:\\ymk\\secrets)') {
      steps {
        withCredentials([file(credentialsId: 'jwt-private-pem', variable: 'JWT_PEM_FILE')]) {
          sh '''
            set -eu
            . ./.compose_root.env
            echo "[secrets] target compose root: $COMPOSE_ROOT"

            docker -H "$DOCKER_HOST" run --rm -v "$COMPOSE_ROOT:/w" busybox sh -lc '
              mkdir -p /w/secrets
              chmod 700 /w/secrets || true
              rm -f /w/secrets/private.pem || true
            '

            echo "[secrets] uploading private.pem from Jenkins secret file"
            cat "$JWT_PEM_FILE" | docker -H "$DOCKER_HOST" run --rm -i -v "$COMPOSE_ROOT:/w" busybox sh -lc '
              cat > /w/secrets/private.pem
              chmod 400 /w/secrets/private.pem || true
            '

            echo "[secrets] verify"
            docker -H "$DOCKER_HOST" run --rm -v "$COMPOSE_ROOT:/w" busybox sh -lc '
              ls -la /w/secrets
              test -f /w/secrets/private.pem
            '
          '''
        }
      }
    }

    stage('ensure network ymk') {
      steps {
        sh '''
          set -eu
          echo "[net] ensure external network: ymk"
          docker -H "$DOCKER_HOST" network inspect ymk >/dev/null 2>&1 || docker -H "$DOCKER_HOST" network create ymk >/dev/null
          docker -H "$DOCKER_HOST" network ls | grep -E '\\bymk\\b' || true
        '''
      }
    }

    stage('deploy auth-service (from C:\\ymk)') {
      steps {
        sh '''
          set -eu
          . ./.compose_root.env
          echo "[deploy] COMPOSE_ROOT=$COMPOSE_ROOT"

          # Remove old container first (prevents interactive prompts / broken recreate state)
          docker -H "$DOCKER_HOST" compose -p ymk -f "$COMPOSE_ROOT/docker-compose.yml" rm -sf auth-service || true

          # Bring up service (Compose v2)
          docker -H "$DOCKER_HOST" compose -p ymk -f "$COMPOSE_ROOT/docker-compose.yml" up -d --no-deps --force-recreate auth-service
        '''
      }
    }

    stage('check secret in container') {
      steps {
        sh '''
          set -eu
          echo "[check] container + secret path"

          CID=$(docker -H "$DOCKER_HOST" ps -q -f name=ymk-auth-service-1 || true)
          echo "[check] CID=$CID"
          test -n "$CID"

          docker -H "$DOCKER_HOST" exec "$CID" sh -lc '
            set -e
            ls -la /run/secrets || true
            ls -la /run/secrets/private.pem || true
          ' || true

          echo "[check] last logs"
          docker -H "$DOCKER_HOST" logs --tail 150 "$CID" || true
        '''
      }
    }
  }

  post {
    always {
      sh '''
        set +e
        echo "[post] docker ps (top 30)"
        docker -H "$DOCKER_HOST" ps -a | head -n 30
      '''
    }
    failure {
      echo 'Backend deploy failed!'
    }
  }
}
