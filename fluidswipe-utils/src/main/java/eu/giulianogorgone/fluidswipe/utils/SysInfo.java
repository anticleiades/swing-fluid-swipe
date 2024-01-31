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

import java.util.Locale;
/**
 * @author Giuliano Gorgone (anticleiades)
 */
public final class SysInfo {
    private static final String osName = System.getProperty("os.name").toLowerCase();
    private static final String jvmVendor = System.getProperty("java.vm.vendor");
    private static final String arch = System.getProperty("os.arch");
    private static final String osVersion = System.getProperty("os.version").toLowerCase(Locale.ENGLISH);
    private static final String jvmName = System.getProperty("java.vm.name");
    private static final String javaRTVersion = System.getProperty("java.runtime.version");
    private static final String minVerMac = "10.13";

    private static final boolean IS_MAC_OS = osName.startsWith("mac");
    public static final boolean IS_SUPPORTED_MAC_OS = IS_MAC_OS && Utils.compareOSVersionString(osVersion, minVerMac) >= 0;
    private static String dbgInfo;

    public static String getDBGInfo() {
        if (dbgInfo == null) {
            return dbgInfo = String.format("DBGInfo: OS: %s (%s) %s; VM: %s by %s; RTVer: %s", osName, arch, osVersion, jvmName, jvmVendor, javaRTVersion);
        } else {
            return dbgInfo;
        }
    }
}
