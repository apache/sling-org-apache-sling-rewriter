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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.sling.rewriter.Generator;
import org.apache.sling.rewriter.GeneratorFactory;
import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.Processor;
import org.apache.sling.rewriter.ProcessorFactory;
import org.apache.sling.rewriter.Serializer;
import org.apache.sling.rewriter.SerializerFactory;
import org.apache.sling.rewriter.Transformer;
import org.apache.sling.rewriter.TransformerFactory;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class FactoryCacheTest {

    @Rule
    public final SlingContext context = new SlingContext();

    @Test
    public void testInitialFactoryReferencesAndRemoval() {
        final GeneratorFactory generatorFactory = Mockito.mock(GeneratorFactory.class);
        final Generator generator = Mockito.mock(Generator.class);
        Mockito.when(generatorFactory.createGenerator()).thenReturn(generator);
        final SerializerFactory serializerFactory = Mockito.mock(SerializerFactory.class);
        final Serializer serializer = Mockito.mock(Serializer.class);
        Mockito.when(serializerFactory.createSerializer()).thenReturn(serializer);
        final TransformerFactory transformerFactory = Mockito.mock(TransformerFactory.class);
        final Transformer transformer = Mockito.mock(Transformer.class);
        Mockito.when(transformerFactory.createTransformer()).thenReturn(transformer);
        final ProcessorFactory processorFactory = Mockito.mock(ProcessorFactory.class);
        final Processor processor = Mockito.mock(Processor.class);
        Mockito.when(processorFactory.createProcessor()).thenReturn(processor);

        final ServiceRegistration<GeneratorFactory> generatorRegistration = context.bundleContext()
                .registerService(GeneratorFactory.class, generatorFactory, factoryProperties("test", 0));
        final ServiceRegistration<SerializerFactory> serializerRegistration = context.bundleContext()
                .registerService(SerializerFactory.class, serializerFactory, factoryProperties("test", 0));
        final ServiceRegistration<TransformerFactory> transformerRegistration = context.bundleContext()
                .registerService(TransformerFactory.class, transformerFactory, factoryProperties("test", 0));
        final ServiceRegistration<ProcessorFactory> processorRegistration = context.bundleContext()
                .registerService(ProcessorFactory.class, processorFactory, factoryProperties("test", 0));
        final FactoryCache cache = context.registerInjectActivateService(new FactoryCache());

        assertSame(generator, cache.getGenerator("test"));
        assertSame(serializer, cache.getSerializer("test"));
        assertSame(transformer, cache.getTransformer("test"));
        assertSame(processor, cache.getProcessor("test"));

        generatorRegistration.unregister();
        serializerRegistration.unregister();
        transformerRegistration.unregister();
        processorRegistration.unregister();

        assertNull(cache.getGenerator("test"));
        assertNull(cache.getSerializer("test"));
        assertNull(cache.getTransformer("test"));
        assertNull(cache.getProcessor("test"));
    }

    @Test
    public void testDynamicFactoryRankingAndRemoval() {
        final FactoryCache cache = context.registerInjectActivateService(new FactoryCache());
        assertNull(cache.getGenerator("test"));
        final GeneratorFactory firstFactory = Mockito.mock(GeneratorFactory.class);
        final Generator first = Mockito.mock(Generator.class);
        Mockito.when(firstFactory.createGenerator()).thenReturn(first);
        final GeneratorFactory secondFactory = Mockito.mock(GeneratorFactory.class);
        final Generator second = Mockito.mock(Generator.class);
        Mockito.when(secondFactory.createGenerator()).thenReturn(second);
        final ServiceRegistration<GeneratorFactory> firstRegistration = context.bundleContext()
                .registerService(GeneratorFactory.class, firstFactory, factoryProperties("test", 10));
        final ServiceRegistration<GeneratorFactory> secondRegistration = context.bundleContext()
                .registerService(GeneratorFactory.class, secondFactory, factoryProperties("test", 10));

        // Equal rankings prefer the service registered first.
        assertSame(first, cache.getGenerator("test"));
        final ServiceRegistration<GeneratorFactory> higherRegistration = context.bundleContext()
                .registerService(GeneratorFactory.class, secondFactory, factoryProperties("test", 20));
        assertSame(second, cache.getGenerator("test"));
        higherRegistration.unregister();
        assertSame(first, cache.getGenerator("test"));
        firstRegistration.unregister();
        assertSame(second, cache.getGenerator("test"));
        secondRegistration.unregister();
        assertNull(cache.getGenerator("test"));
    }

    @Test
    public void testGlobalTransformerReferencesAndUpdates() {
        final FactoryCache cache = context.registerInjectActivateService(new FactoryCache());
        assertEquals(0, cache.getGlobalTransformerFactoryEntries()[0].length);
        assertEquals(0, cache.getGlobalTransformerFactoryEntries()[1].length);

        final TransformerFactory firstFactory = Mockito.mock(TransformerFactory.class);
        final Transformer first = Mockito.mock(Transformer.class);
        Mockito.when(firstFactory.createTransformer()).thenReturn(first);
        final TransformerFactory secondFactory = Mockito.mock(TransformerFactory.class);
        final Transformer second = Mockito.mock(Transformer.class);
        Mockito.when(secondFactory.createTransformer()).thenReturn(second);
        final Dictionary<String, Object> firstProperties = factoryProperties("test", -10);
        firstProperties.put(FactoryCache.PROPERTY_MODE, "global");
        final ServiceRegistration<TransformerFactory> firstRegistration = context.bundleContext()
                .registerService(TransformerFactory.class, firstFactory, firstProperties);
        final Dictionary<String, Object> secondProperties = factoryProperties(null, -20);
        secondProperties.put(FactoryCache.PROPERTY_MODE, "GLOBAL");
        final ServiceRegistration<TransformerFactory> secondRegistration = context.bundleContext()
                .registerService(TransformerFactory.class, secondFactory, secondProperties);

        assertSame(first, cache.getTransformer("test"));
        Transformer[][] result = cache.getGlobalTransformers(null);
        assertEquals(2, result[0].length);
        assertSame(second, result[0][0]);
        assertSame(first, result[0][1]);
        assertEquals(0, result[1].length);

        firstProperties.put(Constants.SERVICE_RANKING, 0);
        firstRegistration.setProperties(firstProperties);
        // This mock OSGi version does not dispatch DS updated callbacks.
        cache.bindTransformerFactory(firstRegistration.getReference(), firstFactory);
        result = cache.getGlobalTransformers(null);
        assertEquals(1, result[0].length);
        assertSame(second, result[0][0]);
        assertEquals(1, result[1].length);
        assertSame(first, result[1][0]);

        firstProperties.put("pipeline.contentTypes", "text/plain");
        firstRegistration.setProperties(firstProperties);
        cache.bindTransformerFactory(firstRegistration.getReference(), firstFactory);
        final ProcessingContext processingContext = Mockito.mock(ProcessingContext.class);
        Mockito.when(processingContext.getRequest()).thenReturn(context.request());
        Mockito.when(processingContext.getContentType()).thenReturn("text/html");
        result = cache.getGlobalTransformers(processingContext);
        assertEquals(0, result[1].length);
        Mockito.when(processingContext.getContentType()).thenReturn("text/plain");
        assertEquals(1, cache.getGlobalTransformers(processingContext)[1].length);

        firstProperties.remove(FactoryCache.PROPERTY_MODE);
        firstRegistration.setProperties(firstProperties);
        cache.bindTransformerFactory(firstRegistration.getReference(), firstFactory);
        result = cache.getGlobalTransformers(processingContext);
        assertEquals(0, result[1].length);
        assertSame(second, result[0][0]);
        assertSame(first, cache.getTransformer("test"));

        secondRegistration.unregister();
        firstRegistration.unregister();
        result = cache.getGlobalTransformers(processingContext);
        assertEquals(0, result[0].length);
        assertEquals(0, result[1].length);
    }

    private Dictionary<String, Object> factoryProperties(final String type, final int ranking) {
        final Dictionary<String, Object> properties = new Hashtable<>();
        if (type != null) {
            properties.put(FactoryCache.PROPERTY_TYPE, type);
        }
        properties.put(Constants.SERVICE_RANKING, ranking);
        return properties;
    }

    @Test
    public void testInitialGlobalPostTransformerOrdering() {
        final TransformerFactory higher = Mockito.mock(TransformerFactory.class);
        final Dictionary<String, Object> higherProperties = factoryProperties(null, 10);
        higherProperties.put(FactoryCache.PROPERTY_MODE, "global");
        context.bundleContext().registerService(TransformerFactory.class, higher, higherProperties);
        final TransformerFactory lower = Mockito.mock(TransformerFactory.class);
        final Dictionary<String, Object> lowerProperties = factoryProperties(null, 0);
        lowerProperties.put(FactoryCache.PROPERTY_MODE, "global");
        context.bundleContext().registerService(TransformerFactory.class, lower, lowerProperties);

        final FactoryCache cache = context.registerInjectActivateService(new FactoryCache());
        final FactoryCache.TransformerFactoryEntry[][] entries = cache.getGlobalTransformerFactoryEntries();
        assertEquals(0, entries[0].length);
        assertEquals(2, entries[1].length);
        assertSame(lower, entries[1][0].factory);
        assertSame(higher, entries[1][1].factory);
    }

    @Test
    public void testCreateTransformers() {
        final FactoryCache cache = new FactoryCache();

        final TransformerFactory tf1 = Mockito.mock(TransformerFactory.class);
        final Transformer t1 = Mockito.mock(Transformer.class);
        Mockito.when(tf1.createTransformer()).thenReturn(t1);

        // empty double array
        Transformer[][] result = cache.createTransformers(TransformerFactoryCache.EMPTY_DOUBLE_FACTORY_ARRAY);
        assertEquals(2, result.length);
        assertEquals(0, result[0].length);
        assertEquals(0, result[1].length);

        // first empty second t1
        final TransformerFactory[][] factories = new TransformerFactory[][] { TransformerFactoryCache.EMPTY_FACTORY_ARRAY, new TransformerFactory[] { tf1 } };
        result = cache.createTransformers(factories);
        assertEquals(2, result.length);
        assertEquals(0, result[0].length);
        assertEquals(1, result[1].length);
        assertEquals(t1, result[1][0]);

        // first t1 second empty
        final TransformerFactory[][] factories2 = new TransformerFactory[][] { new TransformerFactory[] { tf1 }, TransformerFactoryCache.EMPTY_FACTORY_ARRAY };
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
