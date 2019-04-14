#Backups

echo "Testing Backups \n \n Backing up 10k file in Peer 1"

java -classpath bin interfaces.TestApp //127.0.0.1/1 BACKUP "files/test10k" 2

sleep 5

echo "\n Backing up 100k file in Peer 2"

java -classpath bin interfaces.TestApp //127.0.0.1/2 BACKUP "files/test100k" 2

sleep 7

echo "\n Backing up image file in Peer 3"

java -classpath bin interfaces.TestApp //127.0.0.1/3 BACKUP "files/image1.png" 2

sleep 5