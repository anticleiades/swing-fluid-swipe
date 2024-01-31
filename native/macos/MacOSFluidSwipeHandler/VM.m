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

#include "VM.h"

static JavaVM* _vm;
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    _vm = vm;
    return JNI_VERSION_1_8;
}

JNIEnv* getAppKitEnv(void) {
    if(![NSThread isMainThread])  {
         NSLog(@"to be executed only under AppKit Thread; returning NULL!");
        return NULL;
    }
    static JNIEnv* env = NULL;
    if(!env) {
        JavaVMAttachArgs args;
        args.version = JNI_VERSION_1_4;
        args.name = "AppKit Thread";
        args.group = NULL;
        (*_vm)->AttachCurrentThreadAsDaemon(_vm, (void**) &env, &args);
        LOG(eu_giulianogorgone_fluidswipe_utils_log_Logging_CONFIG, "attached AppKit thread as daemon");
    }
    return env;
}

