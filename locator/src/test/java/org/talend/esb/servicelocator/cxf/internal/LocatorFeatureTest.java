/*
 * #%L
 * Service Locator Client for CXF
 * %%
 * Copyright (C) 2011 Talend Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.talend.esb.servicelocator.cxf.internal;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.ClientLifeCycleManager;
import org.apache.cxf.endpoint.ClientLifeCycleManagerImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.talend.esb.servicelocator.cxf.LocatorFeature;

import static org.easymock.EasyMock.expect;

public class LocatorFeatureTest extends EasyMockSupport {

    Bus busMock;
    LocatorRegistrar locatorRegistrarMock;
    Map<String, LocatorSelectionStrategy> locatorSelectionStrategies;
    ClassLoader cll;

    @Before
    public void setUp() {
        busMock = createMock(Bus.class);

        expect(busMock.getExtension(ClassLoader.class)).andStubReturn(cll);

        locatorRegistrarMock = createMock(LocatorRegistrar.class);
        locatorRegistrarMock.startListenForServers(busMock);
        EasyMock.expectLastCall().anyTimes();
        cll = this.getClass().getClassLoader();

        locatorSelectionStrategies = new HashMap<String, LocatorSelectionStrategy>();
        locatorSelectionStrategies.put("defaultSelectionStrategy", new DefaultSelectionStrategy());
        locatorSelectionStrategies.put("randomSelectionStrategy", new RandomSelectionStrategy());
        locatorSelectionStrategies.put("evenDistributionSelectionStrategy",
                new EvenDistributionSelectionStrategy());
    }

    @Test
    public void initializeClient() throws EndpointException {
        LocatorClientEnabler enabler = new LocatorClientEnabler();
        enabler.setBus(busMock);

        enabler.setLocatorSelectionStrategies(locatorSelectionStrategies);
        enabler.setDefaultLocatorSelectionStrategy("evenDistributionSelectionStrategy");

        ServiceLocatorManager slm = new ServiceLocatorManager();

        slm.setBus(busMock);
        slm.setLocatorRegistrar(locatorRegistrarMock);
        slm.setLocatorClientEnabler(enabler);

        expect(busMock.getExtension(ServiceLocatorManager.class)).andStubReturn(slm);

        ClientLifeCycleManager clcm = new ClientLifeCycleManagerImpl();
        expect(busMock.getExtension(ClientLifeCycleManager.class)).andStubReturn(clcm);

        replayAll();

        EndpointInfo ei = new EndpointInfo();
        Service service = new org.apache.cxf.service.ServiceImpl();
        Endpoint endpoint = new EndpointImpl(busMock, service, ei);
        Client client = new ClientImpl(busMock, endpoint);

        LocatorTargetSelector selector = new LocatorTargetSelector();
        selector.setEndpoint(endpoint);

        client.setConduitSelector(selector);

        LocatorFeature lf = new LocatorFeature();
        lf.setSelectionStrategy("randomSelectionStrategy");

        lf.initialize(client, busMock);

        Assert.assertTrue(((LocatorTargetSelector) client.getConduitSelector()).getStrategy()
                instanceof RandomSelectionStrategy);

    }

    @Test
    public void initializeClientsOneWithStrategy() throws EndpointException {
        LocatorClientEnabler enabler = new LocatorClientEnabler();
        enabler.setBus(busMock);

        enabler.setLocatorSelectionStrategies(locatorSelectionStrategies);
        enabler.setDefaultLocatorSelectionStrategy("evenDistributionSelectionStrategy");

        ServiceLocatorManager slm = new ServiceLocatorManager();

        slm.setBus(busMock);
        slm.setLocatorRegistrar(locatorRegistrarMock);
        slm.setLocatorClientEnabler(enabler);

        expect(busMock.getExtension(ServiceLocatorManager.class)).andStubReturn(slm);

        ClientLifeCycleManager clcm = new ClientLifeCycleManagerImpl();
        expect(busMock.getExtension(ClientLifeCycleManager.class)).andStubReturn(clcm);

        replayAll();

        LocatorFeature lf = new LocatorFeature();
        Client client1 = null;
        Client client2 = null;
        {
            EndpointInfo ei = new EndpointInfo();
            Service service = new org.apache.cxf.service.ServiceImpl();
            Endpoint endpoint = new EndpointImpl(busMock, service, ei);
            client1 = new ClientImpl(busMock, endpoint);

            LocatorTargetSelector selector = new LocatorTargetSelector();
            selector.setEndpoint(endpoint);

            client1.setConduitSelector(selector);

            lf.setSelectionStrategy("randomSelectionStrategy");

            lf.initialize(client1, busMock);
        }
        {
            EndpointInfo ei = new EndpointInfo();
            Service service = new org.apache.cxf.service.ServiceImpl();
            Endpoint endpoint = new EndpointImpl(busMock, service, ei);
            client2 = new ClientImpl(busMock, endpoint);

            LocatorTargetSelector selector = new LocatorTargetSelector();
            selector.setEndpoint(endpoint);

            client2.setConduitSelector(selector);

            lf.setSelectionStrategy(null);

            lf.initialize(client2, busMock);
        }
        Assert.assertTrue(((LocatorTargetSelector) client1.getConduitSelector()).getStrategy()
                instanceof RandomSelectionStrategy);
        Assert.assertTrue(((LocatorTargetSelector) client2.getConduitSelector()).getStrategy()
                instanceof EvenDistributionSelectionStrategy);

    }

    @Test
    public void initializeClientDefault() throws EndpointException {

        LocatorClientEnabler enabler = new LocatorClientEnabler();
        enabler.setBus(busMock);

        enabler.setLocatorSelectionStrategies(locatorSelectionStrategies);

        ServiceLocatorManager slm = new ServiceLocatorManager();

        slm.setBus(busMock);
        slm.setLocatorRegistrar(locatorRegistrarMock);
        slm.setLocatorClientEnabler(enabler);

        expect(busMock.getExtension(ServiceLocatorManager.class)).andStubReturn(slm);

        ClientLifeCycleManager clcm = new ClientLifeCycleManagerImpl();
        expect(busMock.getExtension(ClientLifeCycleManager.class)).andStubReturn(clcm);

        replayAll();

        EndpointInfo ei = new EndpointInfo();
        Service service = new org.apache.cxf.service.ServiceImpl();
        Endpoint endpoint = new EndpointImpl(busMock, service, ei);
        Client client = new ClientImpl(busMock, endpoint);

        LocatorTargetSelector selector = new LocatorTargetSelector();
        selector.setEndpoint(endpoint);

        client.setConduitSelector(selector);

        LocatorFeature lf = new LocatorFeature();
        lf.setSelectionStrategy(null);

        lf.initialize(client, busMock);

        Assert.assertTrue(((LocatorTargetSelector) client.getConduitSelector()).getStrategy()
                instanceof DefaultSelectionStrategy);

    }

    @Test
    public void initializeClientsBothWithStrategies() throws EndpointException {

        LocatorClientEnabler enabler = new LocatorClientEnabler();
        enabler.setBus(busMock);

        enabler.setLocatorSelectionStrategies(locatorSelectionStrategies);
        enabler.setDefaultLocatorSelectionStrategy("defaultSelectionStrategy");

        ServiceLocatorManager slm = new ServiceLocatorManager();

        slm.setBus(busMock);
        slm.setLocatorRegistrar(locatorRegistrarMock);
        slm.setLocatorClientEnabler(enabler);

        expect(busMock.getExtension(ServiceLocatorManager.class)).andStubReturn(slm);

        ClientLifeCycleManager clcm = new ClientLifeCycleManagerImpl();
        expect(busMock.getExtension(ClientLifeCycleManager.class)).andStubReturn(clcm);

        replayAll();

        LocatorFeature lf = new LocatorFeature();

        Client client1 = null;
        Client client2 = null;

        {
            EndpointInfo ei = new EndpointInfo();
            Service service = new org.apache.cxf.service.ServiceImpl();
            Endpoint endpoint = new EndpointImpl(busMock, service, ei);
            client1 = new ClientImpl(busMock, endpoint);
            LocatorTargetSelector selector = new LocatorTargetSelector();
            selector.setEndpoint(endpoint);
            client1.setConduitSelector(selector);
            lf.setSelectionStrategy("randomSelectionStrategy");
            lf.initialize(client1, busMock);
        }
        {
            EndpointInfo ei = new EndpointInfo();
            Service service = new org.apache.cxf.service.ServiceImpl();
            Endpoint endpoint = new EndpointImpl(busMock, service, ei);
            client2 = new ClientImpl(busMock, endpoint);
            LocatorTargetSelector selector = new LocatorTargetSelector();
            selector.setEndpoint(endpoint);
            client2.setConduitSelector(selector);
            lf.setSelectionStrategy("evenDistributionSelectionStrategy");
            lf.initialize(client2, busMock);
        }
        Assert.assertTrue(((LocatorTargetSelector) client1.getConduitSelector()).getStrategy()
                instanceof RandomSelectionStrategy);
        Assert.assertTrue(((LocatorTargetSelector) client2.getConduitSelector()).getStrategy()
                instanceof EvenDistributionSelectionStrategy);
    }

    @Test
    public void initializeClientConfiguration() throws EndpointException {
        LocatorClientEnabler enabler = new LocatorClientEnabler();
        enabler.setBus(busMock);

        enabler.setLocatorSelectionStrategies(locatorSelectionStrategies);
        enabler.setDefaultLocatorSelectionStrategy("evenDistributionSelectionStrategy");

        ServiceLocatorManager slm = new ServiceLocatorManager();

        slm.setBus(busMock);
        slm.setLocatorRegistrar(locatorRegistrarMock);
        slm.setLocatorClientEnabler(enabler);

        expect(busMock.getExtension(ServiceLocatorManager.class)).andStubReturn(slm);

        ClientLifeCycleManager clcm = new ClientLifeCycleManagerImpl();
        expect(busMock.getExtension(ClientLifeCycleManager.class)).andStubReturn(clcm);

        replayAll();

        EndpointInfo ei = new EndpointInfo();
        Service service = new org.apache.cxf.service.ServiceImpl();
        Endpoint endpoint = new EndpointImpl(busMock, service, ei);
        ClientConfiguration client = new ClientConfiguration();

        LocatorTargetSelector selector = new LocatorTargetSelector();
        selector.setEndpoint(endpoint);

        client.setConduitSelector(selector);

        LocatorFeature lf = new LocatorFeature();
        lf.setSelectionStrategy("randomSelectionStrategy");

        lf.initialize(client, busMock);

        Assert.assertTrue(((LocatorTargetSelector) client.getConduitSelector()).getStrategy()
                instanceof RandomSelectionStrategy);

    }

    @Test
    public void initializeInterceptorProvider() throws EndpointException {
        LocatorClientEnabler enabler = new LocatorClientEnabler();
        enabler.setBus(busMock);

        enabler.setLocatorSelectionStrategies(locatorSelectionStrategies);
        enabler.setDefaultLocatorSelectionStrategy("evenDistributionSelectionStrategy");

        ServiceLocatorManager slm = new ServiceLocatorManager();

        slm.setBus(busMock);
        slm.setLocatorRegistrar(locatorRegistrarMock);
        slm.setLocatorClientEnabler(enabler);

        expect(busMock.getExtension(ServiceLocatorManager.class)).andStubReturn(slm);

        ClientLifeCycleManager clcm = new ClientLifeCycleManagerImpl();
        expect(busMock.getExtension(ClientLifeCycleManager.class)).andStubReturn(clcm);

        replayAll();

        EndpointInfo ei = new EndpointInfo();
        Service service = new org.apache.cxf.service.ServiceImpl();
        Endpoint endpoint = new EndpointImpl(busMock, service, ei);
        ClientConfiguration client = new ClientConfiguration();

        LocatorTargetSelector selector = new LocatorTargetSelector();
        selector.setEndpoint(endpoint);

        client.setConduitSelector(selector);

        LocatorFeature lf = new LocatorFeature();
        lf.setSelectionStrategy("randomSelectionStrategy");

        lf.initialize((InterceptorProvider) client, busMock);

        Assert.assertTrue(((LocatorTargetSelector) client.getConduitSelector()).getStrategy()
                instanceof RandomSelectionStrategy);
    }
}
