/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.rewriter.impl;

import org.junit.Test;
import org.osgi.framework.ServiceReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FactoryMapTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testAddRemove() {
        final FactoryMap<Object> factories = new FactoryMap<>();

        assertNull(factories.getFactory("a"));
        final Object service1 = new Object();
        final ServiceReference<Object> ref1 = mock(ServiceReference.class);
        when(ref1.getProperty(FactoryCache.PROPERTY_TYPE)).thenReturn("a");
        factories.bind(ref1, service1);

        assertNotNull(factories.getFactory("a"));
        assertEquals(service1, factories.getFactory("a"));

        factories.unbind(ref1);
        assertNull(factories.getFactory("a"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMultipleAddRemove() {
        final FactoryMap<Object> factories = new FactoryMap<>();

        assertNull(factories.getFactory("a"));
        final Object service1 = new Object();
        final Object service2 = new Object();
        final Object service3 = new Object();

        final ServiceReference<Object> ref1 = mock(ServiceReference.class);
        when(ref1.getProperty(FactoryCache.PROPERTY_TYPE)).thenReturn("a");

        final ServiceReference<Object> ref2 = mock(ServiceReference.class);
        when(ref2.getProperty(FactoryCache.PROPERTY_TYPE)).thenReturn("a");

        final ServiceReference<Object> ref3 = mock(ServiceReference.class);
        when(ref3.getProperty(FactoryCache.PROPERTY_TYPE)).thenReturn("a");

        // ordering ref3 has highest ranking, then ref2, then ref1
        when(ref1.compareTo(ref2)).thenReturn(-1);
        when(ref1.compareTo(ref3)).thenReturn(-1);
        when(ref2.compareTo(ref1)).thenReturn(1);
        when(ref2.compareTo(ref3)).thenReturn(-1);
        when(ref3.compareTo(ref1)).thenReturn(1);
        when(ref3.compareTo(ref2)).thenReturn(1);

        factories.bind(ref1, service1);

        assertNotNull(factories.getFactory("a"));
        assertEquals(service1, factories.getFactory("a"));

        factories.bind(ref2, service2);
        assertEquals(service2, factories.getFactory("a"));

        factories.bind(ref3, service3);
        assertEquals(service3, factories.getFactory("a"));

        factories.unbind(ref1);
        assertEquals(service3, factories.getFactory("a"));

        factories.unbind(ref3);
        assertEquals(service2, factories.getFactory("a"));

        factories.unbind(ref2);
        assertNull(factories.getFactory("a"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdatedProperties() {
        final FactoryMap<Object> factories = new FactoryMap<>();
        final ServiceReference<Object> firstReference = mock(ServiceReference.class);
        final ServiceReference<Object> secondReference = mock(ServiceReference.class);
        final Object first = new Object();
        final Object second = new Object();
        when(firstReference.getProperty(FactoryCache.PROPERTY_TYPE)).thenReturn("a");
        when(secondReference.getProperty(FactoryCache.PROPERTY_TYPE)).thenReturn("a");
        when(firstReference.compareTo(secondReference)).thenReturn(1);
        when(secondReference.compareTo(firstReference)).thenReturn(-1);
        factories.bind(firstReference, first);
        factories.bind(secondReference, second);
        assertEquals(first, factories.getFactory("a"));

        when(firstReference.compareTo(secondReference)).thenReturn(-1);
        when(secondReference.compareTo(firstReference)).thenReturn(1);
        factories.bind(secondReference, second);
        assertEquals(second, factories.getFactory("a"));

        when(secondReference.getProperty(FactoryCache.PROPERTY_TYPE)).thenReturn("b");
        factories.bind(secondReference, second);
        assertEquals(first, factories.getFactory("a"));
        assertEquals(second, factories.getFactory("b"));

        when(secondReference.getProperty(FactoryCache.PROPERTY_TYPE)).thenReturn(null);
        factories.bind(secondReference, second);
        assertNull(factories.getFactory("b"));
        when(secondReference.getProperty(FactoryCache.PROPERTY_TYPE)).thenReturn("a");
        factories.bind(secondReference, second);
        assertEquals(second, factories.getFactory("a"));

        factories.unbind(secondReference);
        assertEquals(first, factories.getFactory("a"));
    }
}
