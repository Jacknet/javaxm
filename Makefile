
all:
	rm -rf out
	mkdir out
	find . -name \*.java -exec javac -deprecation -d out -sourcepath . {} \;
	mkdir out/logos
	cp logos/*gif out/logos
	cp COPYING out

jar:
	cd out && jar cfm ../jxm.jar ../Manifest COPYING com edu logos

run:
	java -cp out com.kfu.JXM.JXM

runjar:
	java -jar jxm.jar
