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
package eu.giulianogorgone.fluidswipe.handlers;

/**
 * A {@code FluidSwipeHandler} is an object that provides an abstraction layer over the
 * environment. Captures and handles fluid-swipes, sending them from the native to the Java side.
 *
 * @author Giuliano Gorgone (anticleiades)
 */
public interface FluidSwipeHandler {
    /**
     * This method starts monitoring fluid-swipe-related events in the system;
     * fluid-swipe events are intercepted and sent from the native side to the Java side.
     * Repeated calls shall not have any effect.
     */
    void startEventMonitoring();

    /**
     * This method stops the fluid-swipe-related events monitoring, if started.
     * Undoes the effects of {@link FluidSwipeHandler#startEventMonitoring()}.
     * Repeated calls shall not have any effect.
     */
    void stopEventMonitoring();

    /**
     * This method makes the fluid-swipe event to be ready to logically start.
     *
     * @return {@code true} if the fluid-swipe event is ready to logically start; {@code false}, if any error is encountered during the process.
     */
    boolean logicallyStartFluidSwipe();

    /**
     * This method discards the incoming fluid-swipe event; it is used to implement the vetoing mechanism.
     */
    void vetoFluidSwipe();

    /**
     * This method controls the behavior of this handler when the active input device stops receiving touches.
     * If the argument is {@code true}, any occurring gesture will logically continue, until the gesture amount reaches the maximum possible value (typically {@code 1.0});
     * otherwise, any gesture <b>physically</b> and <b>logically</b> terminates at the same time, when the active input device stops receiving touches.
     * If this feature is not supported, this method shall immediately return, not producing any effect.
     *
     * @param continueGestureOnFingerRelease boolean value expressing whether any occurring gesture must <b>logically</b> continue if <b>physically</b> ended.
     * @return the value passed as argument.
     */
    boolean setContinueGestureOnFingerRelease(final boolean continueGestureOnFingerRelease);
}