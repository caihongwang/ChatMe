#include <pj/timer.h>
#include <pj/pool.h>
#include <pj/os.h>
#include <pj/string.h>
#include <pj/assert.h>
#include <pj/errno.h>
#include <pj/lock.h>
#include <pj/log.h>

#define HEAP_PARENT(X)	(X == 0 ? 0 : (((X) - 1) / 2))
#define HEAP_LEFT(X)	(((X)+(X))+1)


#define DEFAULT_MAX_TIMED_OUT_PER_POLL  (64)

#define THIS_FILE "timer_android.c"


#define MAX_HEAPS 64
#define MAX_ENTRY_PER_HEAP 128
#define REUSE_SLOT_INTERVAL 5

enum
{
    F_DONT_CALL = 1,
    F_DONT_ASSERT = 2,
    F_SET_ID = 4
};

// Forward def of wrapper
int timer_schedule_wrapper(int entry, int entryId, int time);
int timer_cancel_wrapper(int entry, int entryId);


/**
 * The implementation of timer heap.
 */
struct pj_timer_heap_t
{

	unsigned heap_id;

    /** Pool from which the timer heap resize will get the storage from */
    pj_pool_t *pool;

    /** Lock object. */
    pj_lock_t *lock;

    /** Autodelete lock. */
    pj_bool_t auto_delete_lock;

    /** List of known entries. _timer_id helps to find the slot (index is _timer_id -1). _timer_id is also used to compute the android id of the alarm */
    pj_timer_entry* entries[MAX_ENTRY_PER_HEAP];
    /** As we fear android alarm manager does not apply synchronously cancel/enqueue, we do not reuse immediately an entry slot*/
    int entries_reuse_in[MAX_ENTRY_PER_HEAP];

    /** Callback to be called when a timer expires. */
    pj_timer_heap_callback *callback;

};


static pj_timer_heap_t* sHeaps[MAX_HEAPS];
static int sCurrentHeap = 0;


PJ_INLINE(void) lock_timer_heap( pj_timer_heap_t *ht )
{
    if (ht->lock) {
	pj_lock_acquire(ht->lock);
    }
}

PJ_INLINE(void) unlock_timer_heap( pj_timer_heap_t *ht )
{
    if (ht->lock) {
	pj_lock_release(ht->lock);
    }
}


static int get_entry_id(pj_timer_heap_t *ht, pj_timer_entry *entry){
	return (entry->_timer_id -1) + MAX_ENTRY_PER_HEAP * ht->heap_id;
}


// protected by timer heap lock
static pj_status_t schedule_entry( pj_timer_heap_t *ht,
				   pj_timer_entry *entry, 
				   const pj_time_val *future_time,
				   const pj_time_val *delay) {
	int i = 0;
	int timer_slot = -1;
	// Find one empty slot in ht entries
	for (i = 0; i < MAX_ENTRY_PER_HEAP; i++) {
		if (ht->entries[i] == NULL) {
            if(ht->entries_reuse_in[i] <= 0){
                ht->entries[i] = entry;
                timer_slot = i;
                break;
            }else{
                ht->entries_reuse_in[i] --;
            }
		}
	}

	if (timer_slot >= 0) {
		// Obtain the next unique sequence number.
		// Set the entry
		entry->_timer_id = timer_slot + 1;
		entry->_timer_value = *future_time;

		pj_uint32_t ft = PJ_TIME_VAL_MSEC(*delay);

		PJ_LOG(
				5,
				(THIS_FILE, "Scheduling timer %d of %d in %ld ms @ %p", entry->_timer_id, ht->heap_id, ft, entry));

		timer_schedule_wrapper((int) entry, get_entry_id(ht, entry), (int) ft);

		return PJ_SUCCESS;
	} else {
		return PJ_ETOOMANY;
	}
}

// Protected by timer heap lock
static int cancel(pj_timer_heap_t *ht,
        pj_timer_entry *entry,
        unsigned flags) {

	PJ_CHECK_STACK();

	// Check to see if the timer_id is out of range
	if ( (entry->_timer_id < 0) || (entry->_timer_id > MAX_ENTRY_PER_HEAP) ) {
		PJ_LOG(
				5,
				(THIS_FILE, "Ask to cancel something already fired or cancelled : %d @ %p", entry->_timer_id, entry));
		return 0;
	}else if (entry->_timer_id == 0){
	    // Not init timer entry
	    return 0;
	}

	PJ_LOG(5, (THIS_FILE, "Cancel timer %d", entry->_timer_id));

	// This includes case where the entry is not linked to the heap anymore
	if (ht->entries[entry->_timer_id - 1] != entry) {
        PJ_LOG(2,
            (THIS_FILE, "Cancelling something not linked to this heap : %d : %p vs %p", entry->_timer_id,
                    entry, ht->entries[entry->_timer_id]));
        if ((flags & F_DONT_ASSERT) == 0){
          pj_assert(entry == ht->entries[entry->_timer_id - 1]);
        }
		return 0;
	}

	// Note -- due to the fact we rely on android alarm manager, nothing ensure here that cancelCount is actually valid.
	// Previous checks should do the trick to be sure we have actually 1 cancelled timer here.
	int cancelCount = timer_cancel_wrapper((int) entry, get_entry_id(ht, entry));

	if (cancelCount > 0) {
		// Free the slot of this entry in ht
		ht->entries[entry->_timer_id - 1] = NULL;
		ht->entries_reuse_in[entry->_timer_id - 1] = REUSE_SLOT_INTERVAL;
		entry->_timer_id = -1;
	}

    if ((flags & F_DONT_CALL) == 0){
		// Call the close hook.
		(*ht->callback)(ht, entry);
	}

	return cancelCount;

}


/*
 * Calculate memory size required to create a timer heap.
 */
// fixme !
PJ_DEF(pj_size_t) pj_timer_heap_mem_size(pj_size_t count)
{
    return /* size of the timer heap itself: */
           sizeof(pj_timer_heap_t) + 
           /* size of each entry: */
           (count+2) * (sizeof(pj_timer_entry*)+sizeof(pj_timer_id_t)) +
           /* lock, pool etc: */
           132;
}

/*
 * Create a new timer heap.
 */
PJ_DEF(pj_status_t) pj_timer_heap_create( pj_pool_t *pool,
					  pj_size_t size,
                      pj_timer_heap_t **p_heap)
{
    pj_timer_heap_t *ht;
    pj_size_t i;

    PJ_ASSERT_RETURN(pool && p_heap, PJ_EINVAL);

    *p_heap = NULL;

    // TODO - ensure size is lower than MAX_ENTRY_PER_HEAP

    /* Allocate timer heap data structure from the pool */
    ht = PJ_POOL_ALLOC_T(pool, pj_timer_heap_t);
	if (!ht)
		return PJ_ENOMEM;

	/* Initialize timer heap sizes */
	ht->pool = pool;

	/* Lock. */
	ht->lock = NULL;
	ht->auto_delete_lock = 0;

	// Find one free slot on static table of heaps available
	// -- very basic implementation for now that should be sufficiant for our needs
	for (i = sCurrentHeap; i < MAX_HEAPS; i++) {
		if (sHeaps[i] == NULL) {
			ht->heap_id = i;
			sHeaps[i] = ht;
			sCurrentHeap = i;
			break;
		}
	}

    // RAZ table of pointers to entries for this heap
	pj_bzero(ht->entries, MAX_ENTRY_PER_HEAP * sizeof(pj_timer_entry*));
	pj_bzero(ht->entries_reuse_in, MAX_ENTRY_PER_HEAP * sizeof(int));

	*p_heap = ht;
	return PJ_SUCCESS;
}

PJ_DEF(void) pj_timer_heap_destroy( pj_timer_heap_t *ht )
{
	int i;
	lock_timer_heap(ht);
	// Cancel all entries
	for (i = 0; i < MAX_ENTRY_PER_HEAP; i++) {
		if (ht->entries[i] != NULL) {
			pj_timer_entry *entry = ht->entries[i];
			cancel(ht, entry, 1);
		}
	}
	unlock_timer_heap(ht);

	if (ht->lock && ht->auto_delete_lock) {
		pj_lock_destroy(ht->lock);
		ht->lock = NULL;
	}
	sCurrentHeap++;
	sCurrentHeap = sCurrentHeap % MAX_HEAPS;
	sHeaps[ht->heap_id] = NULL;
}

PJ_DEF(void) pj_timer_heap_set_lock(  pj_timer_heap_t *ht,
                                      pj_lock_t *lock,
                                      pj_bool_t auto_del )
{
    if (ht->lock && ht->auto_delete_lock)
        pj_lock_destroy(ht->lock);

    ht->lock = lock;
    ht->auto_delete_lock = auto_del;
}


PJ_DEF(unsigned) pj_timer_heap_set_max_timed_out_per_poll(pj_timer_heap_t *ht,
                                                          unsigned count )
{
    /* Not applicable */
    PJ_UNUSED_ARG(count);
    return MAX_ENTRY_PER_HEAP;
}

PJ_DEF(pj_timer_entry*) pj_timer_entry_init( pj_timer_entry *entry,
                                             int id,
                                             void *user_data,
                                             pj_timer_heap_callback *cb )
{
    pj_assert(entry && cb);

    entry->_timer_id = -1;
    entry->id = id;
    entry->user_data = user_data;
    entry->cb = cb;
    entry->_grp_lock = NULL;

    return entry;
}

PJ_DEF(pj_bool_t) pj_timer_entry_running( pj_timer_entry *entry )
{
    return (entry->_timer_id >= 1);
}

#if PJ_TIMER_DEBUG
static pj_status_t schedule_w_grp_lock_dbg(pj_timer_heap_t *ht,
                                       pj_timer_entry *entry,
                                       const pj_time_val *delay,
                                       pj_bool_t set_id,
                                       int id_val,
                                       pj_grp_lock_t *grp_lock,
                                       const char *src_file,
                                       int src_line)
#else
static pj_status_t schedule_w_grp_lock(pj_timer_heap_t *ht,
                                       pj_timer_entry *entry,
                                       const pj_time_val *delay,
                                       pj_bool_t set_id,
                                       int id_val,
                                       pj_grp_lock_t *grp_lock)
#endif
{
    pj_status_t status;
    pj_time_val expires;

    PJ_ASSERT_RETURN(ht && entry && delay, PJ_EINVAL);
    PJ_ASSERT_RETURN(entry->cb != NULL, PJ_EINVAL);

    /* Prevent same entry from being scheduled more than once */
    PJ_ASSERT_RETURN(entry->_timer_id < 1, PJ_EINVALIDOP);

#if PJ_TIMER_DEBUG
    entry->src_file = src_file;
    entry->src_line = src_line;
#endif

    pj_gettickcount(&expires);
    PJ_TIME_VAL_ADD(expires, *delay);
    
    lock_timer_heap(ht);
    status = schedule_entry(ht, entry, &expires, delay);
    if (status == PJ_SUCCESS) {
        if (set_id)
            entry->id = id_val;
        entry->_grp_lock = grp_lock;
        if (entry->_grp_lock) {
            pj_grp_lock_add_ref(entry->_grp_lock);
        }
    }
    unlock_timer_heap(ht);

    return status;
}

#if PJ_TIMER_DEBUG
PJ_DEF(pj_status_t) pj_timer_heap_schedule_dbg( pj_timer_heap_t *ht,
                       pj_timer_entry *entry,
                       const pj_time_val *delay,
                       const char *src_file,
                       int src_line)
{
    return schedule_w_grp_lock_dbg(ht, entry, delay, PJ_FALSE, 1, NULL, src_file, src_line);
}

PJ_DEF(pj_status_t) pj_timer_heap_schedule_w_grp_lock_dbg(pj_timer_heap_t *ht,
                                                      pj_timer_entry *entry,
                                                      const pj_time_val *delay,
                                                      int id_val,
                                                      pj_grp_lock_t *grp_lock,
                                                      const char *src_file,
                                                      int src_line)
{
    return schedule_w_grp_lock_dbg(ht, entry, delay, PJ_TRUE, id_val, grp_lock, src_file, src_line);
}
#else

PJ_DEF(pj_status_t) pj_timer_heap_schedule( pj_timer_heap_t *ht,
					    pj_timer_entry *entry,
					    const pj_time_val *delay)
{
    return schedule_w_grp_lock(ht, entry, delay, PJ_FALSE, 1, NULL);
}

PJ_DEF(pj_status_t) pj_timer_heap_schedule_w_grp_lock(pj_timer_heap_t *ht,
                                                      pj_timer_entry *entry,
                                                      const pj_time_val *delay,
                                                      int id_val,
                                                      pj_grp_lock_t *grp_lock)
{
    return schedule_w_grp_lock(ht, entry, delay, PJ_TRUE, id_val, grp_lock);
}
#endif

static int cancel_timer(pj_timer_heap_t *ht,
            pj_timer_entry *entry,
            unsigned flags,
            int id_val)
{
    int count;

    PJ_ASSERT_RETURN(ht && entry, PJ_EINVAL);

    lock_timer_heap(ht);
    count = cancel(ht, entry, flags | F_DONT_CALL);
    if (flags & F_SET_ID) {
        entry->id = id_val;
     }
     if (entry->_grp_lock) {
        pj_grp_lock_t *grp_lock = entry->_grp_lock;
        entry->_grp_lock = NULL;
        pj_grp_lock_dec_ref(grp_lock);
    }
    unlock_timer_heap(ht);

    return count;
}


PJ_DEF(int) pj_timer_heap_cancel( pj_timer_heap_t *ht,
				  pj_timer_entry *entry)
{
    return cancel_timer(ht, entry, 0, 0);
}

PJ_DEF(int) pj_timer_heap_cancel_if_active(pj_timer_heap_t *ht,
        pj_timer_entry *entry,
        int id_val)
{
    return cancel_timer(ht, entry, F_SET_ID | F_DONT_ASSERT, id_val);
}


#if PJ_TIMER_DEBUG
PJ_DEF(void) pj_timer_heap_dump(pj_timer_heap_t *ht)
{
    int i;
    lock_timer_heap(ht);

    PJ_LOG(3,(THIS_FILE, "Dumping timer heap:"));
    for (i = 0; i < MAX_ENTRY_PER_HEAP; i++) {
        pj_time_val now;

        PJ_LOG(3,(THIS_FILE, "  Entries: "));
        PJ_LOG(3,(THIS_FILE, "    _id\tId\tElapsed\tSource"));
        PJ_LOG(3,(THIS_FILE, "    ----------------------------------"));

        pj_gettickcount(&now);

        pj_timer_entry *e = ht->entries[i];
        pj_time_val delta;

        if (!e)
        continue;

        if (PJ_TIME_VAL_LTE(e->_timer_value, now)) {
            delta.sec = delta.msec = 0;
        } else {
            delta = e->_timer_value;
            PJ_TIME_VAL_SUB(delta, now);
        }

        PJ_LOG(3,(THIS_FILE, "    %d\t%d\t%d.%03d\t%s:%d",
                        e->_timer_id, e->id,
                        (int)delta.sec, (int)delta.msec,
                        e->src_file, e->src_line));
    }
    unlock_timer_heap(ht);
}
#endif

PJ_DEF(unsigned) pj_timer_heap_poll( pj_timer_heap_t *ht, 
                                     pj_time_val *next_delay )
{
    /* Polling is not necessary on Android, since all async activities
     * are registered to alarmManager.
     */
    PJ_UNUSED_ARG(ht);
    if (next_delay) {
    	next_delay->sec = 600;
    	next_delay->msec = 0;
    }
    return 0;
}

PJ_DEF(pj_size_t) pj_timer_heap_count( pj_timer_heap_t *ht )
{
    PJ_ASSERT_RETURN(ht, 0);
	unsigned count = 0;
	unsigned i;
	for (i = 0; i < MAX_ENTRY_PER_HEAP; i++) {
		if (ht->entries[i] != NULL) {
			count++;
		}
	}
	return count;
}

PJ_DEF(pj_status_t) pj_timer_heap_earliest_time( pj_timer_heap_t * ht,
					         pj_time_val *timeval)
{
    /* We don't support this! */
    PJ_UNUSED_ARG(ht);

    timeval->sec = 1;
    timeval->msec = 0;

    return PJ_SUCCESS;
}

PJ_BEGIN_DECL
PJ_DEF(pj_status_t) pj_timer_fire(int entry_code_id){
    pj_thread_desc a_thread_desc;
	pj_thread_t *a_thread;
	unsigned i, j;
	int entry_id, heap_id;
	pj_timer_entry *entry;

	entry_id = entry_code_id % MAX_ENTRY_PER_HEAP;
	heap_id = entry_code_id / MAX_ENTRY_PER_HEAP;

	if(heap_id < 0 || heap_id >= MAX_HEAPS){
		PJ_LOG(1, (THIS_FILE, "Invalid timer code %d", entry_code_id));
		return PJ_EINVAL;
	}

	// First step is to register the thread if not already done
	if (!pj_thread_is_registered()) {
		char thread_name[160];
		int len = pj_ansi_snprintf(thread_name, sizeof(thread_name),
				"timer_thread_%d", entry_code_id);
		thread_name[len] = '\0';
		pj_thread_register(thread_name, a_thread_desc, &a_thread);
		PJ_LOG(5, (THIS_FILE, "Registered thread %s", thread_name));
	}


	// Find corresponding ht
	pj_timer_heap_t *ht = sHeaps[heap_id];
	if (ht != NULL) {
	    pj_timer_heap_callback* cb = NULL;
	    pj_grp_lock_t *grp_lock;

	    PJ_LOG(5, (THIS_FILE, "FIRE timer %d of heap %d", entry_id + 1, heap_id));

		lock_timer_heap(ht);

		// Get callback if entry valid
		entry = ht->entries[entry_id];
		if (entry != NULL && entry->_timer_id >= 1) {
			cb = entry->cb;
		}
		// Store the group lock
	    grp_lock = entry->_grp_lock;
	    entry->_grp_lock = NULL;

		// Release slot
		ht->entries[entry_id] = NULL;
		ht->entries_reuse_in[entry_id] = REUSE_SLOT_INTERVAL;
		entry->_timer_id = -1;
		unlock_timer_heap(ht);

        // Callback may modify current entry (re-enqueue), so do not fire cb before release the slot
        if (cb) {
            cb(ht, entry);
        }
        if (grp_lock) {
            pj_grp_lock_dec_ref(grp_lock);
        }
		PJ_LOG(5, (THIS_FILE, "FIRE done and released"));
	} else {
		PJ_LOG(2, (THIS_FILE, "FIRE Ignore : No heap found at %d for this entry %d", heap_id, entry_code_id));
	}

	return PJ_SUCCESS;
}
PJ_END_DECL
