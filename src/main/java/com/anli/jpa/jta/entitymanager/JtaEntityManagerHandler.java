package com.anli.jpa.jta.entitymanager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.springframework.transaction.IllegalTransactionStateException;

import static javax.transaction.Status.STATUS_NO_TRANSACTION;

public class JtaEntityManagerHandler implements InvocationHandler {

    private static final String JTA_JPA_ENTITY_MANAGER_KEY = "jta-gpa-entity-manager";

    private final EntityManagerFactory entityManagerFactory;
    private final TransactionManager transactionManager;
    private final TransactionSynchronizationRegistry registry;

    public JtaEntityManagerHandler(EntityManagerFactory entityManagerFactory,
            TransactionManager transactionManager, TransactionSynchronizationRegistry registry) {
        this.entityManagerFactory = entityManagerFactory;
        this.transactionManager = transactionManager;
        this.registry = registry;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("equals")) {
            return (proxy == args[0]);
        } else if (method.getName().equals("hashCode")) {
            return hashCode();
        } else if (method.getName().equals("toString")) {
            return "JtaEntityManager proxy for target factory [" + entityManagerFactory + "]";
        } else if (method.getName().equals("getEntityManagerFactory")) {
            return entityManagerFactory;
        } else if (method.getName().equals("getCriteriaBuilder")) {
            return entityManagerFactory.getCriteriaBuilder();
        } else if (method.getName().equals("getMetamodel")) {
            return entityManagerFactory.getMetamodel();
        } else if (method.getName().equals("unwrap")) {
            Class<?> targetClass = (Class<?>) args[0];
            if (targetClass == null || targetClass.isInstance(proxy)) {
                return proxy;
            }
        } else if (method.getName().equals("isOpen")) {
            return true;
        } else if (method.getName().equals("close")) {
            return null;
        } else if (method.getName().equals("getTransaction")) {
            throw new IllegalStateException(
                    "Not allowed to create transaction on shared EntityManager");
        }
        try {
            return method.invoke(getLocalManager(), args);
        } catch (InvocationTargetException invocationTargetException) {
            throw invocationTargetException.getCause();
        }
    }

    protected EntityManager getLocalManager() throws SystemException, RollbackException {
        checkTransaction();
        EntityManager manager = (EntityManager) registry.getResource(JTA_JPA_ENTITY_MANAGER_KEY);
        if (manager == null) {
            manager = entityManagerFactory.createEntityManager();
            registry.putResource(JTA_JPA_ENTITY_MANAGER_KEY, manager);
            transactionManager.getTransaction()
                    .registerSynchronization(new JtaEntityManagerSynchronization(manager));
        }
        return manager;
    }

    protected void checkTransaction() throws SystemException {
        if (transactionManager.getStatus() == STATUS_NO_TRANSACTION) {
            throw new IllegalTransactionStateException("No transaction associated with thread");
        }
    }
}
