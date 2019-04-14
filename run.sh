#!/usr/bin/env bash
clear
java -Djava.net.preferIPv4Stack=true -classpath bin peer.Peer "$1" "$2" //"$3"/ 224.0.0.0 8000 224.0.0.0 8001 224.0.0.0 8002
