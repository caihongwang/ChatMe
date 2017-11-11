#ifndef ZRTP_ANDROID_CALLBACK_H_
#define ZRTP_ANDROID_CALLBACK_H_


#include <pjsua-lib/pjsua.h>

class ZrtpCallback {
public:
    virtual ~ZrtpCallback() {}
    virtual void on_zrtp_show_sas (pjsua_call_id call_id, const pj_str_t *sas, int verified) {}
    virtual void on_zrtp_update_transport (pjsua_call_id call_id) {}
};


extern "C" {
    void setZrtpCallbackObject(ZrtpCallback* callback);
}


#endif /* ZRTP_ANDROID_CALLBACK_H_ */
