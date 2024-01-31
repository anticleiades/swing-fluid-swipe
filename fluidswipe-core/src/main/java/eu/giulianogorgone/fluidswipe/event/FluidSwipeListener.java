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

import java.io.Serializable;

/**
 * The class interested in processing a {@code FluidSwipeEvent}
 * either implements this interface (and all the methods it
 * contains) or extends the abstract {@code FluidSwipeAdapter} class
 * (overriding only the methods of interest).
 *
 * @author Giuliano Gorgone (anticleiades)
 */
public interface FluidSwipeListener extends Serializable {
    /**
     * Invoked a single time and when the gesture <b>logically</b> starts
     *
     * @param e the event to be processed
     */
    void fluidSwipeBegan(final FluidSwipeEvent e);

    /**
     * Invoked multiple times during gesture progression.
     *
     * @param e the event to be processed
     */
    void fluidSwipeProgressed(final FluidSwipeEvent e);

    /**
     * Invoked a single time and when the gesture <b>logically</b> ends.
     *
     * @param e the event to be processed
     */
    void fluidSwipeEnded(final FluidSwipeEvent e);
}
