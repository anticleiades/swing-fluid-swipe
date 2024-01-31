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
import eu.giulianogorgone.fluidswipe.event.FluidSwipeListener;

import java.awt.*;

/**
 * This interface defines methods to create an object that has the purpose of drawing animated feedback in response to a fluid-swipe gesture.
 * To achieve this aim, the painting process should be delegated to an instance of {@code AnimPainterDelegate}, when the gesture phase ranges from
 * {@linkplain FluidSwipeEvent.Phase#BEGAN} to {@linkplain FluidSwipeEvent.Phase#ENDED}.
 *
 * @author Giuliano Gorgone (anticleiades)
 */
public interface AnimPainterDelegate extends FluidSwipeListener {
    /**
     * This method has the aim of painting the swipe animation. When the delegate is active, this method is expected to be
     * delegated the work of painting the swipe animation by the swipeable component, when the gesture phase ranges from
     * {@linkplain FluidSwipeEvent.Phase#BEGAN} to {@linkplain FluidSwipeEvent.Phase#ENDED}.
     * @param g the <code>Graphics</code> context in which to paint
     */
    void paint(final Graphics g);

    /**
     * Returns whether the delegate is active and thus ready to paint.
     *
     * @return {@code true} if the delegate is ready to paint; {@code false} otherwise
     */
    boolean isActive();
}
