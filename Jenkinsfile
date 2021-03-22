#!groovy

node("executor") {
  checkout scm
  def pennsieveNexusCreds = usernamePassword(
    credentialsId: "pennsieve-nexus-ci-login",
    usernameVariable: "PENNSIEVE_NEXUS_USER",
    passwordVariable: "PENNSIEVE_NEXUS_PW"
  )

  if (["master11"].contains(env.BRANCH_NAME)) {
    // Do nothing
  } else {
    stage("Build") {
      dir("python") {
        sh """make build-release-container"""
      }
      dir("python2.7") {
        sh """make build-release-container"""
      }
      dir("scala") {
        withCredentials([pennsieveNexusCreds]) {
          sh "sbt clean compile"
        }
      }
    }

    stage("Test") {
      withCredentials([pennsieveNexusCreds]) {
        dir("python") {
          sh """make ci-test"""
        }
        dir("python2.7") {
          sh """make ci-test"""
        }
        dir("scala") {
          try {
            sh "sbt test"
          } finally {
            junit '**/target/test-reports/*.xml'
          }
        }
      }
    }
  }
}
