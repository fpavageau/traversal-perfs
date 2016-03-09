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

printf "Warming up"
for i in $(seq 1 100); do
    printf "."
    curl -sS -o /dev/null localhost:7474/traversal-perfs/cache/clear
    curl -sS -o /dev/null 'localhost:7474/traversal-perfs/traverse?depthFirst='
done
echo

echo "Measuring"
for i in $(seq 1 100); do
    curl -sS -o /dev/null localhost:7474/traversal-perfs/cache/clear
    curl -w '%{time_total}\n' -sS -o /dev/null 'localhost:7474/traversal-perfs/traverse?depthFirst='
done |
    sort -n |
    awk -f quantiles.awk
