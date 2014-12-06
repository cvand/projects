/*
 * cache.c -- Library that implements functions for handling a cache.
 * 				The library implements adding and removing objects from the cache,
 * 				checking is objects exist in the cache and other helper functions.
 */
#include "cache.h"

/*
 * put_to_cache - Function that adds an object to the cache
 */
int put_to_cache(cache *cache, cache_o *new_obj) {
	int size = get_buffer_size(new_obj->buffer, new_obj->bufsize);

	//check if the size is not exceeded
	if (size > MAX_OBJECT_SIZE) {
		printf("Cannot save the page, too big\n");
		return -1;
	}

	int total_size = cache->cache_size + size;

	// check if cache has enough space
	if (total_size > MAX_CACHE_SIZE) {
		printf("Warning: Not enough space in cache. Applying LRU policy.\n");
		return apply_lru(cache, new_obj);

	}

	// semaphore lock
	P(&cache->w);

	cache->cache_size = total_size;
	// put the object at the start of the queue
	new_obj->next = cache->cache;
	cache->cache = new_obj;

	// semaphore unlock
	V(&cache->w);

	return 0;
}

/*
 * get_buffer_size - Function that calculates the size of the buffer
 */
int get_buffer_size(char **buffer, int bufsize) {
	int size = 0, i;

	for (i = 0; i < bufsize; i++) {
		size += strlen(buffer[i]);
	}

	return size;
}

/*
 * remove_from_cache - Function to remove an object from the cache
 */
int remove_from_cache(cache *cache, cache_o *c_o) {
	cache_o *obj = cache->cache;

	// no cached objects
	if (obj == NULL) {
		printf("Error: no cached objects to remove\n");
		return -1;
	}

	// only one cached_object
	if (obj->next == NULL) {
		if (!strcmp(c_o->uri, obj->uri)) {
			// write lock
			P(&cache->w);

			cache->cache_size = 0;
			cache->cache = NULL;

			// write unlock
			V(&cache->w);
			return 0;
		}
	}

	while (obj->next != NULL) {
		cache_o *next = obj->next;

		if (!strcmp(c_o->uri, next->uri)) {

			//semaphore write lock
			P(&cache->w);

			// make the predecessor point to the successor and remove size from the total size
			obj->next = next->next;
			cache->cache_size -= next->bufsize;

			//semaphore write unlock
			V(&cache->w);

			return 0;
		}

		obj = obj->next;
	}

	printf("Error: object not found in cache\n");
	return -1;
}

/*
 * is_in_cache - Function that checks if a cache object with the same uri exists in cache
 */
int is_in_cache(cache *cache, char *uri) {
	block_read(cache);

	// look for the uri of every cached_item
	cache_o *obj = cache->cache;

	while (obj != NULL) {

		// if uri found
		if (!strcmp(uri, obj->uri)) {
			unblock_read(cache);
			return 1;
		}

		unblock_read(cache);

		// go to next cache object
		obj = obj->next;
		block_read(cache);
	}

	unblock_read(cache);

	return 0;
}

/*
 * get_from_cache - Function that returns a cache object with the same uri as the input.
 * 					Since the object is accessed, it is put at the beginning of the cache
 * 					because it is the most recently accessed object.
 */
cache_o *get_from_cache(cache *cache, char *uri) {
	block_read(cache);

	cache_o *obj = cache->cache;

	while (obj != NULL) {

		cache_o *next = obj->next;

		if (next == NULL) {
			// if obj is the only element in the queue, chech its uri
			next = obj;
		}

		// if uri found
		if (!strcmp(uri, next->uri)) {
			unblock_read(cache);
			// put the object at the start of the queue
			remove_from_cache(cache, next);
			put_to_cache(cache, next);

			block_read(cache);

			return next;
		}

		unblock_read(cache);

		// go to next cache object
		obj = obj->next;
		block_read(cache);
	}

	unblock_read(cache);

	printf("Error: No item found in cache with such uri\n");
	return NULL;
}

/*
 * block_read - Function that handles the mutex read block
 */
void block_read(cache *cache) {
	P(&cache->mutex);
	cache->readers++;
	if (cache->readers == 1) {
		P(&cache->w);
	}
	V(&cache->mutex);
	return;
}

/*
 * unblock_read - Function that handles the mutex read unblock
 */
void unblock_read(cache *cache) {
	P(&cache->mutex);
	cache->readers--;
	if (cache->readers == 0) {
		V(&cache->w);
	}
	V(&cache->mutex);
	return;
}

/*
 * apply_lru - Function that finds the least recently used cache object in the queue, which
 * 				is the last object in the queue (the one with NULL next object), removes it
 * 				from the queue and adds the new one
 */
int apply_lru(cache *cache, cache_o *new_obj) {
	cache_o *obj = cache->cache;

	while (obj != NULL) {

		// find the last object in cache queue
		if (obj->next == NULL) {
			remove_from_cache(cache, obj);
			put_to_cache(cache, new_obj);
			return 0;
		}

		obj = obj->next;
	}

	printf("Error: apply_lru failed\n");
	return -1;
}

/*
 * initialize_cache - Function that initializes the variables of the struct
 */
void initialize_cache(cache *cache) {
	cache->cache_size = 0;
	cache->readers = 0;
	cache->cache = NULL;
	Sem_init(&(cache->mutex), 0, 1);
	Sem_init(&(cache->w), 0, 1);
	return;
}

/*
 * initialize_cache_object - Function that initializes the variables of the cache object struct
 */
void initialize_cache_object(cache_o *obj) {
	// malloc the arrays
	obj->uri = Malloc(MAXLINE);

	// initial size of the buffer is 50 rows of strings
	obj->buffer = Malloc(50 * (sizeof(char *)));

	// every string is size of RIO_BUFSIZE
	int i;
	for (i = 0; i < 50; i++) {
		obj->buffer[i] = Malloc(RIO_BUFSIZE);
	}

	obj->bufsize = 50;
	obj->next = NULL;

	return;
}

/*
 * print_cache - Helper function for printing the cache
 */
void print_cache(cache *cache) {
	if (cache == NULL) {
		printf("Cache is null\n");
		return;
	}

	cache_o *obj = cache->cache;
	printf("Cache Size: %d\n", cache->cache_size);

	while (obj != NULL) {
		print_cache_object(obj);
		obj = obj->next;
	}
	return;
}

/*
 * print_cache_object - Helper function for printing a cache object
 */
void print_cache_object(cache_o *obj) {
	printf("Cache object:\n\tURI: %s\n\tSIZE: %d\n", obj->uri, obj->bufsize);
	printf("\tCONTENT:");
	int i;
	for (i = 0; i < obj->bufsize; i++) {
		printf("%s", obj->buffer[i]);
	}
	printf("\n\n");
	return;
}
