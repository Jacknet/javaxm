# $Id: Makefile,v 1.11 2004/03/30 17:14:59 nsayer Exp $

all:
	rm -rf out
	mkdir out
	( cd out ; unzip ../secondstring\*.jar )
	find . -name \*.java -exec javac -classpath /System/Library/Java:out:comm.jar -deprecation -d out -sourcepath . {} \;
	mkdir out/logos
	cp logos/*gif out/logos
	mkdir out/images
	cp images/*png out/images
	cp COPYING out

jar:
	cd out && jar cfm ../jxm.jar ../Manifest COPYING com edu logos images

# this is sort of optimized for Nick only.
run:
	DYLD_LIBRARY_PATH=. && java -cp ./comm.jar:/System/Library/Java:out com.kfu.JXM.JXM

# everyone but me should probably run it like this:
runjar:
	java -jar jxm.jar
