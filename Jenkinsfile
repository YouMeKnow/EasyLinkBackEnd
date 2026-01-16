pipeline {
  agent any
  options { timestamps(); disableConcurrentBuilds(); durabilityHint('PERFORMANCE_OPTIMIZED') }

  environment {
    DOCKER_HOST = 'tcp://host.docker.internal:2375'
    IMAGE_TAG   = 'ymk/auth-service:latest'

    // repo is checked out into Jenkins workspace
    BACKEND_DIR = 'EasyLinkBackEnd'   // <- repo folder in C:\ymk (as you showed)
    DOCKERFILE  = 'Dockerfile'
  }

  stages {
    stage('preflight') {
      steps {
        sh '''
          set -eu
          echo "[preflight] DOCKER_HOST=$DOCKER_HOST"
          docker -H "$DOCKER_HOST" version
          docker -H "$DOCKER_HOST" compose version

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
        '''
      }
    }

    stage('checkout') {
      steps { checkout scm }
    }

    stage('build image') {
      steps {
        sh '''
          set -eu
          echo "[image] build $IMAGE_TAG from $BACKEND_DIR/$DOCKERFILE"
          test -f "$BACKEND_DIR/$DOCKERFILE"

          docker -H "$DOCKER_HOST" build \
            -t "$IMAGE_TAG" \
            -f "$BACKEND_DIR/$DOCKERFILE" \
            "$BACKEND_DIR"

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
            echo "[secrets] COMPOSE_ROOT=$COMPOSE_ROOT"

            docker -H "$DOCKER_HOST" run --rm -v "$COMPOSE_ROOT:/w" busybox sh -lc '
              mkdir -p /w/secrets
              chmod 700 /w/secrets || true
              rm -f /w/secrets/private.pem || true
            '

            cat "$JWT_PEM_FILE" | docker -H "$DOCKER_HOST" run --rm -i -v "$COMPOSE_ROOT:/w" busybox sh -lc '
              cat > /w/secrets/private.pem
              chmod 400 /w/secrets/private.pem || true
            '

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
          docker -H "$DOCKER_HOST" network inspect ymk >/dev/null 2>&1 || docker -H "$DOCKER_HOST" network create ymk >/dev/null
          docker -H "$DOCKER_HOST" network ls | grep -E '\\bymk\\b' || true
        '''
      }
    }

    stage('deploy auth-service') {
      steps {
        sh '''
          set -eu
          . ./.compose_root.env
          echo "[deploy] COMPOSE_ROOT=$COMPOSE_ROOT"

          # hard remove old container (no interactive prompts ever)
          docker -H "$DOCKER_HOST" compose -p ymk -f "$COMPOSE_ROOT/docker-compose.yml" rm -sf auth-service || true

          # recreate with latest image
          docker -H "$DOCKER_HOST" compose -p ymk -f "$COMPOSE_ROOT/docker-compose.yml" up -d --no-deps --force-recreate auth-service

          docker -H "$DOCKER_HOST" compose -p ymk -f "$COMPOSE_ROOT/docker-compose.yml" ps
        '''
      }
    }

    stage('logs') {
      steps {
        sh '''
          set -eu
          CID=$(docker -H "$DOCKER_HOST" ps -q -f name=ymk-auth-service-1 || true)
          echo "[logs] CID=$CID"
          if [ -n "$CID" ]; then
            docker -H "$DOCKER_HOST" logs --tail 200 "$CID" || true
          fi
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
    failure { echo 'Backend deploy failed!' }
  }
}
