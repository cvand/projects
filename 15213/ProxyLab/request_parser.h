#include "headers.h"
#include "csapp.h"

#define PRINT
#define DEFAULT_PORT "80"

struct request{
	char *uri;
	char *hostname;
	char *port;
	char *path;
	char *query;
	char *method;
	char *version;
	Header header;
};

typedef struct request Request;

#ifdef PRINT
#define print_func() printf("-- %s --\n", __func__)
#else
#define print_func()
#endif

void parse_uri(char *uri, Request *req);
void read_requesthdrs(rio_t *rp, Request *req);
char *get_hostname(char *uri);
char *get_path(char *path);
char *get_query(char *query);
char *get_port(char *tok);
void create_request_hdrs(char *buf, Request *request);
void print_request(Request *request);
void prepare_request(Request *req, char *uri, char *method, char *version);
void send_request(int clientfd, Request *request);
void send_header(int clientfd, Request *request);
