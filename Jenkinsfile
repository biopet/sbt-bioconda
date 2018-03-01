node('local') {
    try {

        stage('Init') {
            env.JAVA_HOME="${tool 'JDK 8u102'}"
            env.PATH="${env.JAVA_HOME}/bin:${env.PATH}"
            sh 'java -version'
            tool 'sbt 0.13.15'
            checkout scm
            sh 'git submodule update --init --recursive'
            // Remove sbt-biopet SNAPSHOT plugin from cache.
            sh 'rm -rf $HOME/.ivy2/cache/scala_*/sbt_*/com.github.biopet/sbt-biopet/*/*SNAPSHOT*'
        }

        stage('Build & Test') {
            sh "${tool name: 'sbt 0.13.15', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt -no-colors clean scalafmt scripted headerCheck"
            sh "git diff --exit-code || (echo \"ERROR: Git changes detected, please regenerate the readme and run scalafmt with: sbt headerCreate scalafmt\" && exit 1)"
        }

        stage('Results') {
            //step([$class: 'ScoveragePublisher', reportDir: 'target/scala-2.11/scoverage-report/', reportFile: 'scoverage.xml'])
            //junit '**/test-output/junitreports/*.xml'
            // Remove locally published sbt-biopet plugin from cache to avoid conflicts with other tools.
            sh 'rm -rf $HOME/.ivy2/cache/scala_2.10/sbt_0.13/com.github.biopet/sbt-biopet'
        }

        if (env.BRANCH_NAME == 'develop') stage('Publish') {
            sh "${tool name: 'sbt 0.13.15', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt -no-colors publish"
        }

        if (currentBuild.result == null || "SUCCESS" == currentBuild.result) {
            currentBuild.result = "SUCCESS"
            slackSend(color: '#00FF00', message: "${currentBuild.result}: Job '${env.JOB_NAME} #${env.BUILD_NUMBER}' (<${env.BUILD_URL}|Open>)", channel: '#biopet-bot', teamDomain: 'lumc', tokenCredentialId: 'lumc')
        } else {
            slackSend(color: '#FFFF00', message: "${currentBuild.result}: Job '${env.JOB_NAME} #${env.BUILD_NUMBER}' (<${env.BUILD_URL}|Open>)", channel: '#biopet-bot', teamDomain: 'lumc', tokenCredentialId: 'lumc')
        }
    } catch (e) {

        if (currentBuild.result == null || "FAILED" == currentBuild.result) {
            currentBuild.result = "FAILED"
            slackSend(color: '#FF0000', message: "${currentBuild.result}: Job '${env.JOB_NAME} #${env.BUILD_NUMBER}' (<${env.BUILD_URL}|Open>)", channel: '#biopet-bot', teamDomain: 'lumc', tokenCredentialId: 'lumc')
        } else {
            slackSend(color: '#FFFF00', message: "${currentBuild.result}: Job '${env.JOB_NAME} #${env.BUILD_NUMBER}' (<${env.BUILD_URL}|Open>)", channel: '#biopet-bot', teamDomain: 'lumc', tokenCredentialId: 'lumc')
        }
        // Remove sbt-biopet SNAPSHOT plugin from cache.
        sh 'rm -rf $HOME/.ivy2/cache/scala_*/sbt_*/com.github.biopet/sbt-biopet/*SNAPSHOT*'
        //junit '**/test-output/junitreports/*.xml'

        throw e
    }

}
