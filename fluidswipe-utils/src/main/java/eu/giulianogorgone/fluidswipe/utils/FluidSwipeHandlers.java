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
import eu.giulianogorgone.fluidswipe.handlers.FluidSwipeHandler;
import eu.giulianogorgone.fluidswipe.handlers.impl.NopFluidSwipeHandler;
import eu.giulianogorgone.fluidswipe.handlers.macos.impl.MacOSFluidSwipeHandler;

/**
 * @author Giuliano Gorgone (anticleiades)
 */
public final class FluidSwipeHandlers {
    private static final String MAC_OS_LIB_FNAME = "libFluidSwipe.dylib";

    public static FluidSwipeHandler getHandler() {
        Logging.init("FluidSwipe", ConfigFlags.logLevel, ConfigFlags.logFilePath);
        Logging.logConfig(SysInfo.getDBGInfo());
        if (SysInfo.IS_SUPPORTED_MAC_OS && NativeLib.extractAndLoadFromJAR(MacOSFluidSwipeHandler.class, MAC_OS_LIB_FNAME)) {
            // any platform-specific implementation library must expose the "_Java_it_anticleiades_utils_log_Logging_initNative" symbol
            Logging.initNative(ConfigFlags.logLevel);
            return new MacOSFluidSwipeHandler();
            // support for other platforms to be added
            // else if (SysInfo.IS_SUPPORTED_WIN_NT && NativeLib.extractAndLoadFromJAR(WinNTFluidSwipeHandler.class, WIN_NT_LIB_FNAME))
            // else if (SysInfo.IS_SUPPORTED_LWAYLAND && NativeLib.extractAndLoadFromJAR(LinuxWaylandFluidSwipeHandler.class, WIN_NT_LIB_FNAME))
            // else if (SysInfo.IS_SUPPORTED_LX11 && NativeLib.extractAndLoadFromJAR(LinuxX11FluidSwipeHandler.class, WIN_NT_LIB_FNAME))
        } else {
            return new NopFluidSwipeHandler();
        }
    }
}