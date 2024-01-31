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
/**
 * Abstract adapter class for receiving {@code FluidSwipeEvent}s. This class is provided
 * as a convenience for creating listeners, so that the class interested in processing a {@code FluidSwipeEvent} can
 * override only the methods of interest.
 * @author Giuliano Gorgone (anticleiades)
 * @see FluidSwipeListener
 */
public abstract class FluidSwipeAdapter implements FluidSwipeListener {
    @Override
    public void fluidSwipeProgressed(final FluidSwipeEvent e) {
    }

    @Override
    public void fluidSwipeBegan(final FluidSwipeEvent e) {
    }

    @Override
    public void fluidSwipeEnded(final FluidSwipeEvent e) {

    }
}
