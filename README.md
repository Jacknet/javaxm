<p align="center">
    <img src="https://github.com/Jacknet/javaxm/blob/master/images/xm_duke.png?raw=true" alt="JXM" width="100" />
</p>
<h3 align="center"><i>JXM v2</i></h3>

A Java-based GUI control application for the PCR models of XM satellite radio receivers, used to receive content from the SiriusXM service.

The goal of this repo is to provide quality of life improvements to the JXM source base by moving to Git, modernizing the code to support newer Java implementations, and act as reference for porting the XM control code to other programming languages.

## Prerequisites
### RXTX
At this time, JXM uses [`gnu.io`](https://docs.oracle.com/cd/E17802_01/products/products/javacomm/reference/api/javax/comm/package-summary.html) to interface with the XM receiver via serial. This requires the installation of the **[RXTX library](http://rxtx.qbang.org/wiki/index.php/Main_Page)**.

32-bit operating environments can download [either a binary of the library](http://rxtx.qbang.org/pub/rxtx/rxtx-2.1-7-bins-r2.zip) or [build from source](http://rxtx.qbang.org/pub/rxtx/rxtx-2.1-7r2.zip).

64-bit Linux-based environments are to [follow these instructions](http://www.euclideanspace.com/software/language/java/comm/), which includes downloading the [Java Communications API](https://download.oracle.com/otn-pub/java/JAVACOMM/3.0/3.0upd/comm3.0_linux.zip) to extract the `comm.jar` package from Oracle, which is subject to [Oracle's license agreement](https://www.oracle.com/downloads/licenses/java-se-archive-license.html).

Those using other 64-bit environments or wish to omit the Oracle download step must [build RXTX from source](http://rxtx.qbang.org/pub/rxtx/rxtx-2.1-7r2.zip).

---

[Original code](https://sourceforge.net/projects/javaxm/) by [Nick Sayer](https://sourceforge.net/u/nsayer/profile/), under GPLv2

[Windows registry access package](http://www.trustice.com/java/jnireg) by ICE Engineering

[Windows Tray Icon package](http://jeans.studentenweb.org/java/trayicon/trayicon.html) by Jean Struyf

`gnu.io` and `javax.comm` implementation by he [RXTX](http://www.rxtx.org/) project

Fuzzy logic string matching classes by the [SecondString](http://secondstring.sf.net/) project

[BrowserLauncher](http://browserlauncher.sf.net/) class for opening URLs on win32 by Eric Albert

Initial XMPCR reverse engineering efforts by the [OpenXM](http://xmpcr.sf.net/) project

Sirius, XM, SiriusXM and all related marks and logos are trademarks of Sirius XM Radio Inc.
