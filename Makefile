GS = -g 

JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	Neighbor.java \
	RoutingTable.java \
	bfclient.java \

default: all

all: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
