/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.concurrent.ri;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.enterprise.concurrent.ContextService;
import org.glassfish.concurrent.ri.test.ClassloaderContextSetupProvider;
import org.glassfish.concurrent.ri.test.DummyTransactionSetupProvider;
import org.glassfish.concurrent.ri.test.ManagedTaskListenerImpl;
import org.glassfish.concurrent.ri.test.RunnableImpl;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class ContextServiceImplTest {
    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCreateContextualProxy() throws Exception {
        final String classloaderName = "testCreateContextualProxy";
        ClassloaderContextSetupProvider contextSetupProvider = new ClassloaderContextSetupProvider(classloaderName);
        DummyTransactionSetupProvider txSetupProvider = new DummyTransactionSetupProvider();
        RunnableImpl task = new RunnableImpl(null);
        ContextServiceImpl contextService = 
                new ContextServiceImpl("myContextService", contextSetupProvider, txSetupProvider);
        Runnable proxy = contextService.createContextualProxy(task, Runnable.class);

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        // Run on same thread
        proxy.run();

        task.verifyAfterRun(classloaderName);
        assertNull(contextSetupProvider.contextServiceProperties);

        verifyTransactionSetupProvider(txSetupProvider, false);
        // did we revert the classloader back to the original one?
        assertEquals(original, Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testCreateContextualProxy_multiple_interfaces() throws Exception {
        final String classloaderName = "testCreateContextualProxy_multiple_interfaces";
        ClassloaderContextSetupProvider contextSetupProvider = new ClassloaderContextSetupProvider(classloaderName);
        ComparableRunnableImpl task = new ComparableRunnableImpl(null);
        ContextServiceImpl contextService = new ContextServiceImpl("myContextService", contextSetupProvider);
        
        // we can cast the proxy to any of the 2 interfaces
        Object proxy = contextService.createContextualProxy(task, Runnable.class, Comparable.class);
        Comparable comparableProxy = (Comparable) contextService.createContextualProxy(task, Runnable.class, Comparable.class);
        ComparableRunnable comparableRunnableProxy = (ComparableRunnable) contextService.createContextualProxy(task, ComparableRunnable.class);
                
        // we cannot cast to ComparableRunnable
        try {
            ComparableRunnable proxy1 = (ComparableRunnable) contextService.createContextualProxy(task, Runnable.class, Comparable.class);
            fail("expected exception not found");
        } catch (ClassCastException expected) {
            // expected
        }
        // we cannot cast to ComparableRunnableImpl
        try {
            ComparableRunnableImpl proxy1 = (ComparableRunnableImpl) contextService.createContextualProxy(task, Runnable.class, Comparable.class);
            fail("expected exception not found");
        } catch (ClassCastException expected) {
            // expected
        }

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        // Use proxy as Runnable to run on same thread
        ((Runnable)proxy).run();
        
        // Can also use proxy as Comparable
        Comparable compProxy = (Comparable)proxy;

        task.verifyAfterRun(classloaderName);
        assertNull(contextSetupProvider.contextServiceProperties);

        // did we revert the classloader back to the original one?
        assertEquals(original, Thread.currentThread().getContextClassLoader());
    }


    @Test
    public void testCreateContextualProxy_withProperties() throws Exception {
        final String classloaderName = "testCreateContextualProxy_withProperties";
        ClassloaderContextSetupProvider contextSetupProvider = new ClassloaderContextSetupProvider(classloaderName);
        DummyTransactionSetupProvider txSetupProvider = new DummyTransactionSetupProvider();
        Map<String, String> props = new HashMap<>();
        props.put(ContextService.USE_PARENT_TRANSACTION, "true");
        ComparableRunnableImpl task = new ComparableRunnableImpl(null);
        ContextServiceImpl contextService = 
                new ContextServiceImpl("myContextService", contextSetupProvider, txSetupProvider);
        Runnable proxy = (Runnable) contextService.createContextualProxy(task, props, Runnable.class, Comparable.class);

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        // Run on same thread
        proxy.run();

        task.verifyAfterRun(classloaderName);
        assertEquals("true", contextSetupProvider.contextServiceProperties.get(ContextService.USE_PARENT_TRANSACTION));

        verifyTransactionSetupProvider(txSetupProvider, true);
        // did we revert the classloader back to the original one?
        assertEquals(original, Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testCreateContextualProxy_withProperties_multiple_interfaces() throws Exception {
        final String classloaderName = "testCreateContextualProxy_withProperties";
        ClassloaderContextSetupProvider contextSetupProvider = new ClassloaderContextSetupProvider(classloaderName);
        Map<String, String> props = new HashMap<>();
        props.put(ContextService.USE_PARENT_TRANSACTION, "false");
        RunnableImpl task = new RunnableImpl(null);
        ContextServiceImpl contextService = new ContextServiceImpl("myContextService", contextSetupProvider);
        Runnable proxy = contextService.createContextualProxy(task, props, Runnable.class);

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        // Run on same thread
        proxy.run();

        task.verifyAfterRun(classloaderName);
        assertEquals("false", contextSetupProvider.contextServiceProperties.get(ContextService.USE_PARENT_TRANSACTION));

        // did we revert the classloader back to the original one?
        assertEquals(original, Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testCreateContextualProxy_wrongInterface() throws Exception {
        final String classloaderName = "testCreateContextualProxy_wrongInterface";
        ClassloaderContextSetupProvider contextSetupProvider = new ClassloaderContextSetupProvider(classloaderName);
        RunnableImpl task = new RunnableImpl(null);
        ContextServiceImpl contextService = new ContextServiceImpl("myContextService", contextSetupProvider);
        try {
            // RunnableImpl does not implements Callable
            Object proxy = contextService.createContextualProxy(task, Callable.class, Runnable.class);
            fail("expected IllegalArgumentException not thrown");
        } catch (IllegalArgumentException expected) {
            // expected exception
        }
    }

    @Test
    public void testCreateContextualProxy_withProperties_wrongInterface() throws Exception {
        final String classloaderName = "testCreateContextualProxy_withProperties_wrongInterface";
        ClassloaderContextSetupProvider contextSetupProvider = new ClassloaderContextSetupProvider(classloaderName);
        Map<String, String> props = new HashMap<>();
        props.put(ContextService.USE_PARENT_TRANSACTION, "false");
        RunnableImpl task = new RunnableImpl(null);
        ContextServiceImpl contextService = new ContextServiceImpl("myContextService", contextSetupProvider);
        try {
            // RunnableImpl does not implements Callable
            Object proxy = contextService.createContextualProxy(task, props, Callable.class, Runnable.class);
            fail("expected IllegalArgumentException not thrown");
        } catch (IllegalArgumentException expected) {
            // expected exception
        }
    }

    @Test
    public void testContextualProxy_hashCode() throws Exception {
        final String classloaderName = "testContextualProxy_hashCode";
        ClassloaderContextSetupProvider contextSetupProvider = new ClassloaderContextSetupProvider(classloaderName);
        RunnableImpl task = new RunnableImpl(null);
        ContextServiceImpl contextService = new ContextServiceImpl("myContextService", contextSetupProvider);
        Runnable proxy = contextService.createContextualProxy(task, Runnable.class);

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        // Run on same thread
        assertEquals(task.hashCode(), proxy.hashCode());

        assertEquals(original, task.taskRunClassLoader);
    }

    @Test
    public void testContextualProxy_toString() throws Exception {
        final String classloaderName = "testContextualProxy_toString";
        ClassloaderContextSetupProvider contextSetupProvider = new ClassloaderContextSetupProvider(classloaderName);
        RunnableImpl task = new RunnableImpl(null);
        ContextServiceImpl contextService = new ContextServiceImpl("myContextService", contextSetupProvider);
        Runnable proxy = contextService.createContextualProxy(task, Runnable.class);

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        // Run on same thread
        assertEquals(task.toString(), proxy.toString());

        assertEquals(original, task.taskRunClassLoader);
    }

    @Test
    public void testContextualProxy_equals() throws Exception {
        final String classloaderName = "testContextualProxy_equals";
        ClassloaderContextSetupProvider contextSetupProvider = new ClassloaderContextSetupProvider(classloaderName);
        RunnableImpl task = new RunnableImpl(null);
        ContextServiceImpl contextService = new ContextServiceImpl("myContextService", contextSetupProvider);
        Runnable proxy = contextService.createContextualProxy(task, Runnable.class);

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        // Run on same thread
        assertEquals(task.equals(task), proxy.equals(task));

        assertEquals(original, task.taskRunClassLoader);
    }

    @Test
    public void testGetExecutionProperties() throws Exception {
        final String classloaderName = "testGetProperties";
        ClassloaderContextSetupProvider contextSetupProvider = new ClassloaderContextSetupProvider(classloaderName);
        RunnableImpl task = new RunnableImpl(null);
        Map<String, String> props = new HashMap<>();
        final String PROP_NAME = "myProp";
        props.put(PROP_NAME, "true");
        ContextServiceImpl contextService = new ContextServiceImpl("myContextService1", contextSetupProvider);
        Runnable proxy = contextService.createContextualProxy(task, props, Runnable.class);
        
        Map<String, String> copy = contextService.getExecutionProperties(proxy);
        assertEquals("true", copy.get(PROP_NAME));
        
        // update the property value in the copy. Should not affect the property value of the proxy object
        copy.put(PROP_NAME, "false");
        
        Map<String, String> copy2 = contextService.getExecutionProperties(proxy);
        assertEquals("true", copy2.get(PROP_NAME));
    }

    @Test
    public void testGetExecutionProperties_invalidProxy() throws Exception {
        final String classloaderName = "testGetProperties_invalidProxy";
        ClassloaderContextSetupProvider contextSetupProvider = new ClassloaderContextSetupProvider(classloaderName);
        RunnableImpl task = new RunnableImpl(null);
        ContextServiceImpl contextService = new ContextServiceImpl("myContextService1", contextSetupProvider);

        try {
            contextService.getExecutionProperties(task);
            fail("expected exception not thrown");
        } catch (IllegalArgumentException ex) {
            // expected exception
        }
    }

    @Test
    public void testGetExecutionProperties_invalidProxy2() throws Exception {
        final String classloaderName = "testGetProperties_invalidProxy2";
        ClassloaderContextSetupProvider contextSetupProvider = new ClassloaderContextSetupProvider(classloaderName);
        RunnableImpl task = new RunnableImpl(null);
        ContextServiceImpl contextService1 = new ContextServiceImpl("myContextService1", contextSetupProvider);
        ContextServiceImpl contextService2 = new ContextServiceImpl("myContextService2", contextSetupProvider);
        Runnable proxy = (Runnable) contextService1.createContextualProxy(task, new Class[]{Runnable.class});

        try {
            contextService2.getExecutionProperties(proxy);
            fail("expected exception not thrown");
        } catch (IllegalArgumentException ex) {
            // expected exception
        }
    }
    
    protected void verifyTransactionSetupProvider(DummyTransactionSetupProvider provider, boolean useParentTransaction) {
        assertTrue(provider.beforeProxyMethodCalled);
        assertTrue(provider.afterProxyMethodCalled);
        assertTrue(provider.sameTransactionHandle);
        assertEquals(useParentTransaction, provider.useParentTransactionBefore);
        assertEquals(useParentTransaction, provider.useParentTransactionAfter);
    }
    
    public static interface ComparableRunnable extends Runnable, Comparable {
        
    }
    
    public static class ComparableRunnableImpl extends RunnableImpl implements ComparableRunnable {

        public ComparableRunnableImpl(ManagedTaskListenerImpl taskListener, RuntimeException runException) {
            super(taskListener, runException);
        }

        public ComparableRunnableImpl(ManagedTaskListenerImpl taskListener) {
            super(taskListener);
        }

        @Override
        public int compareTo(Object o) {
            // we are not really interested in compareTo. We just want to 
            // have a class that implements multiple interfaces
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }

}