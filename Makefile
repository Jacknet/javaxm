# $Id: Makefile,v 1.12 2004/04/29 15:21:45 nsayer Exp $

all:
	rm -rf out
	mkdir out
	( cd out ; unzip ../secondstring\*.jar )
	find . -name \*.java -exec javac -target 1.4 -classpath /System/Library/Java:out:comm.jar:install-win32/registry.jar:install-win32/trayicon.jar -deprecation -d out -sourcepath . {} \;
	(cd logos ; zip ../logos.jar *gif )
	mkdir out/logos
	cp logos/*gif out/logos
	mkdir out/images
	cp images/*png out/images
	cp COPYING out

jar:
	cd out && jar cfm ../jxm.jar ../Manifest COPYING com edu images logos

# this is sort of optimized for Nick only.
run:
	DYLD_LIBRARY_PATH=. && java -cp ./comm.jar:/System/Library/Java:out com.kfu.JXM.JXM

# everyone but me should probably run it like this:
runjar:
	java -jar jxm.jar
