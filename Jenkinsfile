#!/usr/bin/env groovy

def isPullRequest = !!(env.CHANGE_ID)
def pushToDocker = infra.isTrusted()
String shortCommit = ''
String tag = ''
String dockerImage = ''

if (!isPullRequest) {
    properties([
        buildDiscarder(logRotator(numToKeepStr: '5')),
        pipelineTriggers([[$class:"SCMTrigger", scmpoll_spec:"H/10 * * * *"]]),
    ])
}

node('docker&&linux') {
    /* Make sure we're always starting with a fresh workspace */
    deleteDir()

    stage('Checkout') {
        checkout scm
        sh 'git rev-parse HEAD > GIT_COMMIT'
        shortCommit = readFile('GIT_COMMIT').take(6)
        tag = sh ( script: 'git tag --points-at HEAD', returnStdout: true ).trim()
    }

    timestamps {
        stage('Generate Plugin Data') {
            docker.image('maven').inside {
              sh 'mvn -PgeneratePluginData'
            }
        }

        /*
         * Running everything within an nginx container to provide the
         * DATA_FILE_URL necessary for the build and execution of the docker
         * container
         */
        docker.image('nginx:alpine').withRun('-v $PWD/target:/usr/share/nginx/html') { c ->

            /*
             * Building our war file inside a Maven container which links to
             * the nginx container for accessing the DATA_FILE_URL
             */
            stage('Build') {
                docker.image('maven:3-adoptopenjdk-11').inside("--link ${c.id}:nginx") {
                    withEnv([
                        'DATA_FILE_URL=http://nginx/plugins.json.gzip',
                    ]) {
                        List<String> mvnOptions = ['-Dmaven.test.failure.ignore','verify']
                        infra.runMaven(
                            mvnOptions,
                            /*jdk*/ "8",
                            /*extraEnv*/ null,
                            /*settingsFile*/ null,
                            /*addToolEnv*/ false
                          )
                    }
                }

                /** archive all our artifacts for reporting later */
                junit 'target/surefire-reports/**/*.xml'
            }

            /*
             * Build our application container with some extra parameters to
             * make sure it doesn't leave temporary containers behind on the
             * agent
             */
            def container
            stage('Containerize') {
                if (tag.isEmpty()) {
                  echo "No tag for this commit, creating a docker image with ${shortCommit} version..."
                  dockerImage = "jenkinsciinfra/plugin-site-api:${env.BUILD_ID}-${shortCommit}"
                } else {
                  echo "Tag found for this commit, creating a docker image with ${tag} version..."
                  dockerImage = "jenkinsciinfra/plugin-site-api:${tag}"
                }
                container = docker.build(dockerImage, '--no-cache --rm .')
                if (pushToDocker) {
                    echo "Pushing container ${dockerImage}"
                    infra.withDockerCredentials {
                        container.push()
                    }
                }
            }

            /*
             * Spin up our built container and make sure we can execute API
             * calls against it before calling it successful
             */
            stage('Verify Container') {
                container.withRun("--link ${c.id}:nginx -e DATA_FILE_URL=http://nginx/plugins.json.gzip") { api ->
                    docker.image('cirrusci/wget:latest').inside("--link ${api.id}:api") {
                        sh 'wget --debug -O /dev/null --retry-connrefused --timeout 120 --tries=15 http://api:8080/versions'
                    }
                }
            }

            stage('Tag container as latest') {
                if (pushToDocker) {
                    echo "Tagging ${dockerImage} as latest"
                    infra.withDockerCredentials {
                        container.push('latest')
                    }
                }
            }
        }

        stage('Archive Artifacts') {
            archiveArtifacts artifacts: 'target/*.war, target/*.json.gzip', fingerprint: true
        }
    }
}
