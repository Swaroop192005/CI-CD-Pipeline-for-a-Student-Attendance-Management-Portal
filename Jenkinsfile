// Pipeline as code for the Student Attendance Management Portal (Task 8).
//
// Checkout -> Build -> Test -> Package -> Deploy -> Verify, with the deployment
// target parameterised so the same pipeline serves more than one environment.
pipeline {
    agent any

    parameters {
        string(name: 'DEPLOY_CONTEXT', defaultValue: 'attendance',
               description: 'Tomcat context path. The WAR is deployed as <context>.war, so the app serves at /<context>.')
        string(name: 'ATTENDANCE_THRESHOLD', defaultValue: '75',
               description: 'Minimum attendance percentage below which a student is flagged. Changing this needs no rebuild.')
        string(name: 'REGISTRY', defaultValue: 'localhost:5000',
               description: 'Image registry. Defaults to the local registry; set to docker.io/<user> to publish to Docker Hub.')
        string(name: 'CONTAINER_PORT', defaultValue: '8081',
               description: 'Host port the deployed container is published on.')
        string(name: 'TOMCAT_PORT', defaultValue: '8082',
               description: 'Port of the Tomcat the WAR is deployed to, used by the verification stage.')
        booleanParam(name: 'SKIP_DEPLOY', defaultValue: false,
               description: 'Build and test only. Useful for verifying a branch without touching the server.')
    }

    options {
        // timestamps() is deliberately absent: it needs the timestamper plugin,
        // which this environment cannot install (see docs/07-jenkins-ci.md).
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 20, unit: 'MINUTES')
    }

    environment {
        JAVA_HOME   = '/opt/java/openjdk'
        PATH        = "/opt/java/openjdk/bin:/opt/maven/bin:${env.PATH}"
        // Kept outside the workspace so a `clean` cannot wipe the dependency cache.
        MAVEN_OPTS  = '-Dmaven.repo.local=/var/maven-cache/repository'
        WEBAPPS_DIR = '/deploy/webapps'
        // The browser runs in the samp-selenium container; see jenkins/docker-compose.yml.
        SELENIUM_URL = 'http://localhost:4444/wd/hub'
        IMAGE_NAME   = 'samp-attendance'
        // Every image carries the build number, so a running container can always
        // be traced back to the exact pipeline run that produced it.
        IMAGE_VERSION = "1.0.${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git --no-pager log --oneline -1'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B $MAVEN_OPTS clean compile'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn -B $MAVEN_OPTS test'
            }
            post {
                // always: a failed run must still publish its report, otherwise the
                // quality gate in Task 10 would have nothing to show for a red build.
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('UI quality gate (Selenium)') {
            steps {
                // The gate. Deploy sits after this stage, so a failing journey
                // leaves deployment unexecuted rather than executed-and-rolled-back.
                sh '''
                    mvn -B $MAVEN_OPTS -Pselenium verify \
                        -Dselenium.remote.url=${SELENIUM_URL} \
                        -Dselenium.screenshot.dir=$WORKSPACE/selenium-failures
                '''
            }
            post {
                always {
                    junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: true
                    // A failing journey writes a screenshot; archive it so the
                    // diagnosis is attached to the build that failed.
                    archiveArtifacts artifacts: 'selenium-failures/**', allowEmptyArchive: true
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B $MAVEN_OPTS package -DskipTests'
                archiveArtifacts artifacts: 'target/*.war', fingerprint: true
            }
        }

        stage('Deploy') {
            when {
                expression { return !params.SKIP_DEPLOY }
            }
            steps {
                sh '''
                    set -e
                    echo "Deploying as context '${DEPLOY_CONTEXT}' with threshold ${ATTENDANCE_THRESHOLD}%"
                    test -d "$WEBAPPS_DIR" || { echo "ERROR: $WEBAPPS_DIR not mounted"; exit 1; }

                    # Copy to a temporary name, then rename into place.
                    #
                    # The rename is atomic within the same filesystem, which matters
                    # for a 55 MB artefact: Tomcat's autoDeploy scanner would
                    # otherwise be able to pick the WAR up mid-copy and deploy a
                    # truncated archive. The temp name ends in .tmp so the scanner,
                    # which only matches *.war, ignores it.
                    #
                    # The previously exploded directory is deliberately NOT deleted
                    # here: Tomcat creates it as root with mode 750, so the Jenkins
                    # user cannot remove it, and Tomcat replaces it itself when it
                    # sees a newer WAR. Trying to delete it is what broke build #5.
                    TMP="${WEBAPPS_DIR}/.${DEPLOY_CONTEXT}.war.tmp"
                    cp target/attendance-portal.war "$TMP"
                    mv -f "$TMP" "${WEBAPPS_DIR}/${DEPLOY_CONTEXT}.war"
                    ls -lh "${WEBAPPS_DIR}/"
                '''
            }
        }

        stage('Verify') {
            when {
                expression { return !params.SKIP_DEPLOY }
            }
            steps {
                sh '''
                    set -e
                    URL="http://localhost:${TOMCAT_PORT}/${DEPLOY_CONTEXT}/actuator/health"
                    echo "Waiting for ${URL}"
                    for i in $(seq 1 120); do
                        code=$(curl -s -o /tmp/health.json -w "%{http_code}" --noproxy localhost --max-time 4 "$URL" || true)
                        if [ "$code" = "200" ]; then
                            echo "Healthy after ${i}s:"; cat /tmp/health.json; echo
                            exit 0
                        fi
                        sleep 1
                    done
                    echo "ERROR: deployed application did not become healthy within 120s"
                    exit 1
                '''
            }
        }


        stage('Docker build') {
            steps {
                script {
                    env.GIT_SHA = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                }
                sh '''
                    set -e
                    docker build \
                        --build-arg APP_VERSION=${IMAGE_VERSION} \
                        --build-arg BUILD_NUMBER=${BUILD_NUMBER} \
                        --build-arg GIT_COMMIT=${GIT_SHA} \
                        -t ${IMAGE_NAME}:${IMAGE_VERSION} \
                        -t ${IMAGE_NAME}:latest .
                    docker images ${IMAGE_NAME} --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
                '''
            }
        }

        stage('Publish image') {
            steps {
                // Tagged with the build number AND latest (AC-22.2): the version
                // tag is what makes a rollback possible, because 'latest' cannot
                // name the release you want to go back to.
                sh '''
                    set -e
                    docker tag ${IMAGE_NAME}:${IMAGE_VERSION} ${REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}
                    docker tag ${IMAGE_NAME}:${IMAGE_VERSION} ${REGISTRY}/${IMAGE_NAME}:latest
                    docker push ${REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}
                    docker push ${REGISTRY}/${IMAGE_NAME}:latest
                    echo "Published ${REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}"
                '''
            }
        }

        stage('Deploy container') {
            steps {
                // A fresh container from the image just published, not a restart
                // of the old one, so what runs is exactly what was tested.
                sh '''
                    set -e
                    docker rm -f ${IMAGE_NAME}-run >/dev/null 2>&1 || true
                    docker run -d --name ${IMAGE_NAME}-run \
                        -p ${CONTAINER_PORT}:8080 \
                        -e ATTENDANCE_THRESHOLD=${ATTENDANCE_THRESHOLD} \
                        --restart unless-stopped \
                        ${REGISTRY}/${IMAGE_NAME}:${IMAGE_VERSION}
                    docker ps --filter name=${IMAGE_NAME}-run --format "  {{.Names}} {{.Image}} {{.Status}} {{.Ports}}"
                '''
            }
        }

        stage('Verify container') {
            steps {
                sh '''
                    set -e
                    for i in $(seq 1 90); do
                        state=$(docker inspect --format "{{.State.Health.Status}}" ${IMAGE_NAME}-run 2>/dev/null || echo starting)
                        if [ "$state" = "healthy" ]; then
                            echo "Container healthy after ${i}s"
                            curl -s --noproxy localhost "http://localhost:${CONTAINER_PORT}/actuator/health"; echo
                            exit 0
                        fi
                        sleep 1
                    done
                    echo "ERROR: container did not become healthy"
                    docker logs --tail 40 ${IMAGE_NAME}-run
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            echo "SUCCESS — deployed to http://localhost:${params.TOMCAT_PORT}/${params.DEPLOY_CONTEXT}"
        }
        failure {
            echo 'FAILED — the deploy stage does not run unless build and tests pass.'
        }
        always {
            echo "Build #${env.BUILD_NUMBER} finished with status ${currentBuild.currentResult}"
        }
    }
}
