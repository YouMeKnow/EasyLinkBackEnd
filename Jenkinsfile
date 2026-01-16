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
    stage('checkout') {
      steps {
        checkout scm
      }
    }

    stage('preflight') {
      steps {
        sh '''
          set -eu

          echo "[preflight] workspace: $PWD"
          ls -la

          echo "[preflight] DOCKER_HOST=${DOCKER_HOST:-<unset>}"
          docker -H "$DOCKER_HOST" version

          echo "[preflight] compose tool"
          docker -H "$DOCKER_HOST" run --rm "$COMPOSE_IMG" version

          echo "[preflight] docker-compose.yml exists in repo"
          test -f docker-compose.yml
        '''
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

    stage('prepare secrets (repo-local)') {
      steps {
        withCredentials([file(credentialsId: 'jwt-private-pem', variable: 'JWT_PEM')]) {
          sh '''
            set -eu
            mkdir -p secrets
            cp "$JWT_PEM" secrets/private.pem
            chmod 400 secrets/private.pem || true
            echo "[secrets] repo secrets folder:"
            ls -la secrets
          '''
        }
      }
    }

    stage('deploy auth-service (compose from repo)') {
      steps {
        sh '''
          set -eu

          echo "[deploy] running compose from repo workspace via ${COMPOSE_IMG}"
          docker -H "$DOCKER_HOST" run --rm \
            -v "$PWD:/work" \
            -w /work \
            "${COMPOSE_IMG}" \
            up -d --force-recreate auth-service
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
