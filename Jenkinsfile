pipeline {
  agent any
  options { skipDefaultCheckout(false); timestamps() }
  parameters {
    choice(name: 'ENV', choices: ['dev','staging','prod'], description: 'Deploy environment')
    string(name: 'TAG', defaultValue: '', description: 'Image tag (default: git commit)')
    booleanParam(name: 'USE_CQRS', defaultValue: false, description: 'Deploy CQRS (command/query)')
    booleanParam(name: 'USE_PROJECTOR', defaultValue: false, description: 'Deploy projector')
    booleanParam(name: 'USE_SNAPSHOTTER', defaultValue: false, description: 'Deploy snapshotter')
  }
  environment {
    REGISTRY = credentials('REGISTRY_URL')
    REG_USER = credentials('REGISTRY_USER')
    REG_PASS = credentials('REGISTRY_PASS')
    IMAGE_NAME = 'your-team/your-app'
    CHART_PATH = 'charts/app'
  }
  stages {
    stage('Prepare') {
      steps {
        sh 'git rev-parse --short HEAD > .gitsha'
        script { env.GIT_SHA = readFile('.gitsha').trim() }
      }
    }
    stage('Test') {
      steps {
        sh '''
set -e
if [ -f package.json ]; then npm ci && npm test; fi
if [ -f pom.xml ]; then mvn -B -ntp -DskipITs=true test; fi
if [ -f go.mod ]; then go test ./...; fi
'''
      }
    }
    stage('Build & Push Image') {
      steps {
        sh '''
echo "$REG_PASS" | docker login "$REGISTRY" -u "$REG_USER" --password-stdin
TAG_TO_USE=${TAG:-$GIT_SHA}
docker build -t "$REGISTRY/$IMAGE_NAME:$TAG_TO_USE" -f Dockerfile .
docker push "$REGISTRY/$IMAGE_NAME:$TAG_TO_USE"
'''
      }
    }
    stage('Scan (Trivy)') {
      steps {
        sh '''
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy:0.54.1 image --severity CRITICAL,HIGH "$REGISTRY/$IMAGE_NAME:${TAG:-$GIT_SHA}" || true
'''
      }
    }
    stage('SonarQube') {
      when { expression { return env.SONAR_HOST_URL && env.SONAR_TOKEN } }
      steps {
        withSonarQubeEnv('sonarqube') {
          sh 'sonar-scanner -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.login=$SONAR_TOKEN || true'
        }
      }
    }
    stage('Deploy App') {
      environment {
        KUBECONFIG = credentials('KUBE_CONFIG_${ENV}')
        RELEASE = "RELEASE_NAME_${ENV}"
        NAMESPACE = "NAMESPACE_${ENV}"
      }
      steps {
        sh '''
set -e
TAG_TO_USE=${TAG:-$GIT_SHA}
helm upgrade --install ${!RELEASE} "$CHART_PATH" \
  -n ${!NAMESPACE} --create-namespace \
  -f "$CHART_PATH/values.yaml" -f "$CHART_PATH/values-${ENV}.yaml" \
  --set image.repository=$REGISTRY/$IMAGE_NAME \
  --set image.tag=$TAG_TO_USE \
  --atomic --timeout 10m
'''
      }
    }
    stage('Deploy CQRS/Projector/Snapshotter') {
      when { anyOf { expression { return params.USE_CQRS }; expression { return params.USE_PROJECTOR }; expression { return params.USE_SNAPSHOTTER } } }
      environment {
        KUBECONFIG = credentials('KUBE_CONFIG_${ENV}')
        NAMESPACE = "NAMESPACE_${ENV}"
        RELEASE_CQRS = "RELEASE_NAME_CQRS_${ENV}"
        RELEASE_PROJECTOR = "RELEASE_NAME_PROJECTOR_${ENV}"
        RELEASE_SNAPSHOTTER = "RELEASE_NAME_SNAPSHOTTER_${ENV}"
        IMAGE_NAME_COMMAND = 'your-team/orders-command'
        IMAGE_NAME_QUERY = 'your-team/orders-query'
        IMAGE_NAME_PROJECTOR = 'your-team/orders-projector'
        IMAGE_NAME_SNAPSHOTTER = 'your-team/orders-snapshotter'
      }
      steps {
        sh '''
set -e
TAG_TO_USE=${TAG:-$GIT_SHA}
if [ "${USE_CQRS}" = "true" ]; then
  helm dependency update charts/cqrs
  helm upgrade --install ${!RELEASE_CQRS} charts/cqrs \
    -n ${!NAMESPACE} --create-namespace \
    -f charts/cqrs/values.yaml -f charts/cqrs/values-${ENV}.yaml \
    --set command.image.repository=$REGISTRY/$IMAGE_NAME_COMMAND \
    --set command.image.tag=$TAG_TO_USE \
    --set query.image.repository=$REGISTRY/$IMAGE_NAME_QUERY \
    --set query.image.tag=$TAG_TO_USE \
    --atomic --timeout 10m
fi
if [ "${USE_PROJECTOR}" = "true" ]; then
  helm upgrade --install ${!RELEASE_PROJECTOR} charts/projector \
    -n ${!NAMESPACE} --create-namespace \
    --set image.repository=$REGISTRY/$IMAGE_NAME_PROJECTOR \
    --set image.tag=$TAG_TO_USE \
    --atomic --timeout 10m
fi
if [ "${USE_SNAPSHOTTER}" = "true" ]; then
  helm upgrade --install ${!RELEASE_SNAPSHOTTER} charts/snapshotter \
    -n ${!NAMESPACE} --create-namespace \
    --set image.repository=$REGISTRY/$IMAGE_NAME_SNAPSHOTTER \
    --set image.tag=$TAG_TO_USE \
    --atomic --timeout 10m
fi
'''
      }
    }
  }
  post {
    failure { echo 'Pipeline failed. Helm used --atomic for rollback if deploy failed.' }
  }
}
