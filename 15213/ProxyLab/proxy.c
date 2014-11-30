#include <stdio.h>
#include "csapp.h"
#include "request_parser.h"

/* Recommended max cache and object sizes */
#define MAX_CACHE_SIZE 1049000
#define MAX_OBJECT_SIZE 102400

#define PRINT

#ifdef PRINT
#define print_func() printf("-- %s --\n", __func__)
#else
#define print_func()
#endif

void *thread(void *vargp);
void read_request(int fd);
void clienterror(int fd, char *cause, char *errnum, char *shortmsg,
		char *longmsg);

void serve_request(int fd, rio_t *rio, char *method, char *uri, char *version,
		int is_fwd);
void forward_request(int fd, Request *request);
int is_successful(char *buf);
void check_Rio_writen(int fd, char *buf, size_t length);

void sigpipe_handler(int sig);

struct args {
	struct sockaddr_storage clientaddr;
	socklen_t clientlen;
	int *connfd;
};

int main(int argc, char **argv) {
	int listenfd, *connfd;
	socklen_t clientlen;
	struct sockaddr_storage clientaddr;
	pthread_t tid;

	Signal(SIGPIPE, sigpipe_handler);

	/* Check command line args */
	if (argc != 2) {
		fprintf(stderr, "usage: %s <port>\n", argv[0]);
		exit(1);
	}
	// initialize connection
	listenfd = Open_listenfd(argv[1]);
	while (1) {
		clientlen = sizeof(clientaddr);

		// ready to accept requests
		connfd = Malloc(sizeof(int));
		*connfd = Accept(listenfd, (SA *) &clientaddr, &clientlen);

		struct args arg;
		arg.clientaddr = clientaddr;
		arg.clientlen = clientlen;
		arg.connfd = connfd;

		Pthread_create(&tid, NULL, thread, &arg);
	}
}

/* Thread routine */
void *thread(void *vargp) {
	struct args arg = *((struct args *) vargp);

	int connfd = *arg.connfd;
	Pthread_detach(pthread_self());
	char hostname[MAXLINE], port[MAXLINE];

	printf("Accepted!\n");
	int rc;
	//gets a connected socket and returns the host name and port
	if ((rc = getnameinfo((SA *) &arg.clientaddr, arg.clientlen, hostname, MAXLINE,
			port, MAXLINE, 0)) != 0) {
		printf("getnameinfo failed\n");
		Close(connfd);
		return NULL;
	}

	read_request(connfd);

	Close(connfd);
//	Free(vargp);
	return NULL;
}

/*
 * read_request - handle one HTTP request/response transaction
 */
/* $begin read_request */
void read_request(int fd) {
	char buf[MAXLINE], method[MAXLINE], uri[MAXLINE], version[MAXLINE];
	rio_t rio;

	/* Read request line and headers */
	Rio_readinitb(&rio, fd);
	Rio_readlineb(&rio, buf, MAXLINE);

	sscanf(buf, "%s %s %s", method, uri, version);
	print_func();

	if (strcasecmp(method, "GET")) {
		clienterror(fd, method, "501", "Not Implemented",
				"Proxy does not implement this method");
		return;
	}

	if (strstr(uri, "http:") != uri) {
		clienterror(fd, method, "501", "Not implemented protocol",
				"Proxy does not implement this protocol");
		return;
	}

	serve_request(fd, &rio, method, uri, version, 0);
	return;
}
/* $end read_request */

void serve_request(int fd, rio_t *rio, char *method, char *uri, char *version,
		int is_fwd) {
	Request req;

	print_func();

	prepare_request(&req, uri, method, version);
	/* Parse URI from GET request */
	parse_uri(uri, &req);

	if (!is_fwd) {
		read_requesthdrs(rio, &req);
	} else {
		set_requesthdrs(rio->rio_buf, &req);
	}
	forward_request(fd, &req);

	return;
}

void forward_request(int fd, Request *request) {
	char host[MAXLINE], port[MAXLINE], buf[MAXLINE];
	int clientfd;
	size_t n;
	rio_t rio;

	print_func();
	print_request(request);
	printf("------------\n\n");

	// set host and port for the outgoing request
	strcpy(port, DEFAULT_PORT);
	if (request->port != NULL) {
		strcpy(port, request->port);
	}

	strcpy(host, request->hostname);

	printf("****  host: %s \t port: %s  ****\n", host, port);

	// creating a client to forward the outgoing request to the actual server
	clientfd = Open_clientfd(host, port);

	send_request(clientfd, request);
	send_header(clientfd, request);

	Rio_readinitb(&rio, clientfd);
	printf("%d\n", __LINE__);

	Rio_readlineb(&rio, buf, MAXLINE);
	printf("%d\n", __LINE__);

	if (is_successful(buf)) {
		printf("--- RESPONSE ----\n");

		check_Rio_writen(fd, buf, strlen(buf));

		while ((n = Rio_readlineb(&rio, buf, MAXLINE)) != 0) {
			printf("%s", buf);
			check_Rio_writen(fd, buf, strlen(buf));

			if (n < 0) {
				printf("Error reading from fd. \t errno: %d\n", errno);
				Close(clientfd);
				return;
			}
		}
		printf("\n--- END of RESPONSE ----\n");

		Close(clientfd);
		return;
	}

	// if the response was HTTP 300
	char key[MAXLINE], value[MAXLINE];

	if (((n = Rio_readlineb(&rio, buf, MAXLINE)) < 0)
			&& (errno == ECONNRESET)) {
		Close(clientfd);
		return;
	}

	do {
		if (n < 0) {
			Close(clientfd);
			return;
		}
		sscanf(buf, "%s %s\r\n", key, value);

		if (!strcmp(key, "Location:")) {
			printf("Found location: %s\n", value);

			// run new request with value as uri
			rio_t new_rio;

			// put in rio buffer the host of the previous request
			strcpy(new_rio.rio_buf, request->header.host);
			strcat(new_rio.rio_buf, "\r\n");

			Rio_readinitb(&new_rio, clientfd);

			serve_request(fd, &new_rio, "GET", value, "HTTP/1.0", 1);
			break;
		}
	} while (strcmp(key, "Location:")
			&& ((n = Rio_readlineb(&rio, buf, MAXLINE)) != 0));

	Close(clientfd);
	return;
}

int is_successful(char *buf) {
	printf("%d\n", __LINE__);
	char version[MAXLINE], message[MAXLINE], result[3];
	print_func();

	printf("%d\n", __LINE__);
	sscanf(buf, "%s %s %s", version, result, message);
	int i = atoi(result);

	if ((i >= 300) && (i < 400)) return 0;
	printf("%d\n", __LINE__);
	return 1;
}

/*
 * clienterror - returns an error message to the client
 */
/* $begin clienterror */
void clienterror(int fd, char *cause, char *errnum, char *shortmsg,
		char *longmsg) {
	char buf[MAXLINE], body[MAXBUF];

	/* Build the HTTP response body */
	sprintf(body, "<html><title>Proxy Error</title>");
	sprintf(body, "%s<body bgcolor=" "ffffff" ">\r\n", body);
	sprintf(body, "%s%s: %s\r\n", body, errnum, shortmsg);
	sprintf(body, "%s<p>%s: %s\r\n", body, longmsg, cause);
	sprintf(body, "%s<hr><em>The Tiny Web server</em>\r\n", body);

	/* Print the HTTP response */
	sprintf(buf, "HTTP/1.0 %s %s\r\n", errnum, shortmsg);
	check_Rio_writen(fd, buf, strlen(buf));
	sprintf(buf, "Content-type: text/html\r\n");
	check_Rio_writen(fd, buf, strlen(buf));
	sprintf(buf, "Content-length: %d\r\n\r\n", (int) strlen(body));
	check_Rio_writen(fd, buf, strlen(buf));
	check_Rio_writen(fd, body, strlen(body));
	return;
}
/* $end clienterror */

void check_Rio_writen(int fd, char *buf, size_t length) {
	if (rio_writen(fd, buf, length) != length) {
		printf("check_Rio_writen error\n");
		return;
	}
}

void sigpipe_handler(int sig) {
	printf("sigpipe error\n");
	return;
}
