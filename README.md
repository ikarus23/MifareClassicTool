MifareClassicTool
=================

An Android NFC-App for reading, writing, analyzing, etc. Mifare Classic RFID-Tags.

* **[MifareClassicTool on Google Play]
  (https://play.google.com/store/apps/details?id=de.syss.MifareClassicTool)**
* **[MifareClassicTool (Donate Version) on Google Play]
  (https://play.google.com/store/apps/details?id=de.syss.MifareClassicToolDonate)**
* **[MifareClassicTool on F-Droid]
  (https://f-droid.org/repository/browse/?fdfilter=mifare&fdid=de.syss.MifareClassicTool)**
* **[Download MifareClassicTool (APK file)]
  (http://publications.icaria.de/mct/releases/)**
* **[Screenshots]
  (http://publications.icaria.de/mct/screenshots/)**
  (outdated, check Google Play)
* **[Additional stuff]
  (http://publications.icaria.de/mct/)** (Documentation, etc.)
* **[Thread at the Proxmark forum]
  (http://www.proxmark.org/forum/viewtopic.php?id=1535)**



Features
--------

* Read Mifare Classic tags
* Save, edit and share the tag data you read
* Write to Mifare Classic tags (block-wise)
* Clone Mifare Classic tags  
  (Write dump of a tag to another tag; write 'dump-wise')
* Key management based on dictionary-attack  
  (Write the keys you know in a file (dictionary).  
  MCT will try to authenticate with these  
  keys against all sectors and read as much as possible.  
  See chapter [Getting Started](#getting-started).)
* Format a tag back to the factory/delivery state
* Write the manufacturer block of special Mifare Classic tags
* Create, edit, save and share key files (dictionaries)
* Decode & Encode Mifare Classic Value Blocks
* Decode & Encode Mifare Classic Access Conditions
* Compare dumps (Diff Tool)
* Display generic tag information
* Display the tag data as highlighted hex
* Display the tag data as 7-Bit US-ASCII
* Display the Mifare Classic Access Conditions as a table
* Display Mifare Classic Value Blocks as integer
* In-App (offline) help and information
* It's free software (open source). ;)



General Information
-------------------

This tool provides several features to interact with (and only with)
Mifare Classic RFID-Tags. It is designed for users who have at least
basic familiarity with the Mifare Classic technology.
You also need an understanding of the hexadecimal number system,
because all data input and output is in hexadecimal.

Some important things are:
* The features this tool provides are very basic. There are no such
  fancy things like saving a URL to an RFID-Tag with a nice looking
  graphical user interface. If you want so save things on a tag,
  you have to input the raw hexadecimal data.
* This App **can not crack/hack**
  any Mifare Classic keys. If you want to read/write an RFID-Tag, you
  first need keys for this specific tag. For additional information
  please read/see chapter [Getting Started](#getting-started).
* There will be **no &quot;brute-force&quot; attack**
  capability in this application. It is way too slow due
  to the protocol.
* The first block of the first sector of an **original**
  Mifare Classic tag is **read-only** i.e. not writable. But there
  are **special** Mifare Classic tags that support writing to the
  manufacturer block with a simple write command. This App is able to
  write to such tags and can therefore create fully correct clones.
  However, some special tags require a **special command sequence** to
  put them into the state where writing to the manufacturer block is
  possible. These tags will not work.  
  Remember this when you are shopping for special tags!
* This app **will not work** on the following devices because
  their hardware (NFC-controller) does not support Mifare Classic
  ([read more](https://github.com/ikarus23/MifareClassicTool/issues/1)).
  This list may be incomplete. Have a look at
  [this list](http://www.shopnfc.it/en/content/7-nfc-device-compatibility) too.
  * Asus Zenfone 2
  * Foxcon InFocus M320
  * Google Nexus 4
  * Google Nexus 5
  * Google Nexus 6
  * Google Nexus 7 (2013)
  * Google Nexus 10
  * Huawei G620S
  * LG F60
  * LG G2
  * LG G2 mini
  * LG G3 S
  * LG Optimus L7 II
  * Motorola Moto X (2014, 2ed gen.)
  * Samsung Galaxy A3
  * Samsung Galaxy Ace 3
  * Samsung Galaxy Ace 4
  * Samsung Galaxy Alpha
  * Samsung Galaxy Express 2
  * Samsung Galaxy Mega
  * Samsung Galaxy Note 3
  * Samsung Galaxy Note 4
  * Samsung Galaxy S4
  * Samsung Galaxy S5 (900P, Sprint)
  * Samsung Galaxy S6
  * Samsung Galaxy S6 Edge
  * Sony Xperia Z3 (SOL26)
  * Xiaomi MI 3
* This app **has been known to work** on the following devices.
  * Google Nexus 7 (2012)
  * HTC HTC One
  * Huawei Ascend Mate7
  * Samsung Galaxy Nexus
  * Samsung Galaxy S3 i9300
  * Samsung Galaxy S3 Duo i9300i

For further information about Mifare Classic check
[Wikipedia](https://en.wikipedia.org/wiki/MIFARE),
[do some Google searches](https://www.google.com/search?q=mifare+classic")
or read the
[Mifare Classic (1k) 'Datasheet'](http://www.nxp.com/documents/data_sheet/MF1S50YYX.pdf)
(PDF) from NXP.



Getting Started
---------------

First of all, you need the keys for the tag you want to read.
Due to some weaknesses in Mifare Classic, you can retrieve
all the keys (A and B) of a tag with tools like the
[Proxmark3](http://www.proxmark.org/) or
normal RFID-Readers and some special software
([mfcuk](https://code.google.com/p/mfcuk/),
[mfoc](http://code.google.com/p/mfoc/)).

The application comes with standard key files called
*std.keys* and *extended-std.keys*, which contains the
well known keys and some standard keys from a short Google search.
You can try to read a tag with this key file using
&quot;Read Tag&quot; from main menu.

Once you know some keys, you cam to put them into a simple text
file (one key per line). You can do this on your PC and transfer
the file to the *MifareClassicTool/key-files/*
directory (on external storage), or you can create a new key file via
&quot;Edit or Add Key File&quot; from main menu.
If you are finished setting up your key file, you can read a tag
using &quot;Read Tag&quot; from main menu.

Advantages of the Key Files Concept:
* **You don't have to worry about which key is for which sector.**  
  The application tries to authenticate with all keys from the key
* **You don't have to know all the keys.**  
  If neither key A nor key B for a specific sector is found in the
  key file (dictionary), the application will skip reading said
  sector.

This dictionary-attack based mapping process
(keys &lt;-&gt; sectors) makes it easy for you to read as much as
possible with the keys you know!



License
-------

This application was originally developed by
Gerhard Klostermeier in cooperation with SySS GmbH
([www.syss.de](https://www.syss.de/)) and Aalen
University ([www.htw-aalen.de](http://www.htw-aalen.de/)) in 2012/2013.
It is free software and licensed under the
[GNU General Public License v3.0 (GPLv3)](https://www.gnu.org/licenses/gpl-3.0.txt)

Icons used in this application:
* Oxygen Icons: [www.oxygen-icons.org](http://www.oxygen-icons.org/)  
  ([GNU Lesser General Public License](http://www.gnu.org/licenses/lgpl.html))
* RFID Tag: [www.nfc-tag.de](http://www.nfc-tag.de/)  
  ([Creative Commons 3.0](http://creativecommons.org/licenses/by/3.0/))

MIFARE is a registered trademark of NXP Semiconductors.
