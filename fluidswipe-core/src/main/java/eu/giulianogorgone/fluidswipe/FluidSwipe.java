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

package eu.giulianogorgone.fluidswipe;

import eu.giulianogorgone.fluidswipe.event.handling.FluidSwipeListenerList;
import eu.giulianogorgone.fluidswipe.event.FluidSwipeListener;
import eu.giulianogorgone.fluidswipe.event.handling.FluidSwipeDispatcher;

import javax.swing.*;
import java.util.Objects;

/**
 * This class manages the relationship between {@link JComponent}s and the {@link FluidSwipeListener}s
 * attached to them; it can also be used to customize the behavior of the library.
 *
 * @author Giuliano Gorgone (anticleiades)
 * @see FluidSwipeListener
 */
public final class FluidSwipe {
    private FluidSwipe() {
        throw new AssertionError();
    }

    private static boolean continueGestureOnFingerRelease = true;

    /**
     * This method starts event monitoring. Repeated calls have no effects.
     */
    public static void startEventMonitoring() {
        FluidSwipeDispatcher.nativeHandler.startEventMonitoring();
    }

    /**
     * This method stops event monitoring. Repeated calls have no effects.
     */
    public static void stopEventMonitoring() {
        FluidSwipeDispatcher.nativeHandler.stopEventMonitoring();
    }


    /**
     * This method adds a {@code FluidSwipeListener} to a {@code JComponent} interested in listening for {@code FluidSwipeEvent}s.
     *
     * @param target   the component interested in listening for {@code FluidSwipeEvent}s
     * @param listener the listener to be added to the specified component
     * @throws NullPointerException if any argument is null.
     * @see FluidSwipeListener
     */
    public static void addListenerTo(final JComponent target,
                                     final FluidSwipeListener listener) {
        Objects.requireNonNull(target, "target is null");
        Objects.requireNonNull(listener, "listener is null");
        FluidSwipeListenerList.installIfNeededAndGet(target).listenerList.add(listener);
    }

    /**
     * This method removes a {@code FluidSwipeListener} from a {@code JComponent}. If the listener to be removed
     * is not present, this method has no effect.
     *
     * @param target   the component to remove the {@code listener} from
     * @param listener the listener to be removed from the specified component
     * @throws NullPointerException if any argument is null.
     * @see FluidSwipeListener
     */
    public static void removeListenerFrom(final JComponent target,
                                          final FluidSwipeListener listener) {
        Objects.requireNonNull(target, "target is null");
        Objects.requireNonNull(listener, "listener is null");
        final FluidSwipeListenerList listenerList = FluidSwipeListenerList.get(target);
        if (listenerList != null)
            listenerList.listenerList.remove(listener);
    }

    /**
     * This method removes all {@code FluidSwipeListener}s from a {@code JComponent}. If there are no listeners attached to
     * the component, this method has no effect.
     *
     * @param target the component to remove all {@code listeners} from
     * @throws NullPointerException if target is null
     * @see FluidSwipeListener
     */
    public static void removeAllListenersFrom(final JComponent target) {
        Objects.requireNonNull(target, "target is null");
        final FluidSwipeListenerList listenerList = FluidSwipeListenerList.get(target);
        if (listenerList != null)
            listenerList.listenerList.clear();
    }

    /**
     * This method is used to modify the behavior of the event handler when the active input device stops receiving touches – in accordance with default settings,
     * any occurring gesture logically continues, until the gesture amount reaches the maximum value, typically {@code 1.0}; however,
     * user may desire that any gesture <b>physically</b> and <b>logically</b> terminates at the same time. In such case, pass {@code true} as argument; {@code false} otherwise.
     *
     * @param continueGestureOnFingerRelease boolean value expressing whether any occurring gesture must <b>logically</b> continue if <b>physically</b> ended.
     */
    public static void setContinueGestureOnFingerRelease(final boolean continueGestureOnFingerRelease) {
        FluidSwipe.continueGestureOnFingerRelease = FluidSwipeDispatcher.nativeHandler.setContinueGestureOnFingerRelease(continueGestureOnFingerRelease);
    }

    /**
     * Returns whether any occurring gesture must logically continue when the active input device stops receiving touches.
     *
     * @return {@code true} if any occurring gesture must <b>logically</b> continue if <b>physically</b> ended;
     * {@code false} otherwise.
     */
    public static boolean continuesGestureOnFingerRelease() {
        return continueGestureOnFingerRelease;
    }
}
