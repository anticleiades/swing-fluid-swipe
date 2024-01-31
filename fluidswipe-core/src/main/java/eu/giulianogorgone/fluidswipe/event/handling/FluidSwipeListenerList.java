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

import eu.giulianogorgone.fluidswipe.FluidSwipe;
import eu.giulianogorgone.fluidswipe.event.FluidSwipeListener;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;


/**
 * This class holds the {@code FluidSwipeListener} added to a {@code JComponent} via {@link FluidSwipe#addListenerTo(JComponent, FluidSwipeListener)}.
 * This class is not part of the public API.
 * @author Giuliano Gorgone (anticleiades)
 */
public final class FluidSwipeListenerList implements Serializable {
    private static final String KEY_CLIENT_PROP = "it.anticleiades.fluidswipe—FluidSwipeListenerList";
    public final List<FluidSwipeListener> listenerList = new LinkedList<>();

    private FluidSwipeListenerList() {
    }

    FluidSwipeListener[] copyListeners() {
        return listenerList.toArray(new FluidSwipeListener[0]);
    }

    // return a not-null value if and only if a FluidSwipeListenerList exists.
    public static FluidSwipeListenerList get(final Component component) {
        Object clientPropVal = component instanceof JComponent ? ((JComponent) component).getClientProperty(KEY_CLIENT_PROP) : null;
        return clientPropVal instanceof FluidSwipeListenerList ? ((FluidSwipeListenerList) clientPropVal) : null;
    }

    // returns either the new installed holder, or the one already existing found in client props.
    public static FluidSwipeListenerList installIfNeededAndGet(final JComponent component) {
        FluidSwipeListenerList listenerList = get(component);
        if (listenerList == null)
            component.putClientProperty(KEY_CLIENT_PROP, (listenerList = new FluidSwipeListenerList()));
        return listenerList;
    }
}
