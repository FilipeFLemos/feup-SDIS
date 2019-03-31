#!/usr/bin/env bash
java -classpath bin peer.Peer "$1" "$2" //localhost/ 224.0.0.0 8000 224.0.0.0 8001 224.0.0.0 8002
