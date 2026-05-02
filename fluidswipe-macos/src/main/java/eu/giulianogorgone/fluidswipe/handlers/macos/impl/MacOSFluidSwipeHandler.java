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

package eu.giulianogorgone.fluidswipe.handlers.macos.impl;

import eu.giulianogorgone.fluidswipe.handlers.FluidSwipeHandler;


/**
 * @author Giuliano Gorgone (anticleiades)
 */
public final class MacOSFluidSwipeHandler implements FluidSwipeHandler {

    //====================================================================//
    // Start of native methods – these methods run on the AppKit Thread.  //
    //====================================================================//
    private static native void nativeStartEventMonitoring();

    private static native void nativeStopEventMonitoring();

    private static native void nativeLogicallyStartFluidSwipe();
    // This call will immediately return. See https://developer.apple.com/documentation/appkit/nsevent/1533300-trackswipeeventwithoptions

    private static native void nativeVetoFluidSwipe();

    private static native void nativeSetContinueGestureOnFingerRelease(final boolean b);

    //private static native void nativeSetGestureAmountThreshold(final double amount);

    @Override
    public void startEventMonitoring() {
        nativeStartEventMonitoring();
    }

    @Override
    public void stopEventMonitoring() {
        nativeStopEventMonitoring();
    }

    @Override
    public boolean setContinueGestureOnFingerRelease(boolean continueGestureOnFingerRelease) {
        nativeSetContinueGestureOnFingerRelease(continueGestureOnFingerRelease);
        return continueGestureOnFingerRelease;
    }

    @Override
    public void logicallyStartFluidSwipe() {
        nativeLogicallyStartFluidSwipe();
    }

    @Override
    public void vetoFluidSwipe() {
        nativeVetoFluidSwipe();
    }
}