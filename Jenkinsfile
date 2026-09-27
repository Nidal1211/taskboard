pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    environment {
        SPRING_DATASOURCE_URL = 'jdbc:postgresql://db:5432/taskboard_test'
        SERVER_PORT = '8081'
        MAVEN_OPTS = '-Xmx512m'
        JIRA_URL = 'https://nidhal-taskboard.atlassian.net'
    }

    stages {
        stage('Backend bauen') {
            steps {
                dir('backend') {
                    sh 'chmod +x mvnw'
                    sh './mvnw -B clean package -DskipTests'
                }
            }
        }

        stage('Backend starten') {
            steps {
                dir('backend') {
                    sh '''
                        JENKINS_NODE_COOKIE=dontKillMe nohup java -Xmx384m -jar target/taskboard-backend-*.jar \
                            --spring.profiles.active=test > app.log 2>&1 &
                        echo $! > app.pid
                    '''
                }
                sh '''
                    for i in $(seq 1 60); do
                        if curl -sf http://localhost:8081/actuator/health > /dev/null; then
                            echo "Backend ist bereit"
                            exit 0
                        fi
                        sleep 2
                    done
                    echo "Backend ist nicht gestartet:"
                    cat backend/app.log
                    exit 1
                '''
            }
        }

        stage('API-Tests') {
            steps {
                dir('backend') {
                    sh './mvnw -B -f ../e2e-tests/pom.xml test -DbaseUrl=http://localhost:8081'
                }
            }
        }
    }

        post {
        always {
            junit allowEmptyResults: true, testResults: 'e2e-tests/target/surefire-reports/*.xml'
            withCredentials([usernamePassword(credentialsId: 'jira-credentials',
                    usernameVariable: 'JIRA_EMAIL', passwordVariable: 'JIRA_API_TOKEN')]) {
                sh 'cd ai && python3 report_results.py ../e2e-tests/target/surefire-reports || echo "Rückmeldung an Jira fehlgeschlagen"'
            }
            sh 'if [ -f backend/app.pid ]; then kill $(cat backend/app.pid) || true; fi'
            archiveArtifacts artifacts: 'backend/app.log', allowEmptyArchive: true
        }
    }
}