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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.TransformerFactory;
import org.apache.sling.rewriter.impl.FactoryCache.TransformerFactoryEntry;
import org.osgi.framework.ServiceReference;

final class TransformerFactoryCache extends FactoryMap<TransformerFactory> {

    static final TransformerFactoryEntry[] EMPTY_ENTRY_ARRAY = new TransformerFactoryEntry[0];
    static final TransformerFactoryEntry[][] EMPTY_DOUBLE_ENTRY_ARRAY = {EMPTY_ENTRY_ARRAY, EMPTY_ENTRY_ARRAY};
    static final TransformerFactory[] EMPTY_FACTORY_ARRAY = new TransformerFactory[0];
    static final TransformerFactory[][] EMPTY_DOUBLE_FACTORY_ARRAY = {EMPTY_FACTORY_ARRAY, EMPTY_FACTORY_ARRAY};

    private TransformerFactoryEntry[][] cached;

    @Override
    public synchronized void bind(
            final ServiceReference<TransformerFactory> reference, final TransformerFactory factory) {
        super.bind(reference, factory);
        this.cached = null;
    }

    @Override
    public synchronized void unbind(final ServiceReference<TransformerFactory> reference) {
        super.unbind(reference);
        this.cached = null;
    }

    /**
     * Get global factories in ascending service ranking order, split into pre and post transformers.
     */
    public synchronized TransformerFactoryEntry[][] getGlobalTransformerFactoryEntries() {
        if (this.cached == null) {
            final List<ServiceReference<TransformerFactory>> references = new ArrayList<>(this.services.keySet());
            Collections.sort(references);
            final List<TransformerFactoryEntry> pre = new ArrayList<>();
            final List<TransformerFactoryEntry> post = new ArrayList<>();
            for (final ServiceReference<TransformerFactory> reference : references) {
                if (FactoryCache.MODE_GLOBAL.equalsIgnoreCase(
                        (String) reference.getProperty(FactoryCache.PROPERTY_MODE))) {
                    final TransformerFactoryEntry entry =
                            new TransformerFactoryEntry(this.services.get(reference), reference);
                    if (entry.order < 0) {
                        pre.add(entry);
                    } else {
                        post.add(entry);
                    }
                }
            }
            if (pre.isEmpty() && post.isEmpty()) {
                this.cached = EMPTY_DOUBLE_ENTRY_ARRAY;
            } else {
                this.cached =
                        new TransformerFactoryEntry[][] {pre.toArray(EMPTY_ENTRY_ARRAY), post.toArray(EMPTY_ENTRY_ARRAY)
                        };
            }
        }
        return this.cached;
    }

    /**
     * Get global factories that apply to the current request.
     */
    public TransformerFactory[][] getGlobalTransformerFactories(final ProcessingContext context) {
        final TransformerFactoryEntry[][] entries = this.getGlobalTransformerFactoryEntries();
        if (entries == EMPTY_DOUBLE_ENTRY_ARRAY) {
            return EMPTY_DOUBLE_FACTORY_ARRAY;
        }
        final TransformerFactory[][] factories = new TransformerFactory[2][];
        for (int i = 0; i < 2; i++) {
            factories[i] = new TransformerFactory[entries[i].length];
            for (int j = 0; j < entries[i].length; j++) {
                if (entries[i][j].match(context)) {
                    factories[i][j] = entries[i][j].factory;
                }
            }
        }
        return factories;
    }
}
