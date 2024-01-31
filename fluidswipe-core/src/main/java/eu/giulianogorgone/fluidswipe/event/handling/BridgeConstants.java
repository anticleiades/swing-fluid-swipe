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

import java.lang.annotation.Native;

/**
 * This class holds the constants used by native code to encode the event type.
 * @author Giuliano Gorgone (anticleiades)
 **/
final class BridgeConstants {
    private BridgeConstants() {
        throw new AssertionError();
    }

    @Native
    static final int LOGICALLY_BEGAN = 1;
    @Native
    static final int PROGRESSED = 1 << 1;
    @Native
    static final int PROGRESSED_NO_MORE_TOUCHING = 1 << 2;
    @Native
    static final int COMPLETED = 1 << 3;
    @Native
    static final int CANCELED = 1 << 4;
    @Native
    static final int ENDED_MASK = COMPLETED | CANCELED;
    @Native
    static final int UPDATE_STATE = 1 << 5;
    @Native
    private static final int UNHANDLED = -1;
}
