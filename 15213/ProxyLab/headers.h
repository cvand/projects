#include "csapp.h"

struct header{
	char *host;
	char *user_agent;
	char *accept;
	char *accept_encoding;
	char *connection;
	char *proxy_connection;
	char other_headers[15][MAXLINE];
	int other_headers_counter;
};

typedef struct header Header;

void prepare_header(Header *header);
void set_header(char * header, Header *head);
int set_standard_header(char *key, char *value, Header *header);
void set_other_header(char *buf, Header *header);
int is_length_header(char *buf);
void print_header(Header header);
