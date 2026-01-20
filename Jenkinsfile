pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    durabilityHint('PERFORMANCE_OPTIMIZED')
    skipDefaultCheckout(true)
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
          docker -H "$DOCKER_HOST" compose version

          COMPOSE_FILE=/workspace/ymk/docker-compose.yml
          echo "[preflight] COMPOSE_FILE=$COMPOSE_FILE"

          test -f "$COMPOSE_FILE"
          docker -H "$DOCKER_HOST" compose -f "$COMPOSE_FILE" config >/dev/null

          printf "COMPOSE_FILE=%s\n" "$COMPOSE_FILE" > .compose_root.env
          echo "[preflight] OK"
        '''
      }
    }

    stage('checkout') {
      steps {
        checkout scm
      }
    }

    stage('build image') {
      steps {
        sh '''
          set -eu
          echo "[image] workspace=$(pwd)"
          ls -la

          if [ -f Dockerfile ]; then
            CONTEXT="."
            DF="Dockerfile"
          elif [ -f EasyLinkBackEnd/Dockerfile ]; then
            CONTEXT="EasyLinkBackEnd"
            DF="EasyLinkBackEnd/Dockerfile"
          else
            echo "ERROR: Dockerfile not found"
            exit 1
          fi

          echo "[image] build $IMAGE_TAG using $DF (context=$CONTEXT)"
          docker -H "$DOCKER_HOST" build -t "$IMAGE_TAG" -f "$DF" "$CONTEXT"

          docker -H "$DOCKER_HOST" image ls \
            --format '{{.Repository}}:{{.Tag}}  {{.ID}}  {{.Size}}' \
            | grep '^ymk/auth-service:latest' || true
        '''
      }
    }

    stage('secrets (verify only)') {
      steps {
        sh '''
          set -eu
          echo "[secrets] verify existing host secret"

          test -f /workspace/ymk/secrets/private.pem
          ls -la /workspace/ymk/secrets
        '''
      }
    }

    stage('ensure network ymk') {
      steps {
        sh '''
          set -eu
          docker -H "$DOCKER_HOST" network inspect ymk >/dev/null 2>&1 \
            || docker -H "$DOCKER_HOST" network create ymk >/dev/null

          docker -H "$DOCKER_HOST" network ls | grep -E '\\bymk\\b' || true
        '''
      }
    }

    stage('deploy auth-service') {
      steps {
        sh '''
          set -eu
          . ./.compose_root.env
          echo "[deploy] COMPOSE_FILE=$COMPOSE_FILE"

          docker -H "$DOCKER_HOST" compose -p ymk -f "$COMPOSE_FILE" rm -sf auth-service || true
          docker -H "$DOCKER_HOST" compose -p ymk -f "$COMPOSE_FILE" up -d --no-deps --force-recreate auth-service

          docker -H "$DOCKER_HOST" compose -p ymk -f "$COMPOSE_FILE" ps
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
        docker -H "$DOCKER_HOST" ps -a > /tmp/ps.txt || true
        head -n 30 /tmp/ps.txt || true
      '''
    }
    success {
      echo 'Backend deploy successful!'
    }
    failure {
      echo 'Backend deploy failed!'
    }
  }
}
