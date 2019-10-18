#!groovy

//Download output of test cases
def downloadOutput(stageName){
    return{
      stage ("${stageName}")
            {
          def dirpath = """$WORKSPACE/$BUILD_NUMBER/terraform"""
             dir(dirpath){
                sh """
                echo $dirpath
                sudo -S rm -rf $WORKSPACE/wrf_output.zip 
                """
                sh """sudo -S rm -rf output_testcase""" 
                sh """sudo -S rm -rf wrf_output.zip"""
                sh """sudo -S mkdir output_testcase""" 
                sh """sudo -S aws s3 cp s3://wrf-testcase/output/$BUILD_NUMBER/ output_testcase/ --region us-east-1 --recursive"""
                sh """sudo -S zip -r $WORKSPACE/wrf_output.zip output_testcase"""
             }        
        }
    }
}

//Check Instance Runngin Status for test cases
def checkinstancerunningStatus(stageName) {
    return {
        stage("${stageName}") {
            echo "Running stage : ${stageName}"
            script{
        
            while(Instanceflag()==true){
            def flag=Instanceflag()
            if(flag==true){
            print("Instances are still running")
            }else{
              print("Instances are stopped")
              ///
              print(flag)
              break
              }
             }
            }
        }
    }
}
    
def terraformStage(stageName){
    return {
        stage("${stageName}"){
            // Setting Build number for tagging with terraform
            echo "Running stage: ${stageName} and build number : ${BUILD_NUMBER}"
            echo "Appending ${BUILD_NUMBER} in vars.tf"
            echo "These are environment variables for branch and Github repo\n"
            sh """
                    sudo -S chmod 777 -R $WORKSPACE/$BUILD_NUMBER 
                    sudo -S mkdir -p $WORKSPACE/$BUILD_NUMBER/WRF 
                    echo "Cloning repo into:   $WORKSPACE/$BUILD_NUMBER/WRF "
                    sudo -S git clone -b release-v4.1.3 --single-branch https://github.com/hemuku90/WRF.git $WORKSPACE/$BUILD_NUMBER/WRF
                    sudo -S sed -i 's/default = "wrf-test"/default = "wrf-test-${BUILD_NUMBER}"/' $WORKSPACE/$BUILD_NUMBER/WRF/.ci/terraform/vars.tf
            """        
            for (int i=1;i<=10;i++){
            sh"""
            echo $i
            sudo -S sed -i "3i export GIT_URL=$repo_url\\nexport GIT_BRANCH=$fork_branchName" $WORKSPACE/$BUILD_NUMBER/WRF/.ci/terraform/wrf_testcase_"$i".sh
            sudo -S sed -i "12i cd /home/ubuntu/ && bash my_script.sh output_00"$i" $BUILD_NUMBER" $WORKSPACE/$BUILD_NUMBER/WRF/.ci/terraform/wrf_testcase_"$i".sh
            """
            }
            sh """
            cd $WORKSPACE/$BUILD_NUMBER/WRF/.ci/terraform && sudo terraform init && sudo terraform plan && sudo terraform apply -auto-approve
            """
        }
    }
}

/***
Func to check if instane with current tag is running or not
***/
def Instanceflag() {
    instanceId="""
    aws ec2 describe-instances --query 'Reservations[*].Instances[*].[InstanceId]' --filters Name=instance-state-name,Values=running  "Name=tag:Name,Values=wrf-test-$BUILD_NUMBER" --region us-east-1
    """
    instance=sh(script: instanceId, returnStdout: true)
    def running
    if(instance.size()<=3){
        running=false
    }else{
        running=true
    }
    return running
}

/***
    Kill current build for this JOB
***/
def killall_jobs() {
    def jobname = env.JOB_NAME
    def buildnum = env.BUILD_NUMBER.toInteger()
    echo "${buildnum}"
    echo "From kill all jobs"
    def rmi = """
    sudo -S rm -rf $WORKSPACE/$BUILD_NUMBER
    """
    rm=sh(script: rmi,returnStdout: true)
    echo "${jobname}"
    def job = Jenkins.instance.getItemByFullName(jobname)
    println("Kill task because commits have been found in .md and .txt files for buildNumber or either action is other than open/synchronise")
    println(job.builds.get(0))
    job.builds.get(0).doStop()
    
        
}

//Run any shell script with this function
def mysh(cmd) {
    return sh(script: cmd, returnStdout: true).trim()
}

// Func to return boolean true if in PR we have only .md/.txt files and False in case of anything else
def filterFiles(cmd){
    def list=[]
    list.add(sh(script: cmd, returnStdout: true).trim())
    println("List of changed file are:")
    println(list)
    def bool=list.every { it =~ /(?i)\.(?:md|txt)$/ }
    return bool 
}

pipeline {
    agent any
    stages {
    stage('Clean Workspace') {
      steps ("Cleaning workspace"){
        sh '''
        sudo -S rm -rf $WORKSPACE/$BUILD_NUMBER
        sudo -S rm -rf $WORKSPACE/wrf_output.zip
        '''
        }
    }
    stage('Setting Variables From Webhook Payload'){
        steps ("Setting variables"){
            sh '''
            sudo -S mkdir -p $WORKSPACE/$BUILD_NUMBER
            sudo -S mkdir -p $WORKSPACE/$BUILD_NUMBER/fork_repo_$BUILD_NUMBER
            sudo -S chmod 777 -R $WORKSPACE/$BUILD_NUMBER
            sudo -S echo $payload > $WORKSPACE/$BUILD_NUMBER/sample.json
            '''
        script {
            //Baseowner
            def sh18="""
            cd $WORKSPACE/$BUILD_NUMBER && cat sample.json | jq '.pull_request.base.user.login'
            """
            env.baseowner=mysh(sh18)
            //pull request number
            def sh17="""
            cd $WORKSPACE/$BUILD_NUMBER && cat sample.json | jq .number
            """
            env.pullnumber=mysh(sh17)
            //action variable
            def sh16="""
             cd $WORKSPACE/$BUILD_NUMBER && cat sample.json | jq .action
            """
            env.action=mysh(sh16)
            //SHA ID
            def sh14="""
            cd $WORKSPACE/$BUILD_NUMBER && cat sample.json | jq .pull_request.head.sha
            """
            env.sha=mysh(sh14)
        
        //Github status for current build
        sh """
           curl "https://api.GitHub.com/repos/davegill/WRF/statuses/$sha?access_token=$token" \
           -H "Content-Type: application/json" \
           -X POST \
           -d '{"state": "pending","context": "WRF-BUILD/jenkins", "description": "WRF test build running", "target_url": "http://scala-jenkins-1810560854.us-east-1.elb.amazonaws.com/job/wrf_test_case/$BUILD_NUMBER/console"}'
        """
            
            def sh1="""
            cd $WORKSPACE/$BUILD_NUMBER && cat sample.json | jq .pull_request.id
            """
            pr_id=mysh(sh1)
            println(pr_id)
            
            def sh2="""
            cd $WORKSPACE/$BUILD_NUMBER && cat sample.json | jq .pull_request.head.repo.name
            """
            repo_name=mysh(sh2)
            println(repo_name)
            
            def sh3="""
            cd $WORKSPACE/$BUILD_NUMBER && cat sample.json | jq .pull_request.head.ref
            """
            fork_branchName=mysh(sh3)
            println(fork_branchName)
            
            def sh4="""
            cd $WORKSPACE/$BUILD_NUMBER && cat sample.json | jq .pull_request.head.user.html_url
            """
            fork_url=mysh(sh4)
            println(fork_url)
            
            def sh5="""
            cd $WORKSPACE/$BUILD_NUMBER && cat sample.json | jq .pull_request.base.ref
            """
            base_branchName=mysh(sh5)
            println(base_branchName)
            
            def sh6="""
            cd $WORKSPACE/$BUILD_NUMBER && cat sample.json | jq .pull_request.base.user.html_url
            """
            base_url=mysh(sh6)
            println(base_url)
            
            def sh7="""
            cd $WORKSPACE/$BUILD_NUMBER && cat sample.json | jq .pull_request.head.repo.clone_url
            """
            env.repo_url=mysh(sh7)
            println(repo_url)
            def sh8="""
             git ls-remote $repo_url HEAD
            """
            env.commitID=mysh(sh8)
            //Github userName
            def sh11="""
            cd $WORKSPACE/$BUILD_NUMBER && cat sample.json | jq '.pull_request.user.login'
            """
            env.githubuserName=mysh(sh11)  // Github UserName
            
            //Email ID of user submitting the pull request
            def sh12= """
            curl -s https://api.github.com/users/$githubuserName/events/public| jq ".[].payload.commits[0].author"|grep "email"|awk '{print \$2}'|head -1|cut -d',' -f1 | tr -d '"'

            """
            env.eMailID=mysh(sh12)
            println("Commit ID is")
            println(commitID)
            println("Github User Name")
            println(githubuserName)
            println("Email id of the user is")
            println(eMailID.toString())
        }
       }
       }
    stage('Checking commits to .md/.txt Files and Running/Failing the test cases based on ation is open/synchronise'){
        steps('Filtering .md/.txt files from the commits'){
            script{
            echo "$BUILD_NUMBER"
            echo "fork_repo_$BUILD_NUMBER"
            echo "Pull number is: $pullnumber"
            def sh9="""
            curl -s https://patch-diff.githubusercontent.com/raw/davegill/WRF/pull/${pullnumber}.diff| grep "diff"| awk '{print \$3}'| cut -d '/' -f2-
            """
            println("Executing curl script for file tests")
            bool=filterFiles(sh9)
            println(action)
            /*
            Check for files with .md/.txt extension in commits
            */
            if(bool ==true){
            println("Entering if condition")
            killall_jobs()
            def sh10="""
            echo "Cleaning : $WORKSPACE/$BUILD_NUMBER
            sudo -S rm -rf $WORKSPACE/$BUILD_NUMBER
            curl "https://api.GitHub.com/repos/davegill/WRF/statuses/$sha?access_token=$token" \
            -H "Content-Type: application/json" \
            -X POST \
            -d '{"state": "error","context": "WRF-BUILD/jenkins", "description": "WRF test build aborted", "target_url": "http://scala-jenkins-1810560854.us-east-1.elb.amazonaws.com/job/wrf_test_case/$BUILD_NUMBER/console"}'
            """
            mysh(sh10)
            }
            /*
            Check for action is open/sycnhronise and continue the build job
            */
            else if(action == '"opened"' || action == '"synchronize"'){
                println("Proceeding to another stage because commits have not been found in .md/.txt files and action is open/sycnhronize")
                //Running terraform deployment
                println("Deploying terraform:")
                terraformStage("Running Terraform").call()
                println("Terraform deployment finished. Now checking the status of test cases running/finished:")
                
                //check test cases running status 
                checkinstancerunningStatus("Check Test cases running status").call()
                println("Test Cases finished running. Now downloading the output of test cases from S3 on to Jenkins server")
                
                //Downloads output from S3 to Jenkins server
                downloadOutput("Download output of the current Test build").call()
                println("Test cases downloaded successfully. Now sending e-mail to the stakeholders. Now ready to send e-mail notification")
                }           
            /*
            Kill the job if neither of the above conditions are true 
            */
            else{
                println("Entering else condition because neither commits have been found in .md/.txt files and action is not equal to open/synchronise")
                killall_jobs()
                def sh10="""
                echo "Cleaning : $WORKSPACE/$BUILD_NUMBER
                sudo -S rm -rf $WORKSPACE/$BUILD_NUMBER
                curl "https://api.GitHub.com/repos/davegill/WRF/statuses/$sha?access_token=$token" \
                -H "Content-Type: application/json" \
                -X POST \
                -d '{"state": "error","context": "WRF-BUILD/jenkins", "description": "WRF test build aborted", "target_url": "http://scala-jenkins-1810560854.us-east-1.elb.amazonaws.com/job/wrf_test_case/$BUILD_NUMBER/console"}'
                """
                mysh(sh10)
                 }
                
                }    
            }
        }
    }

    post {
    success {
        echo "Job is successfull. Now sending e-mail notification and cleaning workspace"
        sh """
        curl "https://api.GitHub.com/repos/davegill/WRF/statuses/$sha?access_token=$token" \
        -H "Content-Type: application/json" \
        -X POST \
        -d '{"state": "success","context": "WRF-BUILD/jenkins", "description": "WRF test build is successfull", "target_url": "http://scala-jenkins-1810560854.us-east-1.elb.amazonaws.com/job/wrf_test_case/$BUILD_NUMBER/console"}'
        """
        emailext attachmentsPattern: 'wrf_output.zip', 
        body: 'WRF build test ran successfully. Please find the attachment of the output for WRF BUILD '+ env.BUILD_NUMBER, 
        subject: currentBuild.currentResult + " : " + env.JOB_NAME, 
        to: """hkumar@scalacomputing.com,${eMailID}"""
       
        sh '''
        echo "Cleaning workspace"
        sudo -S rm -rf $WORKSPACE/$BUILD_NUMBER
        sudo -S rm -rf $WORKSPACE/wrf_output.zip
        '''
        }
    failure{
         echo "Job failed. Now sending e-mail notification and cleaning workspace"
         
        sh """
        curl "https://api.GitHub.com/repos/davegill/WRF/statuses/$sha?access_token=$token" \
        -H "Content-Type: application/json" \
        -X POST \
        -d '{"state": "failure","context": "WRF-BUILD/jenkins", "description": "WRF test build failed", "target_url": "http://scala-jenkins-1810560854.us-east-1.elb.amazonaws.com/job/wrf_test_case/$BUILD_NUMBER/console"}'
        """
        emailext body: 'WRF Test Build failed in Jenkins: $PROJECT_NAME - #$BUILD_NUMBER',
        subject: currentBuild.currentResult + " : " + env.JOB_NAME, 
        to: """hkumar@scalacomputing.com,${eMailID}"""
        
        sh '''
        echo "Cleaning workspace"
        sudo -S rm -rf $WORKSPACE/$BUILD_NUMBER
        sudo -S rm -rf $WORKSPACE/wrf_output.zip
        '''
            }
        }
    }

    
