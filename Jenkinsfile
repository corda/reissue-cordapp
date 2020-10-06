@Library('corda-shared-build-pipeline-steps')
import static com.r3.build.BuildControl.killAllExistingBuildsForJob
killAllExistingBuildsForJob(env.JOB_NAME, env.BUILD_NUMBER.toInteger())
pipeline {
    agent { label 'standard' }
    options {
        timestamps()
        timeout(time: 1, unit: 'HOURS')
        buildDiscarder(logRotator(daysToKeepStr: '14', artifactDaysToKeepStr: '14'))
    }
    environment {
        EXECUTOR_NUMBER = "${env.EXECUTOR_NUMBER}"
        BUILD_ID = "${env.BUILD_ID}-${env.JOB_NAME}"
        ARTIFACTORY_CREDENTIALS = credentials('artifactory-credentials')
        CORDA_ARTIFACTORY_USERNAME = "${env.ARTIFACTORY_CREDENTIALS_USR}"
        CORDA_ARTIFACTORY_PASSWORD = "${env.ARTIFACTORY_CREDENTIALS_PSW}"
    }
    stages {
        stage('Compile') {
            steps {
			    sh "./gradlew --no-daemon build -x test"
            }
        }
	stage('Test') {
            steps {
			    sh "./gradlew --no-daemon test "
            }
        }
    }
    post {
        always {
            junit allowEmptyResults: false, testResults: '**/build/test-results/**/*.xml'
        }
        cleanup {
            deleteDir() /* clean up our workspace */
        }
    }
}
