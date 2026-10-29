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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceReference;

/**
 * Stores the highest-ranked factory for each pipeline type.
 */
class FactoryMap<T> {

    protected final Map<ServiceReference<T>, T> services = new HashMap<>();

    private volatile Map<String, T> factories = Collections.emptyMap();

    public T getFactory(final String type) {
        return this.factories.get(type);
    }

    public synchronized void bind(final ServiceReference<T> reference, final T factory) {
        this.services.put(reference, factory);
        this.updateFactories();
    }

    public synchronized void unbind(final ServiceReference<T> reference) {
        this.services.remove(reference);
        this.updateFactories();
    }

    private void updateFactories() {
        final List<ServiceReference<T>> references = new ArrayList<>(this.services.keySet());
        references.sort(Collections.reverseOrder());
        final Map<String, T> updated = new HashMap<>();
        for (final ServiceReference<T> reference : references) {
            final String type = (String) reference.getProperty(FactoryCache.PROPERTY_TYPE);
            if (type != null) {
                updated.putIfAbsent(type, this.services.get(reference));
            }
        }
        this.factories = updated;
    }
}
