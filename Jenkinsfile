pipeline {
    agent any
    stages {
            stage('Build') {
                tools {
                    jdk "jdk21"
                }
                steps {
                    sh './gradlew shadowJar'
                }
            }
        }

    post {
           always {
               archiveArtifacts artifacts: 'build/libs/*-all.jar', fingerprint: true
               cleanWs()
           }
    }
}