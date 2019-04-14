#Backups

echo "Testing Backups \n \n Backing up 10k file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 BACKUP "files/test10k" 2

sleep 5

echo "\n Backing up 100k file in Peer 2"

java -classpath bin interfaces.TestApp //127.0.0.1/2 BACKUP "files/test100k" 2

sleep 7

echo "\n Restoring 10k file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 RESTORE "files/test10k"

sleep 5

echo "\n Restoring 100k file in Peer 2"

java -classpath bin interfaces.TestApp //127.0.0.1/2 RESTORE "files/test100k"
