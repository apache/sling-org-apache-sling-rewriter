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
import static org.mockito.Mockito.spy;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.apache.sling.testing.resourceresolver.MockResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableMap;

public class ProcessorManagerImplTest {

	private Resource testRoot;
	private ResourceResolver resourceResolver;
	private ProcessorManagerImpl processorManager;

	@Before
	public void setUp() throws LoginException, PersistenceException {
		resourceResolver = (MockResourceResolver) new MockResourceResolverFactory().getResourceResolver(null);
		Resource root = resourceResolver.getResource("/");
		testRoot = resourceResolver.create(root, "test", ValueMap.EMPTY);
		createConfigs(resourceResolver);
		processorManager = spy(new ProcessorManagerImpl());
		processorManager.searchPath = new String[] { "/test" };

		Mockito.doReturn(resourceResolver).when(processorManager).createResourceResolver();
	}

	@Test
	public void testAddProcessor() throws LoginException {
		processorManager.addProcessor("rewriter-html", createConfigPath("/test/1"),
				new ProcessorConfigurationImpl(resourceResolver.getResource(createConfigPath("/test/1"))));
		processorManager.addProcessor("rewriter-html", createConfigPath("/test/2"),
				new ProcessorConfigurationImpl(resourceResolver.getResource(createConfigPath("/test/2"))));

		// reverse order
		assertEquals(2, ((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(0)).getOrder());
		assertEquals(1, ((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(1)).getOrder());
	}

	@Test
	public void testRemoveProcessor() throws LoginException {
		processorManager.addProcessor("rewriter-html", createConfigPath("/test/1"),
				new ProcessorConfigurationImpl(resourceResolver.getResource(createConfigPath("/test/1"))));
		processorManager.addProcessor("rewriter-html", createConfigPath("/test/2"),
				new ProcessorConfigurationImpl(resourceResolver.getResource(createConfigPath("/test/2"))));
		processorManager.addProcessor("rewriter-html", createConfigPath("/test/3"),
				new ProcessorConfigurationImpl(resourceResolver.getResource(createConfigPath("/test/3"))));

		processorManager.removeProcessor(createConfigPath("/test/2"));
		// reverse order
		assertEquals(2, processorManager.getProcessorConfigurations().size());
		assertEquals(3, ((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(0)).getOrder());
		assertEquals(1, ((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(1)).getOrder());
	}

	@Test
	public void testUpdateProcessor() throws LoginException, PersistenceException {
		processorManager.addProcessor("rewriter-html", createConfigPath("/test/1"),
				new ProcessorConfigurationImpl(resourceResolver.getResource(createConfigPath("/test/1"))));
		processorManager.addProcessor("rewriter-html", createConfigPath("/test/2"),
				new ProcessorConfigurationImpl(resourceResolver.getResource(createConfigPath("/test/2"))));
		processorManager.addProcessor("rewriter-html", createConfigPath("/test/3"),
				new ProcessorConfigurationImpl(resourceResolver.getResource(createConfigPath("/test/3"))));

		resourceResolver.delete(resourceResolver.getResource("/test/2"));
		resourceResolver.commit();

		createConfig(resourceResolver, "2", 10);
		processorManager.updateProcessor(createConfigPath("/test/2"));
		// reverse order
		assertEquals(3, processorManager.getProcessorConfigurations().size());
		assertEquals(10,
				((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(0)).getOrder());
		assertEquals(3, ((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(1)).getOrder());
		assertEquals(1, ((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(2)).getOrder());
	}

	@Test
	public void testUpdateWithNewProcessor() throws LoginException, PersistenceException {
		processorManager.addProcessor("rewriter-html", createConfigPath("/test/1"),
				new ProcessorConfigurationImpl(resourceResolver.getResource(createConfigPath("/test/1"))));
		processorManager.addProcessor("rewriter-html", createConfigPath("/test/2"),
				new ProcessorConfigurationImpl(resourceResolver.getResource(createConfigPath("/test/2"))));
		processorManager.addProcessor("rewriter-html", createConfigPath("/test/3"),
				new ProcessorConfigurationImpl(resourceResolver.getResource(createConfigPath("/test/3"))));

		createConfig(resourceResolver, "4", 10);
		processorManager.updateProcessor(createConfigPath("/test/4"));

		// reverse order
		assertEquals(4, processorManager.getProcessorConfigurations().size());
		assertEquals(10,
				((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(0)).getOrder());
		assertEquals(3, ((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(1)).getOrder());
		assertEquals(2, ((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(2)).getOrder());
		assertEquals(1, ((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(3)).getOrder());
	}

	void createConfigs(ResourceResolver resolver) throws PersistenceException {
		for (int i = 1; i < 4; i++) {
			createConfig(resolver, Integer.toString(i), i);
		}
	}

	void createConfig(ResourceResolver resolver, String rt, int order) throws PersistenceException {
		Resource root = resourceResolver.create(testRoot, rt, ValueMap.EMPTY);
		Resource configRoot = resourceResolver.create(root, "config", ValueMap.EMPTY);
		Resource rewriterRoot = resourceResolver.create(configRoot, "rewriter", ValueMap.EMPTY);

		resolver.create(rewriterRoot, "rewriter-html",
				ImmutableMap.<String, Object>builder().put("contentTypes", new String[] { "text/html" })
						.put("resourceTypes", new String[] { rt }).put("enabled", true).put("order", order)
						.put("generatorType", order).put("serializerType", "htmlwriter").build());
	}

	String createConfigPath(String root) {
		return root + "/config/rewriter/rewriter-html";
	}
}
