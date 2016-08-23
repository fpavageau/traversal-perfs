#!/usr/bin/awk
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

{
  sum += $1
  vals[NR] = $1
}

END {
  mean = (sum / NR)
  q50 = vals[int(NR * .5)]
  q90 = vals[int(NR * .9)]
  q95 = vals[int(NR * .95)]
  max = vals[NR]
  if (one_line == "yes") {
    print mean "\t" q50 "\t" q90 "\t" q95 "\t" max
  } else {
    print "Mean\t" mean
    print "50%\t" q50
    print "90%\t" q90
    print "95%\t" q95
    print "Max\t" max
  }
}
