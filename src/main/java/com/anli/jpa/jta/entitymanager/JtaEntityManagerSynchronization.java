package com.anli.jpa.jta.entitymanager;

import javax.persistence.EntityManager;
import javax.transaction.Synchronization;

public class JtaEntityManagerSynchronization implements Synchronization {

    private final EntityManager entityManager;

    public JtaEntityManagerSynchronization(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void beforeCompletion() {
        if (entityManager.isOpen()) {
            entityManager.close();
        }
    }

    @Override
    public void afterCompletion(int status) {
    }
}
