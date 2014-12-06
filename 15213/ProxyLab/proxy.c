/*
 * Proxy.c -- This is an implementation of a proxy server. It takes incoming requests, and
 * forwards them to the appropriate host and returns the response back to the client that
 * sent the request. It also implements a cache for the requests that uses LRU policy.
 *
 */
#include <stdio.h>
#include "csapp.h"
#include "request_parser.h"		// library for parsing requests
#include "cache.h"				// library for cache functions

void *thread(void *vargp);
void read_request(int fd);
void clienterror(int fd, char *cause, char *errnum, char *shortmsg,
		char *longmsg);

void serve_request(int fd, rio_t *rio, char *method, char *uri, char *version,
		int is_fwd);
void forward_request(int fd, Request *request);
int is_successful(char *buf);
void check_Rio_writen(int fd, char *buf, size_t length);

int Open_clientfd_w(char *hostname, char *port);

// struct to hold the arguments passed to every thread
struct args {
	struct sockaddr_storage clientaddr;
	socklen_t clientlen;
	int *connfd;
};

// cache object
struct s_cache *c;

int main(int argc, char **argv) {
	int listenfd, *connfd;
	socklen_t clientlen;
	struct sockaddr_storage clientaddr;
	pthread_t tid;

	// initialize cache
	c = Malloc(sizeof(struct s_cache));
	initialize_cache(c);

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

		//prepare arguments for thread
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
	// detach thread from parent
	Pthread_detach(pthread_self());
	char hostname[MAXLINE], port[MAXLINE];

	printf("Accepted request\n");

	int rc;

	//get a connected socket and return the host name and port
	if ((rc = getnameinfo((SA *) &arg.clientaddr, arg.clientlen, hostname,
	MAXLINE, port, MAXLINE, 0)) != 0) {
		printf("getnameinfo failed\n");
		Close(connfd);
		return NULL;
	}

	read_request(connfd);

	Close(connfd);
	return NULL;
}

/*
 * read_request - handle one HTTP request/response transaction
 */
/* $begin read_request */
void read_request(int fd) {
	char buf[MAXLINE], method[MAXLINE], uri[MAXLINE], version[MAXLINE];
	rio_t rio;

	/* Read request line */
	Rio_readinitb(&rio, fd);
	Rio_readlineb(&rio, buf, MAXLINE);

	sscanf(buf, "%s %s %s", method, uri, version);

	// if the request is not GET then do not serve it
	if (strcasecmp(method, "GET")) {
		clienterror(fd, method, "501", "Not Implemented",
				"Proxy does not implement this method");
		return;
	}

	// if the request is not HTTP then do not serve it
	if (strstr(uri, "http:") != uri) {
		clienterror(fd, method, "501", "Not implemented protocol",
				"Proxy does not implement this protocol");
		return;
	}

	// if this uri is already in the cache
	if (is_in_cache(c, uri)) {
		cache_o *obj = Malloc(sizeof(cache_o));
		initialize_cache_object(obj);

		// get the object from cache
		obj = get_from_cache(c, uri);

		int bufsize = obj->bufsize;

		// send the content to the client
		int i;
		for (i = 0; i < bufsize; i++) {
			char *buf = obj->buffer[i];
			check_Rio_writen(fd, buf, strlen(buf));

		}

	} else {
		//forward the request to server
		serve_request(fd, &rio, method, uri, version, 0);
	}

	return;
}
/* $end read_request */

/*
 * serve_request - get the information from the request and forward the request to a server
 */
void serve_request(int fd, rio_t *rio, char *method, char *uri, char *version,
		int is_fwd) {
	Request req;

	prepare_request(&req, uri, method, version);

	/* Parse URI from request and put all information on Request object*/
	parse_uri(uri, &req);

	if (!is_fwd) {
		// if the incoming request is a redirected request
		read_requesthdrs(rio, &req);
	} else {
		set_requesthdrs(rio->rio_buf, &req);
	}

	// forward the request
	forward_request(fd, &req);

	return;
}

/*
 * forward_request - Function that given a request creates a client
 * 					 and sends the request to the server.
 */
void forward_request(int fd, Request *request) {
	char host[MAXLINE], port[MAXLINE], buf[MAXLINE];
	int clientfd;
	size_t n;
	rio_t rio;

	// set host and port for the outgoing request
	strcpy(port, DEFAULT_PORT);
	if (request->port != NULL) {
		strcpy(port, request->port);
	}

	strcpy(host, request->hostname);

	printf("****  host: %s \t port: %s  ****\n", host, port);

	// create a client to forward the outgoing request to the actual server
	clientfd = Open_clientfd_w(host, port);
	if (clientfd < 0) {
		printf("Error: connection refused\n");
		return;
	}

	// send the request
	send_request(clientfd, request);
	//send the headers
	send_header(clientfd, request);

	Rio_readinitb(&rio, clientfd);

	// read the response
	Rio_readlineb(&rio, buf, MAXLINE);

	// if the response is 200
	if (is_successful(buf)) {
		//save the response to a cache object
		cache_o *cache_object = Malloc(sizeof(cache_o));
		int buf_lines = 0, buf_size = 50;
		initialize_cache_object(cache_object);

		// save the uri
		strcpy(cache_object->uri, request->uri);

		// send the response to the client
		check_Rio_writen(fd, buf, strlen(buf));

		// The buffer of the cache object starts with a size of 50 * RIO_BUFSIZE
		strcpy(cache_object->buffer[buf_lines], buf);
		buf_lines++;

		while ((n = Rio_readlineb(&rio, buf, MAXLINE)) > 0) {
			// for every line read by the server, send to the client
			check_Rio_writen(fd, buf, n);

			if (buf_lines == buf_size) {
				//allocate more space for buffer
				int new_size = buf_size + 25;
				cache_object->buffer = realloc(cache_object->buffer,
						new_size * (sizeof(char *)));
				int k;
				for (k = buf_size; k < new_size; k++) {
					cache_object->buffer[k] = Malloc(RIO_BUFSIZE);
				}

				buf_size = new_size;
			}

			strcpy(cache_object->buffer[buf_lines], buf);
			buf_lines++;
		}

		cache_object->bufsize = buf_lines;
		// add the cache object to the cache
		put_to_cache(c, cache_object);

		Close(clientfd);
		return;
	}

	// if the response was HTTP 300
	char key[MAXLINE], value[MAXLINE];

	// read the response until we find the Location header fror the redirection
	if (((n = Rio_readlineb(&rio, buf, MAXLINE)) < 0)
			&& (errno == ECONNRESET)) {
		Close(clientfd);
		return;
	}

	do {
		sscanf(buf, "%s %s\r\n", key, value);
		// if the header is Location
		if (!strcmp(key, "Location:")) {
			// run new request with value as uri
			rio_t new_rio;

			// put in rio buffer the host of the previous request
			strcpy(new_rio.rio_buf, request->header.host);
			strcat(new_rio.rio_buf, "\r\n");

			Rio_readinitb(&new_rio, clientfd);

			// serve the request with the forward flag set to 1
			serve_request(fd, &new_rio, "GET", value, "HTTP/1.0", 1);
			break;
		}
	} while (strcmp(key, "Location:")
			&& ((n = Rio_readlineb(&rio, buf, MAXLINE)) > 0));

	Close(clientfd);
	return;
}

/*
 * is_successful - Function that checks if the buffer containing the response has 200 code
 */
int is_successful(char *buf) {
	char version[MAXLINE], message[MAXLINE], result[3];

	sscanf(buf, "%s %s %s", version, result, message);
	int i = atoi(result);

	if ((i >= 300) && (i < 400)) return 0;

	return 1;
}

/*
 * Open_clientfd_w - Wrapper function for creation of a client
 */
int Open_clientfd_w(char *hostname, char *port) {
	int rc;

	if ((rc = open_clientfd(hostname, port)) < 0) printf("Open_clientfd error");
	return rc;
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

/*
 * check_Rio_written - Wrapper function for rio_written
 */
void check_Rio_writen(int fd, char *buf, size_t length) {
	if (rio_writen(fd, buf, length) != length) {
		printf("check_Rio_writen error\n");
		return;
	}
}
