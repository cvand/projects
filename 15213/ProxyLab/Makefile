#
# Makefile for Proxy Lab 
#
# You may modify is file any way you like (except for the handin
# rule). Autolab will execute the command "make" on your specific 
# Makefile to build your proxy from sources.
#
CC = gcc
CFLAGS = -g -Wall
LDFLAGS = -lpthread

all: proxy

csapp.o: csapp.c csapp.h
	$(CC) $(CFLAGS) -c csapp.c

headers.o: headers.c headers.h
	$(CC) $(CFLAGS) -c headers.c

request_parser.o: request_parser.c request_parser.h csapp.h
	$(CC) $(CFLAGS) -c request_parser.c

proxy.o: proxy.c csapp.h request_parser.h
	$(CC) $(CFLAGS) -c proxy.c

proxy: proxy.o csapp.o request_parser.o headers.o

# Creates a tarball in ../proxylab-handin.tar that you should then
# hand in to Autolab. DO NOT MODIFY THIS!
handin:
	(make clean; cd ..; tar cvf proxylab-handin.tar proxylab-handout --exclude tiny --exclude nop-server.py --exclude proxy --exclude driver.sh --exclude port-for-user.pl --exclude free-port.sh --exclude ".*")

clean:
	rm -f *~ *.o proxy core *.tar *.zip *.gzip *.bzip *.gz

