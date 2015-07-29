package com.anli.jpa.jta.injection;

import com.anli.jpa.jta.entitymanager.JtaEntityManagerHandler;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

import static javax.persistence.PersistenceContextType.TRANSACTION;
import static org.springframework.util.ReflectionUtils.doWithFields;

public class JtaPersistenceContextProcessor implements BeanPostProcessor {

    private final ReflectionUtils.FieldFilter entityManagerFieldFilter;
    private final EntityManager proxy;

    @Inject
    public JtaPersistenceContextProcessor(EntityManagerFactory factory) {
        TransactionManager transactionManager;
        TransactionSynchronizationRegistry registry;
        try {
            transactionManager = InitialContext.doLookup("java:/TransactionManager");
            registry = InitialContext.doLookup("java:comp/TransactionSynchronizationRegistry");
        } catch (NamingException ex) {
            throw new RuntimeException(ex);
        }
        this.proxy = (EntityManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{EntityManager.class},
                new JtaEntityManagerHandler(factory, transactionManager, registry));
        this.entityManagerFieldFilter = new EntityManagerFieldFilter();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        doWithFields(bean.getClass(), new EntityManagerInjector(bean), entityManagerFieldFilter);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    private class EntityManagerFieldFilter implements ReflectionUtils.FieldFilter {

        @Override
        public boolean matches(Field field) {
            if (!EntityManager.class.equals(field.getType())) {
                return false;
            }
            if (!field.isAnnotationPresent(PersistenceContext.class)) {
                return false;
            }
            return field.getAnnotation(PersistenceContext.class).type() == TRANSACTION;
        }
    }

    private class EntityManagerInjector implements ReflectionUtils.FieldCallback {

        private final Object injectable;

        public EntityManagerInjector(Object injectable) {
            this.injectable = injectable;
        }

        @Override
        public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
            field.setAccessible(true);
            field.set(injectable, proxy);
        }
    }
}
