###########
# PJMEDIA #
###########
TOOLCHAIN_PATH:=$(call my-dir)
LOCAL_PATH := $(TOOLCHAIN_PATH)/../../sources/pjmedia
include $(CLEAR_VARS)

LOCAL_MODULE    := pjmedia

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../pjlib/include $(LOCAL_PATH)/../pjlib-util/include \
	$(LOCAL_PATH)/../pjnath/include $(LOCAL_PATH)/include/ $(LOCAL_PATH)/.. \
	$(LOCAL_PATH)/../third_party/srtp/include $(LOCAL_PATH)../third_party/srtp/include \
	$(LOCAL_PATH)/../third_party/srtp/crypto/include $(LOCAL_PATH)/../third_party/build/srtp \
	$(LOCAL_PATH)/../third_party/build/speex $(LOCAL_PATH)/../third_party/speex/include \
	$(LOCAL_PATH)/../third_party


LOCAL_CFLAGS := $(MY_PJSIP_FLAGS)
PJLIB_SRC_DIR := src/pjmedia
PJMEDIADEV_SRC_DIR := src/pjmedia-audiodev
PJMEDIADEV_VIDEO_SRC_DIR := src/pjmedia-videodev
PJMEDIACODEC_SRC_DIR := src/pjmedia-codec

LOCAL_SRC_FILES := $(PJLIB_SRC_DIR)/alaw_ulaw.c $(PJLIB_SRC_DIR)/alaw_ulaw_table.c \
	$(PJLIB_SRC_DIR)/bidirectional.c $(PJLIB_SRC_DIR)/format.c \
	$(PJLIB_SRC_DIR)/clock_thread.c $(PJLIB_SRC_DIR)/codec.c \
	$(PJLIB_SRC_DIR)/conference.c $(PJLIB_SRC_DIR)/conf_switch.c $(PJLIB_SRC_DIR)/delaybuf.c $(PJLIB_SRC_DIR)/echo_common.c \
	$(PJLIB_SRC_DIR)/echo_speex.c $(PJLIB_SRC_DIR)/echo_port.c $(PJLIB_SRC_DIR)/echo_suppress.c $(PJLIB_SRC_DIR)/endpoint.c $(PJLIB_SRC_DIR)/errno.c \
	$(PJLIB_SRC_DIR)/g711.c $(PJLIB_SRC_DIR)/jbuf.c $(PJLIB_SRC_DIR)/master_port.c \
	$(PJLIB_SRC_DIR)/mem_capture.c $(PJLIB_SRC_DIR)/mem_player.c \
	$(PJLIB_SRC_DIR)/null_port.c $(PJLIB_SRC_DIR)/plc_common.c $(PJLIB_SRC_DIR)/port.c $(PJLIB_SRC_DIR)/splitcomb.c \
	$(PJLIB_SRC_DIR)/resample_resample.c $(PJLIB_SRC_DIR)/resample_libsamplerate.c \
	$(PJLIB_SRC_DIR)/resample_port.c $(PJLIB_SRC_DIR)/rtcp.c $(PJLIB_SRC_DIR)/rtcp_xr.c $(PJLIB_SRC_DIR)/rtp.c \
	$(PJLIB_SRC_DIR)/sdp.c $(PJLIB_SRC_DIR)/sdp_cmp.c $(PJLIB_SRC_DIR)/sdp_neg.c \
	$(PJLIB_SRC_DIR)/session.c $(PJLIB_SRC_DIR)/silencedet.c \
	$(PJLIB_SRC_DIR)/sound_port.c $(PJLIB_SRC_DIR)/stereo_port.c \
	$(PJLIB_SRC_DIR)/stream_common.c $(PJLIB_SRC_DIR)/stream_info.c \
	$(PJLIB_SRC_DIR)/stream.c $(PJLIB_SRC_DIR)/tonegen.c $(PJLIB_SRC_DIR)/transport_adapter_sample.c \
	$(PJLIB_SRC_DIR)/transport_ice.c $(PJLIB_SRC_DIR)/transport_loop.c \
	$(PJLIB_SRC_DIR)/transport_srtp.c $(PJLIB_SRC_DIR)/transport_udp.c \
	$(PJLIB_SRC_DIR)/wav_player.c $(PJLIB_SRC_DIR)/wav_playlist.c $(PJLIB_SRC_DIR)/wav_writer.c $(PJLIB_SRC_DIR)/wave.c \
	$(PJLIB_SRC_DIR)/wsola.c \
	$(PJLIB_SRC_DIR)/vid_port.c $(PJLIB_SRC_DIR)/vid_codec.c \
	$(PJLIB_SRC_DIR)/vid_stream.c $(PJLIB_SRC_DIR)/vid_stream_info.c $(PJLIB_SRC_DIR)/vid_tee.c \
	$(PJLIB_SRC_DIR)/converter.c $(PJLIB_SRC_DIR)/event.c \
	$(PJMEDIADEV_SRC_DIR)/audiodev.c $(PJMEDIADEV_SRC_DIR)/audiotest.c $(PJMEDIADEV_SRC_DIR)/errno.c \
	$(PJMEDIADEV_VIDEO_SRC_DIR)/videodev.c $(PJMEDIADEV_VIDEO_SRC_DIR)/colorbar_dev.c $(PJMEDIADEV_VIDEO_SRC_DIR)/errno.c \
	$(PJMEDIACODEC_SRC_DIR)/amr_sdp_match.c

# If not csipsimple, load default audio codecs loader from pjmedia
ifneq ($(MY_USE_CSIPSIMPLE),1)
	LOCAL_SRC_FILES += $(PJMEDIACODEC_SRC_DIR)/audio_codecs.c
endif

ifeq ($(MY_USE_G722),1)
	LOCAL_SRC_FILES += $(PJMEDIACODEC_SRC_DIR)/g722.c $(PJMEDIACODEC_SRC_DIR)/g722/g722_enc.c $(PJMEDIACODEC_SRC_DIR)/g722/g722_dec.c
endif
ifeq ($(MY_USE_SPEEX),1)
	LOCAL_SRC_FILES += $(PJMEDIACODEC_SRC_DIR)/speex_codec.c 
endif
ifeq ($(MY_USE_AMR),1)
	LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../../amr-stagefright
	LOCAL_SRC_FILES += $(PJMEDIACODEC_SRC_DIR)/opencore_amr.c 
endif
ifeq ($(MY_USE_ILBC),1)
	LOCAL_SRC_FILES += $(PJMEDIACODEC_SRC_DIR)/ilbc.c 
endif
ifeq ($(MY_USE_GSM),1)
	LOCAL_SRC_FILES += $(PJMEDIACODEC_SRC_DIR)/gsm.c 
endif

ifeq ($(MY_USE_WEBRTC),1)
	LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../../webrtc/sources
	
	#AEC
	LOCAL_SRC_FILES += $(PJLIB_SRC_DIR)/echo_webrtc_aec.c 
endif

PJ_ANDROID_SRC_DIR := ../../android_sources/pjmedia/src

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../android_sources/pjmedia/include/pjmedia-audiodev \
	$(LOCAL_PATH)/../../../swig-glue
LOCAL_SRC_FILES += $(PJ_ANDROID_SRC_DIR)/pjmedia-audiodev/android_jni_dev.cpp


include $(BUILD_STATIC_LIBRARY)

include $(TOOLCHAIN_PATH)/Codec_g7221.mk
include $(TOOLCHAIN_PATH)/OpenSL_ES.mk
include $(TOOLCHAIN_PATH)/Video.mk
include $(TOOLCHAIN_PATH)/Screencapture.mk

$(call import-module,cpufeatures)

