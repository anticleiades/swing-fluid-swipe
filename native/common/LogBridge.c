/*
 * Copyright 2024 Giuliano Gorgone
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

#include "LogBridge.h"

static jclass class_Logging;
static jmethodID mID_log;
static log_level_t logLevel = eu_giulianogorgone_fluidswipe_utils_log_Logging_OFF;

int nativeLoggingEnabled = 0;

static inline int canLog(int lvl) {
    // the same check is performed at java/util/logging/Logger#isLoggable(Level), but, to avoid unnecessary JNI calls, it's better to do it also in the native side.
    return lvl > eu_giulianogorgone_fluidswipe_utils_log_Logging_OFF && lvl <= eu_giulianogorgone_fluidswipe_utils_log_Logging_ALL && lvl <= logLevel;
}

JNIEXPORT void JNICALL Java_eu_giulianogorgone_fluidswipe_utils_log_Logging_initNative(JNIEnv* env, jclass class, jint level) {
    CHECK_EX_NULL_RET(class);
    class_Logging = (jclass) (*env)->NewGlobalRef(env, class);
    (*env)->DeleteLocalRef(env, class);
    mID_log = (*env)->GetStaticMethodID(env, class_Logging, "log", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V");
    CHECK_EX_NULL_RET(mID_log);
    logLevel = level;
    nativeLoggingEnabled = 1;
}

void jLog(JNIEnv* env, log_level_t level, const char* tag, const char* methodName, const char* msg, jthrowable throwable) {
    if(!canLog(level)) return;
    jstring jTag = (*env)->NewStringUTF(env, tag);
    jstring jMethodName = (*env)->NewStringUTF(env, methodName);
    jstring jMsg = (*env)->NewStringUTF(env, msg);
    (*env)->CallStaticVoidMethod(env, class_Logging, mID_log, level, jTag, jMethodName, jMsg, throwable);
    (*env)->DeleteLocalRef(env, jTag);
    (*env)->DeleteLocalRef(env, jMethodName);
    (*env)->DeleteLocalRef(env, jMsg);
    (*env)->DeleteLocalRef(env, throwable);
}
