#!/usr/bin/env bash
rm -rf bin
rm -f *.jar
mkdir -p bin
javac -Xlint:unchecked -d bin -sourcepath src src/interfaces/TestApp.java src/peer/Peer.java

