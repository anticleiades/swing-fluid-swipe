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

#ifndef LogBridge_h
#define LogBridge_h

#include "Utils.h"
#include "eu_giulianogorgone_fluidswipe_utils_log_Logging.h"

extern int nativeLoggingEnabled;
typedef int log_level_t;

void jLog(JNIEnv* env, const log_level_t level, const char* tag, const char* methodName, const char* msg, jthrowable throwable);
#endif /* LogBridge_h */
