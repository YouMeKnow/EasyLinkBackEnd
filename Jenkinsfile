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

    // compose lives on the host-mounted folder
    COMPOSE_FILE = '/workspace/ymk/docker-compose.yml'

    // <-- your real key path (inside the mounted C:\ymk)
    JWT_PRIVATE_PEM = '/workspace/ymk/EasyLinkBackEnd/src/main/resources/keys/private.pem'
  }

  stages {
    stage('checkout') {
      steps { checkout scm }
    }

    stage('preflight') {
      steps {
        sh '''
          set -eu
          echo "[preflight] whoami=$(whoami)"
          echo "[preflight] pwd=$PWD"
          echo "[preflight] DOCKER_HOST=$DOCKER_HOST"

          docker -H "$DOCKER_HOST" version
          docker -H "$DOCKER_HOST" compose version

          echo "[preflight] COMPOSE_FILE=$COMPOSE_FILE"
          test -f "$COMPOSE_FILE"

          echo "[preflight] key file (must exist): $JWT_PRIVATE_PEM"
          ls -la "$(dirname "$JWT_PRIVATE_PEM")" || true
          test -f "$JWT_PRIVATE_PEM"

          # validate compose syntax
          docker -H "$DOCKER_HOST" compose -f "$COMPOSE_FILE" config >/dev/null

          echo "[preflight] OK"
        '''
      }
    }

    stage('build image') {
      steps {
        sh '''
          set -eu
          echo "[image] workspace=$(pwd)"
          ls -la

          # build using repo Dockerfile
          docker -H "$DOCKER_HOST" build -t "$IMAGE_TAG" -f Dockerfile .

          docker -H "$DOCKER_HOST" image ls --format '{{.Repository}}:{{.Tag}}  {{.ID}}  {{.Size}}' \
            | grep -E '^ymk/auth-service:latest' || true
        '''
      }
    }

    stage('ensure network ymk') {
      steps {
        sh '''
          set -eu
          docker -H "$DOCKER_HOST" network inspect ymk >/dev/null 2>&1 || \
            docker -H "$DOCKER_HOST" network create ymk >/dev/null
          docker -H "$DOCKER_HOST" network ls | grep -E '\\bymk\\b' || true
        '''
      }
    }

    stage('deploy auth-service') {
      steps {
        sh '''
          set -eu
          echo "[deploy] COMPOSE_FILE=$COMPOSE_FILE"
          test -f "$COMPOSE_FILE"

          # remove old container
          docker -H "$DOCKER_HOST" compose -p ymk -f "$COMPOSE_FILE" rm -sf auth-service || true

          # recreate only auth-service
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
        docker -H "$DOCKER_HOST" ps -a | head -n 30
      '''
    }
    success { echo 'Backend deploy successful!' }
    failure { echo 'Backend deploy failed!' }
  }
}
