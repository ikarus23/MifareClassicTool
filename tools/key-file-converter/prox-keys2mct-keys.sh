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
# Usage: ./prox-keys2mct-keys.sh <key-file>
#
########################################################################
#
# Info:
# - "key-file" must be exactly formatted like
#   "example-key-dump.txt".
# - For more information see the README.txt
#
########################################################################


sed -n '4,19 s/^.\{7\}\(.\{12\}\).\{9\}\(.\{12\}\).*$/\U\1\n\U\2/p' ${1:?no key file given} | sort -u
