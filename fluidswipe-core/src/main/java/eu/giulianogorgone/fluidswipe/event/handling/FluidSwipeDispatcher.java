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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bridges native and Java code – this class contains methods used to dispatch fluid-swipe events to the Java listeners.
 * This class is not part of the public API.
 *
 * @author Giuliano Gorgone (anticleiades)
 */
public final class FluidSwipeDispatcher {
    public static final FluidSwipeHandler nativeHandler = FluidSwipeHandlers.getHandler();

    // a pair consisting of an array of FluidSwipeListeners, and the Component the listener holder is installed to.
    private static Pair<Component, FluidSwipeListener[]> currentPair;
    private static FluidSwipeEvent.Direction             currentDirection;
    private static FluidSwipeEvent.State                 currentState = FluidSwipeEvent.State.NOT_YET_DEFINED;

    // workaround for better performances
    private static volatile double        latestGestureAmount = 0.0;
    private static volatile int           latestPhase         = 0;
    private static final    AtomicBoolean isDispatchPending   = new AtomicBoolean(false);


    private FluidSwipeDispatcher() {
        throw new AssertionError();
    }


    // This method called by native code
    @SuppressWarnings("unused")
    private static void notifyFluidSwipeBeganAsync(Window target, double scrollingDeltaX, double eventX, double eventY, final boolean naturalScrollingEnabled) {
        Threading.performOnAWTUIThread(target, () -> { // Using proper thread to perform Swing-related operations, as the caller thread is not necessarily the EDT.
            try {
                if (notifyFluidSwipeBeganCommon(target, scrollingDeltaX, eventX, eventY, naturalScrollingEnabled)) {
                    nativeHandler.logicallyStartFluidSwipe();
                    // if fail
                    //cleanup();
                } else {
                    nativeHandler.vetoFluidSwipe();
                    cleanup();
                }
            } catch (Exception e) {
                nativeHandler.vetoFluidSwipe(); // if any exception occurs, fluid-swipe will not logically start.
                cleanup();
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
     * If the deepest swipeable component found at {@code (x, y)}, its descendants, and its ancestor do not veto the gesture,
     * the incoming fluid-swipe request is considered accepted; consequently, the gesture can logically start.
     * <br>
     * Firstly, the deepest visible descendant
     * {@code Component} of the {@code Window} provided as argument (i. e, the {@code Window} in which event occurred) that contains the location {@code (x, y)} is obtained; secondly,
     * ancestors of {@code deepest}, the obtained component, are visited to find a valid swipeable component. The visit can end before finding
     * the required swipeable component if any of its descendants
     * vetoes the gesture; if a swipeable component is found instead, all its ancestors are verified to permit fluid-swipe.
     * <br>
     * <b>Warning</b>: Components that are not vetoers are assumed to allow fluid-swipe.
     *
     * @param window                  Window in which the event occurred
     * @param scrollingDeltaX         numerical representation of the gesture direction
     * @param eventX                  x-coordinate of the point in which the event occurred
     * @param eventY                  y-coordinate of the point in which the event occurred
     * @param naturalScrollingEnabled indicates whether natural scrolling is enabled at the beginning of the physical gesture. If it is true, the content direction will match the direction of fingers movement.
     * @return {@code true} if any swipeable component exists in {@code target} at {@code (x, y)} and it, its ancestors and its descendants allow fluid-swipe; {@code false} otherwise.
     */
    private static boolean notifyFluidSwipeBeganCommon(
            final Window window,
            final double scrollingDeltaX,
            final double eventX,
            final double eventY,
            final boolean naturalScrollingEnabled
                                                      ) {
        // Running on EDT
        if (FluidSwipeDispatcher.currentPair != null) { // Reject the fluid-swipe request if any fluid-swipe gesture did not logically end.
            Logging.logWarn("please try to reproduce and report the issue: incoming fluid-swipe request when another fluid-swipe gesture appears to be not yet logically ended");
            return false;
        }
        Component deepest = SwingUtilities.getDeepestComponentAt(window, (int) eventX, (int) eventY);
        while (deepest != null && (!deepest.isVisible() || !deepest.isEnabled())) {
            deepest = deepest.getParent();
        }
        if (deepest == null) {
            Logging.logFinest("no visible and enabled component found");
            return false;
        }

        final FluidSwipeEvent.Direction             direction = directionFromScrollingDeltaX(scrollingDeltaX);
        final Pair<Component, FluidSwipeListener[]> target    = getTargetFromDeepest(deepest);
        if (target == null) {
            return false;
        }
        final FluidSwipeEvent event = new FluidSwipeEvent(
                direction,
                naturalScrollingEnabled,
                true,
                0.0D,
                FluidSwipeEvent.Phase.MAY_BEGIN,
                FluidSwipeEvent.State.NOT_YET_DEFINED,
                target.getLeft()
        );

        if (isGestureVetoed(deepest, event)) {
            return false;
        }

        FluidSwipeDispatcher.currentDirection = direction;
        FluidSwipeDispatcher.currentPair      = target;
        Logging.logFinest("require fluid-swipe to logically start: window: " + window + ", direction: " + direction + ", component: " + target.getLeft());

        return true;
    }

    private static boolean isGestureVetoed(Component component, final FluidSwipeEvent e) {
        while (component != null) {
            if (eventVetoedByComponent(component, e)) {
                Logging.logFinest(component + ": vetoed fluid-swipe event");
                return true;
            }
            component = component.getParent();
        }
        return false;
    }


    // This method is called by native code; carries information about the occurring event.
    private static void dispatchFluidSwipeEvent(final double gestureAmount, final int eventPhase, final boolean naturalScrollingEnabled) {
        // Now on AppKit Thread.
        latestGestureAmount = gestureAmount;
        latestPhase         = eventPhase;
        if (eventPhase == BridgeConstants.PROGRESSED || eventPhase == BridgeConstants.PROGRESSED_NO_MORE_TOUCHING) {
            if (isDispatchPending.compareAndSet(false, true)) {
                Threading.performOnAWTUIThread(currentPair == null ? null : currentPair.getLeft(), () -> {
                    isDispatchPending.set(false);
                    if (currentPair == null) {
                        Logging.logFinest("target got cleaned-up!");
                        return;
                    }
                    for (FluidSwipeListener listener : currentPair.getRight()) {
                        listener.fluidSwipeProgressed(new FluidSwipeEvent(currentDirection, naturalScrollingEnabled, latestPhase == BridgeConstants.PROGRESSED,
                                                                          latestGestureAmount, FluidSwipeEvent.Phase.PROGRESS, currentState, currentPair.getLeft()));
                    }
                });
            }
        } else {
            Threading.performOnAWTUIThread(currentPair == null ? null : currentPair.getLeft(), () -> { // Switch to EDT to create and dispatch the Java event.
                if (currentPair == null) {
                    Logging.logFinest("target got cleaned-up!");
                    return;
                }
                if (Utils.hasFlag(eventPhase, BridgeConstants.UPDATE_STATE)) {
                    currentState = Utils.hasFlag(eventPhase, BridgeConstants.COMPLETED) ? FluidSwipeEvent.State.SUCCESS : FluidSwipeEvent.State.CANCELED;
                }
                switch (eventPhase) {
                    case BridgeConstants.LOGICALLY_BEGAN: {
                        for (FluidSwipeListener listener : currentPair.getRight()) {
                            listener.fluidSwipeBegan(new FluidSwipeEvent(currentDirection, naturalScrollingEnabled, true, gestureAmount,
                                                                         FluidSwipeEvent.Phase.BEGAN, currentState, currentPair.getLeft()));
                        }
                        break;
                    }
                    case BridgeConstants.COMPLETED:
                    case BridgeConstants.CANCELED: {
                        for (FluidSwipeListener listener : currentPair.getRight()) {
                            listener.fluidSwipeEnded(new FluidSwipeEvent(currentDirection, naturalScrollingEnabled, false, gestureAmount, FluidSwipeEvent.Phase.ENDED, currentState, currentPair.getLeft()));
                        }
                        cleanup();
                        break;
                    }
                }
            });
        }
    }

    private static void cleanup() {
        // Clean stuff up for future events.
        currentPair      = null;
        currentDirection = null;
        isDispatchPending.set(false);
        currentState = FluidSwipeEvent.State.NOT_YET_DEFINED;
        Logging.logFinest("done");
    }

    static FluidSwipeEvent.Direction directionFromScrollingDeltaX(final double scrollingDeltaX) {
        return scrollingDeltaX >= 0 ? FluidSwipeEvent.Direction.LEFT_TO_RIGHT : FluidSwipeEvent.Direction.RIGHT_TO_LEFT;
    }

    // Tells whether a component vetoes the incoming fluid-swipe gesture.
    static boolean eventVetoedByComponent(final Component component, final FluidSwipeEvent e) {
        if (component instanceof FluidSwipeVetoer)
            return !((FluidSwipeVetoer) component).permitFluidSwipeGesture(e);
        if (component instanceof JScrollPane)
            return !scrollSwipeCoexImpl((JScrollPane) component, e);
        return false;
    }

    static Pair<Component, FluidSwipeListener[]> getTargetFromDeepest(Component deepest) {
        Component              targetComponent = deepest;
        FluidSwipeListenerList listenerList    = null;
        while (targetComponent != null) {
            listenerList = FluidSwipeListenerList.get(targetComponent);
            if (listenerList != null) {
                break;
            }
            targetComponent = targetComponent.getParent();
        }

        if (listenerList == null) {
            Logging.logFinest("no listener holder found");
            return null;
        }
        if (targetComponent == null) {
            Logging.logFinest("no visible and enabled component found");
            return null;
        }
        return new Pair<>(targetComponent, listenerList.copyListeners());
    }

    private static boolean scrollSwipeCoexImpl(JScrollPane scrollPane, FluidSwipeEvent e) {
        final FluidSwipeEvent.Direction contentDirection    = e.getLogicalGestureDirection();
        JScrollBar                      horizontalScrollBar = scrollPane.getHorizontalScrollBar();
        if (horizontalScrollBar == null || scrollPane.getHorizontalScrollBarPolicy() == JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) return true;
        return (contentDirection == FluidSwipeEvent.Direction.RIGHT_TO_LEFT && (horizontalScrollBar.getValue() + horizontalScrollBar.getModel().getExtent()) == horizontalScrollBar.getMaximum())
               || (contentDirection == FluidSwipeEvent.Direction.LEFT_TO_RIGHT && horizontalScrollBar.getValue() == horizontalScrollBar.getMinimum());
    }
}
