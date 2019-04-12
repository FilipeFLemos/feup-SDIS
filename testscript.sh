#Backups

echo "Testing Backups \n \n Backing up 10k file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 BACKUP "files/test10k" 2

sleep 5

echo "\n Backing up 100k file in Peer 2"

java -classpath bin interfaces.TestApp //127.0.0.1/2 BACKUP "files/test100k" 2

sleep 10

echo "\n Backing up 500k file in Peer 3"

java -classpath bin interfaces.TestApp //127.0.0.1/3 BACKUP "files/test500k" 2

sleep 20

echo "\n Backing up image file in Peer 4"

java -classpath bin interfaces.TestApp //127.0.0.1/4 BACKUP "files/image1.png" 2

sleep 5

#Restores

echo "Testing Restores \n \n Restoring 10k file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 RESTORE "files/test10k"

sleep 10

echo "\n Restoring 100k file in Peer 2"

java -classpath bin interfaces.TestApp //127.0.0.1/2 RESTORE "files/test100k"

sleep 10

#Reclaim

echo "\n Reclaiming 80k to peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 RECLAIM 80

#sleep 20

#echo "\n Reclaiming 80k to peer 2"

#java -classpath bin interfaces.TestApp //127.0.0.1/2 RECLAIM 80

#sleep 20

#echo "\n Reclaiming 80k to peer 3"

#java -classpath bin interfaces.TestApp //127.0.0.1/3 RECLAIM 80

#sleep 20

#echo "\n Restoring image file in Peer 4"

#java -classpath bin interfaces.TestApp //127.0.0.1/4 RESTORE "files/image1.png"

#sleep 20

#Delete

#echo "\n Deleting 120k file in Peer 3"

#java -classpath bin interfaces.TestApp //127.0.0.1/3 DELETE "files/test120k"

#sleep 5

#echo "\n Restoring 120k file in Peer 3, Should fail"

#java -classpath bin interfaces.TestApp //127.0.0.1/3 RESTORE "files/test120k"
