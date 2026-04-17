/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.ktestify.match.impl;

import io.github.ktestify.exceptions.ComparisonException;
import io.github.ktestify.match.MatchContext;
import io.github.ktestify.match.MatchResult;
import io.github.ktestify.match.RecordMatcher;
import io.github.ktestify.models.ConsumedRecord;
import io.github.ktestify.utils.FileUtils;
import io.github.ktestify.utils.XMLUtils;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Compares the record value as XML against an expected XML file. Supports optional element exclusion via
 * {@link MatchContext#getExcludedFields()}.
 *
 * <p>Additionally, any element in the expected template whose text content is exactly {@code EXCLUDED} is automatically
 * added to the exclusion list. This allows a single expected file to serve multiple scenarios — scenarios only need to
 * explicitly list elements they want structurally excluded; all others marked {@code EXCLUDED} in the file are
 * suppressed as well.
 *
 * <p>Requires {@link MatchContext#getMatchFilePath()} to be set.
 *
 * @since 0.3.0
 */
@Slf4j
public class XmlRecordMatcher implements RecordMatcher<String> {

    /** Sentinel value used in expected XML templates to mark elements that should be ignored. */
    private static final String EXCLUDED_SENTINEL = "EXCLUDED";

    @Override
    public MatchResult match(List<ConsumedRecord<String>> records, MatchContext context) throws ComparisonException {

        if (context.getMatchFilePath() == null || context.getMatchFilePath().isBlank()) {
            throw new ComparisonException("XmlRecordMatcher requires matchFilePath to be set.");
        }

        String expected = FileUtils.getFileContent(FileUtils.getFile(context.getMatchFilePath()));
        String actual = records.get(0).getValue();
        log.debug("XML comparison — actual:\n{}\nExpected:\n{}", actual, expected);

        // Merge explicit exclusions with any elements marked EXCLUDED in the template
        List<String> effectiveExclusions = buildEffectiveExclusions(context.getExcludedFields(), expected);

        boolean result;
        if (!effectiveExclusions.isEmpty()) {
            log.debug("Excluding XML elements: {}", effectiveExclusions);
            result = XMLUtils.compareXML(actual, expected, effectiveExclusions);
        } else {
            result = XMLUtils.compareXML(actual, expected);
        }

        if (result) {
            return MatchResult.pass(expected, actual);
        }
        return MatchResult.fail(
                "XML documents do not match (excluded: " + effectiveExclusions + ").", expected, actual);
    }

    /**
     * Builds the effective exclusion list by merging the explicitly provided list with the names of any elements whose
     * text content in the expected XML template is exactly {@value #EXCLUDED_SENTINEL}.
     */
    private List<String> buildEffectiveExclusions(List<String> explicit, String expectedXml) {
        List<String> result = new ArrayList<>(explicit != null ? explicit : List.of());
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(new StringReader(expectedXml)), new DefaultHandler() {
                private String currentElement;
                private final StringBuilder text = new StringBuilder();

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    currentElement = qName;
                    text.setLength(0);
                }

                @Override
                public void characters(char[] ch, int start, int length) {
                    text.append(ch, start, length);
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    if (EXCLUDED_SENTINEL.equals(text.toString().trim()) && !result.contains(currentElement)) {
                        log.debug(
                                "Auto-excluding element '{}' — sentinel value '{}' found in template.",
                                currentElement,
                                EXCLUDED_SENTINEL);
                        result.add(currentElement);
                    }
                    currentElement = null;
                    text.setLength(0);
                }
            });
        } catch (Exception e) {
            log.warn("Could not parse expected XML to detect sentinel exclusions: {}", e.getMessage());
        }
        return result;
    }
}
