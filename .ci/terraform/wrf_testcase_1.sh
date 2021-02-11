#!/bin/bash
su - ubuntu << 'EOF'
wget https://wrf-testcase-staging.s3.amazonaws.com/my_script.sh
mkdir /home/ubuntu/wrf-stuff
cd wrf-stuff/
git clone --branch leave_docker_images https://github.com/scala-computing/wrf-coop.git
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

date ; ./single_init.csh Dockerfile     wrf_regtest    > output_1 ; date 
./test_001s.csh > outs & 
./test_001o.csh > outo & 
./test_001m.csh > outm & 
wait 
./single_end.csh wrf_regtest    >> output_1 ; date 
cat SERIAL outs OPENMP outo MPI outm >> output_1
date ; ./last_only_once.csh >> output_1 ; date
rm outs outo outm 
rm SERIAL OPENMP MPI 
EOF
