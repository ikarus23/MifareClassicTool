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
from binascii import unhexlify


def main():
  """ Convert a .eml file (Proxmark3 emulator) to a .mfd file (MIFARE Drump). """
  # Are there enouth arguments?
  if len(argv) != 3:
    usage()

  # Convert the file line by line.
  with open(argv[1], 'r') as emlFile, open(argv[2], 'wb') as mfdFile:
    for line in emlFile:
      line = line.rstrip()
      data = unhexlify(line)
      mfdFile.write(data)


def usage():
  """ Print the usage. """
  print('Usage: ' + argv[0] + ' <eml-file> <output-file-(mfd)>')
  exit(1);


if __name__ == '__main__':
    main()

