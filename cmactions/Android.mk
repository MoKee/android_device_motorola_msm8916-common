LOCAL_PATH:= $(call my-dir)

# exclude harpia from CMActions
ifneq ($(filter lux merlin osprey surnia, $(TARGET_DEVICE)),)
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_PACKAGE_NAME := CMActions
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_STATIC_JAVA_LIBRARIES := \
    org.mokee.platform.internal
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
include $(BUILD_PACKAGE)
endif

include $(call all-makefiles-under,$(LOCAL_PATH))
