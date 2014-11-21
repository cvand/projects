/*
 * mm.c
 *
 * NOTE TO STUDENTS: Replace this header comment with your own header
 * comment that gives a high level description of your solution.
 */
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "mm.h"
#include "memlib.h"

/* If you want debugging output, use the following macro.  When you hand
 * in, remove the #define DEBUG line. */
#define DEBUGx
#define PRINTx
#define PRINTALLx

#ifdef PRINTALL
#define dbg_a_printf(...) printf(__VA_ARGS__)
#else
#define dbg_a_printf(...)
#endif

#ifdef PRINT
#define dbg_printf(...) printf(__VA_ARGS__)
#define dbg_print_free_list() print_free_list()
#define dbg_print_free_block(...) print_free_block(__VA_ARGS__)
#define dbg_print_block(...) printblock(__VA_ARGS__)
#define dbg_print_neighbor_info(...) print_neighbor_info(__VA_ARGS__)
#else
#define dbg_print_free_list()
#define dbg_print_free_block(...)
#define dbg_print_block(...)
#define dbg_print_neighbor_info(...)
#define dbg_printf(...)
#endif

#ifdef DEBUG
#define CHECKHEAP(verbose) printf("------ %s - line %d ------\n", __func__, verbose); mm_checkheap(verbose);
#define dbg_assert(...) assert(__VA_ARGS__);
#else
#define CHECKHEAP(verbose)
#define dbg_assert(...)
#endif

/* Global variables */
static char *heap_listp = 0; /* Pointer to first block */
static char *heap_end = 0; /* Pointer to end of heap */
static unsigned long *free_listp = 0; /* Pointer to first free block */

/* do not change the following! */
#ifdef DRIVER
/* create aliases for driver tests */
#define malloc mm_malloc
#define free mm_free
#define realloc mm_realloc
#define calloc mm_calloc
#endif /* def DRIVER */

/* $begin mallocmacros */
/* Basic constants and macros */
#define WSIZE       4       /* Word and header/footer size (bytes) */ //line:vm:mm:beginconst
#define DSIZE       8       /* Doubleword size (bytes) */
#define CHUNKSIZE  (1<<12)  /* Extend heap by this amount (bytes) */  //line:vm:mm:endconst
#define MIN_BLOCK_SIZE 24

#define MAX(x, y) ((x) > (y)? (x) : (y))

/* Pack a size and allocated bit into a word */
#define PACK(size, alloc)  ((size) | (alloc)) //line:vm:mm:pack

/* Read and write a word at address p */
#define GET(p)       (*(unsigned int *)(p))            //line:vm:mm:get
#define PUT(p, val)  (*(unsigned int *)(p) = (val))    //line:vm:mm:put

/* Read the size and allocated fields from address p */
#define GET_SIZE(p)  (GET(p) & ~0x7)                   //line:vm:mm:getsize
#define GET_ALLOC(p) (GET(p) & 0x1)                    //line:vm:mm:getalloc

/* Given block ptr bp, compute address of its header and footer */
#define HDRP(bp)       ((char *)(bp) - WSIZE)                      //line:vm:mm:hdrp
#define FTRP(bp)       ((char *)(bp) + GET_SIZE(HDRP(bp)) - DSIZE) //line:vm:mm:ftrp

/* Given block ptr bp, compute address of next and previous blocks */
#define NEXT_BLKP(bp)  ((char *)(bp) + GET_SIZE(((char *)(bp) - WSIZE))) //line:vm:mm:nextblkp
#define PREV_BLKP(bp)  ((char *)(bp) - GET_SIZE(((char *)(bp) - DSIZE))) //line:vm:mm:prevblkp

#define PUT_PTR(p, val) (*(char *)(p) = ((char *)&val))

/* single word (4) or double word (8) alignment */
#define ALIGNMENT 8

/* rounds up to the nearest multiple of ALIGNMENT */
#define ALIGN(p) (((size_t)(p) + (ALIGNMENT-1)) & ~0x7)

/* get next free block */
#define SUCC(bp)  ((unsigned long *)(bp))
/* get preveious free block */
#define PRED(bp)  ((unsigned long *)(bp) + 1)
/* Get/put for addresses */
#define GET_ADDRESS(p) (*(unsigned long *)(p))
#define PUT_ADDRESS(p, val)  (*(unsigned long *)(p) = (val))

/* $end mallocmacros */

/* Function prototypes for internal helper routines */
static void *extend_heap(size_t words);
static void place(void *bp, size_t asize);
static void *find_fit(size_t asize);
static void put_to_freelist(void *ptr);
static void *coalesce(void *bp);
static void printblock(void *bp);
static void checkblock(void *bp);
static void *split_block(void *bp, size_t size);
static void remove_free_block(void *bp);
static void print_free_block(void *bp);
static void print_neighbor_info(void *bp);
static void print_free_list();
static void check_free_block(void *bp);
static void has_contiguous_free_blocks(void *bp);
static void missing_free_block(void *bp);
static void has_cycle(void *bp);
static void has_same_metadata(void *bp);
static void is_outside_heap(void *bp);
static void is_aligned(void *bp);
static size_t adapt_size(size_t size);

/*
 * Initialize: return -1 on error, 0 on success.
 */
int mm_init(void) {
	dbg_a_printf("**** %s ****\n", __func__);
	/* Create the initial empty heap */
	if ((heap_listp = mem_sbrk(4 * WSIZE)) == (void *) -1) //line:vm:mm:begininit
		return -1;
	PUT(heap_listp, 0); /* Alignment padding */
	PUT(heap_listp + (1 * WSIZE), PACK(DSIZE, 1)); /* Prologue header */
	PUT(heap_listp + (2 * WSIZE), PACK(DSIZE, 1)); /* Prologue footer */
	PUT(heap_listp + (3 * WSIZE), PACK(0, 1)); /* Epilogue header */
	heap_listp += (2 * WSIZE);                     //line:vm:mm:endinit
	free_listp = NULL;

	/* Extend the empty heap with a free block of CHUNKSIZE bytes */
	if (extend_heap(CHUNKSIZE / WSIZE) == NULL)
		return -1;

	return 0;
	/* $end mminit */
}

/*
 * malloc
 */
void *malloc(size_t size) {
	dbg_a_printf("**** %s ****\n", __func__);
	dbg_printf("want to allocate size: %d\n", (int )size);
	CHECKHEAP(__LINE__);

	size_t asize; /* Adjusted block size */
	size_t extendsize; /* Amount to extend heap if no fit */
	char *bp;

	if (heap_listp == 0) {
		mm_init();
	}
	/* Ignore spurious requests */
	if (size == 0)
		return NULL;

	asize = adapt_size(size);

	/* Search the free list for a fit */
	if ((bp = find_fit(asize)) != NULL) {  //line:vm:mm:findfitcall
		place(bp, asize);                  //line:vm:mm:findfitplace
		return bp;
	}

	/* No fit found. Get more memory and place the block */
	extendsize = MAX(asize, (CHUNKSIZE / WSIZE));         //line:vm:mm:growheap1
	if ((bp = extend_heap(extendsize)) == NULL)
		return NULL;                                  //line:vm:mm:growheap2
	place(bp, asize);                                 //line:vm:mm:growheap3

	dbg_printf("%s finished:\n", __func__);
	dbg_print_block(bp);

	CHECKHEAP(__LINE__);
	return bp;
}

/*
 * free
 */
void free(void *ptr) {
	dbg_a_printf("**** %s ****\n", __func__);
	CHECKHEAP(__LINE__);
	dbg_printf("----- ptr to be freed: %p  -----\n", ptr);

	if (ptr == NULL) {
		return;
	}
	if (!ptr) {
		return;
	}

	if (free_listp == 0) {
		mm_init();
	}
	PUT(HDRP(ptr), PACK(GET_SIZE(HDRP(ptr)), 0));
	PUT(FTRP(ptr), PACK(GET_SIZE(HDRP(ptr)), 0));

	ptr = (char *) coalesce(ptr);
	dbg_printf("ptr changed to %p after coalescing\n", ptr);
	put_to_freelist(ptr);

	CHECKHEAP(__LINE__);
}

/*
 * realloc - you may want to look at mm-naive.c
 */
void *realloc(void *oldptr, size_t size) {
	dbg_a_printf("**** %s ****\n", __func__);
	void *newptr;
	dbg_printf("Attempting to reallocate pointer %p to new size %d\n", oldptr,
			(int )size);
	/* If oldptr is NULL, then this is just malloc. */
	if (oldptr == NULL) {
		dbg_printf("oldptr is null, returning mallloc\n");
		return malloc(size);
	}

	size_t oldsize = GET_SIZE(HDRP(oldptr));

	dbg_print_block(oldptr);

	/* If size == 0 then this is just free, and we return NULL. */
	if (size == 0) {
		dbg_printf("new size is 0, freeing the block\n");
		free(oldptr);
		return 0;
	}

	if (oldsize >= adapt_size(size)) {
		dbg_printf("No need to reallocate\n");
		return oldptr;
	}

	dbg_printf("-- %s BEFORE malloc called\n", __func__);
	newptr = malloc(size);
	dbg_printf("-- %s AFTER malloc called\n", __func__);

	/* If realloc() fails the original block is left untouched  */
	if (!newptr) {
		return 0;
	}

	/* Copy the old data. */
	memcpy(newptr, oldptr, size);

	/* Free the old block. */
	free(oldptr);

	CHECKHEAP(__LINE__);
	dbg_printf("--end of %s\n", __func__);

	return newptr;
}

/*
 * calloc - you may want to look at mm-naive.c
 * This function is not tested by mdriver, but it is
 * needed to run the traces.
 */
void *calloc(size_t nmemb, size_t size) {
	dbg_a_printf("**** %s ****\n", __func__);
	CHECKHEAP(__LINE__);
	size_t total_size = nmemb * size;
	char * bp = malloc(total_size);

	if (bp != NULL) {
		unsigned int i;
		for (i = 0; i < nmemb; i += size) {
			PUT(bp + i, 0);
		}
		CHECKHEAP(__LINE__);
		return bp;
	}

	CHECKHEAP(__LINE__);
	return NULL;
}

/*
 * Return whether the pointer is aligned.
 * May be useful for debugging.
 */
static int aligned(const void *p) {
	dbg_a_printf("**** %s ****\n", __func__);
	return (size_t) ALIGN(p) == (size_t) p;
}

/*
 * mm_checkheap
 */
void mm_checkheap(int verbose) {
	dbg_a_printf("**** %s ****\n", __func__);
	char *bp = heap_listp;

	if (verbose) {
		printf("Heap:      (%p):\n", heap_listp);
		printf("Hea_end:   (%p):\n", heap_end);
		printf("Free list: (%p)\n", free_listp);
	}

	if ((GET_SIZE(HDRP(heap_listp)) != DSIZE) || !GET_ALLOC(HDRP(heap_listp))) {
		printf("Bad prologue header\n");
		assert(
				!((GET_SIZE(HDRP(heap_listp)) != DSIZE) || !GET_ALLOC(HDRP(heap_listp))));
	}

	for (bp = heap_listp; GET_SIZE(HDRP(bp)) > 0; bp = NEXT_BLKP(bp)) {
		if (verbose) {
			printblock(bp);
		}
		checkblock(bp);
	}

	if (verbose)
		printblock(bp);
	checkblock(bp);

	if ((GET_SIZE(HDRP(bp)) != 0) || !GET_ALLOC(HDRP(bp))) {
		dbg_printf("Bad epilogue header\n");
		assert(!((GET_SIZE(HDRP(bp)) != 0) || (!GET_ALLOC(HDRP(bp)))));
	}

	if (free_listp != NULL) {
		unsigned long *fbp = free_listp;
		if (verbose)
			printf("Free blocks list\n");
		while (fbp != NULL) {
			if (verbose) {
				print_free_block(fbp);
			}
			check_free_block(fbp);
			if (!GET_ADDRESS(SUCC(fbp))) {
				break;
			}
			fbp = (void *) GET_ADDRESS(SUCC(fbp));
		}

	}

	if (verbose)
		print_neighbor_info(bp);
	if (verbose)
		print_free_list();

	dbg_printf("%s finished:\n", __func__);
}

static size_t adapt_size(size_t size) {
	dbg_a_printf("**** %s ****\n", __func__);
	size = size + 2 * WSIZE;
	if (size < MIN_BLOCK_SIZE) {
		size = MIN_BLOCK_SIZE;
	}
	size = ALIGN(size);
	return size;
}

static void print_free_list() {
	dbg_a_printf("**** %s ****\n", __func__);
	dbg_printf("*****\n");
	if (free_listp != NULL) {
		unsigned long *fbp = free_listp;
		dbg_printf("Free blocks list\n");
		while (fbp != NULL) {
			print_free_block(fbp);
			if (!GET_ADDRESS(SUCC(fbp)))
				break;
			fbp = (void *) GET_ADDRESS(SUCC(fbp));
		}
	}dbg_printf("*****\n");
}

/*
 * coalesce - Boundary tag coalescing. Return ptr to coalesced block
 */
static void *coalesce(void *bp) {
	dbg_a_printf("**** %s ****\n", __func__);
	if (bp == ((void *) 0x800007258)) {
		dbg_printf("Seg fault CHECK\n");
		dbg_printf("bp is: %p\n", bp);
		dbg_printf("NEXT_BLKP(bp) is: %p\n", NEXT_BLKP(bp));
		dbg_printf("FTR(NEXT_BLKP(bp)) is: %p\n", FTRP(NEXT_BLKP(bp)));
		dbg_printf("ALLOC of FTR(NEXT_BLKP(bp)) is: %d\n", (int)GET_ALLOC(FTRP(NEXT_BLKP(bp))));
		dbg_printf("HDRP(NEXT_BLKP(bp)) is: %p\n", HDRP(NEXT_BLKP(bp)));
		dbg_printf("ALLOC of HDRP(NEXT_BLKP(bp)) is: %d\n", (int)GET_ALLOC(HDRP(NEXT_BLKP(bp))));
	}
	size_t prev_alloc = GET_ALLOC(FTRP(PREV_BLKP(bp)));
	size_t next_alloc = GET_ALLOC(HDRP(NEXT_BLKP(bp)));
	size_t size = GET_SIZE(HDRP(bp));
	dbg_printf("coalesce pointer %p\n", bp);

	if (prev_alloc && next_alloc) { /* Case 1: no neighbor is free */
		dbg_printf("Case 1: no neighbor is free\n");
		size_t header = PACK(size, 0);
		PUT(HDRP(bp), header);
		PUT(FTRP(bp), header);

	} else if (prev_alloc && !next_alloc) { /* Case 2: Right neighbor is free */
		dbg_printf("Case 2: Right neighbor is free\n");
		size_t next_size = GET_SIZE(HDRP(NEXT_BLKP(bp)));
		size += next_size;
		size_t header = PACK(size, 0);
		unsigned long p = GET_ADDRESS(PRED(NEXT_BLKP(bp)));
		unsigned long s = GET_ADDRESS(SUCC(NEXT_BLKP(bp)));
		dbg_printf("Pack: %d\n", (int ) header);
		print_free_block(NEXT_BLKP(bp));

		/* changes to the next block */
		PUT(FTRP(NEXT_BLKP(bp)), header);
		dbg_printf("Footer of next block: %p [%d]\n", FTRP(NEXT_BLKP(bp)),
				(int) GET(FTRP(NEXT_BLKP(bp))));

		/* changes in the block to be freed */
		PUT(HDRP(bp), header);
		//transfer the predecessor and successor of the free block to the left block that is going to be merged with
		PUT_ADDRESS(SUCC(bp), s);
		PUT_ADDRESS(PRED(bp), p);

		dbg_printf("Header: %p [%d]\n", HDRP(bp), (int) GET(HDRP(bp)));
		dbg_printf("*****\n");
//		printblock(bp);
		dbg_print_free_block(bp);
		dbg_printf("*****\n");

		//restore links in free list
		remove_free_block(bp);

	} else if (!prev_alloc && next_alloc) { /* Case 3: Left neighbor is free */
		dbg_printf("Case 3: Left neighbor is free\n");
		size_t prev_size = GET_SIZE(HDRP(PREV_BLKP(bp)));
		size += prev_size;
		size_t header = PACK(size, 0);
		char * prev = PREV_BLKP(bp);

		// changes on the previous block
		PUT(HDRP(PREV_BLKP(bp)), header);

		// changes on the block to be freed
		PUT(FTRP(bp), header);

		//restore links in free list
		remove_free_block(prev);

		bp = prev;
	}

	else { /* Case 4: Left and Right neighbors free */
		dbg_printf("Case 4: Left and Right neighbor are free\n");
		size_t next_size = GET_SIZE(HDRP(NEXT_BLKP(bp)));
		size_t prev_size = GET_SIZE(HDRP(PREV_BLKP(bp)));
		size += next_size + prev_size;
		size_t header = PACK(size, 0);

		//restore links in free list for previous block
		remove_free_block(PREV_BLKP(bp));

		//resotre links in free list for next block
		remove_free_block(NEXT_BLKP(bp));

		// set header and footer of free block
		PUT(HDRP(PREV_BLKP(bp)), header);
		PUT(FTRP(NEXT_BLKP(bp)), header);

		bp = PREV_BLKP(bp);
	}
	/* $end mmfree */

//	CHECKHEAP(__LINE__);
	return bp;
}
static void *split_block(void *bp, size_t size) {
	dbg_a_printf("**** %s ****\n", __func__);
//	CHECKHEAP(__LINE__);
	size_t block_size = GET_SIZE(HDRP(bp));
	if (block_size > size) {
		size_t diff = block_size - size;
		if (diff >= MIN_BLOCK_SIZE) {
//			printf("-> %s - %d \tBEFORE splitting\n", __func__, __LINE__);
//			printf("%d + %d = %d\n\n", (int) size, (int) diff,
//					(int) block_size);
//			print_free_block(bp);
			unsigned long s = GET_ADDRESS(SUCC(bp));

			// set footer of full block to size = diff
			PUT(FTRP(bp), PACK(diff, 0));
			//set header of full block to size = size and allocated bit to 0
			PUT(HDRP(bp), PACK(size, 0));
			//set footer of split to-be-allocated block to size = size and allocated bit = 0
			PUT(FTRP(bp), PACK(size, 0));
			//create header for split free block
			PUT(HDRP(NEXT_BLKP(bp)), PACK(diff, 0));
			dbg_printf("____\n");
			dbg_printf("header %p [%du]\n", HDRP(bp), GET(HDRP(bp)));
			dbg_printf("footer %p [%du]\n", FTRP(bp), GET(FTRP(bp)));
			dbg_print_free_block(bp);
			dbg_printf("____\n");

			dbg_printf("____\n");
			dbg_print_free_block(bp);
			print_free_block(NEXT_BLKP(bp));
			dbg_printf("____\n");

			PUT_ADDRESS(SUCC(NEXT_BLKP(bp)), GET_ADDRESS(SUCC(bp)));
			if (s) { // if the free block is not the last one (= there is a successor)
				unsigned long *succ = (void *) s;
				PUT_ADDRESS(PRED(succ), (size_t)NEXT_BLKP(bp));
			}
			PUT_ADDRESS(PRED(NEXT_BLKP(bp)), (size_t )bp);
			PUT_ADDRESS(SUCC(bp), (size_t)NEXT_BLKP(bp));
		}
	}
	dbg_printf("%s AFTER splitting\n", __func__);
	dbg_print_free_list();
	return bp;
}

/*
 * find_fit - Find a fit for a block with asize bytes
 */
static void *find_fit(size_t asize) {
	/* First fit search */
	dbg_a_printf("**** %s ****\n", __func__);
	void *bp = free_listp;
	while (SUCC(bp) != NULL) {
		dbg_printf(
				"%s free block is:   %p    successor is:   %p  [%p] to allocate size %d\n",
				__func__, bp, SUCC(bp), (void *)GET_ADDRESS(SUCC(bp)),
				(int )asize);
		if (GET_SIZE(HDRP(bp)) >= asize) {
			return bp;
		}
		bp = (void *) GET_ADDRESS(SUCC(bp));
		dbg_printf("next bp in loop: %p\n", bp);
	}
	CHECKHEAP(__LINE__);
	return NULL; /* No fit */
}

static void remove_free_block(void *bp) {
	dbg_a_printf("**** %s ****\n", __func__);
	dbg_printf("*** %s - BEFORE removing\n", __func__);
	dbg_print_neighbor_info(bp);
	dbg_printf("***\n");
	unsigned long s = GET_ADDRESS(SUCC(bp));
	unsigned long p = GET_ADDRESS(PRED(bp));

	if ((!p) && (!s)) { // No predecessor, No successor - removing the only free block
		free_listp = NULL;
	} else if ((!p) && (s)) { // No predecessor, Existing successor - removing the first in free list block
		unsigned long *succ = (void *) s;
		PUT_ADDRESS(PRED(succ), 0);
		free_listp = succ;
	} else if ((p) && (!s)) { // Existing predecessor, No successor - removing the last in free list block
		unsigned long *pred = (void *) p;
		PUT_ADDRESS(SUCC(pred), 0);
	} else { // Existing predecessor, Existing successor
		unsigned long *pred = (void *) p;
		unsigned long *succ = (void *) s;
		PUT_ADDRESS(SUCC(pred), s);
		PUT_ADDRESS(PRED(succ), p);
	}
//	PUT_ADDRESS(SUCC(bp), 0);
//	PUT_ADDRESS(PRED(bp), 0);

	dbg_printf("*** %s - AFTER removing\n", __func__);
	dbg_print_neighbor_info(bp);dbg_printf("***\n");
}

static void print_neighbor_info(void *bp) {
	dbg_a_printf("**** %s ****\n", __func__);
	print_free_block(bp);
	unsigned long p = GET_ADDRESS(PRED(bp));
	unsigned long s = GET_ADDRESS(SUCC(bp));

	if (p) {
		dbg_printf("Predecessor:\n");
		print_free_block((void *) p);
	} else {
		dbg_printf("No predecessor.\n");
	}

	if (s) {
		dbg_printf("Successor:\n");
		print_free_block((void *) s);
	} else {
		dbg_printf("No successor.\n");
	}

	dbg_printf("free_listp: %p\n\n", free_listp);
}

static void put_to_freelist(void *ptr) {
	dbg_a_printf("**** %s ****\n", __func__);
//	CHECKHEAP(__LINE__);
	dbg_printf("--- %s --- for bp %p\n", __func__, ptr);

	PUT_ADDRESS(PRED(ptr), 0);
	if (free_listp == NULL) {
		PUT_ADDRESS(SUCC(ptr), 0);dbg_printf(
				"======= free list p NULL ======\n");
//		printf("successor of ptr: %p\n", (void *) GET_ADDRESS(SUCC(ptr)));
//		printf("predecessor of ptr: %p\n\n", (void *) GET_ADDRESS(PRED(ptr)));

	} else {
		dbg_printf("***** size_t free_list: %x\n",
				(int ) ((size_t ) free_listp));
		dbg_printf("***** size_t   pointer: %x\n", (int ) ((size_t ) ptr));
		PUT_ADDRESS(SUCC(ptr), (size_t )free_listp); //set the successor of the free block to the block that free_listp is pointing to (the previous start of the free list
		PUT_ADDRESS(PRED(free_listp), (size_t )ptr); //set the predecessor of the previously first free block pointing to the newly first free block

//		print_neighbor_info(ptr);
//		printf("successor of ptr: %p\n", (void *) GET_ADDRESS(SUCC(ptr)));
//		printf("predecessor of ptr: %p\n", (void *) GET_ADDRESS(PRED(ptr)));
//		printf("successor of free_listp: %p\n",
//				(void *) GET_ADDRESS(SUCC(free_listp)));
//		printf("predecessor of free_listp: %p\n\n",
//				(void *) GET_ADDRESS(PRED(free_listp)));
	}

	free_listp = ptr;
	dbg_print_free_list();
	CHECKHEAP(__LINE__);
	return;
}

/*
 * extend_heap - Extend heap with free block and return its block pointer
 */
static void *extend_heap(size_t words) {
	dbg_a_printf("**** %s ****\n", __func__);
//	CHECKHEAP(__LINE__);
	char *bp;
	size_t size;
	/* Allocate an even number of words to maintain alignment */
	size = ALIGN(words);
	dbg_printf("Size to be extended: %d\n", (int ) size);
	if ((long) (bp = mem_sbrk(size)) == -1)
		return NULL;                                      //line:vm:mm:endextend

	/* Initialize free block header/footer and the epilogue header */
	PUT(HDRP(bp), PACK(size, 0)); /* Free block header */ //line:vm:mm:freeblockhdr
	PUT(FTRP(bp), PACK(size, 0)); /* Free block footer */ //line:vm:mm:freeblockftr

	PUT(HDRP(NEXT_BLKP(bp)), PACK(0, 1)); /* New epilogue header */ //line:vm:mm:newepihdr
	heap_end = NEXT_BLKP(bp);

	/* Coalesce if the previous block was free */
	bp = (char *) coalesce(bp);
	put_to_freelist(bp);

	CHECKHEAP(__LINE__);
	return bp;                                //line:vm:mm:returnblock
}

/*
 * place - Place block of asize bytes at start of free block bp
 *         and split if remainder would be at least minimum block size
 */
static void place(void *bp, size_t asize) {
	dbg_a_printf("**** %s ****\n", __func__);
//	printf("size to place  %d \n", (int) asize);
//	print_neighbor_info(bp);
	dbg_printf("--- %s BEFORE split block ---\n", __func__);
	bp = split_block(bp, asize);
	dbg_printf("--- %s AFTER split block ---\n", __func__);
	dbg_printf("--- %s BEFORE remove from free list ---\n", __func__);
	remove_free_block(bp);
	dbg_printf("--- %s AFTER remove from free list ---\n", __func__);
	size_t block_size = GET_SIZE(HDRP(bp));
	if (!(asize < block_size)) {
		PUT(HDRP(bp), PACK(asize, 1));
		PUT(FTRP(bp), PACK(asize, 1));
	} else {
		PUT(HDRP(bp), PACK(block_size, 1));
		PUT(FTRP(bp), PACK(block_size, 1));
	}

	dbg_printf("--- %s AFTER allocating the block ---\n", __func__);
	CHECKHEAP(__LINE__);
}

/*
 * print_free_block - Print information about free block
 */
static void print_free_block(void *bp) {
	dbg_a_printf("**** %s ****\n", __func__);
//	printf("%p: \t Succ [%p : %p] \t Pred [%p : %p]\n", bp, SUCC(bp),
//			(void *) GET_ADDRESS(SUCC(bp)), PRED(bp),
//			(void *) GET_ADDRESS(PRED(bp)));
	unsigned long * temp = bp;
	bp = temp;
	dbg_printf("%p: \t Pred [%p : %p] \t Succ [%p : %p]\n", bp, PRED(bp),
			(void *) GET_ADDRESS(PRED(bp)), SUCC(bp),
			(void *) GET_ADDRESS(SUCC(bp)));
}

/*
 * check_free_block - Check accuracy of free block
 */
static void check_free_block(void *bp) {
	dbg_a_printf("**** %s ****\n", __func__);
//	dbg_printf("--- %s ---\n", __func__);

	unsigned long succ = GET_ADDRESS(SUCC(bp));
	unsigned long pred = GET_ADDRESS(PRED(bp));

//	dbg_print_free_block(bp);

	if ((succ != 0) && (pred != 0)) {
		if (succ == pred) {
			printf("Error: successor and predecessor are of same value.\n");
			assert(!(GET_ADDRESS(SUCC(bp)) == GET_ADDRESS(PRED(bp))));
		}
	}
//	dbg_printf("check for allocation bit\n");
	if (GET_ALLOC(HDRP(bp))) {
		printf("Error: allocated block in free list.\n");
		assert(!GET_ALLOC(HDRP(bp)));
	}

//	dbg_printf("check for contiguous free blocks\n");
	has_contiguous_free_blocks(bp);
//	dbg_printf("check for cycle\n");
	has_cycle(bp);

	return;
}

static void printblock(void *bp) {
	dbg_a_printf("**** %s ****\n", __func__);
	size_t hsize, halloc, fsize, falloc;

	hsize = GET_SIZE(HDRP(bp));
	halloc = GET_ALLOC(HDRP(bp));
	char h_alloc = halloc ? 'a' : 'f';
	fsize = GET_SIZE(FTRP(bp));
	falloc = GET_ALLOC(FTRP(bp));
	char f_alloc = falloc ? 'a' : 'f';

	if (hsize == 0) {
		printf("%p: header: [%lu:%c]  EOL\n", bp, hsize, h_alloc);
		return;
	}
	printf("%p: header: [%lu:%c] footer: [%lu:%c]\n", bp, hsize, h_alloc, fsize,
			f_alloc);
//	dbg_printf("end of %s\n", __func__);
}

static void checkblock(void *bp) {
	dbg_a_printf("**** %s ****\n", __func__);
//	printf("checking block ");
//	printblock(bp);

	is_aligned(bp);
	is_outside_heap(bp);
	has_same_metadata(bp);

	if (!GET_ALLOC(HDRP(bp))) {  // free block
		missing_free_block(bp);
	}
//	dbg_printf("end of %s\n", __func__);
}

static void has_same_metadata(void *bp) {
	dbg_a_printf("**** %s ****\n", __func__);
	if (GET_SIZE(HDRP(bp)) == 0) {
		return;
	}
//	dbg_printf("-- %s --\n", __func__);
	if (GET(HDRP(bp)) != GET(FTRP(bp))) {
		printf("Error: header does not match footer\n");
		assert(!(GET(HDRP(bp)) != GET(FTRP(bp))));
	}
}

static void is_outside_heap(void *bp) {
	dbg_a_printf("**** %s ****\n", __func__);
	if (heap_end) {
		int condition = ((((char *) bp) >= ((char *) heap_listp))
				&& (((char *) bp) <= heap_end));
		if (!condition) {
			printf("Error: pointer %p points to an address outside the heap\n",
					bp);
			assert(condition);
		}
	}
}

static void is_aligned(void *bp) {
	dbg_a_printf("**** %s ****\n", __func__);
	if ((size_t) bp % 8) {
		printf("Error: %p is not doubleword aligned\n", bp);
		assert(!((size_t ) bp % 8));
	}

	if (!aligned(bp)) {
		printf("Error: %p is not aligned\n", bp);
		assert(aligned(bp));
	}
}

static void has_cycle(void *bp) {
	dbg_a_printf("**** %s ****\n", __func__);
	if (free_listp != NULL) {
		unsigned long *fbp = free_listp;

		//count how many times bp is referenced in free list (to check for cycles)
		int refs_s_count = 0; //counter for how many times bp is a successor
		int refs_p_count = 0; //counter for how many times bp is a predecessor

		fbp = free_listp;
		while (fbp != NULL) {
			if (GET_ADDRESS(SUCC(fbp)))
				break;
			if ((void *) GET_ADDRESS(SUCC(fbp)) == bp) {
				refs_s_count++;
			}
			if (GET_ADDRESS(PRED(fbp)))
				break;
			if ((void *) GET_ADDRESS(PRED(fbp)) == bp) {
				refs_p_count++;
			}
			fbp = (void *) GET_ADDRESS(SUCC(fbp));
		}
		if ((refs_s_count > 1) || (refs_p_count > 1)) {
			printf("Error: cycle in free list");
			assert(!(refs_s_count > 1));
		}
	}
//	dbg_printf("-- end of %s --\n", __func__);
}

static void has_contiguous_free_blocks(void *bp) {
	dbg_a_printf("**** %s ****\n", __func__);
//	print_free_block(bp);
	unsigned long *prev = (unsigned long *) PREV_BLKP(bp);
	unsigned long *next = (unsigned long *) NEXT_BLKP(bp);
	unsigned long *p = (void *) GET_ADDRESS(PRED(bp));
	unsigned long *s = (void *) GET_ADDRESS(SUCC(bp));

	if ((prev == p) || (next == s)) {
		printf("Error: contiguous free blocks\n");
		assert(!((prev == p) || (next == s)));
	}
	return;
}

static void missing_free_block(void *bp) {
	dbg_a_printf("**** %s ****\n", __func__);
//	dbg_printf("looking for pointer %p\n", bp);
	if (free_listp != NULL) {
		unsigned long *fbp = free_listp;
		int in_list = 0;
		while (fbp != NULL) {
//			printf("parsing block %p   with succ %p\n", fbp, (void *)GET_ADDRESS(SUCC(fbp)));
			if (bp == fbp) {
				in_list = 1;
				break;
			}
			if (!GET_ADDRESS(SUCC(fbp))) {
				break;
			}
			fbp = (void *) GET_ADDRESS(SUCC(fbp));
//			dbg_printf("%d\n", __LINE__);
		}
		if (!in_list) {
			printf("Error: free block not in free list\n");
			assert(in_list);
		}
	}
	return;
}
