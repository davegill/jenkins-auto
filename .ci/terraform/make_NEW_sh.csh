#!/bin/csh

#	This script makes the wrf_testcase_xx.sh files.

#               chm rst  rl  rl  rl  rl  rl  qss bw  rl8 qs8 mov fir hil rl  rl  rl  rl  rl  rl  rl  rl  kpp 
set OLD_NUM  = (  1   2  11  13  21  22   3   4   5   6   7   8   9  10  12  14  15  16  17  18  19  20  23 )
set TEST_NUM = (  1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18  19  20  21  22  23 )
set BUILDS   = ( sm   m  som som som som som som som som som  m  som s   som som som som som sm  som som sm )

set ALL   = 0
foreach COUNT ( $TEST_NUM )
	
	foreach p ( s o m )

		set OK_s = 1
		set OK_o = 1
		set OK_m = 1

		if      ( $p == s ) then
			echo $BUILDS[$COUNT] | grep -q s
			set OK_s = $status
			if ( $OK_s == 0 ) then
				@ ALL ++
			else
				goto NOT_THIS_ONE
			endif
		else if ($p == o ) then
			echo $BUILDS[$COUNT] | grep -q o
			set OK_o = $status
			if ( $OK_o == 0 ) then
				@ ALL ++
			else
				goto NOT_THIS_ONE
			endif
		else if ( $p == m ) then
			echo $BUILDS[$COUNT] | grep -q m
			set OK_m = $status
			if ( $OK_m == 0 ) then
				@ ALL ++
			else
				goto NOT_THIS_ONE
			endif
		endif
	
		set name = wrf_testcase_${ALL}.sh
		if ( -e $name ) then
			rm -f $name
		endif
		touch $name
			
		echo '#\!/bin/bash' >> $name
		echo "su - ubuntu << 'EOF'" >> $name
		echo "wget https://wrf-testcase-staging.s3.amazonaws.com/upload_script.sh" >> $name
		echo "mkdir /home/ubuntu/wrf-stuff" >> $name
		echo "cd wrf-stuff/" >> $name
		echo "git clone --branch regression+feature https://github.com/davegill/wrf-coop.git" >> $name
		echo "cd wrf-coop/" >> $name
		echo 'sed -e "s^_GIT_URL_^$GIT_URL^" -e "s^_GIT_BRANCH_^$GIT_BRANCH^" Dockerfile-sed > Dockerfile' >> $name
		echo "csh build.csh /home/ubuntu/wrf-stuff/wrf-coop /home/ubuntu/wrf-stuff/wrf-coop" >> $name
	
		if ( $OK_s == 0 ) then
			echo 'echo "==============================================================" >  SERIAL' >> $name
			echo 'echo "==============================================================" >> SERIAL' >> $name
			echo 'echo "                         SERIAL START" >> SERIAL' >> $name
			echo 'echo "==============================================================" >> SERIAL' >> $name
			echo "" >> $name
		endif
	
		if ( $OK_o == 0 ) then
			echo 'echo "==============================================================" >  OPENMP' >> $name
			echo 'echo "==============================================================" >> OPENMP' >> $name
			echo 'echo "                         OPENMP START" >> OPENMP' >> $name
			echo 'echo "==============================================================" >> OPENMP' >> $name
			echo "" >> $name
		endif
	
		if ( $OK_m == 0 ) then
			echo 'echo "==============================================================" >  MPI' >> $name
			echo 'echo "==============================================================" >> MPI' >> $name
			echo 'echo "                         MPI START" >> MPI' >> $name
			echo 'echo "==============================================================" >> MPI' >> $name
			echo "" >> $name
		endif
	
		if      ( $ALL <   10 ) then
			set NUM = 00$ALL
		else if ( $ALL <  100 ) then
			set NUM =  0$ALL
		else if ( $ALL < 1000 ) then
			set NUM =   $ALL
		endif
	
		if      ( $COUNT <   10 ) then
			set BASE = test_00$COUNT
		else if ( $COUNT <  100 ) then
			set BASE = test_0$COUNT
		else if ( $COUNT < 1000 ) then
			set BASE = test_$COUNT
		endif
		set NAMES = ${BASE}s.csh
		set NAMEO = ${BASE}o.csh
		set NAMEM = ${BASE}m.csh
		echo "date ; ./single_init.csh Dockerfile     wrf_regtest    > output_$ALL ; date " >> $name
		if ( $OK_s == 0 ) then
			echo "./$NAMES > outs " >> $name
			echo "./single_end.csh wrf_regtest    >> output_$ALL ; date " >> $name
			echo "cat SERIAL outs >> output_$ALL" >> $name
			echo "rm outs SERIAL " >> $name
		endif
		if ( $OK_o == 0 ) then
			echo "./$NAMEO > outo " >> $name
			echo "./single_end.csh wrf_regtest    >> output_$ALL ; date " >> $name
			echo "cat OPENMP outo >> output_$ALL" >> $name
			echo "rm outo OPENMP " >> $name
		endif
		if ( $OK_m == 0 ) then
			echo "./$NAMEM > outm " >> $name
			echo "./single_end.csh wrf_regtest    >> output_$ALL ; date " >> $name
			echo "cat MPI outm >> output_$ALL" >> $name
			echo "rm outm MPI " >> $name
		endif
		echo "date " >> $name
	
		echo "EOF" >> $name
		chmod +x $name

NOT_THIS_ONE:

	end

end
