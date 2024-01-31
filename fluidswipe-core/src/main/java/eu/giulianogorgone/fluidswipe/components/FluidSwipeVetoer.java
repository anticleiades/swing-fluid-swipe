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

package eu.giulianogorgone.fluidswipe.components;

import eu.giulianogorgone.fluidswipe.event.FluidSwipeEvent;

/**
 * This interface offers implementors the facility to decide whether a fluid-swipe gesture must logically start –
 * each {@code FluidSwipeVetoer} instance participate in defining a policy for fluid-swipe activation.
 * Vetoing consists in preventing the performed physical swipe gesture from being interpreted as a fluid-swipe. If vetoing occurs,
 * the gesture will be handled as usual (for example, as a horizontal scroll gesture).
 * Some typical scenarios can be distinguished in which vetoing is required:
 * <ul>
 *   <li>fluid-swipe has to be blocked when some condition is verified:  for instance, considering a navigable view,
 *   we would like to veto the fluid-swipe when there no more pages to navigate in the specified direction
 *   <li>implementation of a scroll/fluid-swipe coexistence mechanism: since the physical gesture that may start a fluid-swipe can also be interpreted as a horizontal scroll gesture,
 *    it is absolutely recommended that components listening for scroll-wheel events, notably {@code JScrollPane}, implement {@code FluidSwipeVetoer},
 *    in order to avoid unexpected behaviours.
 * </ul>
 * <b>Warning</b>: Components that do not implement this interface are assumed to allow <b>any</b> incoming fluid-swipe.
 <br>
 * <b>Note:</b> let <code>w</code> be a visible <code>Window</code> instance; when the physical gesture occurs, assuming the mouse cursor to be over <code>w</code>,
 * let the point <code>(x, y)</code> be the location of the mouse cursor relative to <code>w</code>;
 * let <code>d</code> be a <code>Component</code> instance that is the deepest component in the <code>w</code>'s component hierarchy
 * such that its bounds non-trivially intersect the <code>(x, y)</code> point.
 * A {@code Component} implementing this interface, to be able to block a fluid-swipe gesture must be <code>d</code> or any of its ancestors.
 * @author Giuliano Gorgone (anticleiades)
 */
public interface FluidSwipeVetoer {
    /**
     * The result of this method invocation is internally used in order to determine
     * whether proceed in interpreting the occurring physical swipe gesture as a fluid-swipe gesture.
     * @param e the fluid-swipe event
     * @return {@code true} if component permits fluid-swipe gesture; {@code false} if the
     * incoming physical gesture must not be interpreted as a fluid-swipe.
     */
    boolean permitFluidSwipeGesture(final FluidSwipeEvent e);
}
