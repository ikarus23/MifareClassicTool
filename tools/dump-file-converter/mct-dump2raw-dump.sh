#!/bin/bash

########################################################################
#
# Copyright 2013 Gerhard Klostermeier
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
########################################################################
#
# Usage: ./mct-dump2raw-dump.sh <dumpFile>
#
########################################################################
#
# Info:
# - You have to install "perl"!
# - "dumpFile" must be like "example-dump-file.txt"
# - "dumpFile" must be complete (e.g. sectors 0-15)
# - For more information see the README-mct-dump2raw-dump.txt
#
#########################################################################


sc=$(sed -n '/^\+.*$/p' ${1:?no dump file given} | wc -l)
# Rough check if dump file is complete (consists of 5/16/32/40 sectors).
if [ $sc -eq 5 -o $sc -eq 16 -o $sc -eq 32 -o $sc -eq 40 ]
then
    sed '/^\+.*$/d' $1 | perl -ne 's/([0-9a-f]{2})/print chr hex $1/gie'
else
    echo "Not a (complete) dump."
fi
