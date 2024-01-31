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

#ifndef Utils_h
#define Utils_h
#include "LogBridge.h"

#define STR(str) #str
#define LOG(lvl, msg) if(nativeLoggingEnabled) {jLog(env, lvl, __FILE_NAME__, __func__, msg, NULL);}
#define LOG_EX(lvl, msg, throwable) if(nativeLoggingEnabled) {jLog(env, lvl, __FILE_NAME__, __func__, msg, throwable);}

#define EXC_CHECK_AND_REPORT(code) \
do { \
    if((*env)->ExceptionCheck(env)) { \
        jthrowable thrown = (*env)->ExceptionOccurred(env); \
        LOG_EX(eu_giulianogorgone_fluidswipe_utils_log_Logging_SEVERE, "exception occurred", thrown); \
        (*env)->ExceptionClear(env); \
        {code;}; \
    } \
} while(0); \

#define CHECK_NULL_RET_V(ref, ret) \
    if(!ref) { \
        return ret; \
    } \

#define CHECK_NULL_RET(ref) \
    CHECK_NULL_RET_V(ref,) \

#define CHECK_EX_NULL_RET_V(ref, ret) \
    EXC_CHECK_AND_REPORT(); \
    CHECK_NULL_RET_V(ref, ret) \


#define CHECK_EX_NULL_RET(ref) \
    CHECK_EX_NULL_RET_V(ref,)

#define VALIDATE_REF(ref) ((*env)->GetObjectRefType(env, ref) != JNIInvalidRefType ? ref: (jobject) NULL)

#endif /* Utils_h */
