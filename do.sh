#!/bin/bash
#
# Copyright 2016 Red Hat, Inc. and/or its affiliates
# and other contributors as indicated by the @author tags.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


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
