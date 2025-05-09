#!/usr/bin/env groovy

def isPullRequest = !!(env.CHANGE_ID)
String shortCommit = ''
String tag = ''
String dockerImage = ''

if (!isPullRequest) {
    properties([
        buildDiscarder(logRotator(numToKeepStr: '5')),
        pipelineTriggers([[$class:"SCMTrigger", scmpoll_spec:"H/10 * * * *"]]),
    ])
}

node('linux-arm64') {
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
          infra.runMaven(['-PgeneratePluginData'], '17', null, true, !infra.isTrusted())
        }

        if (!infra.isInfra()) {
            /*
            * Running everything within an nginx container to provide the
            * DATA_FILE_URL necessary for the build and execution of the docker
            * container
            */
            docker.image('nginx:alpine').withRun('-p 80:80 -v $PWD/target:/usr/share/nginx/html') { c ->

                /*
                * Building our war file inside a Maven container which links to
                * the nginx container for accessing the DATA_FILE_URL
                */
                stage('Build') {
                    withEnv([
                        'DATA_FILE_URL=http://localhost/plugins.json.gzip',
                    ]) {
                        infra.runMaven(['-Dmaven.test.failure.ignore',  'verify'], '8', null, true, !infra.isTrusted())
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
                }

                /*
                * Spin up our built container and make sure we can execute API
                * calls against it before calling it successful
                */
                stage('Verify Container') {
                    container.withRun("--link ${c.id}:nginx -p 8080:8080 -e DATA_FILE_URL=http://nginx/plugins.json.gzip") { api ->
                        sh 'wget --debug -O /dev/null --retry-connrefused --timeout 120 --tries=15 http://localhost:8080/versions'
                    }
                }
            }
        } else {
            stage('Maven build') {
                infra.runMaven(['-Dmaven.test.skip=true',  'package'], '8')
                stash name: 'build', includes: 'target/*.war'
            }
            stage('Build and publish Docker image') {
                buildDockerAndPublishImage('plugin-site-api', [unstash: 'build', targetplatforms: 'linux/amd64,linux/arm64', automaticSemanticVersioning: false])
            }
        }

        stage('Archive Artifacts') {
            archiveArtifacts artifacts: 'target/*.war, target/*.json.gzip', fingerprint: true
        }
    }
}
