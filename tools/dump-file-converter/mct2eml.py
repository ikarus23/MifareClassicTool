#!/usr/bin/env python3
# -*- coding: utf-8 -*-
#
########################################################################
#
# Copyright 2015 Gerhard Klostermeier
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

from sys import exit, argv


def main():
  """ Convert a MCT dump file to a .eml file (Proxmark3 emulator). """
  # Are there enouth arguments?
  if len(argv) is not 3:
    usage()

  # TODO: Check if the dump is comple (has all sectors and no unknown data)
  # and if not, create the missing data.
  # (0x00 for data, 0xFF for keys and 0x078069)

  # Convert the file line by line.
  with open(argv[1], 'r') as mctFile, open(argv[2], 'w') as emlFile:
    for line in mctFile:
      if line[:8] == '+Sector:':
        continue
      emlFile.write(line.lower())


def usage():
  """ Print the usage. """
  print('Usage: ' + argv[0] + ' <mct-dump> <output-file-(eml)>')
  print('INFO: MCT dump has to be complete ' +
      '(all sectors and no unknown data).')
  exit(1);


if __name__ == '__main__':
    main()

