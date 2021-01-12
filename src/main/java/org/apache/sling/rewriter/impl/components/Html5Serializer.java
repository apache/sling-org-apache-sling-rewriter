/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.rewriter.impl.components;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.cocoon.components.serializers.encoding.Charset;
import org.apache.cocoon.components.serializers.encoding.CharsetFactory;
import org.apache.cocoon.components.serializers.encoding.Encoder;
import org.apache.cocoon.components.serializers.encoding.HTMLEncoder;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Serializer;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * Serializer for writing HTML5 compliant markup
 */
public class Html5Serializer implements Serializer {

    private static final int CHAR_EQ = '=';

    private static final int CHAR_GT = '>';

    private static final int CHAR_LT = '<';

    private static final int CHAR_QT = '"';

    private static final int CHAR_SP = ' ';

    private static final String DOCTYPE = "<!DOCTYPE html>";

    private static final Set<String> emptyTags = new HashSet<>();
    static {
        emptyTags.addAll(Arrays.asList("area", "base", "br", "col", "embed", "hr", "img", "input", "keygen", "link",
                "meta", "param", "source", "track", "wbr"));
    }
    private PrintWriter writer;

    private Charset charset;

    private Encoder encoder;

    @Override
    public void characters(char[] buffer, int offset, int length) throws SAXException {
        if (length == 0) {
            writer.flush();
        } else {
            writeEncoded(buffer, offset, length);
        }
    }

    @Override
    public void dispose() {
        // Nothing required
    }

    @Override
    public void endDocument() throws SAXException {
        writer.flush();
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (!emptyTags.contains(localName)) {
            writer.write("</");
            writer.write(localName);
            writer.write(CHAR_GT);
        }
    }

    @Override
    public void endPrefixMapping(String s) throws SAXException {
        // Nothing required
    }

    @Override
    public void ignorableWhitespace(char[] ac, int i, int j) throws SAXException {
        // Nothing required
    }

    @Override
    public void init(ProcessingContext context, ProcessingComponentConfiguration config) throws IOException {
        if (context.getWriter() == null) {
            throw new IllegalArgumentException("Failed to initialize HTML5Serializer, null writer specified!");
        } else {
            writer = context.getWriter();
        }
        this.charset = CharsetFactory.newInstance()
                .getCharset(config.getConfiguration().get("encoding", StandardCharsets.UTF_8.name()));
        this.encoder = new HTMLEncoder();
    }

    @Override
    public void processingInstruction(String s, String s1) throws SAXException {
        // Nothing required
    }

    @Override
    public void setDocumentLocator(Locator locator1) {
        // Nothing required
    }

    @Override
    public void skippedEntity(String s) throws SAXException {
        // Nothing required
    }

    @Override
    public void startDocument() throws SAXException {
        writer.println(DOCTYPE);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        boolean endSlash = false;
        writer.write(CHAR_LT);
        writer.write(localName);

        for (int i = 0; i < atts.getLength(); i++) {
            if ("endSlash".equals(atts.getQName(i))) {
                endSlash = true;
            }
            String value = atts.getValue(i);
            if (shouldContinue(localName, atts, i)) {
                continue;
            }
            writer.write(CHAR_SP);
            writer.write(atts.getLocalName(i));

            writer.write(CHAR_EQ);
            writer.write(CHAR_QT);
            char[] data = value.toCharArray();
            this.writeEncoded(data, 0, data.length);
            writer.write(CHAR_QT);
        }

        if (endSlash) {
            writer.write("/");
        }
        writer.write(CHAR_GT);
    }

    private boolean shouldContinue(String localName, Attributes atts, int i) {
        if ("endSlash".equals(atts.getQName(i))) {
            return true;
        }
        if ("a".equals(localName) && "shape".equals(atts.getLocalName(i))) {
            return true;
        }
        if ("iframe".equals(localName)
                && ("frameborder".equals(atts.getLocalName(i)) || "scrolling".equals(atts.getLocalName(i)))) {
            return true;
        }
        if ("br".equals(localName) && ("clear".equals(atts.getLocalName(i)))) {
            return true;
        }
        return atts.getValue(i) == null;
    }

    @Override
    public void startPrefixMapping(String s, String s1) throws SAXException {
        // Nothing required
    }

    /**
     * Encode and write a specific part of an array of characters.
     */
    private void writeEncoded(char[] data, int start, int length) throws SAXException {
        int end = start + length;

        if (data == null) {
            throw new SAXException("Invalid data, null");
        }
        if ((start < 0) || (start > data.length) || (length < 0) || (end > data.length) || (end < 0)) {
            throw new SAXException("Invalid data, out of bounds");
        }
        if (length == 0) {
            return;
        }

        for (int i = start; i < end; i++) {
            char c = data[i];

            if (this.charset.allows(c) && this.encoder.allows(c)) {
                continue;
            }

            if (start != i) {
                writer.write(data, start, i - start);
            }
            writer.write(this.encoder.encode(c));
            start = i + 1;
        }
        if (start != end) {
            writer.write(data, start, end - start);
        }
    }

}
