#!/usr/bin/env python3
# -*- coding: utf-8 -*-

########################################################################
#
# Copyright 2018 Gerhard Klostermeier
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
# Usage: ./key-file-diff.py <file A> <file B>
#
########################################################################
#
# Info:
# - Read two key files and show the keys that are in file B but not in A.
# - Keys must be 12 hex characters long and at the beginning of a line.
#
########################################################################


import re


def main(args):
    """
    Read two key files and show the keys that are in file B but not in A.
    :param args: Path to two key files A and B (positional shell parameters).
    :return: 0 if everything went fine.
    """
    key_file_a = args[1]
    key_file_b = args[2]

    with open(key_file_a, 'r') as f:
        keys_a = parse_keys(f)
    with open(key_file_b, 'r') as f:
        keys_b = parse_keys(f)

    # Show all keys that are in B but not in A.
    keys_diff = keys_b.difference(keys_a)
    for key in keys_diff:
        print(key)

    return 0


def parse_keys(file):
    """
    Parse keys from a file and return them as a set.
    Keys must be 12 hex characters long and at the beginning of a line.
    :param file: Path to a file containing keys.
    :return: A set of keys read from the file.
    """
    keys = set()
    key_regex = re.compile('^[0-9a-fA-F]{12}')
    for line in file:
        key = key_regex.match(line)
        try:
            key = key.group(0).upper()
            keys.add(key)
        except AttributeError:
            pass
    return keys


if __name__ == '__main__':
    import sys
    sys.exit(main(sys.argv))
