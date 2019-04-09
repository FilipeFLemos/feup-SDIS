#Backups

echo "Testing Backups \n \n Backing up 10k file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 BACKUP "files/test10k" 2

sleep 5

echo "\n Backing up 100k file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 BACKUP "files/test100k" 2

sleep 10

echo "\n Backing up 1M file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 BACKUP "files/test1M" 2

sleep 20

echo "\n Backing up image file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 BACKUP "files/image1.png" 2

sleep 5

#Restores

echo "Testing Restores \n \n Restoring 10k file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 RESTORE "files/test10k"

sleep 10

echo "\n Restoring 100k file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 RESTORE "files/test100k"

sleep 30

echo "\n Restoring 1M file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 RESTORE "files/test1M"

sleep 60

#Reclaim

echo "\n Reclaiming 10k from peer 2"

java -classpath bin interfaces.TestApp //127.0.0.1/2 RECLAIM 10

sleep 10

echo "\n Reclaiming 10k from peer 3"

java -classpath bin interfaces.TestApp //127.0.0.1/3 RECLAIM 10

sleep 10

echo "\n Reclaiming 10k from peer 4"

java -classpath bin interfaces.TestApp //127.0.0.1/4 RECLAIM 10

sleep 10

echo "\n Restoring image file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 RESTORE "files/image1.png"

sleep 10

#Delete

echo "\n Deleting 1M file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 DELETE "files/test1M"

sleep 5

echo "\n Restoring 1M file in Peer 1, Should fail"

java -classpath bin interfaces.TestApp //127.0.0.1/1 RESTORE "files/test1M"
