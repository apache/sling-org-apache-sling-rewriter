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

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.rewriter.ProcessingComponentConfiguration;

/**
 * Configuration of a processing component.
 */
public class ProcessingComponentConfigurationImpl implements ProcessingComponentConfiguration {

    /** Empty configuration map. */
    public static final ValueMap EMPTY_CONFIG = new ValueMapDecorator(new HashMap<String, Object>());

    /** Empty configuration. */
    public static final ProcessingComponentConfiguration EMPTY = new ProcessingComponentConfigurationImpl("<empty>", null);

    /** The type of the component. */
    private final String type;

    /** The configuration map. */
    private final ValueMap configuration;

    /**
     * Create a new configuration.
     * @param type The type of the component.
     * @param config The configuration or null if no config is available.
     */
    public ProcessingComponentConfigurationImpl(final String type, final ValueMap config) {
        this.type = type;
        this.configuration = (config == null ? EMPTY_CONFIG : new ValueMapDecorator(new HashMap<>(config)));
        this.configuration.remove("jcr:primaryType");
    }

    /**
     * @see org.apache.sling.rewriter.ProcessingComponentConfiguration#getConfiguration()
     */
    public ValueMap getConfiguration() {
        return this.configuration;
    }

    /**
     * @see org.apache.sling.rewriter.ProcessingComponentConfiguration#getType()
     */
    public String getType() {
        return this.type;
    }

    private String getConfigurationString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        for(final Map.Entry<String, Object> entry : this.configuration.entrySet()) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(entry.getKey());
            sb.append('=');
            if (entry.getValue().getClass().isArray()) {
                sb.append(Arrays.toString((Object[])entry.getValue()));
            } else {
                sb.append(entry.getValue());
            }
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Config(type=".concat(this.type).concat(", config=").concat(this.getConfigurationString()).concat(")");
    }

    void printConfiguration(final PrintWriter pw) {
        pw.print(this.type);
        if (!this.configuration.isEmpty()) {
            pw.print(" : ");
            pw.print(this.getConfigurationString());    
        }
        pw.println();
    }
}
