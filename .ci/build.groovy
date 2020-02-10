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
                """
                sh """ 
                sudo -S mkdir output_testcase 
                sudo -S aws s3 cp s3://wrf-testcase/output/$BUILD_NUMBER/ output_testcase/ --region us-east-1 --recursive
                sudo -S zip -r $WORKSPACE/$BUILD_NUMBER/wrf_output.zip output_testcase
                """
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
                    sudo -S git clone https://github.com/davegill/jenkins-auto.git $WORKSPACE/$BUILD_NUMBER/WRF
                    sudo -S sed -i 's/default = "wrf-test"/default = "wrf-test-${BUILD_NUMBER}"/' $WORKSPACE/$BUILD_NUMBER/WRF/.ci/terraform/vars.tf
            """        
            for (int j=1;j<=10;j++){
            sh"""
            sudo -S sed -i "3i export GIT_URL=$repo_url\\nexport GIT_BRANCH=$fork_branchName" $WORKSPACE/$BUILD_NUMBER/WRF/.ci/terraform/wrf_testcase_"$j".sh
            sudo -S sed -i '\$i cd /home/ubuntu/ && bash my_script.sh output_$j $BUILD_NUMBER' $WORKSPACE/$BUILD_NUMBER/WRF/.ci/terraform/wrf_testcase_"$j".sh
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
    sudo -S aws ec2 describe-instances --query 'Reservations[*].Instances[*].[InstanceId]' --filters Name=instance-state-name,Values=running  "Name=tag:Name,Values=wrf-test-$BUILD_NUMBER" --region us-east-1
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
    echo "${jobname}"
    def rmi = """
    sudo -S mkdir -p $WORKSPACE/$BUILD_NUMBER/WRF
    echo "Cloning repo into:   $WORKSPACE/$BUILD_NUMBER/WRF "
    sudo -S git clone https://github.com/davegill/jenkins-auto.git $WORKSPACE/$BUILD_NUMBER/WRF   
    """
    rm=sh(script: rmi,returnStdout: true)
    def job = Jenkins.instance.getItemByFullName(jobname)
    println("Kill task because commits have been found in .md and .txt files for buildNumber or either action is other than open/synchronise")
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
           curl -s "https://api.GitHub.com/repos/wrf-model/WRF/statuses/$sha?access_token=2194a7c3c5fefe2b291fa87e6b489846641b0d7b" \
           -H "Content-Type: application/json" \
           -X POST \
           -d '{"state": "pending","context": "WRF-BUILD-$BUILD_NUMBER", "description": "WRF regression test running", "target_url": "https://ncar_jenkins.scalacomputing.com/job/WRF-MODEL-TEST/$BUILD_NUMBER/console"}'
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
            //Github userName
            def sh11="""
            cd $WORKSPACE/$BUILD_NUMBER && cat sample.json | jq '.pull_request.user.login'
            """
            env.githubuserName=mysh(sh11)  // Github UserName
            //Cloning the forked repository
            
            sh """
            sudo -s mkdir -p $WORKSPACE/$BUILD_NUMBER/forked_repo
            sudo -s git clone -b $fork_branchName --single-branch $repo_url $WORKSPACE/$BUILD_NUMBER/forked_repo
            """
            def sh8="""
             cd $WORKSPACE/$BUILD_NUMBER/forked_repo && git rev-parse HEAD
            """
            env.commitID=mysh(sh8)
            //Email ID of user submitting the pull request
            def sh12= """
            cd $WORKSPACE/$BUILD_NUMBER/forked_repo && git --no-pager show -s --format='%ae' $commitID
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
            curl -s https://patch-diff.githubusercontent.com/raw/wrf-model/WRF/pull/${pullnumber}.patch| grep -i "SUBJECT"|tail -n 1
            """
            env.prComment=mysh(sh9)
            println("Checking for list of file changes in this commit")
            def sh13="""
            cd $WORKSPACE/$BUILD_NUMBER/forked_repo 
            git diff-tree --no-commit-id --name-only -r $commitID
            """
            bool=filterFiles(sh13)
            /*
            Check for files with .md/.txt extension in a pull request. 
            It returns true if every file is .md/.txt else it returns false.
            */
            if(bool ==true){
            println("Entering if condition")
            killall_jobs()
            currentBuild.result = 'ABORTED'
            
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
                currentBuild.result = 'ABORTED'
                error('Stopping early…')
                 }
                
                }    
            }
        }
    }

    post {
    success {
        script{
        /*
        Setting some more variables for test results
        */
        env.E=mysh("""cd $WORKSPACE/$BUILD_NUMBER/output_testcase && ls -1 | grep output_ | wc -l""")
        env.F=mysh("""cd $WORKSPACE/$BUILD_NUMBER/output_testcase && grep -a " START" output_* | grep -av "CLEAN START" | grep -av "SIMULATION START" | grep -av "LOG START" | wc -l""")
        env.G=mysh("""cd $WORKSPACE/$BUILD_NUMBER/output_testcase && grep -a " = STATUS" output_* | wc -l""")
        env.H=mysh("""cd $WORKSPACE/$BUILD_NUMBER/output_testcase && grep -a "status = " output_* | wc -l""")
        env.I=mysh("""cd $WORKSPACE/$BUILD_NUMBER/output_testcase && grep -a " = STATUS" output_* | grep -av "0 = STATUS" | wc -l""")
        env.J=mysh("""cd $WORKSPACE/$BUILD_NUMBER/output_testcase && grep -a "status = " output_* | grep -av "status = 0" | wc -l """)
        
        if ("""$eMailID"""){    
        sh """
        echo "Job is successfull. Now sending e-mail notification and cleaning workspace"
        curl -s "https://api.GitHub.com/repos/wrf-model/WRF/statuses/$sha?access_token=2194a7c3c5fefe2b291fa87e6b489846641b0d7b" \
        -H "Content-Type: application/json" \
        -X POST \
        -d '{"state": "success","context": "WRF-BUILD-$BUILD_NUMBER", "description": "WRF regression test is successfull", "target_url": "https://ncar_jenkins.scalacomputing.com/job/WRF-MODEL-TEST/$BUILD_NUMBER/console"}'
        echo "#############Job is Successfull############"
        echo "##############Sending E-Mail###############"
        echo "Recipient is:$eMailID"
        cd $WORKSPACE/$BUILD_NUMBER && sudo -S unzip $WORKSPACE/$BUILD_NUMBER/wrf_output.zip
        sudo -S python $WORKSPACE/$BUILD_NUMBER/WRF/mail.py $WORKSPACE/$BUILD_NUMBER/wrf_output.zip SUCCESS $JOB_NAME $BUILD_NUMBER  $eMailID $commitID $githubuserName $pullnumber $WORKSPACE/$BUILD_NUMBER/output_testcase/email_01.txt "$prComment" $E $F $G $H $I $J
        echo "Cleaning workspace"
        sudo -S rm -rf $WORKSPACE/$BUILD_NUMBER
        """
        }
        else{
        sh """
        echo "Job is successfull. Now sending e-mail notification and cleaning workspace"
        curl -s "https://api.GitHub.com/repos/wrf-model/WRF/statuses/$sha?access_token=2194a7c3c5fefe2b291fa87e6b489846641b0d7b" \
        -H "Content-Type: application/json" \
        -X POST \
        -d '{"state": "success","context": "WRF-BUILD-$BUILD_NUMBER", "description": "WRF regression test is successfull", "target_url": "https://ncar_jenkins.scalacomputing.com/job/WRF-MODEL-TEST/$BUILD_NUMBER/console"}'
        echo "#############Job is Successfull############"
        echo "##############Sending E-Mail###############"
        echo "Recipient is: gill@ucar.eduu"
        cd $WORKSPACE/$BUILD_NUMBER && sudo -S unzip $WORKSPACE/$BUILD_NUMBER/wrf_output.zip
        sudo -S python $WORKSPACE/$BUILD_NUMBER/WRF/mail.py $WORKSPACE/$BUILD_NUMBER/wrf_output.zip SUCCESS $JOB_NAME $BUILD_NUMBER gill@ucar.edu $commitID $githubuserName $pullnumber $WORKSPACE/$BUILD_NUMBER/output_testcase/email_01.txt "$prComment" $E $F $G $H $I $J
        echo "Cleaning workspace"
        sudo -S rm -rf $WORKSPACE/$BUILD_NUMBER
        """    
        }
        }
        }
    failure{
        echo "Job failed. Now sending e-mail notification and cleaning workspace"
         
        sh """
        curl -s "https://api.GitHub.com/repos/wrf-model/WRF/statuses/$sha?access_token=2194a7c3c5fefe2b291fa87e6b489846641b0d7b" \
        -H "Content-Type: application/json" \
        -X POST \
        -d '{"state": "failure","context": "WRF-BUILD-$BUILD_NUMBER", "description": "WRF regression test failed", "target_url": "https://ncar_jenkins.scalacomputing.com/job/WRF-MODEL-TEST/$BUILD_NUMBER/console"}'
        echo "#############Job Failed############
        echo "Cleaning workspace"
        sudo -S rm -rf $WORKSPACE/$BUILD_NUMBER
        """
            }
    aborted{
        echo "Job Aborted. Now sending e-mail notification and cleaning workspace"
         
        sh """
        curl -s "https://api.GitHub.com/repos/wrf-model/WRF/statuses/$sha?access_token=2194a7c3c5fefe2b291fa87e6b489846641b0d7b" \
        -H "Content-Type: application/json" \
        -X POST \
        -d '{"state": "success","context": "WRF-BUILD-$BUILD_NUMBER", "description": "WRF regression test is successfull", "target_url": "https://ncar_jenkins.scalacomputing.com/job/WRF-MODEL-TEST/$BUILD_NUMBER/console"}'
        echo "#############Job Aborted############"
        echo "Cleaning workspace"
        sudo -S rm -rf $WORKSPACE/$BUILD_NUMBER
        """
                }   
            }
    }