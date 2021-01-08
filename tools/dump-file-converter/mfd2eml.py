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
from binascii import hexlify


def main():
  """ Convert a .mfd file (MIFARE Drump) to a .eml file (Proxmark3 emulator). """
  # Are there enouth arguments?
  if len(argv) != 3:
    usage()

  # Convert the file line by line.
  with open(argv[1], 'rb') as mfdFile, open(argv[2], 'w') as emlFile:
    while True:
      bytes = mfdFile.read(16)
      if not bytes:
        break
      chars = hexlify(bytes).decode('UTF-8')
      emlFile.write(chars + '\n')


def usage():
  """ Print the usage. """
  print('Usage: ' + argv[0] + ' <mfd-file> <output-file-(eml)>')
  exit(1);


if __name__ == '__main__':
    main()

