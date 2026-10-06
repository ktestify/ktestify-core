/*
 * Copyright 2026 Nil MALHOMME (malhomme.nil+oss@icloud.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.ktestify.utils;

import io.github.ktestify.io.inputs.DynamicVariableProcessor;
import java.io.*;
import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UtilityClass
public final class FileUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    /**
     * Gets a File object from the provided full path.
     *
     * @param fullPath the full path of the file
     * @return the File object
     */
    public static File getFile(String fullPath) {
        return new File(fullPath);
    }

    /**
     * Reads the content of the file into a String using an InputStream.
     *
     * @param file the file to read
     * @return the content of the file as a String
     * @throws RuntimeException if something went wrong
     */
    public static String getFileContent(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            String content = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            if (DynamicVariableProcessor.doesContainDynamicVariable(content)) {
                DynamicVariableProcessor dynamicVariableProcessor = new DynamicVariableProcessor();
                content = dynamicVariableProcessor.process(content);
            }

            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Opens an InputStream for a given file. This method is useful for obtaining a stream to read file content.
     *
     * @param file The file for which to open the InputStream.
     * @return An InputStream for the specified file.
     * @throws RuntimeException If an IOException occurs when opening the InputStream.
     */
    public static InputStream getInputStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (IOException e) {
            LOGGER.error("Something when wrong while getting file content from a File object : {} ", e.getMessage());

            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the content of an InputStream into a string. Similar to the File overload, this method reads the entire
     * stream content and returns it as a UTF-8 encoded string.
     *
     * @param file An InputStream to read from.
     * @return A string representing the content read from the InputStream.
     * @throws RuntimeException If an IOException occurs during stream reading.
     */
    public static String getFileContent(InputStream file) {
        try {
            String content = IOUtils.toString(file, StandardCharsets.UTF_8);
            if (DynamicVariableProcessor.doesContainDynamicVariable(content)) {
                DynamicVariableProcessor dynamicVariableProcessor = new DynamicVariableProcessor();
                content = dynamicVariableProcessor.process(content);
            }

            return content;
        } catch (IOException e) {
            LOGGER.error("Something when wrong while getting file content from a input stream : {} ", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the provided content to a file.
     *
     * @param file The file to which the content will be written.
     * @param content The content to write to the file.
     * @throws IOException If an error occurs during writing.
     */
    public static void writeFileContent(File file, String content) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
