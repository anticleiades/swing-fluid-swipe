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
static BOOL gestureWaitingForJavaDecision = NO;
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

static inline void stopLib(JNIEnv* cleanupEnv) {
    if(!libActive)
        return;
    [NSEvent removeMonitor: eventMonitor]; // releases the event monitor, no need to manually RR an event monitor — ref: AppKit docs
    eventMonitor = nil;
    libActive = NO;
  
    // global-ref are thread-safe; we can use the env we want
    (*cleanupEnv)->DeleteGlobalRef(cleanupEnv, FluidSwipeDispatcher);
    (*cleanupEnv)->DeleteGlobalRef(cleanupEnv, CPlatformWindow);
    deferStopToGestureCompletion = NO;
}

static inline void cleanup(JNIEnv* env) {
    gestureState = GESTURE_STATE_NOT_YET_DEFINED;
    gestureWaitingForJavaDecision = NO;
    gestureActive = NO;
    CLEAR(currentEvent);
    LOG(env, eu_giulianogorgone_fluidswipe_utils_log_Logging_FINEST, "done");
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

static inline void logicallyStartGesture(JNIEnv* _env, NSEvent* event) {
    gestureActive = YES;
    gestureWaitingForJavaDecision = NO;
    if (!_env) {
        NSLog(@"_env is null");
        return;
    }
    jboolean isNaturalScrollingEnabled = (jboolean) [event isDirectionInvertedFromDevice];
    LOG(_env, eu_giulianogorgone_fluidswipe_utils_log_Logging_FINEST, "gesture logically started");
    [event trackSwipeEventWithOptions: SWIPE_TRACK_OPTIONS dampenAmountThresholdMin:-(1.0) max:1.0 usingHandler:^(CGFloat appKitAmount, NSEventPhase phase, BOOL isComplete, BOOL * _Nonnull stop) {
        JNIEnv* blockEnv = getAppKitEnv();
        if (!blockEnv) {
            NSLog(@"getAppKitEnv failed (returned null) ");
            *stop = YES;
            return;
        }
        jint javaPhase = nsEventPhaseToJavaPhase(phase);
        if(hasFlag(javaPhase, eu_giulianogorgone_fluidswipe_event_handling_BridgeConstants_UPDATE_STATE)) {
            gestureState = javaPhase & (eu_giulianogorgone_fluidswipe_event_handling_BridgeConstants_COMPLETED | eu_giulianogorgone_fluidswipe_event_handling_BridgeConstants_CANCELED);
            LOG(blockEnv, eu_giulianogorgone_fluidswipe_utils_log_Logging_FINEST, "gesture physically ended; input device is no longer being touched");
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
            (*blockEnv)->CallStaticVoidMethod(blockEnv, FluidSwipeDispatcher, mID_dispatchFluidSwipeEvent, javaGestureAmount, gestureState, isNaturalScrollingEnabled);
            if(forceCompletion) {
                LOG(blockEnv, eu_giulianogorgone_fluidswipe_utils_log_Logging_FINEST, "forcefully make gesture logically end");
                (*stop) = YES;
            } else
                LOG(blockEnv, eu_giulianogorgone_fluidswipe_utils_log_Logging_FINEST, "gesture logically ended");
            cleanup(blockEnv);
        } else if (javaPhase != eu_giulianogorgone_fluidswipe_event_handling_BridgeConstants_PROGRESSED_NO_MORE_TOUCHING || continueGestureOnFingerRelease) {
            (*blockEnv)->CallStaticVoidMethod(blockEnv, FluidSwipeDispatcher, mID_dispatchFluidSwipeEvent, javaGestureAmount, javaPhase, isNaturalScrollingEnabled);
        }
        EXC_CHECK_AND_REPORT(blockEnv, {});
    }];
}

/*
 * Class:     eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler
 * Method:    nativeSetContinueGestureOnFingerRelease
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler_nativeSetContinueGestureOnFingerRelease(JNIEnv * env, jclass class, jboolean v) {
    runOnAppKitThreadAsync(^ {
        continueGestureOnFingerRelease = v;
    });
}

#ifdef SUPPORT_GESTURE_THRESHOLD
/*
 * Class:     eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler
 * Method:    nativeSetGestureAmountThreshold
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler_nativeSetGestureAmountThreshold(JNIEnv* env, jclass class, jdouble requestedGestureAmountThreshold) {
    runOnAppKitThreadAsync(^ {
        gestureAmountThreshold = requestedGestureAmountThreshold;
    });
}
#endif

/*
 * Class:     eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler
 * Method:    nativeStopEventMonitoring
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler_nativeStopEventMonitoring(JNIEnv* env, jclass class) {
    runOnAppKitThreadAsync(^ {
        if(libActive && !deferStopToGestureCompletion) {
            deferStopToGestureCompletion = gestureActive;
            if(!deferStopToGestureCompletion) {
                //cleanupEnv can be any valid env
                stopLib(env);
            }
        }
    });
    LOG(env, eu_giulianogorgone_fluidswipe_utils_log_Logging_CONFIG, "event monitoring off");
}

static inline jclass globalRefOfClass(JNIEnv* env, const char *className) {
    jclass localRefDispatcher = (jclass) (*env)->FindClass(env, className);
    CHECK_EX_NULL_RET_V(env, localRefDispatcher, NULL);
    jclass global = (jclass) (*env)->NewGlobalRef(env, localRefDispatcher); // create a global reference valid across all threads
    (*env)->DeleteLocalRef(env, localRefDispatcher);
    return global;
}

static inline bool detectHorizontalSwipe(NSEvent *event) {
    if (![event hasPreciseScrollingDeltas]) {
        return false;
    }
    
    CGFloat deltaX = fabs([event scrollingDeltaX]);
    CGFloat deltaY = fabs([event scrollingDeltaY]);
    
    if (deltaX <= deltaY) return false;

    return true;
}

static inline bool isCandidateFluidSwipe(NSEvent * event) {
    // paranoia check: it must be a scroll event
    if ([event type] != NSEventTypeScrollWheel) return false;
    // ensure it is from magic mouse or trackpad
    if (![event hasPreciseScrollingDeltas]) return false;
    return [event phase] == NSEventPhaseBegan && [event momentumPhase] == NSEventPhaseNone && detectHorizontalSwipe(event);
}

/*
 * Class:     eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler
 * Method:    nativeStartEventMonitoring
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler_nativeStartEventMonitoring(JNIEnv* envNotAppKit, jclass class) {
    runOnAppKitThreadAsync(^{
        if(libActive || deferStopToGestureCompletion) {
            return;
        }
        JNIEnv* _env = getAppKitEnv();
        if (!_env) {
            NSLog(@"getAppKitEnv failed (returned null) ");
            return;
        }
        FluidSwipeDispatcher = globalRefOfClass(_env, "eu/giulianogorgone/fluidswipe/event/handling/FluidSwipeDispatcher");
        CHECK_NULL_RET(_env, FluidSwipeDispatcher);

        SEL SELjavaPlatformWindow = sel_registerName("javaPlatformWindow");
        Class AWTWindow =  NSClassFromString(@"AWTWindow");
        if(!AWTWindow) {
            LOG(_env, eu_giulianogorgone_fluidswipe_utils_log_Logging_SEVERE, "AWTWindow class not found. Cannot initialize the library. Ensure that AWT/Swing is properly initialized.");
            return;
        }
        
        LOG(_env, eu_giulianogorgone_fluidswipe_utils_log_Logging_CONFIG, "starting event monitoring");
        
        
        CPlatformWindow = globalRefOfClass(_env, "sun/lwawt/macosx/CPlatformWindow");
        CHECK_NULL_RET(_env, CPlatformWindow)
        
        jmethodID mID_notifyFluidSwipeBegan = (*_env)->GetStaticMethodID(_env, FluidSwipeDispatcher, "notifyFluidSwipeBeganAsync", "(Ljava/awt/Window;DDDZ)V");
        mID_dispatchFluidSwipeEvent = (*_env)->GetStaticMethodID(_env, FluidSwipeDispatcher, "dispatchFluidSwipeEvent", "(DIZ)V");
        jfieldID fID_awtWindow = (*_env)->GetFieldID(_env, CPlatformWindow, "target", "Ljava/awt/Window;");
        
        CHECK_EX_NULL_RET(_env, mID_notifyFluidSwipeBegan);
        CHECK_NULL_RET(_env, mID_dispatchFluidSwipeEvent);
        CHECK_NULL_RET(_env, fID_awtWindow);
        
        eventMonitor = [NSEvent addLocalMonitorForEventsMatchingMask: NSEventMaskScrollWheel handler: ^(NSEvent* event) { // monitoring application for scroll events
            @autoreleasepool {
                JNIEnv* env = getAppKitEnv();
                if (!env) {
                    NSLog(@"getAppKitEnv failed (returned null) ");
                    return event;
                }
                if(isCandidateFluidSwipe(event) && !gestureActive && !gestureWaitingForJavaDecision) {
                    if(![NSEvent isSwipeTrackingFromScrollEventsEnabled]) {
                        LOG(env, eu_giulianogorgone_fluidswipe_utils_log_Logging_CONFIG, "candidate event discarded, as fluid-swipe has been disabled in System Preferences/System Settings");
                        return event;
                    }
                    gestureWaitingForJavaDecision = YES;
                    NSWindow* evt_win = [event window]; // window in which event occurred
                    CHECK_NULL_RET_V(env, evt_win, event) //  since window property is nullable in NSEvent, opportune checks are performed;
                    NSObject* delegate = [evt_win delegate]; //the delegate's class is expected to be AWTWindow
                    CHECK_NULL_RET_V(env, delegate, event)
                    if(!([delegate isKindOfClass:AWTWindow] && [delegate respondsToSelector:SELjavaPlatformWindow])) { // the latter check might be redundant
                        LOG(env, eu_giulianogorgone_fluidswipe_utils_log_Logging_INFO, "the window in which the event occurred is not kind of AWTWindow");
                        return event;
                    }
                    
                    NSPoint location = [event locationInWindow];
                    location.y = [evt_win frame].size.height - location.y; // convert from Cocoa to AWT/Swing coordinate system
                    
                    
                    jobject weakRefPlatformWin = VALIDATE_REF((jobject) [delegate performSelector:SELjavaPlatformWindow]);
                    CHECK_NULL_RET_V(env, weakRefPlatformWin, event)
                    
                    jobject platform_window = (*env)->NewLocalRef(env, weakRefPlatformWin); // "[AWTWindow javaPlatformWindow]" is a global weak reference, promoting it to strong reference
                    CHECK_NULL_RET_V(env, platform_window, event)
                    jobject awtWindow = VALIDATE_REF((*env)->GetObjectField(env, platform_window, fID_awtWindow)); // extracting the AWT/Swing window
                    if(!awtWindow) {
                        (*env)->DeleteLocalRef(env, platform_window);
                        return event;
                    }
                    
                    LOG(env, eu_giulianogorgone_fluidswipe_utils_log_Logging_FINEST, "notifying Java clients that a fluid-swipe gesture physically began and may also logically do");
                    
                    
                    if (currentEvent != event) {
                        CLEAR(currentEvent);
                        currentEvent = [event retain];
                    }
                    // tell Java that a fluid-swipe gesture physically started.
                    (*env)->CallStaticVoidMethod(env, FluidSwipeDispatcher, mID_notifyFluidSwipeBegan, awtWindow, [currentEvent scrollingDeltaX], location.x, location.y, [currentEvent isDirectionInvertedFromDevice]);
                    
                    (*env)->DeleteLocalRef(env, awtWindow);
                    (*env)->DeleteLocalRef(env, platform_window);
                }
                EXC_CHECK_AND_REPORT(env, cleanup(env));
                return event;
            }
        }];
        libActive = true;
    });
}

/*
 * Class:     eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler
 * Method:    nativeVetoFluidSwipe
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler_nativeVetoFluidSwipe(JNIEnv* envNotAppKit, jclass class) {
    runOnAppKitThreadAsync(^ {
        gestureWaitingForJavaDecision = NO;
        JNIEnv* env = getAppKitEnv();
        if (!env) {
            NSLog(@"getAppKitEnv failed (returned null) ");
            return;
        }
        LOG(env, eu_giulianogorgone_fluidswipe_utils_log_Logging_FINEST, "fluid-swipe veto received");
        CLEAR(currentEvent);
    });
}

/*
 * Class:     eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler
 * Method:    nativeLogicallyStartFluidSwipe
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_eu_giulianogorgone_fluidswipe_handlers_macos_impl_MacOSFluidSwipeHandler_nativeLogicallyStartFluidSwipe(JNIEnv* envNotAppKit, jclass class) {
    runOnAppKitThreadAsync(^ {
        logicallyStartGesture(getAppKitEnv(), currentEvent);
    });
}

