#!/bin/csh

#	This script makes the wrf_testcase_xx.sh files.

set TEST_NUM = (  1   2   3   4   5   6   7   8   9  10 )
set BUILDS   = ( som sm  sm  som som som som  m  som  s )

set COUNT = 1
foreach f ( $TEST_NUM )

	set name = wrf_testcase_${f}.sh
	if ( -e $name ) then
		rm -f $name
	endif
	touch $name
		
	echo '#\!/bin/bash' >> $name
	echo "su - ubuntu << 'EOF'" >> $name
	echo "mkdir /home/ubuntu/wrf-stuff" >> $name
	echo "cd wrf-stuff/" >> $name
	echo "git clone git@github.com:davegill/wrf-coop.git" >> $name
	echo "cd wrf-coop/" >> $name
	echo 'sed -e "s^_GIT_URL_^$GIT_URL^" -e "s^_GIT_BRANCH_^$GIT_BRANCH^" Dockerfile-sed > Dockerfile' >> $name
	echo "csh build.csh /home/ubuntu/wrf-stuff/ /home/ubuntu/wrf-stuff/" >> $name

	if      ( $f <   10 ) then
		set NUM = 00$f
	else if ( $f <  100 ) then
		set NUM =  0$f
	else if ( $f < 1000 ) then
		set NUM =   $f
	endif
	set BASE = test_$NUM
	set NAMES = ${BASE}s.csh
	set NAMEO = ${BASE}o.csh
	set NAMEM = ${BASE}m.csh
	if      ( $BUILDS[$COUNT] == som ) then
		echo "date ; ( ./single.csh ; ./$NAMES & ./$NAMEO & ./$NAMEM & wait ) >output_$NUM ; date " >> $name
		echo "date ; ./last_only_once.csh >> output_$NUM ; date" >> $name
	else if ( $BUILDS[$COUNT] == sm ) then
		echo "date ; ( ./single.csh ; ./$NAMES & ./$NAMEM & wait ) >output_$NUM ; date " >> $name
		echo "date ; ./last_only_once.csh >> output_$NUM ; date" >> $name
	else if ( $BUILDS[$COUNT] == s ) then
		echo "date ; ( ./single.csh ; ./$NAMES & wait ) >output_$NUM ; date " >> $name
	else if ( $BUILDS[$COUNT] == m ) then
		echo "date ; ( ./single.csh ; ./$NAMEM & wait ) >output_$NUM ; date " >> $name
	endif

	echo "EOF" >> $name
	@ COUNT ++
end
