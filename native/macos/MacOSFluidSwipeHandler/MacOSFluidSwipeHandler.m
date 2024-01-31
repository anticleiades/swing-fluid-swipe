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
@import Foundation;
@import AppKit;

#include "eu_giulianogorgone_fluidswipe_event_handling_BridgeConstants.h"
#include "eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler.h"
#include "VM.h"

#define GESTURE_STATE_NOT_YET_DEFINED 0

static const NSEventSwipeTrackingOptions SWIPE_TRACK_OPTIONS = (NSEventSwipeTrackingLockDirection | NSEventSwipeTrackingClampGestureAmount);

#ifdef SUPPORT_GESTURE_THRESHOLD
static jdouble gestureAmountThreshold = 1.0; // default=1.0
#endif

static BOOL continueGestureOnFingerRelease = YES; // default=YES

static BOOL libActive = NO;
static BOOL deferStopToGestureCompletion = NO;
static id eventMonitor = nil;
static jmethodID mID_dispatchFluidSwipeEvent = NULL;
static jclass FluidSwipeDispatcher = NULL;
static jclass CPlatformWindow = NULL;

static NSEvent* currentEvent = nil;
static BOOL gestureActive = NO;
static jint gestureState = GESTURE_STATE_NOT_YET_DEFINED; // can be CANCELED, SUCCESS or NOT_YET_DEFINED (0)

BOOL verbose = YES;

static inline BOOL hasFlag(jint x, jint flag) {
    return (x & flag) == flag;
}

static inline void stopLib(JNIEnv* env) {
    [NSEvent removeMonitor: eventMonitor]; // releases the event monitor, no need to manually RR an event monitor — ref: AppKit docs
    eventMonitor = nil;
    libActive = NO;
    (*env)->DeleteGlobalRef(env, FluidSwipeDispatcher);
    (*env)->DeleteGlobalRef(env, CPlatformWindow);
    deferStopToGestureCompletion = NO;
    LOG(eu_giulianogorgone_fluidswipe_utils_log_Logging_CONFIG, "event monitoring off");
}

static inline void cleanup(JNIEnv* env) {
    gestureState = GESTURE_STATE_NOT_YET_DEFINED;
    gestureActive = NO;
    CLEAR(currentEvent)
    LOG(eu_giulianogorgone_fluidswipe_utils_log_Logging_FINEST, "done");
    if(deferStopToGestureCompletion) {
        stopLib(env);
    }
}

static inline jint nsEventPhaseToJavaPhase(NSEventPhase phase) {
    switch (phase) {
        case NSEventPhaseBegan:
            return eu_giulianogorgone_fluidswipe_event_handling_BridgeConstants_LOGICALLY_BEGAN;
        case NSEventPhaseChanged:
            return eu_giulianogorgone_fluidswipe_event_handling_BridgeConstants_PROGRESSED;
        case NSEventPhaseNone:
            return eu_giulianogorgone_fluidswipe_event_handling_BridgeConstants_PROGRESSED_NO_MORE_TOUCHING;
        case NSEventPhaseEnded:
            return eu_giulianogorgone_fluidswipe_event_handling_BridgeConstants_UPDATE_STATE | eu_giulianogorgone_fluidswipe_event_handling_BridgeConstants_COMPLETED;
        case NSEventPhaseCancelled:
            return eu_giulianogorgone_fluidswipe_event_handling_BridgeConstants_UPDATE_STATE | eu_giulianogorgone_fluidswipe_event_handling_BridgeConstants_CANCELED;
        case NSEventPhaseStationary:
        case NSEventPhaseMayBegin:
            return eu_giulianogorgone_fluidswipe_event_handling_BridgeConstants_UNHANDLED;
    }
}

#ifdef SUPPORT_GESTURE_THRESHOLD

static inline BOOL thresholdReached(CGFloat gestureAmount) {
    return gestureAmount >= gestureAmountThreshold;
}

static inline CGFloat clampGestureAmount(CGFloat gestureAmount) {
    return gestureAmount < gestureAmountThreshold ? gestureAmount : gestureAmountThreshold;
}
#endif

static inline jboolean logicallyStartGesture(JNIEnv* env, NSEvent* event) {
    gestureActive = YES;
    jboolean isNaturalScrollingEnabled = (jboolean) [event isDirectionInvertedFromDevice];
    LOG(eu_giulianogorgone_fluidswipe_utils_log_Logging_FINEST, "gesture logically started");
    @try {
        [event trackSwipeEventWithOptions: SWIPE_TRACK_OPTIONS dampenAmountThresholdMin:-(1.0) max:1.0 usingHandler:^(CGFloat appKitAmount, NSEventPhase phase, BOOL isComplete, BOOL * _Nonnull stop) {
            jint javaPhase = nsEventPhaseToJavaPhase(phase);
            if(hasFlag(javaPhase, eu_giulianogorgone_fluidswipe_event_handling_BridgeConstants_UPDATE_STATE)) {
                gestureState = javaPhase & eu_giulianogorgone_fluidswipe_event_handling_BridgeConstants_ENDED_MASK;
                LOG(eu_giulianogorgone_fluidswipe_utils_log_Logging_FINEST, "gesture physically ended; input device is no longer being touched");
            }
            CGFloat appKitAbsAmount = fabs(appKitAmount);
#ifdef SUPPORT_GESTURE_THRESHOLD
            BOOL forceCompletion = (!continueGestureOnFingerRelease || thresholdReached(appKitAbsAmount)) && gestureState != 0 && !isComplete;
            CGFloat javaGestureAmount = clampGestureAmount(appKitAbsAmount);
#else
            BOOL forceCompletion = !continueGestureOnFingerRelease && gestureState != 0 && !isComplete;
            #define javaGestureAmount appKitAbsAmount
#endif
            if (forceCompletion || isComplete) {
                (*env)->CallStaticVoidMethod(env, FluidSwipeDispatcher, mID_dispatchFluidSwipeEvent, javaGestureAmount, gestureState, isNaturalScrollingEnabled);
                if(forceCompletion) {
                    LOG(eu_giulianogorgone_fluidswipe_utils_log_Logging_FINEST, "forcefully make gesture logically end");
                    (*stop) = YES;
                } else
                    LOG(eu_giulianogorgone_fluidswipe_utils_log_Logging_FINEST, "gesture logically ended");
                cleanup(env);
            } else if (javaPhase != eu_giulianogorgone_fluidswipe_event_handling_BridgeConstants_PROGRESSED_NO_MORE_TOUCHING || continueGestureOnFingerRelease) {
                (*env)->CallStaticVoidMethod(env, FluidSwipeDispatcher, mID_dispatchFluidSwipeEvent, javaGestureAmount, javaPhase, isNaturalScrollingEnabled);
            }
            EXC_CHECK_AND_REPORT();
        }];
    } @catch (NSException *exception) {
        NSLog(@"%@", [exception callStackSymbols]);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

/*
 * Class:     eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler
 * Method:    nativeSetContinueGestureOnFingerRelease
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler_nativeSetContinueGestureOnFingerRelease(JNIEnv * env, jclass class, jboolean v) {
    JNI_COCOA(EXECUTE_ON_APPKIT_THREAD_AND_WAIT(continueGestureOnFingerRelease = v))
}

#ifdef SUPPORT_GESTURE_THRESHOLD
/*
 * Class:     eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler
 * Method:    nativeSetGestureAmountThreshold
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler_nativeSetGestureAmountThreshold(JNIEnv* env, jclass class, jdouble requestedGestureAmountThreshold) {
    JNI_COCOA(EXECUTE_ON_APPKIT_THREAD_AND_WAIT(gestureAmountThreshold = requestedGestureAmountThreshold))
}
#endif

/*
 * Class:     eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler
 * Method:    nativeStopEventMonitoring
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler_nativeStopEventMonitoring(JNIEnv* env, jclass class) {
    JNI_COCOA(EXECUTE_ON_APPKIT_THREAD_AND_WAIT(
                                                if(libActive && !deferStopToGestureCompletion) {
                                                    deferStopToGestureCompletion = gestureActive;
                                                    if(!deferStopToGestureCompletion) {
                                                        stopLib(getAppKitEnv());
                                                    }
                                                }
                                                ))
}

static inline jclass globalRefOfClass(JNIEnv* env, const char *className) {
    jclass localRefDispatcher = (jclass) (*env)->FindClass(env, className);
    CHECK_EX_NULL_RET_V(localRefDispatcher, NULL)
    jclass global = (jclass) (*env)->NewGlobalRef(env, localRefDispatcher); // create a global reference valid across all threads
    (*env)->DeleteLocalRef(env, localRefDispatcher);
    return global;
}

/*
 * Class:     eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler
 * Method:    nativeStartEventMonitoring
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler_nativeStartEventMonitoring(JNIEnv* envNotAppKit, jclass class, jboolean sync_mode) {
    JNI_COCOA(EXECUTE_ON_APPKIT_THREAD_AND_WAIT({
        if(libActive || deferStopToGestureCompletion) {
            return;
        }
        JNIEnv* env = getAppKitEnv();
        FluidSwipeDispatcher = globalRefOfClass(env, "eu/giulianogorgone/fluidswipe/event/handling/FluidSwipeDispatcher");
        CHECK_NULL_RET(FluidSwipeDispatcher)
     
        SEL SELjavaPlatformWindow = sel_registerName("javaPlatformWindow");
        Class AWTWindow = NSClassFromString(@"AWTWindow");
        CHECK_NULL_RET(AWTWindow)
     
        if(sync_mode) {
            LOG(eu_giulianogorgone_fluidswipe_utils_log_Logging_CONFIG, "starting event monitoring; mode=SYNC");
        } else {
            LOG(eu_giulianogorgone_fluidswipe_utils_log_Logging_CONFIG, "starting event monitoring; mode=ASYNC");
        }
        
        CPlatformWindow = globalRefOfClass(env, "sun/lwawt/macosx/CPlatformWindow");
        CHECK_NULL_RET(CPlatformWindow)
        
        jmethodID mID_notifyFluidSwipeBegan = (*env)->GetStaticMethodID(env, FluidSwipeDispatcher, sync_mode ? "notifyFluidSwipeBeganSync": "notifyFluidSwipeBeganAsync", sync_mode ? "(Ljava/awt/Window;DDDZ)Z":  "(Ljava/awt/Window;DDDZ)V");
        mID_dispatchFluidSwipeEvent = (*env)->GetStaticMethodID(env, FluidSwipeDispatcher, "dispatchFluidSwipeEvent", "(DIZ)V");
        jfieldID fID_awtWindow = (*env)->GetFieldID(env, CPlatformWindow, "target", "Ljava/awt/Window;");
        
        CHECK_EX_NULL_RET(mID_notifyFluidSwipeBegan)
        CHECK_NULL_RET(mID_dispatchFluidSwipeEvent)
        CHECK_NULL_RET(fID_awtWindow)
        
        eventMonitor = [NSEvent addLocalMonitorForEventsMatchingMask: NSEventMaskScrollWheel handler: ^(NSEvent* event) { // monitoring application for scroll events
            @try {
                if([event buttonNumber] == 0 && [event phase] == NSEventPhaseBegan && [event scrollingDeltaY] == 0.0 && fabs([event scrollingDeltaX]) >= 0 && !gestureActive) {
                    if(![NSEvent isSwipeTrackingFromScrollEventsEnabled]) {
                        LOG(eu_giulianogorgone_fluidswipe_utils_log_Logging_CONFIG, "candidate event discarded, as fluid-swipe has been disabled in System Preferences/System Settings");
                        return event;
                    }
                    NSWindow* evt_win = [event window]; // window in which event occurred
                    CHECK_NULL_RET_V(evt_win, event) //  since window property is nullable in NSEvent, opportune checks are performed;
                    NSObject* delegate = [evt_win delegate]; // the delegate's class is expected to be AWTWindow
                    CHECK_NULL_RET_V(delegate, event)
                    if(!([delegate isKindOfClass:AWTWindow] && [delegate respondsToSelector:SELjavaPlatformWindow])) { // the latter check might be redundant
                        LOG(eu_giulianogorgone_fluidswipe_utils_log_Logging_INFO, "the window in which the event occurred is not kind of AWTWindow");
                        return event;
                    }
                    
                    NSPoint location = [event locationInWindow];
                    location.y = [evt_win frame].size.height - location.y; // convert from Cocoa to AWT/Swing coordinate system
                    
                    
                    jobject weakRefPlatformWin = VALIDATE_REF((jobject) [delegate performSelector:SELjavaPlatformWindow]);
                    CHECK_NULL_RET_V(weakRefPlatformWin, event)
                    
                    jobject platform_window = (*env)->NewLocalRef(env, weakRefPlatformWin); // "[AWTWindow javaPlatformWindow]" is a global weak reference, promoting it to strong reference
                    CHECK_NULL_RET_V(platform_window, event)
                    jobject awtWindow = VALIDATE_REF((*env)->GetObjectField(env, platform_window, fID_awtWindow)); // extracting the AWT/Swing window
                    CHECK_NULL_RET_V(awtWindow, event)
                    
                    LOG(eu_giulianogorgone_fluidswipe_utils_log_Logging_FINEST, "notifying Java clients that a fluid-swipe gesture physically began and may also logically do");
                    
                    if(sync_mode) {
                        jboolean gestureMustLogicallyStart = (*env)->CallStaticBooleanMethod(env, FluidSwipeDispatcher, mID_notifyFluidSwipeBegan, awtWindow, [event scrollingDeltaX], location.x, location.y, [event isDirectionInvertedFromDevice]);
                        BOOL exceptionOccurred = NO;
                        EXC_CHECK_AND_REPORT(exceptionOccurred = YES);
                        if(gestureMustLogicallyStart) { // tell Java that a fluid-swipe gesture physically started
                            logicallyStartGesture(env, event); // the gesture logically starts, as no one blocked it.
                        } else if(!exceptionOccurred) {
                            LOG(eu_giulianogorgone_fluidswipe_utils_log_Logging_FINEST, "fluid-swipe veto received");
                        }
                    } else { // async mode
                        currentEvent = [event retain];
                        // tell Java that a fluid-swipe gesture physically started.
                        (*env)->CallStaticVoidMethod(env, FluidSwipeDispatcher, mID_notifyFluidSwipeBegan, awtWindow, [currentEvent scrollingDeltaX], location.x, location.y, [currentEvent isDirectionInvertedFromDevice]);
                    }
                    (*env)->DeleteLocalRef(env, awtWindow);
                    (*env)->DeleteLocalRef(env, platform_window);
                }
            } @catch (NSException *exception) {
                NSLog(@"%@", [exception callStackSymbols]);
            } @finally {
                EXC_CHECK_AND_REPORT(cleanup(env));
                return event;
            };
        }];
        libActive = true;
    }))
}

/*
 * Class:     eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler
 * Method:    nativeVetoFluidSwipe
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler_nativeVetoFluidSwipe(JNIEnv* env, jclass class) {
    JNI_COCOA(EXECUTE_ON_APPKIT_THREAD_AND_WAIT({
        LOG(eu_giulianogorgone_fluidswipe_utils_log_Logging_FINEST, "fluid-swipe veto received");
        CLEAR(currentEvent))
    })
}

/*
 * Class:     eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler
 * Method:    nativeLogicallyStartFluidSwipe
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler_nativeLogicallyStartFluidSwipe(JNIEnv* env, jclass class) {
    JNI_COCOA({
        __block jboolean result = false;
        EXECUTE_ON_APPKIT_THREAD_AND_WAIT(result = logicallyStartGesture(getAppKitEnv(), currentEvent))
        return result;
    })
    return JNI_FALSE;
}

