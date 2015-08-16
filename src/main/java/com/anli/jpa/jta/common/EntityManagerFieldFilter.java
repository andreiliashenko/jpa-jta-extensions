package com.anli.jpa.jta.common;

import java.lang.reflect.Field;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.util.ReflectionUtils;

import static javax.persistence.PersistenceContextType.TRANSACTION;

public class EntityManagerFieldFilter implements ReflectionUtils.FieldFilter {

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
