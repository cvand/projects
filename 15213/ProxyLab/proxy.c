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

///* You won't lose style points for including these long lines in your code */
//static const char *user_agent_hdr =
//		"User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:10.0.3) Gecko/20120305 Firefox/10.0.3\r\n";
//static const char *accept_hdr =
//		"Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n";
//static const char *accept_encoding_hdr = "Accept-Encoding: gzip, deflate\r\n";

void read_request(int fd);
void serve_static(int fd, char *filename, int filesize);
void get_filetype(char *filename, char *filetype);
void serve_dynamic(int fd, char *filename, char *cgiargs);
void clienterror(int fd, char *cause, char *errnum, char *shortmsg,
		char *longmsg);

void serve_request(int fd, rio_t *rio, char *method, char *uri, char *version);
void forward_request(int fd, Request *request);
int is_successful(char *buf);

int main(int argc, char **argv) {
	int listenfd, connfd;
	char hostname[MAXLINE], port[MAXLINE];
	socklen_t clientlen;
	struct sockaddr_storage clientaddr;

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
		connfd = Accept(listenfd, (SA *) &clientaddr, &clientlen);

		int rc;
		//gets a connected socket and returns the host name and port
		if ((rc = getnameinfo((SA *) &clientaddr, clientlen, hostname, MAXLINE,
				port, MAXLINE, 0)) != 0) {
			printf("getnameinfo failed\n");
			Close(connfd);
			continue;
		}

		printf("Accepted connection from (%s, %s)\n", hostname, port);

		read_request(connfd);
		Close(connfd);
	}
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
	if (!Rio_readlineb(&rio, buf, MAXLINE)) return;

	sscanf(buf, "%s %s %s", method, uri, version);
	print_func();
	printf("buf = %s", buf);
	printf("uri = %s\n", uri);

	if (strcasecmp(method, "GET")) {
		clienterror(fd, method, "501", "Not Implemented",
				"Proxy does not implement this method");
		return;
	}

	if (strstr(uri, "http:") != uri) {
		clienterror(fd, method, "501", "Not implemented protocol",
				"Proxy does not implement this protocol");
	}

	serve_request(fd, &rio, method, uri, version);
	return;
}
/* $end read_request */

void serve_request(int fd, rio_t *rio, char *method, char *uri, char *version) {
	Request req;

	print_func();

	prepare_request(&req, uri, method, version);
	/* Parse URI from GET request */
	parse_uri(uri, &req);

	read_requesthdrs(rio, &req);

	printf("CHECK HEADER\n");
	print_request(&req);
	printf("CHECK HEADER -- END\n");

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

	Rio_readlineb(&rio, buf, MAXLINE);
	if (is_successful(buf)) {
		printf("--- RESPONSE ----\n");
		while ((n = Rio_readlineb(&rio, buf, MAXLINE)) != 0) {
			printf("%s", buf);
			Rio_writen(fd, buf, strlen(buf));
		}

		printf("\n--- END of RESPONSE ----\n");
		Close(clientfd);
		return;
	}

	// if the response was not HTTP 200
	char key[MAXLINE], value[MAXLINE];
	do {
		Rio_readlineb(&rio, buf, MAXLINE);
		sscanf(buf, "%s %s\r\n", key, value);
		printf("loop %s\n", buf);
		if (!strcmp(key, "Location:")) {
			printf("Found location: %s\n", value);

//			Close(clientfd);
			// run new request with value as uri
			rio_t new_rio;
			Rio_readinitb(&new_rio, clientfd);
			serve_request(fd, &new_rio, "GET", value, "HTTP/1.0");
			break;
		}
	} while (strcmp(key, "Location:") && ((n = Rio_readlineb(&rio, buf, MAXLINE)) != 0));

	Close(clientfd);
	return;
}

int is_successful(char *buf) {
	char version[MAXLINE], message[MAXLINE], result[3];
	print_func();

	sscanf(buf, "%s %s %s", version, result, message);
	int i = atoi(result);

	printf("result: %d\n", i);

	if ( i >= 300)
		return 0;
	return 1;
}

/*
 * get_filetype - derive file type from file name
 */
void get_filetype(char *filename, char *filetype) {
	if (strstr(filename, ".html")) strcpy(filetype, "text/html");
	else if (strstr(filename, ".gif")) strcpy(filetype, "image/gif");
	else if (strstr(filename, ".png")) strcpy(filetype, "image/png");
	else if (strstr(filename, ".jpg")) strcpy(filetype, "image/jpeg");
	else strcpy(filetype, "text/plain");
}

/*
 * serve_static - copy a file back to the client
 */
/* $begin serve_static */
void serve_static(int fd, char *filename, int filesize) {
	int srcfd;
	char *srcp, filetype[MAXLINE], buf[MAXBUF];

	/* Send response headers to client */
	get_filetype(filename, filetype);          //line:netp:servestatic:getfiletype
	sprintf(buf, "HTTP/1.0 200 OK\r\n");          //line:netp:servestatic:beginserve
	sprintf(buf, "%sServer: Proxy Server\r\n", buf);
	sprintf(buf, "%sConnection: close\r\n", buf);
	sprintf(buf, "%sContent-length: %d\r\n", buf, filesize);
	sprintf(buf, "%sContent-type: %s\r\n\r\n", buf, filetype);
	Rio_writen(fd, buf, strlen(buf));          //line:netp:servestatic:endserve
	printf("Response headers:\n");
	printf("%s", buf);

	/* Send response body to client */
	srcfd = Open(filename, O_RDONLY, 0);          //line:netp:servestatic:open
	srcp = Mmap(0, filesize, PROT_READ, MAP_PRIVATE, srcfd, 0);          //line:netp:servestatic:mmap
	Close(srcfd);                          //line:netp:servestatic:close
	Rio_writen(fd, srcp, filesize);          //line:netp:servestatic:write
	Munmap(srcp, filesize);               //line:netp:servestatic:munmap
}
/* $end serve_static */

/*
 * serve_dynamic - run a CGI program on behalf of the client
 */
/* $begin serve_dynamic */
void serve_dynamic(int fd, char *filename, char *cgiargs) {
	char buf[MAXLINE], *emptylist[] = { NULL };

	/* Return first part of HTTP response */
	sprintf(buf, "HTTP/1.0 200 OK\r\n");
	Rio_writen(fd, buf, strlen(buf));
	sprintf(buf, "Server: Proxy Server\r\n");
	Rio_writen(fd, buf, strlen(buf));

	if (Fork() == 0) { /* Child */          //line:netp:servedynamic:fork
		/* Real server would set all CGI vars here */
		setenv("QUERY_STRING", cgiargs, 1);          //line:netp:servedynamic:setenv
		Dup2(fd, STDOUT_FILENO); /* Redirect stdout to client */          //line:netp:servedynamic:dup2
		Execve(filename, emptylist, environ); /* Run CGI program */          //line:netp:servedynamic:execve
	}
	Wait(NULL); /* Parent waits for and reaps child */          //line:netp:servedynamic:wait
}
/* $end serve_dynamic */

/*
 * clienterror - returns an error message to the client
 */
/* $begin clienterror */
void clienterror(int fd, char *cause, char *errnum, char *shortmsg,
		char *longmsg) {
	char buf[MAXLINE], body[MAXBUF];

	/* Build the HTTP response body */
	sprintf(body, "<html><title>Tiny Error</title>");
	sprintf(body, "%s<body bgcolor=" "ffffff" ">\r\n", body);
	sprintf(body, "%s%s: %s\r\n", body, errnum, shortmsg);
	sprintf(body, "%s<p>%s: %s\r\n", body, longmsg, cause);
	sprintf(body, "%s<hr><em>The Tiny Web server</em>\r\n", body);

	/* Print the HTTP response */
	sprintf(buf, "HTTP/1.0 %s %s\r\n", errnum, shortmsg);
	Rio_writen(fd, buf, strlen(buf));
	sprintf(buf, "Content-type: text/html\r\n");
	Rio_writen(fd, buf, strlen(buf));
	sprintf(buf, "Content-length: %d\r\n\r\n", (int) strlen(body));
	Rio_writen(fd, buf, strlen(buf));
	Rio_writen(fd, body, strlen(body));
}
/* $end clienterror */
