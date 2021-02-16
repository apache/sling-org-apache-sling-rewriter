/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.rewriter.impl.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.impl.ProcessingComponentConfigurationImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class Html5SerializerTest {

    private ProcessingContext context;
    private StringWriter writer;
    private ProcessingComponentConfigurationImpl config;
    private Html5Serializer testSerializer;

    @Before
    public void init() throws IOException {
        context = Mockito.mock(ProcessingContext.class);
        writer = Mockito.spy(new StringWriter());
        Mockito.when(context.getWriter()).thenReturn(new PrintWriter(writer));

        config = new ProcessingComponentConfigurationImpl("/apps/config",
                new ValueMapDecorator(Collections.emptyMap()));

        testSerializer = new Html5Serializer();
        testSerializer.init(context, config);
    }

    @Test
    public void testNoWriter() throws IOException {
        Html5Serializer serializer = new Html5Serializer();
        try {
            serializer.init(Mockito.mock(ProcessingContext.class), config);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Failed to initialize HTML5Serializer, null writer specified!", e.getMessage());
        }
    }

    @Test
    public void testCharacters() throws IOException, SAXException {

        testSerializer.characters("Hello World!".toCharArray(), 0, 5);
        assertEquals("Hello", writer.toString());

        testSerializer.characters(" & Goodbye".toCharArray(), 0, 10);
        assertEquals("Hello &amp; Goodbye", writer.toString());
    }

    @Test
    public void testZeroLengthCharacters() throws IOException, SAXException {
        testSerializer.characters(new char[0], 0, 0);
        Mockito.verify(writer).flush();
    }

    @Test
    public void testInvalidLengthCharacters() throws IOException {
        try {
            testSerializer.characters(new char[0], 0, 2);
            fail();
        } catch (SAXException e) {
            // expected
        }
    }

    @Test
    public void testEndDocument() throws IOException, SAXException {
        testSerializer.endDocument();
        Mockito.verify(writer).flush();
    }

    @Test
    public void testEndElement() throws IOException, SAXException {
        testSerializer.endElement("", "img", "img");
        Mockito.verify(writer, Mockito.never()).write(Mockito.anyString());
        Mockito.verify(writer, Mockito.never()).write(Mockito.anyChar());

        testSerializer.endElement("", "a", "a");
        assertEquals("</a>", writer.toString());
    }

    @Test
    public void testStartDocument() throws IOException, SAXException {
        testSerializer.startDocument();
        assertTrue(writer.toString().matches("<!DOCTYPE html>\\r\\n?|\\n"));
    }

    @Test
    public void testStartElement() throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "class", "", "string", "my-class");
        atts.addAttribute("", "id", "", "string", "div-id");
        atts.addAttribute("", "data-thing", "", null, null);
        testSerializer.startElement("", "div", "div", atts);

        assertEquals("<div class=\"my-class\" id=\"div-id\">", writer.toString());
    }

    @Test
    public void testStartClosingElement() throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "name", "", "string", "bob");
        atts.addAttribute("", "value", "", "string", "\"<script>alert('XSS')</script>\"");
        atts.addAttribute("", "endSlash", "endSlash", null, null);

        testSerializer.startElement("", "input", "input", atts);

        assertEquals("<input name=\"bob\" value=\"&quot;&lt;script&gt;alert('XSS')&lt;/script&gt;&quot;\"/>",
                writer.toString());
    }

    @Test
    public void testSkippedAttributes() throws SAXException {
        AttributesImpl a = new AttributesImpl();
        a.addAttribute("", "shape", "", "string", "parallelogram");
        a.addAttribute("", "class", "", "string", "my-link");
        testSerializer.startElement("", "a", "a", a);

        AttributesImpl iframe = new AttributesImpl();
        iframe.addAttribute("", "frameborder", "", "string", "big&ugly");
        iframe.addAttribute("", "scrolling", "", "string", "sotrue");
        iframe.addAttribute("", "class", "", "string", "framey");
        testSerializer.startElement("", "iframe", "iframe", iframe);

        AttributesImpl br = new AttributesImpl();
        br.addAttribute("", "clear", "", "string", "ly");
        br.addAttribute("", "class", "", "string", "clear");
        testSerializer.startElement("", "br", "br", br);

        assertEquals("<a class=\"my-link\"><iframe class=\"framey\"><br class=\"clear\">", writer.toString());
    }

    @Test
    public void testNoOp() throws SAXException {
        testSerializer.dispose();
        testSerializer.endPrefixMapping("s");
        testSerializer.ignorableWhitespace(new char[] { 'd', 'u', 'd', 'e' }, 0, 4);
        testSerializer.processingInstruction("do", "the dew");
        testSerializer.setDocumentLocator(null);
        testSerializer.skippedEntity("skippy");
        testSerializer.startPrefixMapping("s", "1001");
        assertEquals("", writer.toString());
    }
}
