/*
 * headers.c - Library that contains functions about the headers of a request
 */
#include "headers.h"

/* You won't lose style points for including these long lines in your code */
static char *host_hdr = "Host: www.cmu.edu\r\n";
static char *user_agent_hdr =
		"User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:10.0.3) Gecko/20120305 Firefox/10.0.3\r\n";
static char *accept_hdr =
		"Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n";
static char *accept_encoding_hdr = "Accept-Encoding: gzip, deflate\r\n";
static char *connection_hdr = "Connection: close\r\n";
static char *proxy_connection_hdr = "Proxy-Connection: close\r\n";

/*
 * prepare_header - Function for initializing a header object, setting default values
 */
void prepare_header(Header *header) {
	header->other_headers_counter = 0;
	header->host = host_hdr;
	header->user_agent = user_agent_hdr;
	header->accept = accept_hdr;
	header->accept_encoding = accept_encoding_hdr;
	header->connection = connection_hdr;
	header->proxy_connection = proxy_connection_hdr;

	return;
}

/*
 * set_header - Function that creates a header objects out of a string
 */
void set_header(char *header, Header *head) {
	char *buf = malloc(MAXLINE);
	strcpy(buf, header);

	char *key = strtok(buf, ": ");
	char *value = strtok(NULL, "");

	// if the header is one of the standard ones add it and return
	if (set_standard_header(key, value, head)) {
		return;
	}

	// if the header is additional, add it
	set_other_header(buf, head);
	return;
}

/*
 * set_standard_header - Function that sets on of the headers that are required in the header,
 * 							according to the handout
 */
int set_standard_header(char *key, char *value, Header *header) {

	if (!strcmp(key, "Host")) {
		// if host header found, change the value
		char host[MAXLINE] = "";
		strcat(host, "Host: ");
		strcat(host, value);

		header->host = malloc(sizeof(host));
		strcpy(header->host, host);

		// ignore any other standard header, keep the default value
	} else if ((!strcmp(key, "User-Agent")) || (!strcmp(key, "Accept"))
			|| (!strcmp(key, "Accept-Encoding")) || (!strcmp(key, "Connection"))
			|| (!strcmp(key, "Proxy-Connection"))) {
		return 1;
	} else {
		return 0;
	}

	return 1;
}

/*
 * set_other_header - Function that adds any additional header in the header object
 */
void set_other_header(char *buf, Header *header) {
	int counter = header->other_headers_counter;
	strcpy(header->other_headers[counter], buf);

	header->other_headers_counter++;
	return;
}

/*
 * is_length_header - Function that checks if the header is the Content-length header
 */
int is_length_header(char *buf) {
	char key[MAXLINE], value[MAXLINE];

	sscanf(buf, "%s %s\r\n", key, value);

	if (!strcmp(key, "Content-length:")) {
		int length = atoi(value);
		return length;
	}

	return 0;
}

/*
 * print_header - Helper function that prints header
 */
void print_header(Header header) {
	printf("-- Header --\n");
	printf("%s", header.host);
	printf("%s", header.user_agent);
	printf("%s", header.accept);
	printf("%s", header.accept_encoding);
	printf("%s", header.connection);
	printf("%s", header.proxy_connection);

	int i;
	for (i = 0; i < header.other_headers_counter; i++) {
		char *hdr = header.other_headers[i];
		printf("%s", hdr);
	}

	return;
}
