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

import java.util.logging.Level;

/**
 * This class is not part of the public API.
 *
 * @author Giuliano Gorgone (anticleiades)
 */
public final class ConfigFlags {
    static final String PROP_PREFIX = "fluidswipe.";
    static final String KEY_LOG_FILE_PATH = PROP_PREFIX + "logFilePath";
    static final String KEY_LOG_LEVEL = PROP_PREFIX + "logLevel";
    static final String FLAG_ENFORCE_HIGH_PRIORITY = PROP_PREFIX + "eventDispatchEnforceHighPriority";

    public static final boolean enforceEvtDispatchHighPriority = Boolean.getBoolean(FLAG_ENFORCE_HIGH_PRIORITY);
    public static final Level logLevel = Level.parse(System.getProperty(KEY_LOG_LEVEL, "INFO").toUpperCase());
    public static final String logFilePath = System.getProperty(KEY_LOG_FILE_PATH);
}


