#!/bin/bash

BUILD=$1
BRANCH=$(git rev-parse --abbrev-ref HEAD)

sudo rm -rf ${HOME}/wrf-test-output/*

sudo mkdir -p ${HOME}/wrf-test-output/${BUILD}

if [[ ${BRANCH} == "master" ]];
then
    bucket-name="wrf-testcase"
    sudo -S aws s3 cp s3://${bucket-name}/raw_output/${BUILD}/ ${HOME}/wrf-test-output/${BUILD} --region us-east-1 --recursive
elif [[ ${BRANCH} == "staging" ]];
then
    bucket-name=wrf-testcase-staging
    sudo -S aws s3 cp s3://${bucket-name}/raw_output/${BUILD}/ ${HOME}/wrf-test-output/${BUILD} --region us-east-1 --recursive
fi