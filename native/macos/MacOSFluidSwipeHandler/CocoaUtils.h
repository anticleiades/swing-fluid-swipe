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

#ifndef CocoaUtils_h
#define CocoaUtils_h
#include "Utils.h"

#define PRINT_VAR(var) NSLog(@STR(var: %@), @(var));

static inline void runOnAppKitThreadAndWait(dispatch_block_t block) {
    if ([NSThread isMainThread]) {
        @autoreleasepool {
            block();
        }
    } else {
        dispatch_sync(dispatch_get_main_queue(), ^{
            @autoreleasepool {
                block();
            }
        });
    }
}

static inline void runOnAppKitThreadAsync(dispatch_block_t block) {
    if ([NSThread isMainThread]) {
        @autoreleasepool { 
            block();
        }
    } else {
        dispatch_async(dispatch_get_main_queue(), ^{
            @autoreleasepool {
                block();
            }
        });
    }
}

#define CLEAR(ref) \
do { \
    if(ref) {\
        [ref release]; \
        ref = nil; \
    } \
} while (0)


#endif /* CocoaUtils_h */
