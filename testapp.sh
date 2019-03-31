#!/usr/bin/env bash
java -classpath bin interfaces.TestApp //127.0.0.1/1 "$1" "$2" "${3:-nothing}"
