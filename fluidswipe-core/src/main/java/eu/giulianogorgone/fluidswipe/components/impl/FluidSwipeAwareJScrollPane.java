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

package eu.giulianogorgone.fluidswipe.components.impl;

import eu.giulianogorgone.fluidswipe.components.FluidSwipeVetoer;
import eu.giulianogorgone.fluidswipe.event.FluidSwipeEvent;

import javax.swing.*;
import java.awt.*;

/**
 * A {@code FluidSwipeVetoer} implementation consisting in
 * a {@code JScrollPane} in which fluid-swipe gesture can coexist properly with the horizontal scroll gesture.
 * <p>
 * The gesture is interpreted as a scroll gesture if there is scrollable content in the direction in which it occurs.
 *
 * @author Giuliano Gorgone (anticleiades)
 * @see FluidSwipeVetoer
 */
public class FluidSwipeAwareJScrollPane extends JScrollPane implements FluidSwipeVetoer {
    public FluidSwipeAwareJScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
        super(view, vsbPolicy, hsbPolicy);
    }

    public FluidSwipeAwareJScrollPane(Component view) {
        this(view, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    public FluidSwipeAwareJScrollPane(int vsbPolicy, int hsbPolicy) {
        this(null, vsbPolicy, hsbPolicy);
    }

    public FluidSwipeAwareJScrollPane() {
        this(null, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    @Override
    public boolean permitFluidSwipeGesture(FluidSwipeEvent e) {
        final FluidSwipeEvent.Direction contentDirection = e.getLogicalGestureDirection();
        if (horizontalScrollBar == null || getHorizontalScrollBarPolicy() == HORIZONTAL_SCROLLBAR_NEVER) return true;
        return (contentDirection == FluidSwipeEvent.Direction.RIGHT_TO_LEFT && (horizontalScrollBar.getValue() + horizontalScrollBar.getModel().getExtent()) == horizontalScrollBar.getMaximum())
                || (contentDirection == FluidSwipeEvent.Direction.LEFT_TO_RIGHT && horizontalScrollBar.getValue() == horizontalScrollBar.getMinimum());
    }
}
