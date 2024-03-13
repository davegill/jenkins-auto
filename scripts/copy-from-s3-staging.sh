#!/bin/bash

sudo rm -rf /home/centos/wrf-test-output/*

sudo mkdir -p /home/centos/wrf-test-output

sudo -S aws s3 cp s3://wrf-testcase-staging/raw_output/$1/ /home/centos/wrf-test-output/ --region us-east-1 --recursive