#!/usr/bin/env bash
java -classpath bin test.TestApp "$1" "$2" "$3" "${4:-nothing}"
