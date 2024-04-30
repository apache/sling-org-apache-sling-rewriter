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
package org.apache.sling.rewriter.impl;

import java.util.Arrays;

import org.apache.sling.rewriter.ProcessingContext;
import org.apache.sling.rewriter.TransformerFactory;
import org.apache.sling.rewriter.impl.FactoryCache.TransformerFactoryEntry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TransformerFactoryServiceTracker extends HashingServiceTrackerCustomizer<TransformerFactory> {

    private final Logger LOGGER = LoggerFactory.getLogger(TransformerFactoryServiceTracker.class);

    private String getMode(final ServiceReference<TransformerFactory> ref) {
        final String mode = (String) ref.getProperty(FactoryCache.PROPERTY_MODE);
        return mode;
    }

    private boolean isGlobal(final ServiceReference<TransformerFactory> ref) {
        return FactoryCache.MODE_GLOBAL.equalsIgnoreCase(this.getMode(ref));
    }

    public static final TransformerFactoryEntry[] EMPTY_ENTRY_ARRAY = new TransformerFactoryEntry[0];
    public static final TransformerFactoryEntry[][] EMPTY_DOUBLE_ENTRY_ARRAY = new TransformerFactoryEntry[][] {EMPTY_ENTRY_ARRAY, EMPTY_ENTRY_ARRAY};

    public static final TransformerFactory[] EMPTY_FACTORY_ARRAY = new TransformerFactory[0];
    public static final TransformerFactory[][] EMPTY_DOUBLE_FACTORY_ARRAY = new TransformerFactory[][] {EMPTY_FACTORY_ARRAY, EMPTY_FACTORY_ARRAY};

    private TransformerFactoryEntry[][] cached = EMPTY_DOUBLE_ENTRY_ARRAY;

    /** flag for tracking cache updates. */
    private volatile int currentTrackingCount;

    public TransformerFactoryServiceTracker(final BundleContext bc, final String serviceClassName) {
        super(bc, serviceClassName);
        this.currentTrackingCount = super.getTrackingCount();
    }

    /**
     * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
     */
    @Override
    public TransformerFactory addingService(ServiceReference<TransformerFactory> reference) {
        final boolean isGlobal = isGlobal(reference);
        LOGGER.debug("Adding service {}, isGlobal={}", reference.getClass(), isGlobal);
        TransformerFactory obj = super.addingService(reference);
        if ( isGlobal && getType(reference) == null ) {
            obj = this.context.getService(reference);
        }
        return obj;
    }

    /**
     * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    @Override
    public void removedService(ServiceReference<TransformerFactory> reference, TransformerFactory service) {
        final boolean isGlobal = isGlobal(reference);
        LOGGER.debug("Removing service {}, isGlobal={}", reference.getClass(), isGlobal);
        super.removedService(reference, service);
        if ( isGlobal && getType(reference) == null ) {
            this.context.ungetService(reference);
        }
    }

    /**
     * Get all global transformer factories.
     * @return Two arrays of transformer factories
     */
    public TransformerFactoryEntry[][] getGlobalTransformerFactoryEntries() {
        if (this.currentTrackingCount != this.getTrackingCount()) {
            synchronized ( this ) {
                if (this.currentTrackingCount != this.getTrackingCount()) {
                    final ServiceReference<TransformerFactory>[] refs = this.getServiceReferences();
                    LOGGER.debug("Found {} service references", refs.length);
                    if ( refs == null || refs.length == 0 ) {
                        this.cached = EMPTY_DOUBLE_ENTRY_ARRAY;
                    } else {
                        Arrays.sort(refs);

                        int preCount = 0;
                        int postCount = 0;
                        for(final ServiceReference<TransformerFactory> ref : refs) {
                            if ( isGlobal(ref) ) {
                                final Object r = ref.getProperty(Constants.SERVICE_RANKING);
                                int ranking = (r instanceof Integer ? (Integer)r : 0);
                                if ( ranking < 0 ) {
                                    preCount++;
                                } else {
                                    postCount++;
                                }
                            }
                        }
                        final TransformerFactoryEntry[][] globalFactories = new TransformerFactoryEntry[2][];
                        if ( preCount == 0 ) {
                            globalFactories[0] = EMPTY_ENTRY_ARRAY;
                        } else {
                            globalFactories[0] = new TransformerFactoryEntry[preCount];
                        }
                        if ( postCount == 0) {
                            globalFactories[1] = EMPTY_ENTRY_ARRAY;
                        } else {
                            globalFactories[1] = new TransformerFactoryEntry[postCount];
                        }
                        int index = 0;
                        for(final ServiceReference<TransformerFactory> ref : refs) {
                            if ( isGlobal(ref) ) {
                                LOGGER.debug("Initializing {} global TransformerFactory for service ref: {}", index < preCount ? "pre" : "post", ref.getClass());
                                final TransformerFactory factory = this.getService(ref);
                                if ( factory == null) {
                                    LOGGER.debug("TransformerFactory is null for service ref: {}", ref);
                                } else {
                                    if ( index < preCount ) {
                                        globalFactories[0][index] = new TransformerFactoryEntry(factory, ref);
                                    } else {
                                        globalFactories[1][index - preCount] = new TransformerFactoryEntry(factory, ref);
                                    }
                                }
                                index++;
                            }
                        }
                        if (globalFactories[0].length > 1) {
                            Arrays.sort(globalFactories[0]);
                        }
                        if (globalFactories[1].length > 1) {
                            Arrays.sort(globalFactories[1]);
                        }
                        this.cached = globalFactories;
                    }
                    this.currentTrackingCount = this.getTrackingCount();
                }
            }
        }

        return this.cached;
    }

    /**
     * Get all global transformer factories that apply to the current request.
     * @param context The current processing context.
     * @return Two arrays containing the transformer factories.
     */
    public TransformerFactory[][] getGlobalTransformerFactories(final ProcessingContext context) {
        final TransformerFactoryEntry[][] globalFactoryEntries = this.getGlobalTransformerFactoryEntries();
        // quick check
        if ( globalFactoryEntries == EMPTY_DOUBLE_ENTRY_ARRAY ) {
            LOGGER.debug("No TransformerFactory found");
            return EMPTY_DOUBLE_FACTORY_ARRAY;
        }
        final TransformerFactory[][] factories = new TransformerFactory[2][];
        for(int i=0; i<2; i++) {
            if ( globalFactoryEntries[i] == EMPTY_ENTRY_ARRAY ) {
                LOGGER.debug("No {} TransformerFactory found for context {}", i == 0 ? "pre" : "post", context);
                factories[i] = EMPTY_FACTORY_ARRAY;
            } else {
                factories[i] = new TransformerFactory[globalFactoryEntries[i].length];
                for(int m=0; m<globalFactoryEntries[i].length; m++) {
                    final TransformerFactoryEntry entry = globalFactoryEntries[i][m];
                    if ( entry.match(context) ) {
                        factories[i][m] = entry.factory;
                    }
                }
                LOGGER.debug("Found {} Transformer factories {} for context {}", i == 0 ? "pre" : "post", factories[i], context);
            }
        }
        return factories;
    }
}