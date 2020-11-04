#!/bin/bash
su - ubuntu << 'EOF'
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 858085030508.dkr.ecr.us-east-1.amazonaws.com
wget https://wrf-testcase-staging.s3.amazonaws.com/my_script.sh
mkdir /home/ubuntu/wrf-stuff
cd wrf-stuff/
git clone git@github.com:davegill/wrf-coop.git
cd wrf-coop/
sed -e "s^_GIT_URL_^$GIT_URL^" -e "s^_GIT_BRANCH_^$GIT_BRANCH^" Dockerfile-sed > Dockerfile
sed -e "s^_GIT_URL_^$GIT_URL^" -e "s^_GIT_BRANCH_^$GIT_BRANCH^" Dockerfile-sed-NMM > Dockerfile-NMM
csh build.csh /home/ubuntu/wrf-stuff/wrf-coop /home/ubuntu/wrf-stuff/wrf-coop
echo "==============================================================" >  SERIAL
echo "==============================================================" >> SERIAL
echo "                         SERIAL START" >> SERIAL
echo "==============================================================" >> SERIAL

echo "==============================================================" >  OPENMP
echo "==============================================================" >> OPENMP
echo "                         OPENMP START" >> OPENMP
echo "==============================================================" >> OPENMP

echo "==============================================================" >  MPI
echo "==============================================================" >> MPI
echo "                         MPI START" >> MPI
echo "==============================================================" >> MPI

date ; ./single_init.csh Dockerfile     wrf_regtest    > output_7 ; date 
./test_007s.csh > outs & 
./test_007o.csh > outo & 
./test_007m.csh > outm & 
wait 
./single_end.csh wrf_regtest    >> output_7 ; date 
cat SERIAL outs OPENMP outo MPI outm >> output_7
date ; ./last_only_once.csh >> output_7 ; date
rm outs outo outm 
rm SERIAL OPENMP MPI 
EOF
