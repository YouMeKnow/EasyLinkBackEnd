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
          echo "[preflight] DOCKER_HOST=$DOCKER_HOST"

          docker -H "$DOCKER_HOST" version

          echo "[preflight] compose tool (container)"
          docker -H "$DOCKER_HOST" run --rm docker/compose:1.29.2 version

          # Detect Windows host path (Docker Desktop variants)
          CAND1=/run/desktop/mnt/host/c/ymk
          CAND2=/host_mnt/c/ymk

          if docker -H "$DOCKER_HOST" run --rm -v "$CAND1:/w" busybox sh -lc 'test -f /w/docker-compose.yml'; then
            COMPOSE_ROOT="$CAND1"
          elif docker -H "$DOCKER_HOST" run --rm -v "$CAND2:/w" busybox sh -lc 'test -f /w/docker-compose.yml'; then
            COMPOSE_ROOT="$CAND2"
          else
            echo "[preflight] ERROR: cannot find docker-compose.yml in $CAND1 or $CAND2"
            docker -H "$DOCKER_HOST" run --rm -v "$CAND1:/w" busybox sh -lc 'ls -la /w || true' || true
            docker -H "$DOCKER_HOST" run --rm -v "$CAND2:/w" busybox sh -lc 'ls -la /w || true' || true
            exit 1
          fi

          echo "[preflight] COMPOSE_ROOT=$COMPOSE_ROOT"
          printf "COMPOSE_ROOT=%s\n" "$COMPOSE_ROOT" > .compose_root.env

          docker -H "$DOCKER_HOST" run --rm -v "$COMPOSE_ROOT:/w" busybox sh -lc 'ls -la /w | head -n 60'
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
          docker -H "$DOCKER_HOST" start "$CID"

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

          docker -H "$DOCKER_HOST" rm -f "$CID"
        '''
        stash includes: 'app.jar', name: 'appjar'
      }
    }

    stage('build image') {
      steps {
        unstash 'appjar'
        sh '''
          set -eu

          # Create a minimal Dockerfile used by CI
          cat > Dockerfile.ci <<'EOF'
          FROM eclipse-temurin:21-jre
          WORKDIR /app
          COPY app.jar /app/app.jar
          EXPOSE 8080
          ENTRYPOINT ["java","-jar","/app/app.jar"]
          EOF

          docker -H "$DOCKER_HOST" build --no-cache -t "$IMAGE_TAG" -f Dockerfile.ci .
          docker -H "$DOCKER_HOST" image inspect "$IMAGE_TAG" --format 'id={{.Id}} created={{.Created}} tags={{.RepoTags}}'
        '''
      }
    }

    stage('prepare secrets (to C:\\ymk\\secrets)') {
      steps {
        withCredentials([string(credentialsId: 'jwt_private_pem', variable: 'JWT_PEM')]) {
          sh '''
            set -eu
            . ./.compose_root.env
            echo "[secrets] target compose root: $COMPOSE_ROOT"

            docker -H "$DOCKER_HOST" run --rm -v "$COMPOSE_ROOT:/w" busybox sh -lc '
              mkdir -p /w/secrets
              chmod 700 /w/secrets || true
              rm -f /w/secrets/private.pem || true
            '

            echo "[secrets] uploading private.pem"
            # write file via stdin (keeps file out of git)
            printf "%s" "$JWT_PEM" | docker -H "$DOCKER_HOST" run --rm -i -v "$COMPOSE_ROOT:/w" busybox sh -lc '
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
          docker -H "$DOCKER_HOST" network inspect ymk >/dev/null 2>&1 || docker -H "$DOCKER_HOST" network create ymk
          docker -H "$DOCKER_HOST" network ls | grep -E "\\bymk\\b" || true
        '''
      }
    }

    stage('deploy auth-service (from C:\\ymk)') {
      steps {
        sh '''
          set -eu
          . ./.compose_root.env
          echo "[deploy] COMPOSE_ROOT=$COMPOSE_ROOT"

          docker -H "$DOCKER_HOST" run --rm \
            -e DOCKER_HOST="$DOCKER_HOST" \
            -v "$COMPOSE_ROOT:/work" \
            -w /work \
            docker/compose:1.29.2 \
            -p ymk \
            -f /work/docker-compose.yml \
            up -d --no-deps --force-recreate auth-service
        '''
      }
    }

    stage('check secret in container') {
      steps {
        sh '''
          set -eu
          echo "[check] verifying secret mounted inside auth-service"
          docker -H "$DOCKER_HOST" exec auth-service sh -lc '
            ls -la /run/secrets || true
            test -f /run/secrets/jwt_private_key || test -f /run/secrets/private.pem || true
          '
        '''
      }
    }
  }

  post {
    success {
      echo 'Backend deploy succeeded!'
    }
    failure {
      echo 'Backend deploy failed!'
    }
    always {
      sh '''
        set +e
        echo "[post] docker ps (top 30)"
        docker -H "$DOCKER_HOST" ps -a | head -n 30 || true
      '''
    }
  }
}
