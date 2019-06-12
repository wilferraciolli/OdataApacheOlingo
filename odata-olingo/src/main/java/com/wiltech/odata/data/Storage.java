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

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.server.api.uri.UriParameter;

import com.wiltech.odata.service.DemoEdmProvider;
import com.wiltech.odata.utils.Util;

/**
 * The type Storage.
 * class Storage.java to simulate the data layer (in a real scenario, this would be e.g. a database or any other data storage)
 */
public class Storage {

    // represent our database
    private final List<Entity> productList;
    private final List<Entity> categoryList;
    private final List<Entity> supplierList;

    public Storage() {

        productList = new ArrayList<Entity>();
        categoryList = new ArrayList<Entity>();
        supplierList = new ArrayList<Entity>();

        // creating some sample data
        initProductSampleData();
        initCategorySampleData();
        initSupplierSampleData();
    }

    /* PUBLIC FACADE */

    public EntityCollection readEntitySetData(final EdmEntitySet edmEntitySet) {
        EntityCollection entitySet = null;

        if (edmEntitySet.getName().equals(DemoEdmProvider.ES_PRODUCTS_NAME)) {
            entitySet = getProducts();
        } else if (edmEntitySet.getName().equals(DemoEdmProvider.ES_CATEGORIES_NAME)) {
            entitySet = getCategories();
        }

        return entitySet;
    }

    public Entity readEntityData(final EdmEntitySet edmEntitySet, final List<UriParameter> keyParams) {
        Entity entity = null;

        final EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        if (edmEntityType.getName().equals(DemoEdmProvider.ET_PRODUCT_NAME)) {
            entity = getProduct(edmEntityType, keyParams);
        } else if (edmEntityType.getName().equals(DemoEdmProvider.ET_CATEGORY_NAME)) {
            entity = getCategory(edmEntityType, keyParams);
        } else if (edmEntityType.getName().equals(DemoEdmProvider.ET_SUPPLIER_NAME)) {
            entity = getSupplier(edmEntityType, keyParams);
        }

        return entity;
    }

    // Navigation

    public Entity getRelatedEntity(final Entity entity, final EdmEntityType relatedEntityType) {
        final EntityCollection collection = getRelatedEntityCollection(entity, relatedEntityType);
        if (collection.getEntities().isEmpty()) {
            return null;
        }
        return collection.getEntities().get(0);
    }

    public Entity getRelatedEntity(final Entity entity, final EdmEntityType relatedEntityType, final List<UriParameter> keyPredicates) {

        final EntityCollection relatedEntities = getRelatedEntityCollection(entity, relatedEntityType);
        return Util.findEntity(relatedEntityType, relatedEntities, keyPredicates);
    }

    public EntityCollection getRelatedEntityCollection(final Entity sourceEntity, final EdmEntityType targetEntityType) {
        final EntityCollection navigationTargetEntityCollection = new EntityCollection();

        final FullQualifiedName relatedEntityFqn = targetEntityType.getFullQualifiedName();
        final String sourceEntityFqn = sourceEntity.getType();

        if (sourceEntityFqn.equals(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString())
                && relatedEntityFqn.equals(DemoEdmProvider.ET_CATEGORY_FQN)) {
            // relation Products->Category (result all categories)
            final int productID = (Integer) sourceEntity.getProperty("ID").getValue();
            if (productID == 1 || productID == 2) {
                navigationTargetEntityCollection.getEntities().add(categoryList.get(0));
            } else if (productID == 3 || productID == 4) {
                navigationTargetEntityCollection.getEntities().add(categoryList.get(1));
            } else if (productID == 5 || productID == 6) {
                navigationTargetEntityCollection.getEntities().add(categoryList.get(2));
            }
        } else if (sourceEntityFqn.equals(DemoEdmProvider.ET_CATEGORY_FQN.getFullQualifiedNameAsString())
                && relatedEntityFqn.equals(DemoEdmProvider.ET_PRODUCT_FQN)) {
            // relation Category->Products (result all products)
            final int categoryID = (Integer) sourceEntity.getProperty("ID").getValue();
            if (categoryID == 1) {
                // the first 2 products are notebooks
                navigationTargetEntityCollection.getEntities().addAll(productList.subList(0, 2));
            } else if (categoryID == 2) {
                // the next 2 products are organizers
                navigationTargetEntityCollection.getEntities().addAll(productList.subList(2, 4));
            } else if (categoryID == 3) {
                // the first 2 products are monitors
                navigationTargetEntityCollection.getEntities().addAll(productList.subList(4, 6));
            }
        } else if (sourceEntityFqn.equals(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString())
                && relatedEntityFqn.equals(DemoEdmProvider.ET_SUPPLIER_FQN)) {
            final int productID = (Integer) sourceEntity.getProperty("ID").getValue();
            if (productID == 1) {
                navigationTargetEntityCollection.getEntities().add(supplierList.get(0));
                navigationTargetEntityCollection.getEntities().add(supplierList.get(1));
            } else if (productID == 2) {
                navigationTargetEntityCollection.getEntities().add(supplierList.get(2));
            } else if (productID == 3) {
                navigationTargetEntityCollection.getEntities().add(supplierList.get(3));
            } else if (productID == 4) {
                navigationTargetEntityCollection.getEntities().add(supplierList.get(4));
            } else if (productID == 5) {
                navigationTargetEntityCollection.getEntities().add(supplierList.get(5));
            } else if (productID == 6) {
                navigationTargetEntityCollection.getEntities().add(supplierList.get(6));
            }
        }

        if (navigationTargetEntityCollection.getEntities().isEmpty()) {
            return null;
        }

        return navigationTargetEntityCollection;
    }

    /* INTERNAL */

    private EntityCollection getProducts() {
        final EntityCollection retEntitySet = new EntityCollection();

        for (final Entity productEntity : this.productList) {
            retEntitySet.getEntities().add(productEntity);
        }

        return retEntitySet;
    }

    private EntityCollection getSuppliers() {
        final EntityCollection retEntitySet = new EntityCollection();

        for (final Entity supplierEntity : this.supplierList) {
            retEntitySet.getEntities().add(supplierEntity);
        }

        return retEntitySet;
    }

    private Entity getProduct(final EdmEntityType edmEntityType, final List<UriParameter> keyParams) {

        // the list of entities at runtime
        final EntityCollection entityCollection = getProducts();

        /* generic approach to find the requested entity */
        return Util.findEntity(edmEntityType, entityCollection, keyParams);
    }

    private Entity getSupplier(final EdmEntityType edmEntityType, final List<UriParameter> keyParams) {

        // the list of entities at runtime
        final EntityCollection entityCollection = getSuppliers();

        /* generic approach to find the requested entity */
        return Util.findEntity(edmEntityType, entityCollection, keyParams);
    }

    private EntityCollection getCategories() {
        final EntityCollection entitySet = new EntityCollection();

        for (final Entity categoryEntity : this.categoryList) {
            entitySet.getEntities().add(categoryEntity);
        }

        return entitySet;
    }

    private Entity getCategory(final EdmEntityType edmEntityType, final List<UriParameter> keyParams) {

        // the list of entities at runtime
        final EntityCollection entitySet = getCategories();

        /* generic approach to find the requested entity */
        return Util.findEntity(edmEntityType, entitySet, keyParams);
    }

    /* HELPER */

    private void initProductSampleData() {

        Entity entity = new Entity();

        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 1));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Notebook Basic 15"));
        entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
                "Notebook Basic, 1.7GHz - 15 XGA - 1024MB DDR2 SDRAM - 40GB"));
        entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        productList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 2));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Notebook Professional 17"));
        entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
                "Notebook Professional, 2.8GHz - 15 XGA - 8GB DDR3 RAM - 500GB"));
        entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        productList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 3));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "1UMTS PDA"));
        entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
                "Ultrafast 3G UMTS/HSDPA Pocket PC, supports GSM network"));
        entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        productList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 4));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Comfort Easy"));
        entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
                "32 GB Digital Assitant with high-resolution color screen"));
        entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        productList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 5));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Ergo Screen"));
        entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
                "19 Optimum Resolution 1024 x 768 @ 85Hz, resolution 1280 x 960"));
        entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        productList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 6));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Flat Basic"));
        entity.addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
                "Optimum Hi-Resolution max. 1600 x 1200 @ 85Hz, Dot Pitch: 0.24mm"));
        entity.setType(DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        productList.add(entity);
    }

    private void initCategorySampleData() {

        Entity entity = new Entity();

        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 1));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Notebooks"));
        entity.setType(DemoEdmProvider.ET_CATEGORY_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        categoryList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 2));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Organizers"));
        entity.setType(DemoEdmProvider.ET_CATEGORY_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        categoryList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 3));
        entity.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Monitors"));
        entity.setType(DemoEdmProvider.ET_CATEGORY_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "ID"));
        categoryList.add(entity);
    }

    private void initSupplierSampleData() {

        Entity entity = new Entity();

        entity.addProperty(new Property(null, "SupplierID", ValueType.PRIMITIVE, 1));
        entity.addProperty(new Property(null, "CompanyName", ValueType.PRIMITIVE, "DESIGN INDEX INDIA"));
        entity.addProperty(new Property(null, "Fax", ValueType.PRIMITIVE,
                "null"));

        Property p1 = new Property(null, "City", ValueType.PRIMITIVE, "Delhi");
        Property p2 = new Property(null, "Country", ValueType.PRIMITIVE, "India");
        ComplexValue complexValue = new ComplexValue();
        complexValue.getValue().add(p1);
        complexValue.getValue().add(p2);

        entity.addProperty(new Property(null, "Address", ValueType.COMPLEX, complexValue));
        entity.setType(DemoEdmProvider.ET_SUPPLIER_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "SupplierID", "Suppliers", "1"));
        supplierList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "SupplierID", ValueType.PRIMITIVE, 2));
        entity.addProperty(new Property(null, "CompanyName", ValueType.PRIMITIVE, "Advance Label Ltd"));
        entity.addProperty(new Property(null, "Fax", ValueType.PRIMITIVE,
                "(617) 555-3389"));
        p1 = new Property(null, "City", ValueType.PRIMITIVE, "Tsuen Wan");
        p2 = new Property(null, "Country", ValueType.PRIMITIVE, "Hong Kong");
        complexValue = new ComplexValue();
        complexValue.getValue().add(p1);
        complexValue.getValue().add(p2);

        entity.addProperty(new Property(null, "Address", ValueType.COMPLEX, complexValue));
        entity.setType(DemoEdmProvider.ET_SUPPLIER_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "SupplierID", "Suppliers", "1"));
        supplierList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "SupplierID", ValueType.PRIMITIVE, 3));
        entity.addProperty(new Property(null, "CompanyName", ValueType.PRIMITIVE, "TECH GROUP INTERNATIONAL LIMITED "));
        entity.addProperty(new Property(null, "Fax", ValueType.PRIMITIVE,
                "null"));
        p1 = new Property(null, "City", ValueType.PRIMITIVE, "Bangalore");
        p2 = new Property(null, "Country", ValueType.PRIMITIVE, "India");
        complexValue = new ComplexValue();
        complexValue.getValue().add(p1);
        complexValue.getValue().add(p2);

        entity.addProperty(new Property(null, "Address", ValueType.COMPLEX, complexValue));
        entity.setType(DemoEdmProvider.ET_SUPPLIER_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "SupplierID", "Suppliers", "2"));
        supplierList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "SupplierID", ValueType.PRIMITIVE, 4));
        entity.addProperty(new Property(null, "CompanyName", ValueType.PRIMITIVE, "BenQ India Pvt Ltd"));
        entity.addProperty(new Property(null, "Fax", ValueType.PRIMITIVE,
                "43844115"));
        p1 = new Property(null, "City", ValueType.PRIMITIVE, "Gurgaon");
        p2 = new Property(null, "Country", ValueType.PRIMITIVE, "India");
        complexValue = new ComplexValue();
        complexValue.getValue().add(p1);
        complexValue.getValue().add(p2);

        entity.addProperty(new Property(null, "Address", ValueType.COMPLEX, complexValue));
        entity.setType(DemoEdmProvider.ET_SUPPLIER_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "SupplierID", "Suppliers", "3"));
        supplierList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "SupplierID", ValueType.PRIMITIVE, 5));
        entity.addProperty(new Property(null, "CompanyName", ValueType.PRIMITIVE, "Sharp"));
        entity.addProperty(new Property(null, "Fax", ValueType.PRIMITIVE,
                "(12345) 1210"));
        p1 = new Property(null, "City", ValueType.PRIMITIVE, "Osaka");
        p2 = new Property(null, "Country", ValueType.PRIMITIVE, "Japan");
        complexValue = new ComplexValue();
        complexValue.getValue().add(p1);
        complexValue.getValue().add(p2);

        entity.addProperty(new Property(null, "Address", ValueType.COMPLEX, complexValue));
        entity.setType(DemoEdmProvider.ET_SUPPLIER_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "SupplierID", "Suppliers", "4"));
        supplierList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "SupplierID", ValueType.PRIMITIVE, 6));
        entity.addProperty(new Property(null, "CompanyName", ValueType.PRIMITIVE, "Wonder World"));
        entity.addProperty(new Property(null, "Fax", ValueType.PRIMITIVE,
                "43844115"));
        p1 = new Property(null, "City", ValueType.PRIMITIVE, "Zürich");
        p2 = new Property(null, "Country", ValueType.PRIMITIVE, "Switzerland");
        complexValue = new ComplexValue();
        complexValue.getValue().add(p1);
        complexValue.getValue().add(p2);

        entity.addProperty(new Property(null, "Address", ValueType.COMPLEX, complexValue));
        entity.setType(DemoEdmProvider.ET_SUPPLIER_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "SupplierID", "Suppliers", "5"));
        supplierList.add(entity);

        entity = new Entity();
        entity.addProperty(new Property(null, "SupplierID", ValueType.PRIMITIVE, 7));
        entity.addProperty(new Property(null, "CompanyName", ValueType.PRIMITIVE, "Touch International"));
        entity.addProperty(new Property(null, "Fax", ValueType.PRIMITIVE,
                "43847115"));
        p1 = new Property(null, "City", ValueType.PRIMITIVE, "Austin");
        p2 = new Property(null, "Country", ValueType.PRIMITIVE, "USA");
        complexValue = new ComplexValue();
        complexValue.getValue().add(p1);
        complexValue.getValue().add(p2);

        entity.addProperty(new Property(null, "Address", ValueType.COMPLEX, complexValue));
        entity.setType(DemoEdmProvider.ET_SUPPLIER_FQN.getFullQualifiedNameAsString());
        entity.setId(createId(entity, "SupplierID", "Suppliers", "6"));
        supplierList.add(entity);
    }

    private URI createId(final Entity entity, final String idPropertyName) {
        return createId(entity, idPropertyName, null);
    }

    private URI createId(final Entity entity, final String idPropertyName, final String navigationName) {
        try {
            final StringBuilder sb = new StringBuilder(getEntitySetName(entity)).append("(");
            final Property property = entity.getProperty(idPropertyName);
            sb.append(property.asPrimitive()).append(")");
            if (navigationName != null) {
                sb.append("/").append(navigationName);
            }
            return new URI(sb.toString());
        } catch (final URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create (Atom) id for entity: " + entity, e);
        }
    }

    private URI createId(final Entity entity, final String idPropertyName, final String navigationName, final String sourceId) {
        try {
            final StringBuilder sb = new StringBuilder(getEntitySetName(entity)).append("(");
            final Property property = entity.getProperty(idPropertyName);
            sb.append(sourceId).append(")");
            if (navigationName != null) {
                sb.append("/").append(navigationName);
                sb.append("(").append(property.asPrimitive()).append(")");
            }
            return new URI(sb.toString());
        } catch (final URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create (Atom) id for entity: " + entity, e);
        }
    }

    private String getEntitySetName(final Entity entity) {
        if (DemoEdmProvider.ET_CATEGORY_FQN.getFullQualifiedNameAsString().equals(entity.getType())) {
            return DemoEdmProvider.ES_CATEGORIES_NAME;
        } else if (DemoEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString().equals(entity.getType())) {
            return DemoEdmProvider.ES_PRODUCTS_NAME;
        } else if (DemoEdmProvider.ET_SUPPLIER_FQN.getFullQualifiedNameAsString().equals(entity.getType())) {
            return DemoEdmProvider.ES_PRODUCTS_NAME;
        }
        return entity.getType();
    }
}
