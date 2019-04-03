#!/usr/bin/env bash
java -Djava.net.preferIPv4Stack=true -classpath bin interfaces.TestApp //127.0.0.1/1 "$1" "$2" "${3:-nothing}"
