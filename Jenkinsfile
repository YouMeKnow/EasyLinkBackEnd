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
          echo "[preflight] DOCKER_HOST=$DOCKER_HOST"
          docker -H "$DOCKER_HOST" version
          docker -H "$DOCKER_HOST" compose version

          # This path exists INSIDE the Jenkins container because you mount:
          #   C:/ymk -> /workspace/ymk
          COMPOSE_FILE=/workspace/ymk/docker-compose.yml
          echo "[preflight] COMPOSE_FILE=$COMPOSE_FILE"
          test -f "$COMPOSE_FILE"

          printf "COMPOSE_FILE=%s\n" "$COMPOSE_FILE" > .compose_root.env
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
          echo "[image] workspace=$(pwd)"
          ls -la

          if [ -f Dockerfile ]; then
            CONTEXT="."
            DF="Dockerfile"
          elif [ -f EasyLinkBackEnd/Dockerfile ]; then
            CONTEXT="EasyLinkBackEnd"
            DF="EasyLinkBackEnd/Dockerfile"
          else
            echo "ERROR: Dockerfile not found (./Dockerfile or ./EasyLinkBackEnd/Dockerfile)"
            exit 1
          fi

          echo "[image] build $IMAGE_TAG using $DF (context=$CONTEXT)"
          docker -H "$DOCKER_HOST" build -t "$IMAGE_TAG" -f "$DF" "$CONTEXT"

          docker -H "$DOCKER_HOST" image ls --format '{{.Repository}}:{{.Tag}}  {{.ID}}  {{.Size}}' | grep -E '^ymk/auth-service:latest' || true
        '''
      }
    }

    stage('prepare secrets (to C:\\ymk\\secrets)') {
      steps {
        withCredentials([file(credentialsId: 'jwt-private-pem', variable: 'JWT_PEM_FILE')]) {
          sh '''
            set -eu

            # Write directly into the mounted host folder (C:\\ymk) via the container path.
            TARGET_DIR=/workspace/ymk/secrets
            echo "[secrets] TARGET_DIR=$TARGET_DIR"

            mkdir -p "$TARGET_DIR"
            chmod 700 "$TARGET_DIR" || true

            rm -f "$TARGET_DIR/private.pem" || true
            cp "$JWT_PEM_FILE" "$TARGET_DIR/private.pem"
            chmod 400 "$TARGET_DIR/private.pem" || true

            ls -la "$TARGET_DIR"
            test -f "$TARGET_DIR/private.pem"
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
          echo "[deploy] COMPOSE_FILE=$COMPOSE_FILE"
          test -f "$COMPOSE_FILE"

          # hard remove old container (no interactive prompts ever)
          docker -H "$DOCKER_HOST" compose -p ymk -f "$COMPOSE_FILE" rm -sf auth-service || true

          # recreate with latest image
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
    failure { echo 'Backend deploy failed!' }
  }
}
