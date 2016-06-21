#!/bin/bash

BUILD=package
RUN=run

if [ $# -gt 0 ]; then
    ARGS=("$@")
    for (( i = 0; i < ${#ARGS[@]}; ++i)); do
        if [ ${ARGS[$i]} = "build" ]; then
            unset RUN
        elif [ ${ARGS[$i]} = "run" ]; then
            unset BUILD
        fi
    done
fi

if [[ -n $BUILD ]]; then
    mvn clean install
fi

if [[ -n $RUN ]]; then
    java "-Dmaven.home=/home/olubyans/maven" -jar ./tool/target/pm-tool.jar
fi
