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

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.*;

/**
 * Utility class for comparing XML documents, with optional element exclusion and XPath-based comparison.
 *
 * @since 0.3.0
 */
@UtilityClass
public final class XMLUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLUtils.class);
    private static final int XMLNS_PREFIX_LENGTH = "xmlns:".length();

    /**
     * Creates a hardened {@link SAXParserFactory} with XXE protections enabled.
     *
     * <p>Enables secure processing and disables external general/parameter entities, DOCTYPE declarations, and external
     * DTD loading. Use this factory for every SAX parse of test data, including expected template files.
     *
     * @return a hardened, non-namespace-aware SAXParserFactory
     * @since 1.1.4
     */
    public static SAXParserFactory newHardenedSAXParserFactory() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        // Namespace-awareness is intentionally NOT enabled here: getNamespacesFromString
        // relies on xmlns:* attributes being reported as regular attributes via getQName(),
        // which namespace-aware mode would suppress. XXE hardening features below are
        // independent of this setting.
        try {
            factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            LOGGER.warn("Could not enable secure processing on SAXParserFactory: {}", e.getMessage());
        }
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            LOGGER.warn("Could not disallow DOCTYPE declarations on SAXParserFactory: {}", e.getMessage());
        }
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        } catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            LOGGER.warn("Could not disable external general entities on SAXParserFactory: {}", e.getMessage());
        }
        try {
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            LOGGER.warn("Could not disable external parameter entities on SAXParserFactory: {}", e.getMessage());
        }
        try {
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            LOGGER.warn("Could not disable external DTD loading on SAXParserFactory: {}", e.getMessage());
        }
        return factory;
    }

    /**
     * Creates a hardened {@link DocumentBuilderFactory} with XXE protections enabled.
     *
     * <p>Disables external general/parameter entities, DOCTYPE declarations, and external DTD loading. Used by
     * XMLUnit's {@link DiffBuilder} via {@code withDocumentBuilderFactory}.
     *
     * @return a hardened DocumentBuilderFactory
     */
    private static DocumentBuilderFactory newHardenedDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (ParserConfigurationException e) {
            LOGGER.warn("Could not disallow DOCTYPE declarations on DocumentBuilderFactory: {}", e.getMessage());
        }
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        } catch (ParserConfigurationException e) {
            LOGGER.warn("Could not disable external general entities on DocumentBuilderFactory: {}", e.getMessage());
        }
        try {
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (ParserConfigurationException e) {
            LOGGER.warn("Could not disable external parameter entities on DocumentBuilderFactory: {}", e.getMessage());
        }
        try {
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (ParserConfigurationException e) {
            LOGGER.warn("Could not disable external DTD loading on DocumentBuilderFactory: {}", e.getMessage());
        }
        factory.setExpandEntityReferences(false);
        factory.setXIncludeAware(false);
        return factory;
    }

    /**
     * Extracts XML namespace declarations from a raw XML string.
     *
     * @param xmlContent the XML document as a string
     * @return a map of prefix → URI, or {@code null} if parsing fails
     */
    public static Map<String, String> getNamespacesFromString(String xmlContent) {
        Map<String, String> namespaces = new HashMap<>();
        try {
            SAXParserFactory factory = newHardenedSAXParserFactory();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(new InputSource(new StringReader(xmlContent)), new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    for (int i = 0; i < attributes.getLength(); i++) {
                        if (attributes.getQName(i).startsWith("xmlns:")) {
                            namespaces.put(
                                    attributes.getQName(i).substring(XMLNS_PREFIX_LENGTH), attributes.getValue(i));
                        }
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.error("Error parsing XML namespaces: {}", e.getMessage());
            return null;
        }
        return namespaces;
    }

    /**
     * Compares two XML strings for strict identity (order and content must match).
     *
     * @param actualValue the actual XML
     * @param expectedValue the expected XML
     * @return {@code true} if the documents are identical
     */
    public static boolean compareXML(String actualValue, String expectedValue) {
        LOGGER.debug("Comparing XML:\nActual  : {}\nExpected: {}", actualValue, expectedValue);
        Diff diff = DiffBuilder.compare(actualValue)
                .withTest(expectedValue)
                .withDocumentBuilderFactory(newHardenedDocumentBuilderFactory())
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
                .checkForIdentical()
                .build();

        if (diff.hasDifferences()) {
            LOGGER.error("XML documents are not identical.");
            logDiff(diff);
            return false;
        }
        LOGGER.info("XML documents are identical.");
        return true;
    }

    /**
     * Compares two XML strings, ignoring specified element names.
     *
     * @param actualValue the actual XML
     * @param expectedValue the expected XML
     * @param ignoredElements element names to exclude from comparison
     * @return {@code true} if the documents are similar (ignoring excluded elements)
     */
    public static boolean compareXML(String actualValue, String expectedValue, List<String> ignoredElements) {
        LOGGER.debug(
                "Comparing XML (excluded elements: {}):\nActual  : {}\nExpected: {}",
                ignoredElements,
                actualValue,
                expectedValue);

        Diff diff = DiffBuilder.compare(actualValue)
                .withTest(expectedValue)
                .withDocumentBuilderFactory(newHardenedDocumentBuilderFactory())
                .normalizeWhitespace()
                .ignoreWhitespace()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
                .checkForSimilar()
                .withDifferenceEvaluator(new ExclusionDifferenceEvaluator(ignoredElements))
                .withNamespaceContext(getNamespacesFromString(expectedValue))
                .withNamespaceContext(getNamespacesFromString(actualValue))
                .build();

        if (diff.hasDifferences()) {
            LOGGER.error("XML documents differ (excluded: {}).", ignoredElements);
            logDiff(diff);
            return false;
        }
        LOGGER.info("XML documents are identical (excluded elements: {}).", ignoredElements);
        return true;
    }

    /**
     * Compares two XML strings using a single XPath expression as the node selector.
     *
     * @param actualValue the actual XML
     * @param expectedValue the expected XML
     * @param xPathExpression the XPath expression
     * @return {@code true} if the nodes selected by the XPath are identical
     */
    public static boolean compareXMLByXPath(String actualValue, String expectedValue, String xPathExpression) {
        try {
            LOGGER.info("Comparing XML by XPath: {}", xPathExpression);
            Map<String, String> namespaces = getNamespacesFromString(expectedValue);
            Diff diff = DiffBuilder.compare(actualValue)
                    .withTest(expectedValue)
                    .withDocumentBuilderFactory(newHardenedDocumentBuilderFactory())
                    .withNodeMatcher(new DefaultNodeMatcher(
                            ElementSelectors.byXPath(xPathExpression, namespaces, ElementSelectors.byNameAndText)))
                    .checkForIdentical()
                    .build();

            if (diff.hasDifferences()) {
                LOGGER.error("XML nodes differ for XPath '{}'.", xPathExpression);
                logDiff(diff);
                return false;
            }
            LOGGER.info("XML nodes are identical for XPath '{}'.", xPathExpression);
            return true;
        } catch (Exception e) {
            LOGGER.error("Error comparing XML by XPath '{}': {}", xPathExpression, e.getMessage());
            return false;
        }
    }

    /**
     * Compares two XML strings using multiple XPath expressions. All expressions must match for the method to return
     * {@code true}.
     *
     * @param actualValue the actual XML
     * @param expectedValue the expected XML
     * @param xPathExpressions list of XPath expressions
     * @return {@code true} if all XPath comparisons pass
     */
    public static boolean compareXMLByXPaths(String actualValue, String expectedValue, List<String> xPathExpressions) {
        for (String xPath : xPathExpressions) {
            if (!compareXMLByXPath(actualValue, expectedValue, xPath)) {
                return false;
            }
        }
        return true;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private static void logDiff(Diff diff) {
        for (Difference difference : diff.getDifferences()) {
            Comparison c = difference.getComparison();
            LOGGER.debug(
                    """
                    Difference found:
                    -----------------------------------
                    Control (expected) XPath   : {}
                    Test    (actual)   XPath   : {}
                    Control Parent XPath       : {}
                    Test    Parent XPath       : {}
                    Control Value              : {}
                    Test    Value              : {}
                    -----------------------------------""",
                    c.getControlDetails().getXPath(),
                    c.getTestDetails().getXPath(),
                    c.getControlDetails().getParentXPath(),
                    c.getTestDetails().getParentXPath(),
                    c.getControlDetails().getValue(),
                    c.getTestDetails().getValue());
        }
    }

    private static class ExclusionDifferenceEvaluator implements DifferenceEvaluator {
        private final List<String> excludedElements;

        ExclusionDifferenceEvaluator(List<String> excludedElements) {
            this.excludedElements = excludedElements;
        }

        @Override
        public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
            if (outcome == ComparisonResult.EQUAL) {
                return outcome;
            }

            String controlXPath = comparison.getControlDetails().getXPath();
            String testXPath = comparison.getTestDetails().getXPath();

            // 1. Direct hit: the differing node IS an excluded element
            if (isExcluded(controlXPath) || isExcluded(testXPath)) {
                return ComparisonResult.SIMILAR;
            }

            // 2. Child-count difference caused by an excluded element being present in one
            //    document but absent in the other.  XMLUnit fires CHILD_NODELIST_LENGTH on
            //    the *parent* node, so the XPath points to the parent (e.g. /order[1]), not
            //    to the excluded child.
            //
            //    Without DOM access we cannot determine which excluded elements are actual
            //    children of this parent.  We conservatively suppress the length difference
            //    when the delta does not exceed the total number of excluded elements.
            //    This is safe because XMLUnit also fires CHILD_LOOKUP (point 3 below) for
            //    each missing/extra child: any non-excluded child will be caught there and
            //    reported as a real difference.
            if (comparison.getType() == ComparisonType.CHILD_NODELIST_LENGTH) {
                Object controlVal = comparison.getControlDetails().getValue();
                Object testVal = comparison.getTestDetails().getValue();
                if (controlVal instanceof Integer controlCount && testVal instanceof Integer testCount) {
                    int delta = Math.abs(controlCount - testCount);
                    if (delta > 0 && delta <= excludedElements.size()) {
                        LOGGER.debug(
                                "Suppressing CHILD_NODELIST_LENGTH difference,  "
                                        + "delta {} within {} excluded element(s); "
                                        + "non-excluded children will be caught by CHILD_LOOKUP.",
                                delta,
                                excludedElements.size());
                        return ComparisonResult.SIMILAR;
                    }
                }
            }

            // 3. CHILD_LOOKUP: one side is null (node absent in one document)
            //    Check whichever side is non-null.
            if (comparison.getType() == ComparisonType.CHILD_LOOKUP) {
                String nonNullXPath = controlXPath != null ? controlXPath : testXPath;
                if (isExcluded(nonNullXPath)) {
                    return ComparisonResult.SIMILAR;
                }
            }

            return outcome;
        }

        /** Returns true if {@code xPath} refers to the excluded element or any descendant. */
        private boolean isExcluded(String xPath) {
            if (xPath == null) return false;
            return excludedElements.stream()
                    .anyMatch(el -> xPath.contains("/" + el + "[")
                            || xPath.contains("/" + el + "/")
                            || xPath.endsWith("/" + el));
        }
    }
}
