# $Id: Makefile,v 1.8 2004/03/09 07:09:11 nsayer Exp $

all:
	rm -rf out
	mkdir out
	( cd out ; unzip ../secondstring\*.jar )
	find . -name \*.java -exec javac -deprecation -d out -sourcepath . {} \;
	mkdir out/logos
	cp logos/*gif out/logos
	mkdir out/images
	cp images/*png out/images
	cp COPYING out

jar:
	cd out && jar cfm ../jxm.jar ../Manifest COPYING com edu logos images

run:
	java -cp out com.kfu.JXM.JXM

runjar:
	java -jar jxm.jar
