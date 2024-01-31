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

package eu.giulianogorgone.fluidswipe.event.handling;

import eu.giulianogorgone.fluidswipe.components.FluidSwipeVetoer;
import eu.giulianogorgone.fluidswipe.event.FluidSwipeEvent;
import eu.giulianogorgone.fluidswipe.event.FluidSwipeListener;
import eu.giulianogorgone.fluidswipe.utils.Utils;
import eu.giulianogorgone.fluidswipe.handlers.FluidSwipeHandler;
import eu.giulianogorgone.fluidswipe.utils.FluidSwipeHandlers;
import eu.giulianogorgone.fluidswipe.utils.Threading;
import eu.giulianogorgone.fluidswipe.utils.log.Logging;
import eu.giulianogorgone.fluidswipe.utils.pair.Pair;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Bridges native and Java code – this class contains method used to dispatch fluid-swipe events to the Java listeners.
 * This class is not part of the public API.
 *
 * @author Giuliano Gorgone (anticleiades)
 */
public final class FluidSwipeDispatcher {
    public static final FluidSwipeHandler nativeHandler = FluidSwipeHandlers.getHandler();

    // a pair consisting of an array of FluidSwipeListeners, and the JComponent the listener holder is installed to.
    private static Pair<JComponent, FluidSwipeListener[]> currentPair;
    private static FluidSwipeEvent.Direction currentDirection;
    private static FluidSwipeEvent.State currentState = FluidSwipeEvent.State.NOT_YET_DEFINED;

    private FluidSwipeDispatcher() {
        throw new AssertionError();
    }


    // This method called by native code
    private static void notifyFluidSwipeBeganAsync(Window target, double scrollingDeltaX, double eventX, double eventY, final boolean naturalScrollingEnabled) {
        Threading.performOnAWTUIThread(target, () -> { // Using proper thread to perform Swing-related operations, as the caller thread is not necessarily the EDT.
            try {
                if (notifyFluidSwipeBeganCommon(target, scrollingDeltaX, eventX, eventY, naturalScrollingEnabled)) {
                    if (!nativeHandler.logicallyStartFluidSwipe()) {
                        cleanup();
                    }
                } else {
                    nativeHandler.vetoFluidSwipe();
                }
            } catch (Exception e) {
                nativeHandler.vetoFluidSwipe(); // if any exception occurs, fluid-swipe will not logically start.
                throw e; // rethrow e
                /*
                The following may also be a viable option!
                final Thread EDT = Thread.currentThread();
                handle the exception as you would do if this had been thrown in any swing listener.
                EDT.getUncaughtExceptionHandler().uncaughtException(EDT, e.getCause());
                */
            }
        });
    }

    /**
     * The purpose of this method is to respond to fluid-swipe requests issued by the active handler.
     * If the deepest swipeable component found at {@code (x, y)}, its descendants and its ancestor do not veto the gesture,
     * the incoming fluid-swipe request is considered accepted; consequently, the gesture can logically start.
     * <br>
     * Firstly, the deepest visible descendant
     * {@code Component} of the {@code Window} provided as argument (i. e, the {@code Window} in which event occurred) that contains the location {@code (x, y)} is obtained; secondly,
     * ancestors of {@code deepest}, the obtained component, are visited in order to find a valid swipeable component. The visit can end prior to finding
     * the required swipeable component if any of its descendants
     * vetoes the gesture; if a swipeable component is found instead, all its ancestors are verified to permit fluid-swipe.
     * <br>
     * <b>Warning</b>: Components that are not vetoers are assumed to allow fluid-swipe.
     *
     * @param target                  Window in which event occurred
     * @param scrollingDeltaX         numerical representation of the gesture direction
     * @param eventX                  x-coordinate of the point in which event occurred
     * @param eventY                  y-coordinate of the point in which event occurred
     * @param naturalScrollingEnabled indicates whether natural scrolling is enabled at the beginning of the physical gesture. If it is true, the content direction will match the direction of fingers movement.
     * @return {@code true} if any swipeable component exists in {@code target} at {@code (x, y)} and it, its ancestors and its descendants allow fluid-swipe; {@code false} otherwise.
     */
    private static boolean notifyFluidSwipeBeganCommon(final Window target,
                                                       final double scrollingDeltaX,
                                                       final double eventX,
                                                       final double eventY,
                                                       final boolean naturalScrollingEnabled) {
        // Running on EDT
        if (FluidSwipeDispatcher.currentPair != null) { // Reject the fluid-swipe request if any fluid-swipe gesture did not logically end.
            Logging.logWarn("please try to reproduce and report the issue: incoming fluid-swipe request when another fluid-swipe gesture appears to be not yet logically ended");
            return false;
        }
        final Component deepest = SwingUtilities.getDeepestComponentAt(target, (int) eventX, (int) eventY);
        final FluidSwipeEvent.Direction direction = directionFromScrollingDeltaX(scrollingDeltaX);
        final FluidSwipeEvent event = new FluidSwipeEvent(direction, naturalScrollingEnabled, true, 0.0D, FluidSwipeEvent.Phase.MAY_BEGIN, FluidSwipeEvent.State.NOT_YET_DEFINED);
        final Pair<JComponent, FluidSwipeListener[]> pair = getDeepestComponentWithListeners(deepest, event);
        final boolean acceptFluidSwipeRequest = pair != null && !eventVetoedByAncestors(pair.getLeft(), event);
        if (acceptFluidSwipeRequest) {
            FluidSwipeDispatcher.currentDirection = direction;
            FluidSwipeDispatcher.currentPair = pair;
            Logging.logFinest("require fluid-swipe to logically start: window: " + target + "eventX: " + eventX + ", eventY: " + eventY + ", direction: " + direction + ", component: " + pair.getLeft());
        }
        return acceptFluidSwipeRequest;
    }

    // Called by native code in the AppKit Thread when in SYNC mode.
    private static boolean notifyFluidSwipeBeganSync(Window target, double scrollingDeltaX, double eventX, double eventY,
                                                     final boolean naturalScrollingEnabled) {
        try {
            AtomicBoolean b = new AtomicBoolean(false);
            Threading.performOnAWTUIThreadAndWait(target, () -> {
                        b.set(notifyFluidSwipeBeganCommon(target, scrollingDeltaX, eventX, eventY, naturalScrollingEnabled));
                    }
            );
            return b.get();
        } catch (InterruptedException e) {
            Logging.logSevere("interrupted", e);
        } catch (InvocationTargetException e) {
            SwingUtilities.invokeLater(() -> {
                final Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    // rethrow
                    throw (RuntimeException) cause;
                } else {
                    // wrap the checked exception
                    throw new RuntimeException(cause);
                }
                /*
                The following may also be a viable option!
                final Thread EDT = Thread.currentThread();
                handle the exception as you would do if this had been thrown in any swing listener.
                EDT.getUncaughtExceptionHandler().uncaughtException(EDT, e.getCause());
                */
            });
        }
        return false;
    }

    // This method is called by native code; carries information about the occurring event.
    private static void dispatchFluidSwipeEvent(final double gestureAmount, final int eventPhase, final boolean naturalScrollingEnabled) {
        // Now on AppKit Thread.
        Threading.performOnAWTUIThread(currentPair == null ? null : currentPair.getLeft(), () -> { // Switch to EDT to create and dispatch the Java event.
            switch (eventPhase) {
                case BridgeConstants.LOGICALLY_BEGAN: {
                    for (FluidSwipeListener listener : currentPair.getRight()) {
                        listener.fluidSwipeBegan(new FluidSwipeEvent(currentDirection, naturalScrollingEnabled, true, gestureAmount,
                                FluidSwipeEvent.Phase.BEGAN, currentState));
                    }
                    break;
                }
                case BridgeConstants.PROGRESSED:
                case BridgeConstants.PROGRESSED_NO_MORE_TOUCHING: {
                    for (FluidSwipeListener listener : currentPair.getRight()) {
                        listener.fluidSwipeProgressed(new FluidSwipeEvent(currentDirection, naturalScrollingEnabled, eventPhase == BridgeConstants.PROGRESSED, gestureAmount,
                                FluidSwipeEvent.Phase.PROGRESS, currentState));
                    }
                    break;
                }
                case BridgeConstants.COMPLETED: {
                    try {
                        for (FluidSwipeListener listener : currentPair.getRight()) {
                            listener.fluidSwipeEnded(new FluidSwipeEvent(currentDirection, naturalScrollingEnabled, false, gestureAmount, FluidSwipeEvent.Phase.ENDED, FluidSwipeEvent.State.SUCCESS));
                        }
                    } catch (Exception e) {
                        cleanup();
                        throw e;
                    }
                    break;
                }
                case BridgeConstants.CANCELED: {
                    try {
                        for (FluidSwipeListener listener : currentPair.getRight()) {
                            listener.fluidSwipeEnded(new FluidSwipeEvent(currentDirection, naturalScrollingEnabled, false, gestureAmount, FluidSwipeEvent.Phase.ENDED, FluidSwipeEvent.State.CANCELED));
                        }
                    } catch (Exception e) {
                        cleanup();
                        throw e;
                    }
                    break;
                }
            }
            if (Utils.hasFlag(eventPhase, BridgeConstants.UPDATE_STATE)) {
                currentState = Utils.hasFlag(eventPhase, BridgeConstants.COMPLETED) ? FluidSwipeEvent.State.SUCCESS : FluidSwipeEvent.State.CANCELED;
            } else if ((eventPhase & BridgeConstants.ENDED_MASK) != 0) {
                cleanup(); // cleanup when the gesture logically ends.
            }
        });
    }

    private static void cleanup() {
        // Clean stuff up for future events.
        currentPair = null;
        currentDirection = null;
        currentState = FluidSwipeEvent.State.NOT_YET_DEFINED;
        Logging.logFinest("done");
    }

    static FluidSwipeEvent.Direction directionFromScrollingDeltaX(final double scrollingDeltaX) {
        return scrollingDeltaX >= 0 ? FluidSwipeEvent.Direction.LEFT_TO_RIGHT : FluidSwipeEvent.Direction.RIGHT_TO_LEFT;
    }

    // Tells whether a component vetoes the incoming fluid-swipe gesture.
    static boolean eventVetoedByComponent(final Component component, final FluidSwipeEvent e) {
        return component instanceof FluidSwipeVetoer && !((FluidSwipeVetoer) component).permitFluidSwipeGesture(e);
    }

    // Visit the component's ancestors to ensure that no one vetoes the incoming fluid-swipe gesture.
    static boolean eventVetoedByAncestors(Component component, final FluidSwipeEvent e) {
        while (component != null) {
            if (eventVetoedByComponent(component, e)) {
                Logging.logFinest(component + ": vetoed fluid-swipe event as ancestor");
                return true;
            }
            component = component.getParent();
        }
        return false;
    }

    static Pair<JComponent, FluidSwipeListener[]> getDeepestComponentWithListeners(Component component, final FluidSwipeEvent e) {
        FluidSwipeListenerList listenerList = null;
        while (listenerList == null && component != null) { // Stops when a ListenerList is found.
            if (eventVetoedByComponent(component, e)) {
                Logging.logFinest(component + ": vetoed fluid-swipe event");
                return null; // fluid-swipe veto occurred.
            }
            if ((listenerList = FluidSwipeListenerList.get(component)) == null) {
                // if the current component has not any FluidSwipeListener, visit its parent.
                component = component.getParent();
            }
        }
        return listenerList != null ? new Pair<>((JComponent) component, listenerList.copyListeners()) : null;
    }
}
