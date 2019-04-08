#Backups

echo -e "Testing Backups \n \n Backing up 10k file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 BACKUP "files/test10k" 2

sleep(5)

echo -e "\n Backing up 100k file in Peer 2"

java -classpath bin interfaces.TestApp //127.0.0.1/2 BACKUP "files/test100k" 2

sleep(5)

echo -e "\n Backing up 1M file in Peer 3"

java -classpath bin interfaces.TestApp //127.0.0.1/3 BACKUP "files/test1M" 2

sleep(5)

echo -e "\n Backing up 10M file in Peer 4"

java -classpath bin interfaces.TestApp //127.0.0.1/4 BACKUP "files/test10M" 2

sleep(10)

#Restores

echo -e "Testing Restores \n \n Restoring 10k file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 RESTORE "files/test10k"

sleep(5)

echo -e "\n Restoring 100k file in Peer 2"

java -classpath bin interfaces.TestApp //127.0.0.1/2 RESTORE "files/test100k"

sleep(5)

echo -e "\n Restoring 1M file in Peer 3"

java -classpath bin interfaces.TestApp //127.0.0.1/3 RESTORE "files/test1M"

sleep(5)

echo -e "\n Restoring 10M file in Peer 4"

java -classpath bin interfaces.TestApp //127.0.0.1/4 RESTORE "files/test10M"

sleep(10)

#Delete

echo -e "\n Deleting 1M file in Peer 3"

java -classpath bin interfaces.TestApp //127.0.0.1/3 DELETE "files/test1M"

sleep(5)

echo -e "\n Restoring 1M file in Peer 3, Should fail"

java -classpath bin interfaces.TestApp //127.0.0.1/3 RESTORE "files/test1M"

#Reclaim

echo -e "\n Reclaiming 100k from peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 RECLAIM 100

sleep(10)

echo -e "\n Reclaiming 100k from peer 2"

java -classpath bin interfaces.TestApp //127.0.0.1/2 RECLAIM 100

sleep(10)

echo -e "\n Reclaiming 100k from peer 3"

java -classpath bin interfaces.TestApp //127.0.0.1/3 RECLAIM 100

sleep(10)

echo -e "\n Restoring 10M file in Peer 4"

java -classpath bin interfaces.TestApp //127.0.0.1/4 RESTORE "files/test10M"

sleep(10)
