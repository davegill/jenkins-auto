#!/bin/bash
su - ubuntu << 'EOF'
wget https://wrf-testcase-staging.s3.amazonaws.com/upload_script.sh
mkdir /home/ubuntu/wrf-stuff
cd wrf-stuff/
git clone --branch varun_test https://github.com/davegill/wrf-coop.git
cd wrf-coop/
sed -e "s^_GIT_URL_^$GIT_URL^" -e "s^_GIT_BRANCH_^$GIT_BRANCH^" Dockerfile-sed > Dockerfile
sed -e "s^_GIT_URL_^$GIT_URL^" -e "s^_GIT_BRANCH_^$GIT_BRANCH^" Dockerfile-sed-NMM > Dockerfile-NMM
csh build.csh /home/ubuntu/wrf-stuff/wrf-coop /home/ubuntu/wrf-stuff/wrf-coop

echo "==============================================================" >  MPI
echo "==============================================================" >> MPI
echo "                         MPI START" >> MPI
echo "==============================================================" >> MPI

date ; ./single_init.csh Dockerfile     wrf_regtest    > output_38 ; date 
./test_008m.csh > outm & 
wait 
./single_end.csh wrf_regtest    >> output_38 ; date 
cat MPI outm >> output_38

rm outm 
rm SERIAL OPENMP MPI 
EOF
