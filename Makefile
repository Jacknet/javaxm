
all:
	rm -rf out
	mkdir out
	find . -name \*.java -exec javac -d out -sourcepath . {} \;
	mkdir out/logos
	cp logos/*gif out/logos

run:
	java -cp out com.kfu.JXM.JXM
