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

package eu.giulianogorgone.fluidswipe.utils.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
/**
 * @author Giuliano Gorgone (anticleiades)
 */
class DefaultLogFormatter extends Formatter {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm:ss");

    private static String getThrowableContent(final LogRecord record) {
        final StringWriter strWriter = new StringWriter();
        final PrintWriter prtWriter = new PrintWriter(strWriter);
        prtWriter.println();
        record.getThrown().printStackTrace(prtWriter);
        prtWriter.close();
        return strWriter.toString();
    }

    DefaultLogFormatter() {
    }

    @Override
    public String format(LogRecord record) {
        final StringBuilder sb = new StringBuilder();
        sb.append(dateTimeFormatter.format(record.getInstant().atZone(ZoneId.systemDefault()))).append(' ')
                .append('[').append(record.getLevel()).append("] ");
        final Optional<String> clsSrc = Optional.ofNullable(record.getSourceClassName());
        final Optional<String> mthSrc = Optional.ofNullable(record.getSourceMethodName());
        final Object[] parameters = record.getParameters();
        if (parameters != null && parameters.length >= 1) {
            sb.append(parameters[0]).append(' ');
        }
        if (Logging.isLogRequestFromNative(record)) {
            sb.append("[JNI] ");
        }
        sb.append(clsSrc.orElse(record.getLoggerName()))
                .append("::")
                .append(mthSrc.orElse("<unkMethod>"))
                .append(": ").append(formatMessage(record));
        if (record.getThrown() != null)
            sb.append(getThrowableContent(record));
        sb.append('\n');
        return sb.toString();
    }
}
