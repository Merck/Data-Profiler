package com.dataprofiler.lastmile;

/*-
 *
 * dataprofiler-lastmile
 *
 * Copyright 2021 Merck & Co., Inc. Kenilworth, NJ, USA.
 *
 * 	Licensed to the Apache Software Foundation (ASF) under one
 * 	or more contributor license agreements. See the NOTICE file
 * 	distributed with this work for additional information
 * 	regarding copyright ownership. The ASF licenses this file
 * 	to you under the Apache License, Version 2.0 (the
 * 	"License"); you may not use this file except in compliance
 * 	with the License. You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * 	Unless required by applicable law or agreed to in writing,
 * 	software distributed under the License is distributed on an
 * 	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * 	KIND, either express or implied. See the License for the
 * 	specific language governing permissions and limitations
 * 	under the License.
 *
 */

import static java.util.Objects.nonNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dataprofiler.lastmile.model.Entry;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see "https://www.baeldung.com/hibernate-entitymanager"
 * @see
 *     "https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/"
 */
public class AbstractJpaTestCase {
  private static final Logger logger = LoggerFactory.getLogger(AbstractJpaTestCase.class);
  private static EntityManagerFactory entityManagerFactory;
  protected EntityManager entityManager;
  protected EntityTransaction entityTransaction;

  @BeforeClass
  public static void beforeClass() {
    entityManagerFactory = Persistence.createEntityManagerFactory("lastmile-test");
  }

  @AfterClass
  public static void afterClass() {
    if (nonNull(entityManagerFactory)) {
      entityManagerFactory.close();
    }

    entityManagerFactory = null;
  }

  /** extend this class and call <code>super().setup()</code> in the <code>@Before</code> method */
  public void setup() {
    // entityManagerFactory is thread safe
    // entityManager is not thread safe
    entityManager = entityManagerFactory.createEntityManager();
    if (logger.isDebugEnabled()) {
      logger.debug("setup entity manager " + entityManager);
    }
    entityTransaction = entityManager.getTransaction();
  }

  /**
   * extend this class and call <code>super().teardown()</code> in the <code>@Before</code> method
   */
  public void teardown() {
    if (nonNull(entityManager)) {
      entityManager.close();
    }
    entityTransaction = null;
    entityManager = null;
  }

  public EntityManager getEntityManager() {
    return entityManager;
  }

  public Session getSession() {
    return vendorSpecificHibernateSession(entityManager);
  }

  protected Session vendorSpecificHibernateSession(EntityManager entityManager) {
    if (entityManager instanceof EntityManagerFactoryBuilderImpl) {
      EntityManagerFactoryBuilderImpl impl = (EntityManagerFactoryBuilderImpl) entityManager;
      SessionFactory sessionFactory = impl.getMetadata().buildSessionFactory();
      if (sessionFactory instanceof SessionFactoryImpl) {
        SessionFactoryImpl sessionFactoryImpl = (SessionFactoryImpl) sessionFactory;
        return sessionFactoryImpl.getCurrentSession();
      }
    }
    throw new IllegalArgumentException("unknown entity manager vendor");
  }

  protected <T extends Entry> void assertEqualityConsistency(Class<T> clazz, T entity) {
    Set<T> tuples = new HashSet<>();
    assertFalse(tuples.contains(entity));
    tuples.add(entity);
    assertTrue(tuples.contains(entity));

    doInJPA(
        entityManager -> {
          entityManager.persist(entity);
          entityManager.flush();
          assertTrue(
              "The entity is not found in the Set after it's persisted.", tuples.contains(entity));
        });

    assertTrue(tuples.contains(entity));

    doInJPA(
        entityManager -> {
          T entityProxy = entityManager.getReference(clazz, entity.getId());
          assertTrue("The entity proxy is not equal with the entity.", entityProxy.equals(entity));
        });

    doInJPA(
        entityManager -> {
          T entityProxy = entityManager.getReference(clazz, entity.getId());
          assertTrue("The entity is not equal with the entity proxy.", entity.equals(entityProxy));
        });

    doInJPA(
        entityManager -> {
          T _entity = entityManager.merge(entity);
          assertTrue(
              "The entity is not found in the Set after it's merged.", tuples.contains(_entity));
        });

    doInJPA(
        entityManager -> {
          entityManager.unwrap(Session.class).update(entity);
          assertTrue(
              "The entity is not found in the Set after it's reattached.", tuples.contains(entity));
        });

    doInJPA(
        entityManager -> {
          T _entity = entityManager.find(clazz, entity.getId());
          assertTrue(
              "The entity is not found in the Set after it's loaded in a different Persistence Context.",
              tuples.contains(_entity));
        });

    doInJPA(
        entityManager -> {
          T _entity = entityManager.getReference(clazz, entity.getId());
          assertTrue(
              "The entity is not found in the Set after it's loaded as a proxy in a different Persistence Context.",
              tuples.contains(_entity));
        });

    T deletedEntity =
        doInJPA(
            entityManager -> {
              T _entity = entityManager.getReference(clazz, entity.getId());
              entityManager.remove(_entity);
              return _entity;
            });

    assertTrue(
        "The entity is not found in the Set even after it's deleted.",
        tuples.contains(deletedEntity));
  }

  protected void doInJPA(Consumer<EntityManager> func) {
    func.accept(entityManager);
  }

  protected <T> T doInJPA(Function<EntityManager, T> func) {
    return func.apply(entityManager);
  }

  protected void doInJPAWithTransaction(Consumer<EntityManager> func) {
    entityTransaction.begin();
    func.accept(entityManager);
    entityTransaction.commit();
  }

  protected void doInJPA(BiConsumer<EntityManager, EntityTransaction> func) {
    func.accept(entityManager, entityTransaction);
  }
}
