#!/bin/bash
su - ubuntu << 'EOF'
wget https://wrf-testcase.s3.amazonaws.com/my_script.sh
mkdir /home/ubuntu/wrf-stuff
cd wrf-stuff/
git clone --branch regression+feature https://github.com/davegill/wrf-coop.git
cd wrf-coop/
sed -e "s^_GIT_URL_^$GIT_URL^" -e "s^_GIT_BRANCH_^$GIT_BRANCH^" Dockerfile-sed > Dockerfile
csh build.csh /home/ubuntu/wrf-stuff/wrf-coop /home/ubuntu/wrf-stuff/wrf-coop
echo "==============================================================" >  OPENMP
echo "==============================================================" >> OPENMP
echo "                         OPENMP START" >> OPENMP
echo "==============================================================" >> OPENMP

date ; ./single_init.csh Dockerfile     wrf_regtest    > output_5 ; date 
./test_003o.csh > outo 
./single_end.csh wrf_regtest    >> output_5 ; date 
cat OPENMP outo >> output_5
rm outo OPENMP 
date 
EOF
