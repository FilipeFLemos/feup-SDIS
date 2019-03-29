#!/usr/bin/env bash
java -classpath bin interfaces.TestApp "$1" "$2" "$3" "${4:-nothing}"
