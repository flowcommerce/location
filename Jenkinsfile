properties([pipelineTriggers([githubPush()])])

pipeline {
  options {
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '10'))
    timeout(time: 30, unit: 'MINUTES')
  }

  agent {
    kubernetes {
      inheritFrom 'kaniko-slim'

      containerTemplates([
        containerTemplate(name: 'play', image: 'flowdocker/play_builder:latest-java17', alwaysPullImage: true, resourceRequestMemory: '1Gi', command: 'cat', ttyEnabled: true)
      ])
    }
  }

  environment {
    ORG      = 'flowcommerce'
    GOOGLE_API_KEY = credentials('location-google-api-key')
    IP2LOCATION_V4_FILE_URI = credentials('ip2location-v4-file-uri')
    IP2LOCATION_V6_FILE_URI = credentials('ip2location-v4-file-uri')
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
        }
      }
      steps {
        script {
          container('helm') {
            helmCommonDiff(['location'])
          }
        }
      }
    }

    stage("All in parallel") {
      parallel {
        stage('SBT Test') {
          steps {
            container('play') {
              script {
                try {
                  sh 'sbt clean flowLint coverage test scalafmtSbtCheck scalafmtCheck doc'
                  sh 'sbt coverageAggregate'
                }
                finally {
                  postSbtReport()
                }
              }
            }
          }
        }
        stage('Build and deploy location') {
          when { branch 'main' }
          stages {

            stage('Build and push docker image release') {
              steps {
                container('kaniko') {
                  script {
                    semver = VERSION.printable()
                    
                    sh """
                      /kaniko/executor -f `pwd`/Dockerfile -c `pwd` \
                      --snapshot-mode=redo --use-new-run  \
                      --destination ${env.ORG}/location:$semver
                    """
                  }
                }
              }
            }
            stage('Deploy location') {
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
