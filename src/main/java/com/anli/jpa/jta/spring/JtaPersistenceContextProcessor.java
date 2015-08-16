package com.anli.jpa.jta.spring;

import com.anli.jpa.jta.common.EntityManagerFieldFilter;
import com.anli.jpa.jta.entitymanager.JtaEntityManagerProxyFactory;
import java.lang.reflect.Field;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

import static org.springframework.util.ReflectionUtils.doWithFields;

public class JtaPersistenceContextProcessor implements BeanPostProcessor {

    private final ReflectionUtils.FieldFilter entityManagerFieldFilter;
    private final EntityManager proxy;

    @Inject
    public JtaPersistenceContextProcessor(EntityManagerFactory factory) {
        this.proxy = JtaEntityManagerProxyFactory.getEntityManagerProxy(factory);
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
