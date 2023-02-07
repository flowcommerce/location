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
    GOOGLE_API_KEY = credentials('location-google-api-key')
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

    stage('Display Helm Diff') {
      when {
        allOf {
          not { branch 'main' }
          changeRequest()
          expression {
            return changesCheck.hasChangesInDir('deploy')
          }        
        }
      }
      steps {
        script {
          container('helm') {
            new helmDiff().diff('location')
          }
        }
      }
    }

    stage("All in parallel") {  
      parallel {
        stage('SBT Test') {
          steps {
            container('docker') {
              script {
                docker.withRegistry('https://index.docker.io/v1/', 'jenkins-dockerhub') {
                    docker.image('flowdocker/play_builder:latest-java13').inside("--network=host ") {
                      sh 'sbt clean flowLint test doc'
                      junit allowEmptyResults: true, testResults: '**/target/test-reports/*.xml'
                    }
                }
              }
            }
          }
        }
        stage('Build and deploy location') {
          when { branch 'main'}
          stages {

            stage('Build and push docker image release') {
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

            stage('deploy location') {
              steps {
                script {
                  container('helm') {
                    new helmCommonDeploy().deploy('location', 'production', VERSION.printable(), 900)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
