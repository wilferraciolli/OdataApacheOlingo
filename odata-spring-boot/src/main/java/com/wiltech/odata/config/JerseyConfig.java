/*
 * (c) Midland Software Limited 2019
 * Name     : JerseyConfig.java
 * Author   : ferraciolliw
 * Date     : 12 Jun 2019
 */
package com.wiltech.odata.config;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.olingo.odata2.api.ODataServiceFactory;
import org.apache.olingo.odata2.core.rest.ODataRootLocator;
import org.apache.olingo.odata2.core.rest.app.ODataApplication;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.wiltech.odata.service.CarsODataJPAServiceFactory;

/**
 * Jersey JAX-RS configuration
 */
@Component
@ApplicationPath("/odata")
public class JerseyConfig extends ResourceConfig {

    /**
     * Instantiates a new Jersey config.
     * @param serviceFactory the service factory
     * @param emf the emf
     */
    public JerseyConfig(CarsODataJPAServiceFactory serviceFactory, EntityManagerFactory emf) {

        ODataApplication app = new ODataApplication();
        app
                .getClasses()
                .forEach(c -> {
                    // Avoid using the default RootLocator, as we want a Spring Managed one
                    if (!ODataRootLocator.class.isAssignableFrom(c)) {
                        register(c);
                    }
                });

        register(new CarsRootLocator(serviceFactory));
        register(new EntityManagerFilter(emf));
    }

    /**
     * This filter handles the EntityManager transaction lifecycle.
     */
    @Provider
    public static class EntityManagerFilter implements ContainerRequestFilter, ContainerResponseFilter {

        private static final Logger log = LoggerFactory.getLogger(EntityManagerFilter.class);
        public static final String EM_REQUEST_ATTRIBUTE = EntityManagerFilter.class.getName() + "_ENTITY_MANAGER";

        private final EntityManagerFactory emf;

        @Context
        private HttpServletRequest httpRequest;

        /**
         * Instantiates a new Entity manager filter.
         * @param emf the emf
         */
        public EntityManagerFilter(EntityManagerFactory emf) {
            this.emf = emf;
        }

        @Override
        public void filter(ContainerRequestContext ctx) throws IOException {
            log.info("[I60] >>> filter");
            EntityManager em = this.emf.createEntityManager();
            httpRequest.setAttribute(EM_REQUEST_ATTRIBUTE, em);

            // Start a new transaction unless we have a simple GET
            if (!"GET".equalsIgnoreCase(ctx.getMethod())) {
                em.getTransaction()
                        .begin();
            }
        }

        @Override
        public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

            log.info("[I68] <<< filter");
            EntityManager em = (EntityManager) httpRequest.getAttribute(EM_REQUEST_ATTRIBUTE);

            if (!"GET".equalsIgnoreCase(requestContext.getMethod())) {
                EntityTransaction t = em.getTransaction();
                if (t.isActive()) {
                    if (!t.getRollbackOnly()) {
                        t.commit();
                    }
                }
            }

            em.close();
        }
    }

    /**
     * The type Cars root locator.
     */
    @Path("/")
    public static class CarsRootLocator extends ODataRootLocator {

        private CarsODataJPAServiceFactory serviceFactory;

        /**
         * Instantiates a new Cars root locator.
         * @param serviceFactory the service factory
         */
        public CarsRootLocator(CarsODataJPAServiceFactory serviceFactory) {
            this.serviceFactory = serviceFactory;
        }

        @Override
        public ODataServiceFactory getServiceFactory() {
            return this.serviceFactory;
        }

    }
}
