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

package eu.giulianogorgone.fluidswipe.utils;

import eu.giulianogorgone.fluidswipe.utils.log.Logging;
import sun.awt.SunToolkit;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.function.BiConsumer;

/**
 * This class provides methods used in order to dispatch the {@code FluidSwipeEvent}s.
 * This class is not part of the public API.
 *
 * @author Giuliano Gorgone (anticleiades)
 */
public final class Threading {
    private Threading() {
        // utility class should not have any public constructor.
        throw new AssertionError();
    }

    private static final BiConsumer<Component, Runnable> uiThreadExecutor;
    private static final ExecutorBiConsumer<Component, Runnable> uiThreadExecutorWait;

    static {
        final Module modJavaDesktop = Component.class.getModule();
        final Module ourModule = Threading.class.getModule();
        if (modJavaDesktop.isExported("sun.awt", ourModule)) { // The non-public Sun API is preferred if available.
            uiThreadExecutor = SunToolkit::executeOnEventHandlerThread;
            uiThreadExecutorWait = SunToolkit::executeOnEDTAndWait;
            Logging.logConfig("Using SunToolkit API to dispatch FluidSwipeEvents");
        } else {
            if (ConfigFlags.enforceEvtDispatchHighPriority) {
                // If the non-public Sun API is enforced, but it is not available, notify the user and fallback to the SwingUtilities API.
                Logging.logSevere(String.format("In order for \"-D%s=true\" to be effective, \"--add-exports java.desktop/sun.awt=%s\" is also needed; falling back to the SwingUtilities API.",
                        ConfigFlags.FLAG_ENFORCE_HIGH_PRIORITY, ourModule.isNamed() ? ourModule.getName() : "ALL-UNNAMED"));
            } else {
                Logging.logConfig("Using SwingUtilities API to dispatch FluidSwipeEvents");
            }
            uiThreadExecutor = (target, r) -> SwingUtilities.invokeLater(r);
            uiThreadExecutorWait = (target, r) -> SwingUtilities.invokeAndWait(r);
        }
    }

    public static void performOnAWTUIThread(final Component target, final Runnable r) {
        if (target == null) {
            Logging.logWarn("requested UI operation with a null target component; ignoring.");
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            uiThreadExecutor.accept(target, r);
        }
    }

    public static void performOnAWTUIThreadAndWait(final Component target, final Runnable r) throws InterruptedException, InvocationTargetException {
        uiThreadExecutorWait.accept(target, r);
    }
}
