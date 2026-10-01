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

                    # Remove the previously exploded directory so Tomcat redeploys
                    # rather than serving a stale mix of old and new classes.
                    rm -rf "${WEBAPPS_DIR}/${DEPLOY_CONTEXT}"
                    cp target/attendance-portal.war "${WEBAPPS_DIR}/${DEPLOY_CONTEXT}.war"
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
                    for i in $(seq 1 60); do
                        code=$(curl -s -o /tmp/health.json -w "%{http_code}" --noproxy localhost --max-time 4 "$URL" || true)
                        if [ "$code" = "200" ]; then
                            echo "Healthy after ${i}s:"; cat /tmp/health.json; echo
                            exit 0
                        fi
                        sleep 1
                    done
                    echo "ERROR: deployed application did not become healthy within 60s"
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
