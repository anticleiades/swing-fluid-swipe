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

package eu.giulianogorgone.fluidswipe.event;

import eu.giulianogorgone.fluidswipe.components.FluidSwipeVetoer;
import eu.giulianogorgone.fluidswipe.FluidSwipe;

import javax.swing.*;
import java.io.Serializable;

/**
 * Event indicating that a fluid-swipe action occurred in a given component <code>comp</code>.
 * Let <code>w</code> be a visible <code>Window</code> instance, <code>comp</code> be a <code>JComponent</code> instance such that <code>comp</code> is a descendant of <code>w</code> in the component hierarchy;
 * when the physical gesture occurs, assuming the mouse cursor to be positioned over <code>w</code>, let the point <code>(x, y)</code> be the location of the mouse cursor relative to <code>w</code>;
 * let <code>d</code> be a <code>Component</code> instance that is the deepest component in the component hierarchy of <code>w</code> such that its bounds non-trivially intersect the <code>(x, y)</code> point.
 * <br>
 * A fluid-swipe gesture can logically start (i.e., the physical gesture is interpreted as a fluid-swipe gesture) in <code>comp</code> if and only if all the following conditions are satisfied:
 * <ul>
 *     <li>any fluid-swipe handler implementation supporting the running environment is being used</li>
 *     <li>event monitoring is turned on by calling {@link FluidSwipe#startEventMonitoring()}</li>
 *     <li>system settings are tuned as described either in <code>README.md</code> or in <code>package-info</code></li>
 *     <li>supported hardware and software are being used as described either in <code>README.md</code> or in <code>package-info</code></li>
 *     <li><code>comp</code> is enabled to receive <code>FluidSwipeEvent</code>s by invoking {@link FluidSwipe#addListenerTo(JComponent, FluidSwipeListener)}.</li>
 *     <li><code>comp</code> is <code>d</code> or any of its ancestors.</li>
 *     <li>neither <code>d</code> nor any of its ancestors are currently blocking the gesture as vetoer components.</li>
 * </ul>
 * <br>
 * For instance, using default settings, the following sequence of events will be fired if the user performs a physical swipe
 * that is interpreted as a fluid-swipe gesture. In this example, the user release fingers when <code>gestureAmount = 1.0</code> and does not invert the swipe direction during all the gesture life-cycle.
 * <pre>{@code
 * | phase      | state            | gestureAmount            |
 * |------------|------------------|--------------------------|
 * | MAY_BEGIN  | NOT_YET_DEFINED  | 0.0                      |
 * | BEGAN      | NOT_YET_DEFINED  | (0.0 < v < 1.0)          |
 * | PROGRESS   | NOT_YET_DEFINED  | (0.0 ≤ v ≤ 1.0)          |
 * | PROGRESS   | SUCCESS          | (0.0 < v < 1.0)          |
 * | ENDED      | SUCCESS          | 1.0                      |
 * }</pre>
 * Moreover, a fluid-swipe event conveys information about state of the active gesture, as described in
 * {@link Phase} and {@link State} respectively.
 *
 * @author Giuliano Gorgone (anticleiades)
 * @see FluidSwipeVetoer
 * @see FluidSwipeAdapter
 * @see FluidSwipeListener
 * @see FluidSwipe
 */
public final class FluidSwipeEvent implements Serializable {
    private static final long serialVersionUID = 762156773559898001L;

    /**
     * Describes life-cycle phases of fluid-swipe gesture events. It applies only to the <b>logical</b> gesture, not the physical one.
     * <br>
     * Phases in temporal order: {@code MAY_BEGIN}, {@code BEGAN}, {@code PROGRESS}, {@code ENDED}.
     */
    public enum Phase implements Serializable {
        /**
         * One and one only event with this phase is dispatched, when the <b>logical</b> gesture may be about to start – user performed a physical gesture, but it has to be determined whether to interpret that gesture as a fluid-swipe gesture.
         * Only {@code FluidSwipeVetoer} components containing the point <code>(x, y)</code> in which event occurs receive events with this phase.
         * <code>gestureAmount</code> is always equal to <code>0.0</code>.
         * At the time of the event dispatching, the physical gesture is ambiguous.
         */
        MAY_BEGIN,
        /**
         * One and one only event with this phase is fired – the gesture has been yet <b>physically</b> started and <b>logically</b> starts at the time of the event dispatching.
         * The gesture is no more ambiguous.
         * <p>
         * Corresponding event-listener method: {@linkplain FluidSwipeListener#fluidSwipeBegan(FluidSwipeEvent)}
         */
        BEGAN,
        /**
         * A set of events with this phase is dispatched either while the user is actively continuing the gesture (i.e., the user moves the fingers onto the input device)
         * or, if {@linkplain FluidSwipe#continuesGestureOnFingerRelease()} returns {@code true}, after the <b>physical</b> termination of the gesture,
         * until the gesture amount reaches {@code 0.0} (if {@code state=CANCEL}) or {@code 1.0} (if {@code state=SUCCESS}).
         * <p>
         * Corresponding event-listener method: {@linkplain FluidSwipeListener#fluidSwipeProgressed(FluidSwipeEvent)}
         */
        PROGRESS,
        /**
         * One and one only event with this phase is dispatched – the event fired when the gesture <b>logically</b> ends.
         * Any gesture must <b>physically</b> terminate prior to <b>logically</b>.
         * <p>
         * Corresponding event-listener method: {@linkplain FluidSwipeListener#fluidSwipeEnded(FluidSwipeEvent)}
         */
        ENDED
    }

    /**
     * Describes the gesture state. Gesture state is determined only when gesture <b>physically</b> ends (i.e., the input device receives no more touches).
     */
    public enum State implements Serializable {
        /**
         * When a fluid-swipe event comes with this state, it is unpredictable if the active gesture will be either completed or canceled.
         */
        NOT_YET_DEFINED,
        /**
         * A gesture with this state has been successfully completed.
         */
        SUCCESS,
        /**
         * A gesture with this state has been canceled, performing a swipe in the opposite direction than the initial and then releasing the input device.
         */
        CANCELED
    }

    /**
     * Logically describes the notion of swipe direction.
     */
    public enum Direction implements Serializable {

        LEFT_TO_RIGHT {
            @Override
            public Direction opposite() {
                return RIGHT_TO_LEFT;
            }
        },

        RIGHT_TO_LEFT {
            @Override
            public Direction opposite() {
                return LEFT_TO_RIGHT;
            }
        };

        /**
         * Returns the opposite direction.
         *
         * @return {@code LEFT_TO_RIGHT} if this equals {@code RIGHT_TO_LEFT}; if this equals {@code RIGHT_TO_LEFT} instead, the method returns {@code LEFT_TO_RIGHT}
         */
        public abstract Direction opposite();
    }


    /**
     * This value can be {@code LEFT_TO_RIGHT} or {@code RIGHT_TO_LEFT}, and represents the direction in which content is expected to move to.
     * It is determined by the direction in which swipe is started at first, and it will remain constant along the whole
     * event life-cycle (i.e., from the {@code MAY_BEGIN} to the {@code ENDED} phase).
     * In addition, if natural scrolling is enabled, this value equals {@code physicalGestureDirection}
     *
     * @see #getLogicalGestureDirection()
     */
    final Direction logicalDirection;
    /**
     * This value can be {@code LEFT_TO_RIGHT} or {@code RIGHT_TO_LEFT}, and logically represents the direction of the <b>physical</b> gesture.
     * It is equal to {@code LEFT_TO_RIGHT} if user swipes from the left to right onto the input device; conversely, it is equal to {@code RIGHT_TO_LEFT}
     * if user swipes from the left to right onto the input device.
     * It is determined by the direction in which swipe is started at first, and it will remain constant along the whole
     * event life-cycle (i.e., from the {@code MAY_BEGIN} to the {@code ENDED} phase).
     * In addition, if natural scrolling is enabled, this value equals {@code logicalDirection}.
     *
     * @see #getPhysicalGestureDirection()
     */
    final Direction physicalGestureDirection;

    /**
     * Indicates whether the input device surface is being touched.
     *
     * @see #isInputDeviceBeingTouched()
     */
    final boolean inputDeviceBeingTouched;
    /**
     * This value represents the fractional amount of the active gesture; ranges between {@code 0.0} (inclusive) and {@code 1.0} (inclusive).
     *
     * @see #getGestureAmount()
     */
    final double gestureAmount;

    /**
     * Indicates whether natural scrolling is enabled at the beginning of the physical gesture.
     *
     * @see #isNaturalScrollingEnabled()
     */
    final boolean naturalScrollingEnabled;
    /**
     * Indicates the gesture phase.
     *
     * @see #getGesturePhase()
     */
    final Phase gesturePhase;
    /**
     * Indicates the gesture state.
     *
     * @see #getGestureState()
     */
    final State gestureState;

    /**
     * Indicates the time when this event was created.
     *
     * @see #getWhen()
     */
    final long when;

    public FluidSwipeEvent(final Direction logicalDirection, final boolean naturalScrollingEnabled,
                           final boolean inputDeviceBeingTouched, final double gestureAmount,
                           final Phase gesturePhase, final State gestureState) {
        this.logicalDirection = logicalDirection;
        this.inputDeviceBeingTouched = inputDeviceBeingTouched;
        this.gestureAmount = gestureAmount;
        this.naturalScrollingEnabled = naturalScrollingEnabled;
        this.physicalGestureDirection = isNaturalScrollingEnabled() ? logicalDirection : logicalDirection.opposite();
        this.gesturePhase = gesturePhase;
        this.gestureState = gestureState;
        this.when = System.currentTimeMillis();
    }

    /**
     * Returns the direction of the <b>logical</b> gesture, that is the direction
     * in which the swipeable content is expected to move to.
     * <p>
     * If natural scrolling is enabled, the returned value will equal the value provided by {@linkplain #getPhysicalGestureDirection()}
     *
     * @return <ul>
     * <li> Natural scrolling <b>ON</b>: if a left-to-right swipe occurs, {@code LEFT_TO_RIGHT} is returned; otherwise,
     * if a right-to-left swipe occurs, {@code RIGHT_TO_LEFT} is returned.
     * <li> Natural scrolling <b>OFF</b>: if a left-to-right swipe occurs, {@code RIGHT_TO_LEFT} is returned; otherwise,
     * if a right-to-left swipe occurs, {@code LEFT_TO_RIGHT} is returned.
     * </ul>
     * The returned value is determined by the direction in which swipe is started at first, and it will remain constant along the whole
     * event life-cycle (i.e., from the {@code MAY_BEGIN} to the {@code ENDED} phase).
     * Therefore, if the user starts to swipe in the opposite direction than they started, the returned value will not vary accordingly.
     * @see Direction
     */
    public Direction getLogicalGestureDirection() {
        return logicalDirection;
    }

    /**
     * Returns whether the input device surface is being touched.
     *
     * @return {@code true} if input device surface is being touched; {@code false} otherwise.
     */
    public boolean isInputDeviceBeingTouched() {
        return inputDeviceBeingTouched;
    }

    /**
     * Returns a number between {@code 0.0} (inclusive) and {@code 1.0} (inclusive) that represents the fractional amount of the active gesture.
     *
     * @return the fractional amount of the active gesture.
     */
    public double getGestureAmount() {
        return gestureAmount;
    }

    /**
     * Returns whether natural scrolling is enabled <b>at the beginning</b> of the physical gesture.
     * @return {@code true} if natural scrolling is enabled <b>at the beginning</b> of the physical gesture; {@code false}, otherwise
     */

    public boolean isNaturalScrollingEnabled() {
        return naturalScrollingEnabled;
    }

    /**
     * Returns the direction of the <b>physical</b> gesture.
     * If natural scrolling is enabled, the returned value will equal the value provided by {@linkplain #getLogicalGestureDirection()}
     *
     * @return if a left-to-right swipe occurs, {@code LEFT_TO_RIGHT} is returned; otherwise,
     * if a right-to-left swipe occurs, {@code RIGHT_TO_LEFT} is returned.
     * The returned value is determined by the direction in which swipe is started at first, and it will remain constant along the whole
     * event life-cycle (i.e., from the {@code MAY_BEGIN} to the {@code ENDED} phase).
     * Therefore, if the user starts to swipe in the opposite direction than they started, the returned value will not vary accordingly.
     * @see Direction
     */
    public Direction getPhysicalGestureDirection() {
        return this.physicalGestureDirection;
    }

    /**
     * Returns the gesture phase.
     *
     * @return the gesture phase – possible values are {@linkplain Phase#MAY_BEGIN}, {@linkplain Phase#BEGAN}, {@linkplain Phase#PROGRESS}, {@linkplain Phase#ENDED}}
     * @see Phase
     */
    public Phase getGesturePhase() {
        return gesturePhase;
    }

    /**
     * This method returns the gesture state.
     *
     * @return the gesture state – possible values are {@linkplain State#NOT_YET_DEFINED}, {@linkplain State#SUCCESS},{@linkplain State#CANCELED}
     * @see State
     */
    public State getGestureState() {
        return gestureState;
    }

    /**
     * Returns the difference in milliseconds between the timestamp
     * of when this event was created and midnight, January 1, 1970 UTC.
     *
     * @return the difference in milliseconds between the creation time of this event and midnight, January 1, 1970 UTC.
     */
    public long getWhen() {
        return when;
    }

    @Override
    public String toString() {
        return "FluidSwipeEvent{" +
                "logicalDirection=" + logicalDirection +
                ", physicalGestureDirection=" + physicalGestureDirection +
                ", inputDeviceBeingTouched=" + inputDeviceBeingTouched +
                ", gestureAmount=" + gestureAmount +
                ", naturalScrollingEnabled=" + naturalScrollingEnabled +
                ", gesturePhase=" + gesturePhase +
                ", gestureState=" + gestureState +
                ", when=" + when +
                '}';
    }
}
