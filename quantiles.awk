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
  print "Mean\t" (sum / NR)
  print "50%\t" vals[int(NR * .5)]
  print "90%\t" vals[int(NR * .9)]
  print "95%\t" vals[int(NR * .95)]
  print "Max\t" vals[NR]
}
