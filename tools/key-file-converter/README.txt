########################################################################
# PROXMARK KEYS TO MIFARE CLASSIC TOOL KEYS - Key file converter       #
########################################################################

The script "prox-keys2mct-keys.sh" converts the output of
a Proxmark3 which cracked a MIFARE Classic Tag using the nested
attack, into a key file suitable for the MCT-App.

Dependencies:
- sed ;)

########################################################################

Usage: ./prox-keys2mct-keys.sh <key-file>

key-file:
  The key file is the screen output of the Proxmark3 nested attack
  copied into a file. It should look exactly like the
  "example-key-dump.txt" (of course with different keys,
  see "example-files" directory).

Usage Examples:
  ./prox-keys2mct-keys.sh example-key-dump.txt > example.keys
  You can then transfer the "example.keys" to the
  "MifareClassicTool/key-files/" directory of your Android-Device's
  external storage.

########################################################################
