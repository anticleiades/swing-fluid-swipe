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
package eu.giulianogorgone.fluidswipe.handlers.impl;

import eu.giulianogorgone.fluidswipe.handlers.FluidSwipeHandler;

/**
 * {@code NopFluidSwipeHandler} is the fallback implementation of {@code FluidSwipeHandler}. It is used
 * when the environment in which the JVM runs does not support fluid-swipe.
 * Each method of this class has no side effects and immediately returns.
 * @author Giuliano Gorgone (anticleiades)
 */
public final class NopFluidSwipeHandler implements FluidSwipeHandler {
    @Override
    public void startEventMonitoring() {

    }

    @Override
    public void stopEventMonitoring() {

    }

    @Override
    public boolean logicallyStartFluidSwipe() {
        // to obey to the interface method contract, false is always returned, as it is always not possible to successfully start a fluid-swipe if this handler is used.
        return false;
    }

    @Override
    public void vetoFluidSwipe() {

    }

    @Override
    public boolean setContinueGestureOnFingerRelease(boolean continueGestureOnFingerRelease) {
        return continueGestureOnFingerRelease;
    }
}
