#!/bin/bash
su - ubuntu << 'EOF'
wget https://wrf-testcase-staging.s3.amazonaws.com/upload_script.sh
mkdir /home/ubuntu/wrf-stuff
cd wrf-stuff/
git clone --branch leave_docker_images https://github.com/vlakshmanan-scala/wrf-coop.git
cd wrf-coop/
echo $GIT_URL
echo The git url is $GIT_URL
sed -e "s^_GIT_URL_^$GIT_URL^" -e "s^_GIT_BRANCH_^$GIT_BRANCH^" Dockerfile-sed > Dockerfile
sed -e "s^_GIT_URL_^$GIT_URL^" -e "s^_GIT_BRANCH_^$GIT_BRANCH^" Dockerfile-sed-NMM > Dockerfile-NMM
csh build.csh /home/ubuntu/wrf-stuff/wrf-coop /home/ubuntu/wrf-stuff/wrf-coop
echo "==============================================================" >  SERIAL
echo "==============================================================" >> SERIAL
echo "                         SERIAL START" >> SERIAL
echo "==============================================================" >> SERIAL
date ; ./single_init.csh Dockerfile     wrf_regtest    > output_20 ; date 
./test_001s.csh > outs &

wait 
./single_end.csh wrf_regtest    >> output_20 ; date 
cat SERIAL outs  >> output_20

rm outs 
rm SERIAL OPENMP MPI 
EOF
