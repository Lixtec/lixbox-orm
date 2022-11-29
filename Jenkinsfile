BUILD_STATUS = 'success';
gitUri = 'https://github.com/lixbox-team/lixbox-orm.git';
teamsHook  = '${TEAMS_BOT_URI}';
channel = 'lixbox';
branchName = 'jdk-11'

@NonCPS
def onFailed(e) {
    currentBuild.result = 'FAILED'
    BUILD_STATUS = 'FAILED'
    def msg = 'The '+JOB_NAME+' - Build # '+BUILD_NUMBER+' is '+BUILD_STATUS+'. \n Check console output at '+BUILD_URL+' to view the results ';   
    office365ConnectorSend message: msg, status: BUILD_STATUS, webhookUrl: teamsHook, color: "dd4040"
    
}

withCredentials([usernamePassword(credentialsId: 'e1529c62-f3ec-4b12-bbad-2a352fda9af2', usernameVariable: 'JENKINS_LOGIN', passwordVariable: 'JENKINS_PWD')]) {
	node('slave-gradle-jdk11') {    
	    stage('Init'){
	        echo 'Initialisation started'
	        office365ConnectorSend message: 'The '+JOB_NAME+' - Build # '+BUILD_NUMBER+'  start. \n Check console output at '+BUILD_URL+' to view the results ' , webhookUrl: teamsHook, color: "rgb(184, 255, 184)"
	        
	        try{
	            sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew --stop -Djenkins.login=${JENKINS_LOGIN} -Djenkins.password=${JENKINS_PWD} '
	        }
	        catch (e){
	        }
	        try
	        {
	            deleteDir()
	            git url: gitUri, branch: branchName
	            sh 'chmod -R 755 ${WORKSPACE}'
	            sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew  -Djenkins.login=${JENKINS_LOGIN} -Djenkins.password=${JENKINS_PWD} clean --stacktrace'            
	        }
	        catch (e){
	            onFailed(e);
	            error e
	        }
	        echo 'Initialisation finished'
	    }
	    
	    stage('Check'){
	        echo 'Check started'
	        try{
	            sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew  -Djenkins.login=${JENKINS_LOGIN} -Djenkins.password=${JENKINS_PWD}  check --stacktrace'         
	        }
	        catch (e){
	            onFailed(e);
	            error e
	        }
	        echo 'Check finished'
	    }
	    
	    stage('Distribution for test'){
	        echo 'Distribution for test started'
	        retry(2){ 
	            try{
                    sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew  -Djenkins.login=${JENKINS_LOGIN} -Djenkins.password=${JENKINS_PWD}  -Dorg.ajoberstar.grgit.auth.username=${JENKINS_LOGIN} -Dorg.ajoberstar.grgit.auth.password=${JENKINS_PWD} --stacktrace publish -Psigning.password=${KS_PWD}'
	            }
	            catch (e){
	                onFailed(e);
	                error e
	            }
	        }
	        echo 'Distribution for test finished'
	    }
	       
	    stage('Code review & report'){
	        echo 'Code review & report started'
	        try{
	            sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew  -Djenkins.login=${JENKINS_LOGIN} -Djenkins.password=${JENKINS_PWD}  -x test --stacktrace sonar checkSonarQualityGate'
	        }
	        catch (e){
	            onFailed(e);
	            error e
	        }
	        echo 'Code review & report finished'
	    }
	    
	    stage('Distribution for production'){
	        echo 'Distribution for production started'
	        retry(2){ 
	            try{
	            	withCredentials([usernamePassword(credentialsId: 'f33ba02a-2b78-47ae-9577-86299b22dbde', usernameVariable: 'MVCT_LOGIN', passwordVariable: 'MVCT_PWD')]) {
	                    sh 'export SOURCE_BUILD_NUMBER=${BUILD_NUMBER} && ${WORKSPACE}/gradlew -Djenkins.login=${JENKINS_LOGIN} -Djenkins.password=${JENKINS_PWD}  -Dmavencentral.login=${MVCT_LOGIN} -Dmavencentral.password=${MVCT_PWD} -Dgit.token=${GIT_TOKEN} -Penv=prod --stacktrace publish -Psigning.password=${KS_PWD}'
                    }
	                if(!currentBuild.result)
	                {
	                   currentBuild.result = 'SUCCESS'
	                   BUILD_STATUS = 'SUCCESS'
	                }
	                def msg = 'The '+JOB_NAME+' - Build # '+BUILD_NUMBER+' is '+BUILD_STATUS+'. \n Check console output at '+BUILD_URL+' to view the results.';
    				office365ConnectorSend message: msg, status: BUILD_STATUS, webhookUrl: teamsHook, color: "rgb(184, 255, 184)"
	            }
	            catch (e){
	                onFailed(e);
	                error e
	            }
	        }
	        echo 'Distribution for production finished'
	    }
	}
}