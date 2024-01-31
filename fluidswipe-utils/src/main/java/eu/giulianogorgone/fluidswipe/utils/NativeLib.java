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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

/**
 * Utility class for loading native libraries from the JAR.
 * This class is not part of the public API.
 *
 * @author Giuliano Gorgone (anticleiades),
 * @see <a href=
 * "https://github.com/adamheinrich/native-utils">https://github.com/adamheinrich/native-utils</a>
 */

public final class NativeLib {
    private NativeLib() {
        // utility class should not have any public constructor.
        throw new AssertionError();
    }

    // returns true if the library loading succeeds; false otherwise
    public static boolean extractAndLoadFromJAR(final Class<?> clazz, final String libFilename) {
        boolean loadSuccess = false;
        final Path nativeLibraryPath = extractFromJAR(clazz, libFilename);
        if (nativeLibraryPath == null) return false;
        Logging.logFinest("native library extraction: success, nativeLibraryPath: " + nativeLibraryPath);
        try {
            System.load(nativeLibraryPath.toString());
            Logging.logFinest("native library \"" + libFilename + "\" loading: success");
            loadSuccess = true;
        } catch (SecurityException | UnsatisfiedLinkError e) {
            Logging.logSevere("error while loading native library", e);
        } finally {
            deleteTempLibFile(nativeLibraryPath);
        }
        return loadSuccess;
    }

    // returns a non-null Path instance pointing to the temporary copy of the native library if and only if
    // the native library can be successfully extracted from the JAR and copied in the default temporary-file directory.
    private static Path extractFromJAR(final Class<?> clazz, final String libFilename) {
        try (final InputStream inputStream = clazz.getResourceAsStream(libFilename)) {
            if (inputStream == null) {
                Logging.logSevere("could not find native library in JAR – extraction impossible");
                return null;
            }
            final byte[] data = inputStream.readAllBytes();
            final Path nativeLibPath = Files.createTempFile(libFilename + "-", String.valueOf(System.nanoTime()));
            Files.write(nativeLibPath, data);
            return nativeLibPath;
        } catch (Exception e) {
            Logging.logSevere("error while extracting native library", e);
        }
        return null;
    }

    private static void deleteTempLibFile(final Path nativeLibraryPath) {
        // https://github.com/adamheinrich/native-utils/blob/master/src/main/java/cz/adamh/utils/NativeUtils.java
        if (isPosixCompliant()) {
            try {
                Files.delete(nativeLibraryPath);
            } catch (IOException e) {
                Logging.logSevere("could not delete the temporary copy of the library at: " + nativeLibraryPath, e);
            }
        } else nativeLibraryPath.toFile().deleteOnExit();
    }

    // https://github.com/adamheinrich/native-utils/blob/master/src/main/java/cz/adamh/utils/NativeUtils.java
    private static boolean isPosixCompliant() {
        try {
            return FileSystems.getDefault()
                    .supportedFileAttributeViews()
                    .contains("posix");
        } catch (FileSystemNotFoundException
                 | ProviderNotFoundException
                 | SecurityException e) {
            return false;
        }
    }
}
