# $Id: Makefile,v 1.10 2004/03/20 16:24:36 nsayer Exp $

all:
	rm -rf out
	mkdir out
	( cd out ; unzip ../secondstring\*.jar )
	find . -name \*.java -exec javac -classpath /System/Library/Java -deprecation -d out -sourcepath . {} \;
	mkdir out/logos
	cp logos/*gif out/logos
	mkdir out/images
	cp images/*png out/images
	cp COPYING out

jar:
	cd out && jar cfm ../jxm.jar ../Manifest COPYING com edu logos images

run:
	DYLD_LIBRARY_PATH=. && java -cp /System/Library/Java:out com.kfu.JXM.JXM

runjar:
	java -jar jxm.jar
