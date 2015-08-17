package com.anli.jpa.jta.guice;

import com.anli.jpa.jta.common.EntityManagerFieldFilter;
import com.anli.jpa.jta.entitymanager.JtaEntityManagerProxyFactory;
import com.google.inject.MembersInjector;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import java.lang.reflect.Field;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.springframework.util.ReflectionUtils;

import static org.springframework.util.ReflectionUtils.doWithFields;

public class JtaPersistenceContextTypeListener implements TypeListener {

    private final ReflectionUtils.FieldFilter entityManagerFieldFilter;
    private final EntityManager proxy;

    public JtaPersistenceContextTypeListener(EntityManagerFactory entityManagerFactory) {
        this.proxy = JtaEntityManagerProxyFactory.getEntityManagerProxy(entityManagerFactory);
        this.entityManagerFieldFilter = new EntityManagerFieldFilter();
    }

    @Override
    public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
        doWithFields(type.getRawType(), new EntityManagerInjectionCreator(encounter),
                entityManagerFieldFilter);
    }

    private class EntityManagerInjectionCreator implements ReflectionUtils.FieldCallback {

        private final TypeEncounter encounter;

        public EntityManagerInjectionCreator(TypeEncounter encounter) {
            this.encounter = encounter;
        }

        @Override
        public void doWith(final Field field) throws IllegalArgumentException, IllegalAccessException {
            encounter.register(new ProxyInjector(field));
        }
    }

    private class ProxyInjector implements MembersInjector {

        protected final Field field;

        public ProxyInjector(Field field) {
            this.field = field;
        }

        @Override
        public void injectMembers(Object instance) {
            try {
                field.setAccessible(true);
                field.set(instance, proxy);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
