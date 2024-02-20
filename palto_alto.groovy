pipeline {
    agent any

    parameters{
        string(name: 'sourceCode_Url', defaultValue: 'https://github.com/NAGAVENKATASAIDURGA/paloalto.git', description: 'Enter Repository URL here')
        string(name: 'branch', defaultValue: 'paloalto', description: 'Enter Branch name here')
        choice(name: 'SourceCode_Language', choices:['Java'], description: 'Select Source Code Language')
        choice(name: 'Build_Tool', choices:['Maven'], description: 'Select Build Tool')
        choice(name: 'Code_Quality_Analysis', choices:['SonarQube'], description: 'Select Code Quality Analysis Tool')
        string(name: 'dockerTag', defaultValue: 'latest', description: 'Enter Docker image tag here')
    }
    stages {
        stage('Git Clone'){
            steps{
            git branch: "${params.branch}", url: "${params.sourceCode_Url}"
            }
        }
        
        stage('Build') {
            tools {
                maven 'mvn3.6.3'
            }
            steps {
                
                sh 'mvn clean install'
               
            }
        }
        stage('SonarQube') {
            steps {
                 withSonarQubeEnv(credentialsId: 'sonarQube', installationName: 'QualityChecks') {
                                sh "mvn clean verify sonar:sonar -Dsonar.projectKey=genai -Dsonar.projectName='genai'"
            }
        }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
        stage('Write Dockerfile') {
            steps {
                sh 'echo "FROM openjdk:8-jre" >> dockerfile.docker'
                sh 'echo "EXPOSE 8080" >> dockerfile.docker'
                sh 'echo "COPY target/*.jar /usr/src/myapp.jar" >> dockerfile.docker'
            }
        }
        stage('Docker Build') {
            steps {
                sh "docker build -t abhishekraov/palto_alto:${params.dockerTag} ."
            }
        }
        stage('Push to DockerHub') {
            steps {
            script {
            sh "docker push abhishekraov/palto_alto:${params.dockerTag} "
            }
        }
        }
        stage('Deploy as Pod'){
            steps{
                script{
                    sh "kubectl delete pod paltoalto || true"
                    sh "kubectl run paltoalto --image=abhishekraov/palto_alto:${params.dockerTag}"
                }
            }
        }
    }
}
