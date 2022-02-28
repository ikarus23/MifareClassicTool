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
  """ Convert a .eml file (Proxmark3 emulator) to a MCT dump file. """
  # Are there enouth arguments?
  if len(argv) != 3:
    usage()

  # Convert the file line by line.
  with open(argv[1], 'r') as emlFile, open(argv[2], 'w') as mctFile:
    sector = 0
    block = 0
    for line in emlFile:
      if (sector < 32 and block % 4 == 0) or \
          (sector >= 32 and block % 16 == 0):
        mctFile.write('+Sector: ' + str(sector) + '\n')
        sector += 1
      line = line.rstrip()
      mctFile.write(line.upper() + '\n')
      block += 1


def usage():
  """ Print the usage. """
  print('Usage: ' + argv[0] + ' <eml-file> <output-mct-dump>')
  exit(1);


if __name__ == '__main__':
    main()
 v5.3.4
ATS:80318066B1840C016E0183009000
00 A4 04 00 0E 32 50 41 59 2E 53 59 53 2E 44 44 46 30 31 00
6F 5E 84 0E 32 50 41 59 2E 53 59 53 2E 44 44 46 30 31 A5 4C BF 0C 49 61 28 4F 07 A0 00 00 00 04 10 10 50 10 4D 61 73 74 65 72 63 61 72 64 20 44 65 62 69 74 87 01 01 5F 55 02 55 53 42 03 52 77 13 61 1D 4F 07 A0 00 00 00 04 22 03 50 05 44 65 62 69 74 87 01 02 5F 55 02 55 53 42 03 52 77 13 90 00
00 A4 04 00 07 A0 00 00 00 04 10 10 00
6F 47 84 07 A0 00 00 00 04 10 10 A5 3C 50 10 4D 61 73 74 65 72 63 61 72 64 20 44 65 62 69 74 87 01 01 5F 2D 02 65 6E BF 0C 1F 9F 5D 03 01 00 00 5F 55 02 55 53 42 03 52 77 13 9F 4D 02 0B 0A 9F 6E 07 08 40 00 00 30 30 00 90 00
80 A8 00 00 02 83 00 00
77 12 82 02 19 81 94 0C 08 01 01 00 10 01 01 01 20 01 03 00 90 00
00 B2 01 0C 00
70 68 9F 6C 02 00 01 9F 62 06 00 00 00 00 07 00 9F 63 06 00 00 00 00 00 FE 56 28 42 YY YY 5E 20 2F 5E 32 37 30 31 32 30 31 35 30 30 30 30 30 30 30 30 30 30 30 9F 64 01 03 9F 65 02 07 00 9F 66 02 00 FE 9F 6B 12 XX XX D2 70 12 01 50 00 00 00 00 00 9F 67 01 03 90 00
80 CA 9F 4F 00
9F 4F 1A 9F 27 01 9F 02 06 5F 2A 02 9A 03 9F 36 02 9F 52 06 DF 3E 01 9F 21 03 9F 7C 14 90 00
00 B2 01 5C 00
6A 83
80 CA 9F 17 00
9F 17 01 01 90 00
80 CA 9F 36 00
6A 88
00 A4 04 00 07 A0 00 00 00 04 22 03 00
6F 3C 84 07 A0 00 00 00 04 22 03 A5 31 50 05 44 65 62 69 74 87 01 02 5F 2D 02 65 6E BF 0C 1F 9F 5D 03 01 00 00 5F 55 02 55 53 42 03 52 77 13 9F 4D 02 0B 0A 9F 6E 07 08 40 00 00 30 30 00 90 00
80 A8 00 00 02 83 00 00
77 0E 82 02 19 81 94 08 10 01 01 01 20 01 03 00 90 00
00 B2 01 14 00
70 81 A5 9F 42 02 08 40 5F 25 03 22 02 01 5F 24 03 27 01 31 5A 08 XX XX 5F 34 01 00 9F 07 02 FF C0 9F 08 02 00 02 8C 27 9F 02 06 9F 03 06 9F 1A 02 95 05 5F 2A 02 9A 03 9C 01 9F 37 04 9F 35 01 9F 45 02 9F 4C 08 9F 34 03 9F 21 03 9F 7C 14 8D 0C 91 0A 8A 02 95 05 9F 37 04 9F 4C 08 8E 0C 00 00 00 00 00 00 00 00 02 03 1F 03 9F 51 03 9F 37 04 9F 0D 05 B4 50 84 00 0C 9F 0E 05 00 00 00 00 00 9F 0F 05 B4 70 84 80 0C 5F 28 02 08 40 9F 4A 01 82 57 0E XX XX D2 70 12 01 53 12 90 00
80 CA 9F 4F 00
9F 4F 1A 9F 27 01 9F 02 06 5F 2A 02 9A 03 9F 36 02 9F 52 06 DF 3E 01 9F 21 03 9F 7C 14 90 00
00 B2 01 5C 00
00 00 00 00 02 00 00 08 40 22 02 26 00 14 20 00 01 22 00 00 00 02 38 13 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 90 00
00 B2 02 5C 00
00 00 00 00 00 00 00 08 40 22 02 26 00 13 20 10 01 22 00 00 00 02 37 25 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 90 00
00 B2 03 5C 00
6A 83
80 CA 9F 17 00
9F 17 01 01 90 00
80 CA 9F 36 00
6A 88


