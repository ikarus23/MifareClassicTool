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
# Usage: ./raw-dump2mct-dump.sh <dumpFile>
#
########################################################################
#
# Info:
# - "dumpFile" must be like "example-dump-file.raw"
# - "dumpFile" must be complete (e.g. 1024 Byte)
# - For more information see the README-raw-dump2mct-dump.txt
#
#########################################################################


# Rough check if dump file is complete.
# (File size: 320/1024/2048/4096 Byte)
file=${1:?no dump file given}
s=$(ls -l $file | cut -d ' ' -f 5)
if [ $s -eq 320 -o $s -eq 1024 -o $s -eq 2048 -o $s -eq 4096 ]
then
    hex=$(xxd -p $file | tr -d '\n')
    line=""
    i=1
    sec=0
    blk=4
    out=""
    while (( i <= ${#hex} ))
    do
        if [ $sec -ge 0 -a $sec -le 32 -a $blk -eq 4 -o $sec -gt 32 -a $blk -eq 16 ]
        then
            # New sector.
            out="$out\n+Sector: $sec"
            (( sec++ ))
            blk=0
        fi

        # Add block to sector.
        out_blk=$(expr substr "$hex" $i 32)
        out="$out\n$out_blk"
        (( i=i+32 ))
        (( blk++ ))
    done
    echo -e "$out"
else
    echo "Not a (complete) dump."
fi
