#Backups

echo "Testing Backups \n \n Backing up 10k file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 BACKUP "files/test10k" 2

sleep 5

echo "\n Backing up 100k file in Peer 2"

java -classpath bin interfaces.TestApp //127.0.0.1/2 BACKUP "files/test100k" 2

sleep 7

echo "\n Backing up 120k file in Peer 3"

java -classpath bin interfaces.TestApp //127.0.0.1/3 BACKUP "files/test120k" 2

sleep 7

echo "\n Reclaiming to 80k file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 RECLAIM 80

sleep 20

echo "\n Reclaiming to 80k file in Peer 2"

java -classpath bin interfaces.TestApp //127.0.0.1/2 RECLAIM 80

sleep 20

sh testState.sh




