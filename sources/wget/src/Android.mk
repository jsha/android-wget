# Makefile to build wget for Android using the Android NDK:
# http://developer.android.com/sdk/ndk/1.5_r1/index.html
# 
# You need to run configure first with these options:
# ./configure --without-ssl --disable-nls --build=arm-eabi
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


LOCAL_MODULE    := wget
LOCAL_SRC_FILES := alloca.c cmpt.c connect.c convert.c cookies.c ftp-basic.c ftp-ls.c ftp-opie.c ftp.c gnu-md5.c gen-md5.c getopt.c  hash.c host.c html-parse.c html-url.c http.c init.c log.c main.c netrc.c progress.c ptimer.c recur.c res.c retr.c safe-ctype.c snprintf.c spider.c url.c utils.c version.c xmalloc.c

NDK_APP_CPPFLAGS := -DHAVE_CONFIG_H

include $(BUILD_EXECUTABLE)
