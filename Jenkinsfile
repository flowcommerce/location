properties([pipelineTriggers([githubPush()])])

pipeline {
  options {
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '10'))
    timeout(time: 30, unit: 'MINUTES')
  }

  agent {
    kubernetes {
      label 'worker-location'
      inheritFrom 'default'

      containerTemplates([
        containerTemplate(name: 'helm', image: "flowcommerce/k8s-build-helm2:0.0.50", command: 'cat', ttyEnabled: true),
        containerTemplate(name: 'docker', image: 'docker:18', resourceRequestCpu: '1', resourceRequestMemory: '2Gi', command: 'cat', ttyEnabled: true),
      ])
    }
  }

  environment {
    ORG      = 'flowcommerce'
  }

  stages {
    stage('Checkout') {
      steps {
        checkoutWithTags scm

        script {
          VERSION = new flowSemver().calculateSemver() //requires checkout
        }
      }
    }

    stage('Commit SemVer tag') {
      when { branch 'main' }
      steps {
        script {
          new flowSemver().commitSemver(VERSION)
        }
      }
    }

    stage('Build and push docker image release') {
      when { branch 'main' }
      steps {
        container('docker') {
          script {
            semver = VERSION.printable()
            
            docker.withRegistry('https://index.docker.io/v1/', 'jenkins-dockerhub') {
              db = docker.build("$ORG/location:$semver", '--network=host -f Dockerfile .')
              db.push()
            }
            
          }
        }
      }
    }

    stage ('Display Helm Diff') {
      when {
        allOf {
         changeRequest()
         changeset "deploy/**" 
        }
      }
            steps {
              script {
                container('helm') {
                  withCredentials([string(credentialsId: "jenkins-hub-api-token", variable: 'GITHUB_TOKEN')]) {
                  sh ("helm init")
                  sh ("helm repo add generic-charts-helm https://flow.jfrog.io/artifactory/api/helm/generic-charts-helm")
                  sh ("helm repo update generic-charts-helm")
                  helm_diff = sh(returnStdout: true, script: 'helm diff upgrade location  generic-charts-helm/flow-generic --version ^1.0.0 --set deployments.live.version=$(git describe) --values deploy/location/values.yaml -q  --no-color').trim()             
                  pullRequest.comment('```diff\n'+"${helm_diff}"+'\n```')
                  }
                
                }
              }  
            }
    }

    stage('Deploy Helm chart') {
      when { branch 'main' }
      parallel {
        
        stage('deploy location') {
          steps {
            script {
              container('helm') {
                new helmCommonDeploy().deploy('location', 'production', VERSION.printable())
              }
            }
          }
        }
        
      }
    }
  }
}
