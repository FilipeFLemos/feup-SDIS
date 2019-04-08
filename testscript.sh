java -classpath bin interfaces.TestApp //127.0.0.1/1 BACKUP "files/test1b" 2

sleep(5)

java -classpath bin interfaces.TestApp //127.0.0.1/1 BACKUP "files/test10b" 2

sleep(5)

java -classpath bin interfaces.TestApp //127.0.0.1/2 BACKUP "files/test1k" 2

sleep(5)

java -classpath bin interfaces.TestApp //127.0.0.1/2 BACKUP "files/test10k" 2

sleep(5)

java -classpath bin interfaces.TestApp //127.0.0.1/3 BACKUP "files/test100k" 2

sleep(5)

java -classpath bin interfaces.TestApp //127.0.0.1/3 BACKUP "files/test1M" 2

sleep(5)

java -classpath bin interfaces.TestApp //127.0.0.1/4 BACKUP "files/test10M" 2

sleep(5)

java -classpath bin interfaces.TestApp //127.0.0.1/4 BACKUP "files/image1.png" 2
