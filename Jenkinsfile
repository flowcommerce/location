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
    IP2LOCATION_V4_FILE_URI = 's3://io-flow-location/ip2location/IPV4-COUNTRY.csv'
    IP2LOCATION_V6_FILE_URI = 's3://io-flow-location/ip2location/IPV6-COUNTRY.csv'
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

    stage("Build, Deploy, SBT test") {
      stages {
          stage('Build location images') {
              when { branch 'main' }
              stages {
                  stage('Build and push docker image release') {
                      stages {
                          stage('parallel image builds') {
                              parallel {
                                  // we are going reuse main pipeline agent to build location-api image 
                                  stage("Build x86_64/amd64 location") {
                                      steps {
                                          container('kaniko') {
                                              script {
                                                  String semversion = VERSION.printable()
                                                  imageBuild(
                                                      orgName: 'flowcommerce',
                                                      serviceName: 'location',
                                                      platform: 'amd64',
                                                      dockerfilePath: '/Dockerfile',
                                                      semver: semversion
                                                  )
                                              }
                                          }
                                      }
                                  }
                                  // create new agent to avoid conflicts with the main pipeline agent
                                  stage("Build arm64 location") {
                                      agent {
                                          kubernetes {
                                              label 'location-arm64'
                                              inheritFrom 'kaniko-slim-arm64'
                                          }
                                      }
                                      steps {
                                          container('kaniko') {
                                              script {
                                                  String semversion = VERSION.printable()
                                                  imageBuild(
                                                      orgName: 'flowcommerce',
                                                      serviceName: 'location',
                                                      platform: 'arm64',
                                                      dockerfilePath: '/Dockerfile',
                                                      semver: semversion
                                                  )
                                              }
                                          }
                                      }
                                  }
                              }
                          }
                          stage('run manifest tool for location') {
                              steps {
                                  container('kaniko') {
                                      script {
                                          semver = VERSION.printable()
                                          String templateName = "location-ARCH:${semver}"
                                          String targetName = "location:${semver}"
                                          String orgName = "flowcommerce"
                                          String jenkinsAgentArch = "amd64"
                                          manifestTool(templateName, targetName, orgName, jenkinsAgentArch)
                                      }
                                  }
                              }
                          }
                      }
                  }
              }
          }
          stage('Deploy location services') {
              when { branch 'main' }
              stages {
                  stage('Build location-api and location-jobs servcies') {
                      parallel {
                          stage('Deploy location Service') {
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
      }
    }
  }
}
