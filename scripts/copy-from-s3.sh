#!/bin/bash

BUILD=$1

sudo rm -rf /home/centos/wrf-test-output/*

sudo mkdir -p /home/centos/wrf-test-output/${BUILD}

sudo -S aws s3 cp s3://wrf-testcase/raw_output/${BUILD}/ /home/centos/wrf-test-output/${BUILD} --region us-east-1 --recursive