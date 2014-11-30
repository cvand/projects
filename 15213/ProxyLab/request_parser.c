#include "request_parser.h"

#define VERSION " HTTP/1.0"
#define METHOD "GET "
/*
 * parse_uri - parse URI into filename and CGI args
 *             return 0 if dynamic content, 1 if static
 */
/* $begin parse_uri */
void parse_uri(char *uri, Request *req) {

	print_func();

	//create a copy of uri for splitting it
	char url[MAXLINE];
	strcpy(url, uri);

	// split string by : twice to get the second part of the split string
	char *saveptr;
	char *tok = strtok_r(url, ":", &saveptr);
	tok = strtok_r(NULL, "", &saveptr);

	char *path, *query;
	path = malloc(sizeof(url));
	query = malloc(sizeof(url));
	strcpy(path, tok);
	strcpy(query, tok);

	// get hostname from substring
	char *host = get_hostname(tok);
	req->hostname = malloc(sizeof(host));
	strcpy(req->hostname, host);

	// get port number from substring
	char *port = get_port(tok);
	req->port = malloc(sizeof(port));
	strcpy(req->port, port);

	// get path from substring
	path = get_path(path);
	req->path = malloc(sizeof(path));
	strcpy(req->path, path);

	// get query from substring
	query = get_query(query);
	req->query = malloc(sizeof(query));
	strcpy(req->query, query);

	return;
}
/* $end parse_uri */

void create_request_hdrs(char *buf, Request *request) {
	char h[RIO_BUFSIZE];
	memset(h, 0, RIO_BUFSIZE);
	Header header = request->header;
	strcat(h, header.host);
	strcat(h, header.user_agent);
	strcat(h, header.accept);
	strcat(h, header.accept_encoding);
	strcat(h, header.connection);
	strcat(h, header.proxy_connection);

	int i;
	for (i = 0; i < header.other_headers_counter; i++) {
		char *hdr = header.other_headers[i];
		strcat(h, hdr);
	}

	strcat(h, "\r\n");
//	buf = malloc(sizeof(h));
	strcpy(buf, h);
}

/*
 * read_requesthdrs - read HTTP request headers
 */
/* $begin read_requesthdrs */
void read_requesthdrs(rio_t *rp, Request *req) {
	char buf[RIO_BUFSIZE];

	print_func();
	prepare_header(&(req->header));

	do {
		Rio_readlineb(rp, buf, RIO_BUFSIZE);
		if ((strlen(buf) == 0) || (!strcmp(buf, "\r\n"))) {
			break;
		}
		set_header(buf, &(req->header));

	} while ((strlen(buf) > 0) || (strcmp(buf, "\r\n")));

	return;
}
/* $end read_requesthdrs */

void set_requesthdrs(char *rio_buf, Request *req) {
	print_func();
	prepare_header(&(req->header));

	char buf[RIO_BUFSIZE];
	strcpy(buf, rio_buf);

	char *header, *save;
	header = strtok_r(buf, "\r\n", &save);
	do {

		set_header(strcat(header, "\r\n"), &(req->header));
	} while ((header = strtok_r(NULL, "\r\n", &save)) != NULL);

	return;
}

void prepare_request(Request *req, char *uri, char *method, char *version) {
	print_func();

	req->uri = malloc(MAXLINE);
	req->method = malloc(MAXLINE);
	req->version = malloc(MAXLINE);

	strcpy(req->uri, uri);
	strcpy(req->method, method);
	strcpy(req->version, version);
	return;
}

void send_request(int clientfd, Request *request) {
	char buf[RIO_BUFSIZE];

	print_func();

	memset(buf, 0, sizeof(buf));

	//create the request to send
	strcat(buf, "");
	strcat(buf, METHOD);

	strcat(buf, "/");
	if ((request->path != NULL) && (strcmp(request->path, ""))) {
		strcat(buf, request->path);
	}

	if ((request->query != NULL) && (strcmp(request->query, ""))) {

		strcat(buf, "?");
		strcat(buf, request->query);
	}

//	strcat(buf, "\r\n");

	strcat(buf, VERSION);
	strcat(buf, "\r\n");

	printf("%s %d - buf: %s", __func__, __LINE__, buf);

	Rio_writen(clientfd, buf, strlen(buf));
//	Rio_readinitb(&rio, clientfd);
//	Fputs(buf, stdout);

	return;
}

void send_header(int clientfd, Request *request) {
	char buf[RIO_BUFSIZE];

	print_func();

	//copy headers to the rio buffer
	create_request_hdrs(buf, request);

	Rio_writen(clientfd, buf, strlen(buf));
	printf("end %s\n", __func__);
	return;
}

char *get_hostname(char *uri) {
	char *host, *saveptr;

	print_func();

	// make a copy of the substring of uri
	char temp[MAXLINE];
	strcpy(temp, uri);
	// remove the / characters
	host = strtok_r(temp, "/", &saveptr);
	if (strstr(host, ":") != NULL) {
		host = strtok_r(host, ":", &saveptr);
	}

	return host;
}

char *get_port(char *tok) {
	char *port, *saveptr;

	print_func();
	// if the token of the uri contains :, it means that there is a port number
	if (strstr(tok, ":") != NULL) {
		// split the string and get the substring after :
		tok = strtok_r(tok, ":", &saveptr);
		tok = strtok_r(NULL, ":", &saveptr);

		// remove the / to get the port number
		port = strtok_r(tok, "/", &saveptr);
	} else {
		port = malloc(sizeof(int));
		sprintf(port, "%s", DEFAULT_PORT);
	}

	return port;
}

char *get_path(char *path) {
	char *p, *saveptr;

	print_func();

	// remove hostname
	p = strtok_r(path, "/", &saveptr);

	if ((p = strtok_r(NULL, "", &saveptr)) != NULL) {
		// if the request has arguments, remove them
		char *c;
		if ((c = strstr(p, "?")) != NULL) {
			if (c == p) {
				p = "";
			} else {
				p = strtok_r(p, "?", &saveptr);
			}
		}
	} else {
		p = "";
	}

	return p;
}

char *get_query(char *query) {
	char *q, *saveptr;
	q = malloc(MAXLINE * sizeof(char));

	print_func();

	q = strtok_r(query, "/", &saveptr);

	// if there is a path
	if ((q = strtok_r(NULL, "/", &saveptr)) != NULL) {
		if (strstr(q, "?") != NULL) {
			// split the string and get the substring after ?
			q = strtok_r(q, "?", &saveptr);
		} else {
			q = "";
		}
	} else {
		q = "";
	}

	return q;
}

void print_request(Request *request) {
	if (request == NULL) return;
	printf("-- Request --\n");
	printf("uri: %s\n", request->uri);
	printf("hostanme: %s\n", request->hostname);
	printf("port: %s\n", request->port);
	printf("path: %s\n", request->path);
	printf("query: %s\n", request->query);
	printf("method: %s\n", request->method);
	printf("version: %s\n", request->version);
	print_header(request->header);
}
