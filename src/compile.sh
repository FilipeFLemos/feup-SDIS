#!/usr/bin/env bash
rm -rf bin
mkdir -p bin
javac -d bin -sourcepath . interfaces/TestApp.java peer/Peer.java -Xlint:unchecked

