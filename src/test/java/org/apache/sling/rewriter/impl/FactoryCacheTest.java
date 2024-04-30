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
package org.apache.sling.rewriter.impl;

import static org.junit.Assert.assertEquals;

import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

public class FactoryCacheTest {

    @Test
    public void testCreateTransformers() throws InvalidSyntaxException {
        final BundleContext bc = Mockito.mock(BundleContext.class);
        final FactoryCache cache = new FactoryCache(bc);

        final TransformerFactory tf1 = Mockito.mock(TransformerFactory.class);
        final Transformer t1 = Mockito.mock(Transformer.class);
        Mockito.when(tf1.createTransformer()).thenReturn(t1);

        // empty double array
        Transformer[][] result = cache.createTransformers(TransformerFactoryServiceTracker.EMPTY_DOUBLE_FACTORY_ARRAY);
        assertEquals(2, result.length);
        assertEquals(0, result[0].length);
        assertEquals(0, result[1].length);

        // first empty second t1
        final TransformerFactory[][] factories = new TransformerFactory[][] { TransformerFactoryServiceTracker.EMPTY_FACTORY_ARRAY, new TransformerFactory[] { tf1 } };
        result = cache.createTransformers(factories);
        assertEquals(2, result.length);
        assertEquals(0, result[0].length);
        assertEquals(1, result[1].length);
        assertEquals(t1, result[1][0]);

        // first t1 second empty
        final TransformerFactory[][] factories2 = new TransformerFactory[][] { new TransformerFactory[] { tf1 }, TransformerFactoryServiceTracker.EMPTY_FACTORY_ARRAY };
        result = cache.createTransformers(factories2);
        assertEquals(2, result.length);
        assertEquals(1, result[0].length);
        assertEquals(0, result[1].length);
        assertEquals(t1, result[0][0]);

        // first t1 second t1
        final TransformerFactory[][] factories3 = new TransformerFactory[][] { new TransformerFactory[] { tf1 }, new TransformerFactory[] { tf1 } };
        result = cache.createTransformers(factories3);
        assertEquals(2, result.length);
        assertEquals(1, result[0].length);
        assertEquals(1, result[1].length);
        assertEquals(t1, result[0][0]);
        assertEquals(t1, result[1][0]);

        // create second transformer factory
        final TransformerFactory tf2 = Mockito.mock(TransformerFactory.class);
        final Transformer t2 = Mockito.mock(Transformer.class);
        Mockito.when(tf2.createTransformer()).thenReturn(t2);

        // first t1 and t2, second t2 and t1
        final TransformerFactory[][] factories4 = new TransformerFactory[][] { new TransformerFactory[] { tf1, tf2 }, new TransformerFactory[] { tf2, tf1 } };
        result = cache.createTransformers(factories4);
        assertEquals(2, result.length);
        assertEquals(2, result[0].length);
        assertEquals(2, result[1].length);
        assertEquals(t1, result[0][0]);
        assertEquals(t2, result[0][1]);
        assertEquals(t2, result[1][0]);
        assertEquals(t1, result[1][1]);

        // first t1 and null, second null and t1
        final TransformerFactory[][] factories5 = new TransformerFactory[][] { new TransformerFactory[] { tf1, null }, new TransformerFactory[] { null, tf1 } };
        result = cache.createTransformers(factories5);
        assertEquals(2, result.length);
        assertEquals(1, result[0].length);
        assertEquals(1, result[1].length);
        assertEquals(t1, result[0][0]);
        assertEquals(t1, result[1][0]);
    }
}
