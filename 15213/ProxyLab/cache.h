#include "csapp.h"
#include <time.h>

/* Recommended max cache and object sizes */
#define MAX_CACHE_SIZE 1049000
#define MAX_OBJECT_SIZE 102400

struct cache_object {
		char *uri;
		char **buffer;
		int bufsize;

		struct cache_object *next;
};

typedef struct cache_object cache_o;

struct s_cache {
		cache_o *cache;
		int cache_size;
		int readers;
		sem_t mutex, w;
};

typedef struct s_cache cache;

int put_to_cache(cache *cache, cache_o *new_obj);
int remove_from_cache(cache *cache, cache_o *c_o);
int is_in_cache(cache *cache, char *uri);
cache_o *get_from_cache(cache *cache, char *uri);

int apply_lru(cache *cache, cache_o *new_obj);
void initialize_cache(cache *cache);
void initialize_cache_object(cache_o *obj);
void print_cache(cache *cache);
void print_cache_object(cache_o *obj);
int get_buffer_size(char **buffer, int bufsize);

void block_read(cache *cache);
void unblock_read(cache *cache);
