#Backups

echo "Testing Backups \n \n Backing up 10k file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 BACKUP "files/test10k" 2

sleep 5

echo "\n Backing up 100k file in Peer 2"

java -classpath bin interfaces.TestApp //127.0.0.1/2 BACKUP "files/test100k" 2

sleep 15

echo "\n Backing up 1M file in Peer 3"

java -classpath bin interfaces.TestApp //127.0.0.1/3 BACKUP "files/test1M" 2

sleep 35

java -classpath bin interfaces.TestApp //127.0.0.1/1 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/2 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/3 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/4 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/4 RECLAIM 300

sleep 10

java -classpath bin interfaces.TestApp //127.0.0.1/1 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/2 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/3 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/4 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/4 RECLAIM 300

sleep 10

java -classpath bin interfaces.TestApp //127.0.0.1/1 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/2 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/3 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/4 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/2 RECLAIM 300

sleep 10

java -classpath bin interfaces.TestApp //127.0.0.1/1 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/2 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/3 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/4 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/2 RECLAIM 300

sleep 10

java -classpath bin interfaces.TestApp //127.0.0.1/1 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/2 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/3 STATE

java -classpath bin interfaces.TestApp //127.0.0.1/4 STATE


