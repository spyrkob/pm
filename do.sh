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
    mvn clean package
fi

if [[ -n $RUN ]]; then
    java -jar ./pm-tool/target/pm-tool.jar
fi
