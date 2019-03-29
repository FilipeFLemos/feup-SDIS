#!/usr/bin/env bash
java -classpath bin -Djava.net.preferIPv4Stack=true -Djava.rgit peer.Peer "$1" "$2" "$3" "${4:-232.1.1.0}" "${5:-4465}" "${6:-232.1.1.1}" "${7:-4466}" "${8:-232.1.1.2}" "${9:-4467}" &
