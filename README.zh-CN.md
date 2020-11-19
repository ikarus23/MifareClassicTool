MIFARE Classic Tool (MCT)
=========================

一个用于对MIFARE Classic RFID标签进行读取、写入、分析等操作的Android NFC应用程序。

<a href="https://play.google.com/store/apps/details?id=de.syss.MifareClassicTool"><img src="metadata/common/assets/google-play-badge.png" alt="从Play商店中下载" height="80"></a>
<a href="https://f-droid.org/packages/de.syss.MifareClassicTool/"><img src="metadata/common/assets/fdroid-badge.png" alt="从F-Droid商店中下载" height="80"></a>
<a href="https://www.icaria.de/mct/releases/"><img src="metadata/common/assets/direct-apk-download-badge.png" alt="直接下载APK文件" height="80"></a>

以其他语言阅读本文:
* [English](README.md)
* [简体中文](README.zh-CN.md)

链接:
* [Google Play上的MIFARE Classic Tool (捐赠版)](https://play.google.com/store/apps/details?id=de.syss.MifareClassicToolDonate)
* [华为AppGallery上的MIFARE Classic Tool](https://appgallery1.huawei.com/#/app/C101783071)
* [截图](https://www.icaria.de/mct/screenshots/latest/)
* [帮助与信息/用户手册](https://www.icaria.de/mct/help-and-info/)
* [其他内容](https://www.icaria.de/mct/) (文档，APK文件等)
* [Proxmark论坛上的主题](http://www.proxmark.org/forum/viewtopic.php?id=1535)
* [通过Paypal捐赠](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=24ET8A36XLMNW) [![捐赠](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=24ET8A36XLMNW)



特性
--------

* 读取MIFARE Classic标签
* 保存，编辑和分享您读取的标签数据
* 写入MIFARE Classic标签(逐块)
* 克隆MIFARE Classic标签
  (将标签的转储写入另一个标签；逐文件写入)
* 基于字典攻击的密钥管理
  (将已知密钥写入字典文件)
  MCT将尝试对所有扇区使用这些密钥进行身份验证，并尽可能多地读取。请参阅章节[入门](#入门)。
* 将标签格式化为出厂/交付状态
* 对特殊类型的MIFARE Classic标签写入制造商块(块0)
* 使用外部NFC读取器，例如ACR 122U
  (请参阅[帮助与信息](https://publications.icaria.de/mct/help-and-info/#external_nfc)获得更多信息)
* 创建，编辑，保存并分享密钥文件(字典)
* 解码和编码MIFARE Classic值块
* 解码和编码MIFARE Classic访问控制条件
* 比较转储(通过比较工具)
* 显示标签的一般信息
* 以高亮的十六进制形式查看标签数据
* 以7-Bit US-ASCII形式查看标签数据
* 以表格形式查看MIFARE Classic访问控制条件
* 以整数形式查看标签数据
* 计算BCC(Block Check Character/信息组校验码)
* 快速UID克隆功能
* 导入/导出/转换文件
* 内建离线的帮助与信息
* 它是一个免费软件(开源哦) ;)



一般信息
-------------------

本工具提供了一些操作MIFARE Classic RFID标签的功能。本工具针对至少基本熟悉MIFARE Classic技术的用户。您还需要了解十六进制数系统，因为所有的数据输入和输出都是十六进制的。

一些重要信息：
* 此工具提供的功能非常底层。
  没有什么能够比通过漂亮的图形界面将URL保存到RFID标签更有趣了。但如果您想在标签上保存什么，您必须输入原始的十六进制数据。
* 此应用程序**无法破解/破解**任何MIFARE经典密钥。
  如果您想读/写RFID标签，您首先需要这个特定标签的密钥。欲了解更多信息，请参阅章节[入门](#入门)。
* 此应用程序不会提供**暴力攻击**功能。因为协议太慢了。
* 注意！卸载此应用程序将会永久删除其保存的所有文件（转储/密钥）。
* **原始**MIFARE Classic标签中的第一个块是**只读的**，即无法向其写入任何数据。
  但是有一些特殊的MIFARE Classic标签支持通过简单的写命令写入制造商块(即块0)，该应用程序能够写入此类标签，因此可以创建完全正确的克隆。（通常称为“ 2代魔术标签”，即中文购物网站上通常可以见到的CUID卡；某些CUID卡无法使用，详见[MIFARE Classic DirectWrite](https://github.com/RfidResearchGroup/proxmark3/blob/master/doc/magic_cards_notes.md#mifare-classic-directwrite-aka-gen2-aka-cuid)，但在中文购物网站上似乎不必担心遇到这类特殊的卡片；FUID/UFUID理论上也可使用，但未经测试，欢迎在issue中反馈使用效果。）
  然而，某些特殊标签需要**特殊命令序列**以使其可以写入制造商块（“ 1代魔术标签”，即中文购物网站上通常可以见到的UID卡）。本应用无法写入这类标签的制造商块，请购买时注意区分！
  另外，请确保BCC值（请使用“ BCC计算器”）、SAK和ATQA值正确。如果您只想克隆一个UID，请使用“克隆UID工具”。
* 本应用程序在某些硬件（NFC控制器）不支持MIFARE Classic的设备上**无法使用** ([了解更多](https://github.com/ikarus23/MifareClassicTool/issues/1))。
  **您可以在[这里](https://github.com/ikarus23/MifareClassicTool/blob/master/INCOMPATIBLE_DEVICES.md)找到已知不兼容设备列表**

有关MIFARE Classic的更多信息，请参阅[维基百科](https://en.wikipedia.org/wiki/MIFARE)，[百度搜索](https://www.baidu.com/s?ie=UTF-8&wd=MIFARE%20Classic)或者阅读NXP的[MIFARE Classic (1k) '数据表'](https://www.nxp.com/docs/en/data-sheet/MF1S50YYX_V1.pdf)(PDF)。


入门
---------------

首先，您需要待读取标签的密钥。由于MIFARE Classic中的某些缺陷，您可以使用[Proxmark3](http://www.proxmark.org/)、普通的RFID读卡器以及一些特殊工具 ([mfcuk](https://github.com/nfc-tools/mfcuk)、[mfoc](https://github.com/nfc-tools/mfoc))找回全部密钥（包括密钥A与密钥B）。

该应用程序自带名为*std.keys*和*extended-std.keys*的标准密钥文件，其中包含常见的的密钥和通过Google搜索得到的一些标准密钥。您可以通过应用程序主界面的“读标签”尝试用其读取标签数据。

如有已知密钥，可以将其放入一个简单的文本文件中（每行一个密钥）。您可以在PC上进行此操作，然后使用MCT的“导入/导出工具”导入文件，也可以通过“编辑/新建密钥文件”创建新的密钥文件。如果完成了密钥文件的设置，则可以通过应用程序主界面的“读标签”来读取标签。

密钥文件的优点：
* **您不必担心哪个密钥属于哪个扇区**  
  应用程序尝试使用文件中的所有密钥进行身份验证
* **您不必知道所有密钥**  
  如果在密钥文件（字典）中找不到用于特定扇区的密钥A和密钥B，则应用程序将跳过读取该扇区的操作。
这种基于字典攻击的映射过程(密钥&lt;-&gt;扇区) 使您可以轻松读取尽可能多的已知密钥！



许可
-------

此应用程序最初由Gerhard Klostermeier与Sys GmbH[(www.syss.de)](https://www.syss.de/)及亚伦大学[(www.htw-aalen.de)](http://www.htw-aalen.de/)在2012/2013年合作开发。它是免费软件，并通过[GNU通用公共许可证版本3(GPLv3)](https://www.gnu.org/licenses/gpl-3.0.txt)授权。


应用中的图标:
* Logo: [Beneke Traub](http://www.beneketraub.com/)  
  ([Creative Commons 4.0](http://creativecommons.org/licenses/by-nc-sa/4.0/))
* Oxygen Icons: [www.oxygen-icons.org](http://www.oxygen-icons.org/)  
  ([GNU Lesser General Public License](http://www.gnu.org/licenses/lgpl.html))
* RFID Tag: [www.nfc-tag.de](http://www.nfc-tag.de/)  
  ([Creative Commons 3.0](http://creativecommons.org/licenses/by/3.0/))

MIFARE®是NXP Semiconductors的注册商标。
