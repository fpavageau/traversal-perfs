#!/bin/bash
#
# Copyright 2016 Frank Pavageau
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

export LC_ALL=C

declare warm_up=yes
declare clear_cache=no
declare query
declare iterations=100
declare one_line=no

while [[ $# -gt 0 ]]; do
    case $1 in
        "--help")
            printf "$0 [--help] [--no-warm-up] [--clear-cache] [--cache [cache1[,cache2[,...]]]] [-n iterations]\n"
            printf "\t--cache        enable the named caches (label, property)\n"
            printf "\t--clear-cache  clear the caches before each traversal\n"
            printf "\t--help         this message\n"
            printf "\t-n             set the number of iterations (default: 100)\n"
            printf "\t--no-warm-up   do not warm up before measuring\n"
            printf "\t--one-line     print the results on a single parseable line\n"
            exit 1
            ;;
        "--no-warm-up")
            warm_up=no
            ;;
        "--clear-cache")
            clear_cache=yes
            ;;
        "--depth-first")
            query+=${query:+&}depthFirst=
            ;;
        "--cache")
            shift
            query+=${query:+&}cache=$1
            ;;
        "-n")
            shift
            iterations=$1
            ;;
        "--one-line")
            one_line=yes
            ;;
    esac
    shift
done

query=${query:+?}$query

if [ $warm_up == "yes" ]; then
    [ $one_line == "no" ] && printf "Warming up"
    for i in $(seq 1 $iterations); do
        [ $one_line == "no" ] && printf "."
        if [ $clear_cache == "yes" ]; then
            curl -sS -o /dev/null localhost:7474/traversal-perfs/cache/clear
        fi
        curl -sS -o /dev/null localhost:7474/traversal-perfs/traverse$query
    done
    [ $one_line == "no" ] && printf "\n"
fi

[ $one_line == "no" ] && echo "Measuring"
for i in $(seq 1 $iterations); do
    if [ $clear_cache == "yes" ]; then
        curl -sS -o /dev/null localhost:7474/traversal-perfs/cache/clear
    fi
    curl -w '%{time_total}\n' -sS -o /dev/null localhost:7474/traversal-perfs/traverse$query
done |
    sort -n |
    awk -v one_line=$one_line -f quantiles.awk
