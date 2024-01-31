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

/**
 * Provides Fluid-Swipe support on OS X/macOS.
 * @author Giuliano Gorgone (anticleiades)
 */
module fluidswipe.macos {
    exports eu.giulianogorgone.fluidswipe.handlers.macos.impl to fluidswipe.utils;
    requires fluidswipe.handler.api;
}