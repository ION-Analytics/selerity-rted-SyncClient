pipeline {
    agent {
        label 'ec2-fleet'
    }

    triggers {
        snapshotDependencies()  // Automatically rebuild when Maven SNAPSHOT dependencies are updated
    }

    environment {
        // JDK and Maven are set by githubCheckout with defaults
        // Override here if needed:
        // jdk = 'JDK 21 (Corretto, Local)'
        // maven = 'Maven 3.9'

        jdk = 'OpenJDK 1.8'
        version = "unversioned"
        buildDocker = false
        buildDockerRepo = ''

        // Teams notification webhook URL
        TEAMS_WEBHOOK_URL = credentials('teams-webhook-url')
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
        ansiColor('xterm')
    }

    stages {
        stage('Preparation') {
            steps {
                script {
                    // GitHub multibranch pipeline provides BRANCH_NAME, CHANGE_ID automatically
                    echo "Branch: ${env.BRANCH_NAME}"
                    if (env.CHANGE_ID) {
                        echo "Pull Request: #${env.CHANGE_ID}"
                        echo "PR source: ${env.CHANGE_BRANCH} -> ${env.CHANGE_TARGET}"
                    }
                }

                githubCheckout(
                    projectName: 'SyncClient',
                    projectRepo: 'https://github.com/ION-Analytics/selerity-rted-SyncClient.git'
                )

                cleanLocalM2Repository()

                script {
                    causeShortDesc = currentBuild.getBuildCauses()[0].shortDescription
                    committer = sh(returnStdout: true, script: 'git log --format="%ae" -1 | grep -o --perl ".*(?=@)"').trim()
                    shortCommit = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
                    gitBranch = env.BRANCH_NAME

                    // Determine build type
                    isReview = (env.CHANGE_ID != null)  // This is a PR

                    if (isReview) {
                        currentBuild.displayName = "PR #${env.CHANGE_ID} Build $BUILD_NUMBER"
                    } else {
                        version = readMavenPom().getVersion()
                        currentBuild.displayName = "Build ${version}.$BUILD_NUMBER." + shortCommit
                    }
                }

                rtMavenDeployer(
                    id: "MAVEN_DEPLOYER",
                    serverId: "artifactory1",
                    releaseRepo: "libs-releases-local",
                    snapshotRepo: "libs-snapshots-local",
                    includePatterns: ["*.jar", "*.pom", "*.war"],
                    deployEvenIfUnstable: true
                )
            }
        }

        stage('Build') {
            steps {
                configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
                    mavenCompile()
                }
            }
        }

        stage('Test') {
            steps {
                configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
                    mavenTest()
                }
            }
        }

        stage('Package') {
            steps {
                configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
                    updateDependenciesGraph(isReview: isReview)
                    generateThirdPartyLicenses()
                }
            }
        }

        stage('Validate') {
            steps {
                configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
                    checkVulnerabilities()
                    publishThirdPartyReport()
                    publishTestResultsAndConverage()
                }
            }
        }

        stage('Publish') {
            when {
                expression {
                    version != 'unversioned'
                }
            }
            steps {
                configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
                    mavenPackage(buildNumber: "${BUILD_NUMBER}", revision: shortCommit, branch: gitBranch)
                    archiveArtifacts artifacts: 'target/*.jar', onlyIfSuccessful: true
                    publishArtifactoryBuildInfo()
                }
            }
        }
    }

    post {
        always {
            recordIssues(
                enabledForFailure: true,
                healthy: 5,
                minimumSeverity: 'HIGH',
                tools: [java(), mavenConsole()],
                unhealthy: 15
            )
        }
        success {
            teamsNotify(
                env.TEAMS_WEBHOOK_URL,
                'Success',
                "Build ${currentBuild.displayName} succeeded"
            )
        }
        unstable {
            teamsNotify(
                env.TEAMS_WEBHOOK_URL,
                'Warn',
                "Build ${currentBuild.displayName} unstable"
            )
        }
        failure {
            teamsNotify(
                env.TEAMS_WEBHOOK_URL,
                'Failure',
                "Build ${currentBuild.displayName} failed"
            )
        }
    }
}
