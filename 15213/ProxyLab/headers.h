#include "csapp.h"

#define PRINT

struct header{
	char *host;
	char *user_agent;
	char *accept;
	char *accept_encoding;
	char *connection;
	char *proxy_connection;
	char **other_headers;
	int other_headers_counter;
};

typedef struct header Header;

#ifdef PRINT
#define print_func() printf("-- %s --\n", __func__)
#else
#define print_func()
#endif

void prepare_header(Header *header);
void set_header(char * header, Header *head);
int set_standard_header(char *key, char *value, Header *header);
void set_other_header(char *buf, Header *header);
void print_header(Header header);
