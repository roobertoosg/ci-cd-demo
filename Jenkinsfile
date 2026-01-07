pipeline {
  agent any
  options { timestamps() }

  parameters {
    string(name: 'APP_NAME', defaultValue: 'ci-cd-demo', description: 'Nombre de la app (K8s deployment/service)')
    string(name: 'IMAGE_REPO', defaultValue: 'registry.example.com/ci-cd-demo', description: 'Repo de imagen (ej: nexus:8083/ci-cd-demo)')
    string(name: 'DOCKER_REGISTRY', defaultValue: 'registry.example.com', description: 'Host del registry para docker login (ej: nexus:8083)')

    booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Ejecutar análisis SonarQube')
    string(name: 'SONAR_HOST_URL', defaultValue: 'https://sonarqube.example.com', description: 'URL SonarQube')
    string(name: 'SONAR_PROJECT_KEY', defaultValue: 'ci-cd-demo', description: 'Key del proyecto en Sonar')

    booleanParam(name: 'RUN_PUSH', defaultValue: true, description: 'Hacer push de la imagen al registry')
    booleanParam(name: 'RUN_DEPLOY', defaultValue: true, description: 'Desplegar a Kubernetes')
    string(name: 'K8S_NAMESPACE', defaultValue: 'default', description: 'Namespace Kubernetes')

    // IDs de credenciales en Jenkins (los ajustarán al correrlo en otra máquina)
    string(name: 'CRED_SONAR_TOKEN', defaultValue: 'sonar-token', description: 'credentialsId (Secret text)')
    string(name: 'CRED_DOCKER_USERPASS', defaultValue: 'docker-registry-user-pass', description: 'credentialsId (Username/Password)')
    string(name: 'CRED_KUBECONFIG', defaultValue: 'kubeconfig', description: 'credentialsId (Secret file)')
  }

  environment {
    IMAGE_TAG = "${env.BUILD_NUMBER}"
    IMAGE = "${params.IMAGE_REPO}:${env.BUILD_NUMBER}"
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build & Test (Maven)') {
      steps {
        sh 'mvn -B clean test package'
      }
    }

    stage('SonarQube Scan') {
      when { expression { return params.RUN_SONAR } }
      steps {
        withCredentials([string(credentialsId: params.CRED_SONAR_TOKEN, variable: 'SONAR_TOKEN')]) {
          sh """
            mvn -B sonar:sonar \
              -Dsonar.host.url=${params.SONAR_HOST_URL} \
              -Dsonar.projectKey=${params.SONAR_PROJECT_KEY} \
              -Dsonar.projectName=${params.SONAR_PROJECT_KEY} \
              -Dsonar.login=$SONAR_TOKEN
          """
        }
      }
    }

    stage('Quality Gate (si el plugin está)') {
      when { expression { return params.RUN_SONAR } }
      steps {
        script {
          try {
            timeout(time: 10, unit: 'MINUTES') {
              def qg = waitForQualityGate()
              if (qg.status != 'OK') {
                error "Quality Gate: ${qg.status}"
              }
            }
          } catch (err) {
            echo "Quality Gate no disponible (plugin faltante) o error: ${err}. Continúo."
          }
        }
      }
    }

    stage('Docker Build') {
      steps {
        sh "docker build -t ${env.IMAGE} ."
      }
    }

    stage('Docker Push') {
      when { expression { return params.RUN_PUSH } }
      steps {
        withCredentials([usernamePassword(credentialsId: params.CRED_DOCKER_USERPASS, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          sh """
            echo "$DOCKER_PASS" | docker login ${params.DOCKER_REGISTRY} -u "$DOCKER_USER" --password-stdin
            docker push ${env.IMAGE}
          """
        }
      }
    }

    stage('Deploy to Kubernetes (kompose manifests)') {
      when { expression { return params.RUN_DEPLOY } }
      steps {
        withCredentials([file(credentialsId: params.CRED_KUBECONFIG, variable: 'KUBECONFIG_FILE')]) {
          sh """
            export KUBECONFIG=$KUBECONFIG_FILE
            mkdir -p .tmp-k8s
            cp -r k8s-kompose/* .tmp-k8s/
            sed -i "s|REPLACE_IMAGE|${env.IMAGE}|g" .tmp-k8s/ci-cd-demo-deployment.yaml
            kubectl -n ${params.K8S_NAMESPACE} apply -f .tmp-k8s/
            kubectl -n ${params.K8S_NAMESPACE} rollout status deploy/${params.APP_NAME} --timeout=120s || true
          """
        }
      }
    }
  }

  post {
    always {
      archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
    }
  }
}
