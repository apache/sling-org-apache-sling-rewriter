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
package org.apache.sling.rewriter;

import java.io.IOException;

import org.xml.sax.ContentHandler;

/**
 * The <code>Serializer</code> marks the end of the pipeline.
 */
public interface Serializer extends ContentHandler {

    /**
     * Initialize this component.
     * @param context The invocation context.
     * @param config The configuration for this component.
     * @throws IOException in case of problems
     */
    void init(ProcessingContext context, ProcessingComponentConfiguration config)
    throws IOException;

    /**
     * Dispose the serializer.
     * This method is always invoked by the rewriter in order to
     * allow the serializer to release any resources etc. After
     * this method has been called the instance is considered
     * unusable.
     */
    void dispose();
}
