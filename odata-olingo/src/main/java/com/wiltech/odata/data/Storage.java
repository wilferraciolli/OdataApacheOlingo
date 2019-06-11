/*
 * (c) Midland Software Limited 2019
 * Name     : Storage.java
 * Author   : ferraciolliw
 * Date     : 11 Jun 2019
 */
package com.wiltech.odata.data;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmKeyPropertyRef;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

import com.wiltech.odata.service.DemoEdmProvider;
import com.wiltech.odata.utils.Util;

/**
 * The type Storage.
 * class Storage.java to simulate the data layer (in a real scenario, this would be e.g. a database or any other data storage)
 */
public class Storage {

    private final List<Entity> productList;

    /**
     * Instantiates a new Storage.
     */
    public Storage() {
        productList = new ArrayList<Entity>();
        initSampleData();
    }

    /* PUBLIC FACADE */

    /**
     * Read entity set data entity collection.
     * @param edmEntitySet the edm entity set
     * @return the entity collection
     * @throws ODataApplicationException the o data application exception
     */
    public EntityCollection readEntitySetData(final EdmEntitySet edmEntitySet) throws ODataApplicationException {

        // actually, this is only required if we have more than one Entity Sets
        if (edmEntitySet.getName().equals(DemoEdmProvider.ES_PRODUCTS_NAME)) {
            return getProducts();
        }

        return null;
    }

    public Entity createEntityData(final EdmEntitySet edmEntitySet, final Entity entityToCreate) {

        final EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
        if (edmEntityType.getName().equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
            return createProduct(edmEntityType, entityToCreate);
        }

        return null;
    }

    /**
     * This method is invoked for PATCH or PUT requests
     * @param edmEntitySet the edm entity set
     * @param keyParams the key params
     * @param updateEntity the update entity
     * @param httpMethod the http method
     * @throws ODataApplicationException the o data application exception
     */
    public void updateEntityData(final EdmEntitySet edmEntitySet, final List<UriParameter> keyParams, final Entity updateEntity,
            final HttpMethod httpMethod) throws ODataApplicationException {

        final EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
        if (edmEntityType.getName().equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
            updateProduct(edmEntityType, keyParams, updateEntity, httpMethod);
        }
    }

    /**
     * Delete entity data.
     * @param edmEntitySet the edm entity set
     * @param keyParams the key params
     * @throws ODataApplicationException the o data application exception
     */
    public void deleteEntityData(final EdmEntitySet edmEntitySet, final List<UriParameter> keyParams)
            throws ODataApplicationException {

        final EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
        if (edmEntityType.getName().equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
            deleteProduct(edmEntityType, keyParams);
        }
    }

    /**
     * Read entity data entity.
     * @param edmEntitySet the edm entity set
     * @param keyParams the key params
     * @return the entity
     * @throws ODataApplicationException the o data application exception
     */
    public Entity readEntityData(final EdmEntitySet edmEntitySet, final List<UriParameter> keyParams) throws ODataApplicationException {

        final EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
        if (edmEntityType.getName().equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
            return getProduct(edmEntityType, keyParams);
        }

        return null;
    }

    /*  INTERNAL */
    private EntityCollection getProducts() {
        final EntityCollection retEntitySet = new EntityCollection();

        for (final Entity productEntity : this.productList) {
            retEntitySet.getEntities().add(productEntity);
        }

        return retEntitySet;
    }

    private Entity getProduct(final EdmEntityType edmEntityType, final List<UriParameter> keyParams) throws ODataApplicationException {

        // the list of entities at runtime
        final EntityCollection entitySet = getProducts();

        /*  generic approach  to find the requested entity */
        final Entity requestedEntity = Util.findEntity(edmEntityType, entitySet, keyParams);

        if (requestedEntity == null) {
            // this variable is null if our data doesn't contain an entity for the requested key
            // Throw suitable exception
            throw new ODataApplicationException("Entity for requested key doesn't exist",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        return requestedEntity;
    }

    private Entity createProduct(final EdmEntityType edmEntityType, final Entity entity) {

        // the ID of the newly created product entity is generated automatically
        int newId = 1;
        while (productIdExists(newId)) {
            newId++;
        }

        final Property idProperty = entity.getProperty("ID");
        if (idProperty != null) {
            idProperty.setValue(ValueType.PRIMITIVE, Integer.valueOf(newId));
        } else {
            // as of OData v4 spec, the key property can be omitted from the POST request body
            entity.getProperties().add(new Property(null, "ID", ValueType.PRIMITIVE, newId));
        }
        entity.setId(createId("Products", newId));
        this.productList.add(entity);

        return entity;

    }

    private boolean productIdExists(final int id) {

        for (final Entity entity : this.productList) {
            final Integer existingID = (Integer) entity.getProperty("ID").getValue();
            if (existingID.intValue() == id) {
                return true;
            }
        }

        return false;
    }

    private void updateProduct(final EdmEntityType edmEntityType, final List<UriParameter> keyParams, final Entity entity,
            final HttpMethod httpMethod)
            throws ODataApplicationException {

        final Entity productEntity = getProduct(edmEntityType, keyParams);
        if (productEntity == null) {
            throw new ODataApplicationException("Entity not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        // loop over all properties and replace the values with the values of the given payload
        // Note: ignoring ComplexType, as we don't have it in our odata model
        final List<Property> existingProperties = productEntity.getProperties();
        for (final Property existingProp : existingProperties) {
            final String propName = existingProp.getName();

            // ignore the key properties, they aren't updateable
            if (isKey(edmEntityType, propName)) {
                continue;
            }

            final Property updateProperty = entity.getProperty(propName);
            // the request payload might not consider ALL properties, so it can be null
            if (updateProperty == null) {
                // if a property has NOT been added to the request payload
                // depending on the HttpMethod, our behavior is different
                if (httpMethod.equals(HttpMethod.PATCH)) {
                    // in case of PATCH, the existing property is not touched
                    continue; // do nothing
                } else if (httpMethod.equals(HttpMethod.PUT)) {
                    // in case of PUT, the existing property is set to null
                    existingProp.setValue(existingProp.getValueType(), null);
                    continue;
                }
            }

            // change the value of the properties
            existingProp.setValue(existingProp.getValueType(), updateProperty.getValue());
        }
    }

    private void deleteProduct(final EdmEntityType edmEntityType, final List<UriParameter> keyParams)
            throws ODataApplicationException {

        final Entity productEntity = getProduct(edmEntityType, keyParams);
        if (productEntity == null) {
            throw new ODataApplicationException("Entity not found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        this.productList.remove(productEntity);
    }

    /* HELPERS */

    private boolean isKey(final EdmEntityType edmEntityType, final String propertyName) {
        final List<EdmKeyPropertyRef> keyPropertyRefs = edmEntityType.getKeyPropertyRefs();
        for (final EdmKeyPropertyRef propRef : keyPropertyRefs) {
            final String keyPropertyName = propRef.getName();
            if (keyPropertyName.equals(propertyName)) {
                return true;
            }
        }
        return false;
    }

    private void initSampleData() {

        // add some sample product entities
        final Entity e1 = new Entity()
                .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 1))
                .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Notebook Basic 15"))
                .addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
                        "Notebook Basic, 1.7GHz - 15 XGA - 1024MB DDR2 SDRAM - 40GB"));
        e1.setId(createId("Products", 1));
        productList.add(e1);

        final Entity e2 = new Entity()
                .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 2))
                .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "1UMTS PDA"))
                .addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
                        "Ultrafast 3G UMTS/HSDPA Pocket PC, supports GSM network"));
        e2.setId(createId("Products", 1));
        productList.add(e2);

        final Entity e3 = new Entity()
                .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 3))
                .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Ergo Screen"))
                .addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
                        "19 Optimum Resolution 1024 x 768 @ 85Hz, resolution 1280 x 960"));
        e3.setId(createId("Products", 1));
        productList.add(e3);
    }

    private URI createId(final String entitySetName, final Object id) {
        try {
            return new URI(entitySetName + "(" + String.valueOf(id) + ")");
        } catch (final URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }
}
