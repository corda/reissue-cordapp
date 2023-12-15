#!groovy

/**
 * Kill already started job.
 * Assume new commit takes precendence and results from previous
 * unfinished builds are not required.
 * This feature doesn't play well with disableConcurrentBuilds() option
 */
@Library('existing-build-control')
import static com.r3.build.BuildControl.killAllExistingBuildsForJob
import groovy.transform.Field
killAllExistingBuildsForJob(env.JOB_NAME, env.BUILD_NUMBER.toInteger())

def isReleaseTag() {
    return (env.TAG_NAME =~ /^release-.*$/)
}

def isReleaseCandidate() {
    return (isReleaseTag()) && (env.TAG_NAME =~ /.*-(RC|HC)\d+(-.*)?/)
}

def isReleaseBranch() {
    return (env.BRANCH_NAME =~ /^release\/.*$/)
}

def isRelease = isReleaseTag() || isReleaseCandidate() || isReleaseBranch()
String publishOptions = isRelease ? "-s --info" : "--no-daemon -s -PversionFromGit"

/**
 * Common Gradle arguments for all Gradle executions
 */
String COMMON_GRADLE_PARAMS = [
        '--no-daemon',
        '--stacktrace',
        '--info',
        /*
        ** revert default behavour for `ignoreFailures` and
        ** do not ignore test failures in PR builds
        */
        '-Ptests.ignoreFailures=false',
        '-Pcompilation.warningsAsErrors=false',
        '-Ptests.failFast=true',
        '-Ddependx.branch.origin="${GIT_COMMIT}"',    // DON'T change quotation - GIT_COMMIT variable is substituted by SHELL!!!!
        '-Ddependx.branch.target="${CHANGE_TARGET}"', // DON'T change quotation - CHANGE_TARGET variable is substituted by SHELL!!!!
].join(' ')

pipeline {
agent { label 'standard' }

    parameters {
        booleanParam name: 'DO_PUBLISH', defaultValue: isRelease, description: 'Publish artifacts to Artifactory?'
    }

    options { timestamps() }

    environment {
        ARTIFACTORY_CREDENTIALS = credentials('artifactory-credentials')
        CORDA_ARTIFACTORY_USERNAME = "${env.ARTIFACTORY_CREDENTIALS_USR}"
        CORDA_ARTIFACTORY_PASSWORD = "${env.ARTIFACTORY_CREDENTIALS_PSW}"
        EXECUTOR_NUMBER = "${env.EXECUTOR_NUMBER}"
        LOOPBACK_ADDRESS = "172.17.0.1"
        DOCKER_CREDENTIALS = credentials('docker-for-oracle-login')
        SNYK_TOKEN = credentials('c4-ent-snyk-api-token-secret')
        JAVA_HOME = "/usr/lib/jvm/java-17-amazon-corretto"
    }

    stages {
        stage('Build') {
            steps {
                sh script: [
                        './gradlew',
                        COMMON_GRADLE_PARAMS,
                        'assemble'
                ].join(' ')

            }
        }

        stage('Snyk Security') {
            when {
                expression { isReleaseTag() || isReleaseBranch() }
            }
            steps {
                script {
                    // Invoke Snyk for each Gradle sub project we wish to scan
                    def modulesToScan = ['contracts', 'workflows']
                    modulesToScan.each { module ->
                        snykSecurityScan(env.SNYK_TOKEN, "--sub-project=$module --configuration-matching='^runtimeClasspath\$' --prune-repeated-subdependencies --debug --remote-repo-url='${env.GIT_URL}' --target-reference='${env.BRANCH_NAME}' --project-tags=Branch='${env.BRANCH_NAME.replaceAll("[^0-9|a-z|A-Z]+", "_")}'", false, true)
                    }
                }
            }
        }

        stage('Unit Tests') {
            steps {
                timeout(30) {
                    sh script: [
                            './gradlew',
                            COMMON_GRADLE_PARAMS,
                            'test'
                    ].join(' ')

                }
            }
        }

        stage('Publish to Artifactory') {
            when {
                expression { params.DO_PUBLISH }
                beforeAgent true
            }
            steps {
                rtServer(
                        id: 'R3-Artifactory',
                        url: 'https://software.r3.com/artifactory',
                        credentialsId: 'artifactory-credentials'
                )
                rtGradleDeployer(
                        id: 'deployer',
                        serverId: 'R3-Artifactory',
                        repo: isRelease ? 'corda-lib' : 'corda-lib-dev'
                )
                rtGradleRun(
                        usesPlugin: true,
                        useWrapper: true,
                        switches: publishOptions,
                        tasks: 'artifactoryPublish',
                        deployerId: 'deployer',
                        buildName: env.ARTIFACTORY_BUILD_NAME
                )
                rtPublishBuildInfo(
                        serverId: 'R3-Artifactory',
                        buildName: env.ARTIFACTORY_BUILD_NAME
                )
            }
        }

    }

    post {
        always {
            junit '**/build/test-results/**/*.xml'
        }
        cleanup {
            deleteDir() /* clean up our workspace */
        }
    }
}
