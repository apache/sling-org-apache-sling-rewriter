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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.apache.sling.testing.resourceresolver.MockResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

import com.google.common.collect.ImmutableMap;

public class ProcessorManagerImplTest {

	private Resource testRoot;
	private ResourceResolver resourceResolver;
	private ProcessorManagerImpl processorManager;

	@Before
	public void setUp() throws LoginException, PersistenceException {
		resourceResolver = (MockResourceResolver) new MockResourceResolverFactory().getResourceResolver(null);
		Resource root = resourceResolver.getResource("/");
		testRoot = resourceResolver.create(root, "apps", ValueMap.EMPTY);
		createConfigs(resourceResolver);
		processorManager = spy(new ProcessorManagerImpl());

		Mockito.doReturn(resourceResolver).when(processorManager).createResourceResolver();
	}

	@Test
	public void testInitialize() throws LoginException, InvalidSyntaxException {
		processorManager.activate(mock(BundleContext.class));

		assertEquals(3, processorManager.getProcessorConfigurations().size());
		// reverse order
		assertEquals(3, ((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(0)).getOrder());
		assertTrue(
				processorManager.getProcessorConfigurations().get(0).toString().contains(createConfigPath("/apps/3")));
		assertEquals(2, ((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(1)).getOrder());
		assertTrue(
				processorManager.getProcessorConfigurations().get(1).toString().contains(createConfigPath("/apps/2")));
		assertEquals(1, ((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(2)).getOrder());
		assertTrue(
				processorManager.getProcessorConfigurations().get(2).toString().contains(createConfigPath("/apps/1")));
	}

	@Test
	public void testRemoveProcessor() throws LoginException, InvalidSyntaxException, InterruptedException {
		processorManager.activate(mock(BundleContext.class));

		ResourceChange resourceChange = mock(ResourceChange.class);
		when(resourceChange.getPath()).thenReturn(createConfigPath("/apps/2"));
		when(resourceChange.getType()).thenReturn(ChangeType.REMOVED);

		processorManager.onChange(Arrays.asList(resourceChange));
		Thread.sleep(1000);
		// reverse order
		assertEquals(2, processorManager.getProcessorConfigurations().size());
		assertEquals(3, ((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(0)).getOrder());
		assertTrue(
				processorManager.getProcessorConfigurations().get(0).toString().contains(createConfigPath("/apps/3")));
		assertEquals(1, ((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(1)).getOrder());
		assertTrue(
				processorManager.getProcessorConfigurations().get(1).toString().contains(createConfigPath("/apps/1")));
	}

	@Test
	public void testUpdateProcessor()
			throws LoginException, PersistenceException, InvalidSyntaxException, InterruptedException {
		processorManager.activate(mock(BundleContext.class));

		resourceResolver.delete(resourceResolver.getResource("/apps/2"));
		resourceResolver.commit();

		createConfig(resourceResolver, "2", 10);

		ResourceChange resourceChange = mock(ResourceChange.class);
		when(resourceChange.getPath()).thenReturn(createConfigPath("/apps/2"));
		when(resourceChange.getType()).thenReturn(ChangeType.CHANGED);

		processorManager.onChange(Arrays.asList(resourceChange));

		Thread.sleep(1000);

		assertEquals(3, processorManager.getProcessorConfigurations().size());

		assertOrderRT((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(0),
				createConfigPath("/apps/2"), 10);
		assertOrderRT((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(1),
				createConfigPath("/apps/3"), 3);
		assertOrderRT((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(2),
				createConfigPath("/apps/1"), 1);

	}

	@Test
	public void testUpdateWithNewProcessor()
			throws LoginException, PersistenceException, InvalidSyntaxException, InterruptedException {
		processorManager.activate(mock(BundleContext.class));

		createConfig(resourceResolver, "4", 10);
		ResourceChange resourceChange = mock(ResourceChange.class);
		when(resourceChange.getPath()).thenReturn(createConfigPath("/apps/4"));
		when(resourceChange.getType()).thenReturn(ChangeType.CHANGED);

		processorManager.onChange(Arrays.asList(resourceChange));

		Thread.sleep(1000);

		// reverse order
		assertEquals(4, processorManager.getProcessorConfigurations().size());

		assertOrderRT((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(0),
				createConfigPath("/apps/4"), 10);
		assertOrderRT((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(1),
				createConfigPath("/apps/3"), 3);
		assertOrderRT((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(2),
				createConfigPath("/apps/2"), 2);
		assertOrderRT((ProcessorConfigurationImpl) processorManager.getProcessorConfigurations().get(3),
				createConfigPath("/apps/1"), 1);

	}

	void createConfigs(ResourceResolver resolver) throws PersistenceException {
		for (int i = 1; i < 4; i++) {
			createConfig(resolver, Integer.toString(i), i);
		}
	}

	void assertOrderRT(ProcessorConfigurationImpl configuration, String rt, int order) {
		assertEquals(order, configuration.getOrder());
		assertTrue(configuration.toString().contains(rt));
	}

	void createConfig(ResourceResolver resolver, String appName, int order) throws PersistenceException {
		Resource root = resourceResolver.create(testRoot, appName, ValueMap.EMPTY);
		Resource configRoot = resourceResolver.create(root, "config", ValueMap.EMPTY);
		Resource rewriterRoot = resourceResolver.create(configRoot, "rewriter", ValueMap.EMPTY);

		resolver.create(rewriterRoot, "rewriter-html",
				ImmutableMap.<String, Object>builder().put("contentTypes", new String[] { "text/html" })
						.put("resourceTypes", new String[] { createConfigPath("/apps/" + appName) })
						.put("enabled", true).put("order", order).put("generatorType", order)
						.put("serializerType", "htmlwriter").build());
	}

	String createConfigPath(String root) {
		return root + "/config/rewriter/rewriter-html";
	}
}
