#!/bin/bash
su - ubuntu << 'EOF'
wget https://wrf-testcase.s3.amazonaws.com/my_script.sh
mkdir /home/ubuntu/wrf-stuff
cd wrf-stuff/
git clone git@github.com:davegill/wrf-coop.git
cd wrf-coop/
sed -e "s^_GIT_URL_^$GIT_URL^" -e "s/_GIT_BRANCH_/$GIT_BRANCH/" Dockerfile-sed > Dockerfile
sed -e "s^_GIT_URL_^$GIT_URL^" -e "s/_GIT_BRANCH_/$GIT_BRANCH/" Dockerfile-sed-NMM > Dockerfile-NMM
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

date ; ./single.csh Dockerfile     > output_18 ; date 
./test_018s.csh > outs & 
./test_018o.csh > outo & 
./test_018m.csh > outm & 
wait 
cat SERIAL outs OPENMP outo MPI outm >> output_18
date ; ./last_only_once.csh >> output_18 ; date
rm outs outo outm 
rm SERIAL OPENMP MPI 
EOF
