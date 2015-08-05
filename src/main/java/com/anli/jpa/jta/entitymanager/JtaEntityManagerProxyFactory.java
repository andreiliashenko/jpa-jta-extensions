package com.anli.jpa.jta.entitymanager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

public class JtaEntityManagerProxyFactory {

    private static final Class proxyClass =
            Proxy.getProxyClass(JtaEntityManagerProxyFactory.class.getClassLoader(), EntityManager.class);
    private static TransactionManager transactionManager = null;
    private static TransactionSynchronizationRegistry registry = null;

    public static EntityManager getEntityManagerProxy(EntityManagerFactory factory) {
        try {
            lookupResources();
            return (EntityManager) getProxyInstance(factory);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void lookupResources() throws NamingException {
        if (transactionManager == null) {
            transactionManager = InitialContext.doLookup("java:/TransactionManager");
        }
        if (registry == null) {
            registry = InitialContext.doLookup("java:comp/TransactionSynchronizationRegistry");
        }
    }

    private static Object getProxyInstance(EntityManagerFactory factory) throws Exception {
        return proxyClass.getConstructor(InvocationHandler.class)
                .newInstance(new JtaEntityManagerHandler(factory, transactionManager, registry));
    }
}
