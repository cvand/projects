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

void prepare_header(Header *header) {
	print_func();

	header->other_headers_counter = 0;
	header->host = host_hdr;
	header->user_agent = user_agent_hdr;
	header->accept = accept_hdr;
	header->accept_encoding = accept_encoding_hdr;
	header->connection = connection_hdr;
	header->proxy_connection = proxy_connection_hdr;

	return;
}

void set_header(char *header, Header *head) {
	char *buf = malloc(MAXLINE);
	strcpy(buf, header);

	char *key = strtok(buf, ": ");
	char *value = strtok(NULL, "");

	if (set_standard_header(key, value, head)) {
		return;
	}

	set_other_header(buf, head);
	return;
}

int set_standard_header(char *key, char *value, Header *header) {

	if (!strcmp(key, "Host")) {
		char host[MAXLINE] = "";
		strcat(host, "Host: ");
		strcat(host, value);

		header->host = malloc(sizeof(host));
		strcpy(header->host, host);

	} else if ((!strcmp(key, "User-Agent")) || (!strcmp(key, "Accept"))
			|| (!strcmp(key, "Accept-Encoding")) || (!strcmp(key, "Connection"))
			|| (!strcmp(key, "Proxy-Connection"))) {
		return 1;
	} else {
		return 0;
	}

	return 1;
}

void set_other_header(char *buf, Header *header) {
	print_func();
	int counter = header->other_headers_counter;
	printf("counter %d\n", counter);
	printf("buf header %s\n", buf);

//	header->other_headers[counter] = malloc(sizeof(buf));

	strcpy(header->other_headers[counter], buf);

	header->other_headers_counter++;
	return;
}

int is_length_header(char *buf) {
	print_func();
	char key[MAXLINE], value[MAXLINE];

	sscanf(buf, "%s %s\r\n", key, value);

	if (!strcmp(key, "Content-length:")) {
		int length = atoi(value);
		printf("parsed length %d\n", length);
		return length;
	}

	return 0;
}

void print_header(Header header) {
//	if (header == NULL) return;
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

void copy_header(Header *dst, Header *src) {
	strcpy(dst->host, src->host);
	strcpy(dst->user_agent, src->user_agent);
	strcpy(dst->accept, src->accept);
	strcpy(dst->accept_encoding, src->accept_encoding);
	strcpy(dst->connection, src->connection);
	strcpy(dst->proxy_connection, src->proxy_connection);
//	if (src->other_headers != NULL) {
//		strcpy(dst->other_headers, src->other_headers);
//	}
	dst->other_headers_counter = src->other_headers_counter;
	return;
}
