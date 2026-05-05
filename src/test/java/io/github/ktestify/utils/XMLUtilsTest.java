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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class XMLUtilsTest {

    @Test
    void getNamespacesFromString_returnsCorrectNamespaces() {
        String xmlContent = "<root xmlns:ns=\"http://example.com\"></root>";
        Map<String, String> expectedNamespaces = new HashMap<>();
        expectedNamespaces.put("ns", "http://example.com");

        Map<String, String> actualNamespaces = XMLUtils.getNamespacesFromString(xmlContent);

        assertEquals(expectedNamespaces, actualNamespaces);
    }

    @Test
    void getNamespacesFromString_returnsEmptyMapForNoNamespaces() {
        String xmlContent = "<root></root>";

        Map<String, String> actualNamespaces = XMLUtils.getNamespacesFromString(xmlContent);

        assertTrue(actualNamespaces.isEmpty());
    }

    @Test
    void compareXML_returnsTrueForIdenticalXML() {
        String xml1 = "<root><child>text</child></root>";
        String xml2 = "<root><child>text</child></root>";

        assertTrue(XMLUtils.compareXML(xml1, xml2));
    }

    @Test
    void compareXML_returnsFalseForDifferentXML() {
        String xml1 = "<root><child>text</child></root>";
        String xml2 = "<root><child>different text</child></root>";

        assertFalse(XMLUtils.compareXML(xml1, xml2));
    }

    @Test
    void compareXML_ignoresSpecifiedElements() {
        String xml1 = "<root><child>text</child><ignored>text</ignored></root>";
        String xml2 = "<root><child>text</child><ignored>different text</ignored></root>";

        assertTrue(XMLUtils.compareXML(xml1, xml2, List.of("ignored")));
    }

    @Test
    void compareXMLByXPath_returnsTrueForIdenticalNodes() {
        String xml1 = "<root><child>text</child></root>";
        String xml2 = "<root><child>text</child></root>";
        String xPathExpression = "/root/child";

        assertTrue(XMLUtils.compareXMLByXPath(xml1, xml2, xPathExpression));
    }

    @Test
    void compareXMLByXPath_returnsFalseForDifferentNodes() {
        String xml1 = "<root><child>text</child></root>";
        String xml2 = "<root><child>different text</child></root>";
        String xPathExpression = "/root/child";

        assertFalse(XMLUtils.compareXMLByXPath(xml1, xml2, xPathExpression));
    }

    @Test
    void compareXMLByXPaths_returnsTrueForIdenticalNodes() {
        String xml1 = "<root><child>text</child><anotherChild>text</anotherChild></root>";
        String xml2 = "<root><child>text</child><anotherChild>text</anotherChild></root>";
        String xPathExpression1 = "/root/child";
        String xPathExpression2 = "/root/anotherChild";

        assertTrue(XMLUtils.compareXMLByXPaths(xml1, xml2, Arrays.asList(xPathExpression1, xPathExpression2)));
    }

    @Test
    void compareXMLByXPaths_returnsFalseForDifferentNodes() {
        String xml1 = "<root><child>text</child><anotherChild>text</anotherChild></root>";
        String xml2 = "<root><child>text</child><anotherChild>different text</anotherChild></root>";
        String xPathExpression1 = "/root/child";
        String xPathExpression2 = "/root/anotherChild";

        assertFalse(XMLUtils.compareXMLByXPaths(xml1, xml2, Arrays.asList(xPathExpression1, xPathExpression2)));
    }

    @Test
    void getNamespacesFromString_throwsExceptionForInvalidXML() {
        String xmlContent = "<>/// \\\root xmlnsns=\"http://example.com\"></root>";

        assertNull(XMLUtils.getNamespacesFromString(xmlContent));
    }

    @Test
    void compareXML_throwsExceptionForInvalidXML() {
        String xml1 = "<root><child>text</child></root>";
        String xml2 = "<root><child>tevxt</child></root>";

        assertFalse(XMLUtils.compareXML(xml1, xml2));
    }
}
