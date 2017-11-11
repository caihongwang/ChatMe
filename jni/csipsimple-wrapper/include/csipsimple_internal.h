#ifndef CSIPSIMPLE_INTERNAL_H_
#define CSIPSIMPLE_INTERNAL_H_

#include "pjsua_jni_addons.h"

PJ_BEGIN_DECL

PJ_DECL(void*) get_library_factory(dynamic_factory *impl);


struct css_data {
    pj_pool_t	    *pool;	    /**< Pool for the css app. */

    // Audio codecs
	unsigned 		extra_aud_codecs_cnt;
	dynamic_factory 	extra_aud_codecs[64];

	// Video codecs
	unsigned 		extra_vid_codecs_cnt;
	dynamic_factory 	extra_vid_codecs[64];
	dynamic_factory 	extra_vid_codecs_destroy[64];

	// About ringback
    int			    ringback_slot;
    int			    ringback_cnt;
    pjmedia_port	   *ringback_port;
    pj_bool_t ringback_on;

    // About zrtp cfg
    pj_bool_t default_use_zrtp;
    char zid_file[512];

    jobject context;

};

extern struct css_data css_var;

PJ_END_DECL

#endif /* CSIPSIMPLE_INTERNAL_H_ */
