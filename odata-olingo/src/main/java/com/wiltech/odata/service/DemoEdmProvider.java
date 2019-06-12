/*
 * (c) Midland Software Limited 2019
 * Name     : DemoEdmProvider.java
 * Author   : ferraciolliw
 * Date     : 10 Jun 2019
 */
package com.wiltech.odata.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;

/**
 * The type Demo edm provider.
 */
public class DemoEdmProvider extends CsdlAbstractEdmProvider {

    // Service Namespace
    public static final String NAMESPACE = "OData.Demo";

    // EDM Container
    public static final String CONTAINER_NAME = "Container";

    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    // Entity Types Names
    public static final String ET_PRODUCT_NAME = "Product";
    public static final FullQualifiedName ET_PRODUCT_FQN = new FullQualifiedName(NAMESPACE, ET_PRODUCT_NAME);

    public static final String ET_CATEGORY_NAME = "Category";
    public static final FullQualifiedName ET_CATEGORY_FQN = new FullQualifiedName(NAMESPACE, ET_CATEGORY_NAME);

    public static final String ET_SUPPLIER_NAME = "Supplier";
    public static final FullQualifiedName ET_SUPPLIER_FQN = new FullQualifiedName(NAMESPACE, ET_SUPPLIER_NAME);

    public static final String CT_ADDRESS_NAME = "Address";
    public static final FullQualifiedName CT_ADDRESS_FQN = new FullQualifiedName(NAMESPACE, CT_ADDRESS_NAME);

    // Entity Set Names
    public static final String ES_PRODUCTS_NAME = "Products";
    public static final String ES_CATEGORIES_NAME = "Categories";
    public static final String ES_SUPPLIERS_NAME = "Suppliers";

    @Override
    public CsdlEntityType getEntityType(final FullQualifiedName entityTypeName) throws ODataException {
        // this method is called for each EntityType that are configured in the Schema
        CsdlEntityType entityType = null;

        if (entityTypeName.equals(ET_PRODUCT_FQN)) {
            // create EntityType properties
            final CsdlProperty id = new CsdlProperty().setName("ID")
                    .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            final CsdlProperty name = new CsdlProperty().setName("Name")
                    .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            final CsdlProperty description = new CsdlProperty().setName("Description")
                    .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

            // create PropertyRef for Key element
            final CsdlPropertyRef propertyRef = new CsdlPropertyRef();
            propertyRef.setName("ID");

            // navigation property: many-to-one, null not allowed (product must have a category)
            final CsdlNavigationProperty navProp = new CsdlNavigationProperty().setName("Category")
                    .setType(ET_CATEGORY_FQN)
                    .setNullable(false)
                    .setPartner("Products");
            final CsdlNavigationProperty contNavProp = new CsdlNavigationProperty().setName("Suppliers")
                    .setType(ET_SUPPLIER_FQN)
                    .setContainsTarget(true)
                    .setCollection(true);
            final List<CsdlNavigationProperty> navPropList = new ArrayList<CsdlNavigationProperty>();
            navPropList.add(navProp);
            navPropList.add(contNavProp);

            // configure EntityType
            entityType = new CsdlEntityType();
            entityType.setName(ET_PRODUCT_NAME);
            entityType.setProperties(Arrays.asList(id, name, description));
            entityType.setKey(Arrays.asList(propertyRef));
            entityType.setNavigationProperties(navPropList);

        } else if (entityTypeName.equals(ET_CATEGORY_FQN)) {
            // create EntityType properties
            final CsdlProperty id = new CsdlProperty().setName("ID")
                    .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            final CsdlProperty name = new CsdlProperty().setName("Name")
                    .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

            // create PropertyRef for Key element
            final CsdlPropertyRef propertyRef = new CsdlPropertyRef();
            propertyRef.setName("ID");

            // navigation property: one-to-many
            final CsdlNavigationProperty navProp = new CsdlNavigationProperty().setName("Products")
                    .setType(ET_PRODUCT_FQN).setCollection(true).setPartner("Category");
            final List<CsdlNavigationProperty> navPropList = new ArrayList<CsdlNavigationProperty>();
            navPropList.add(navProp);

            // configure EntityType
            entityType = new CsdlEntityType();
            entityType.setName(ET_CATEGORY_NAME);
            entityType.setProperties(Arrays.asList(id, name));
            entityType.setKey(Arrays.asList(propertyRef));
            entityType.setNavigationProperties(navPropList);
        } else if (entityTypeName.equals(ET_SUPPLIER_FQN)) {
            // create EntityType properties
            final CsdlProperty supplierId = new CsdlProperty().setName("SupplierID")
                    .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            final CsdlProperty companyName = new CsdlProperty().setName("CompanyName")
                    .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            final CsdlProperty fax = new CsdlProperty().setName("Fax")
                    .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            final CsdlProperty address = new CsdlProperty().setName(CT_ADDRESS_NAME)
                    .setType(CT_ADDRESS_FQN);

            // create PropertyRef for Key element
            final CsdlPropertyRef propertyRef = new CsdlPropertyRef();
            propertyRef.setName("SupplierID");

            // configure EntityType
            entityType = new CsdlEntityType();
            entityType.setName(ET_SUPPLIER_NAME);
            entityType.setProperties(Arrays.asList(supplierId, companyName, fax, address));
            entityType.setKey(Arrays.asList(propertyRef));
        }

        return entityType;
    }

    @Override
    public CsdlComplexType getComplexType(final FullQualifiedName complexTypeName) {
        CsdlComplexType complexType = null;
        if (complexTypeName.equals(CT_ADDRESS_FQN)) {
            complexType = new CsdlComplexType().setName(CT_ADDRESS_NAME)
                    .setProperties(Arrays.asList(
                            new CsdlProperty()
                                    .setName("City")
                                    .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                            new CsdlProperty()
                                    .setName("Country")
                                    .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())));
        }
        return complexType;
    }

    @Override
    public CsdlEntitySet getEntitySet(final FullQualifiedName entityContainer, final String entitySetName) throws ODataException {

        CsdlEntitySet entitySet = null;

        if (entityContainer.equals(CONTAINER)) {

            if (entitySetName.equals(ES_PRODUCTS_NAME)) {

                entitySet = new CsdlEntitySet();
                entitySet.setName(ES_PRODUCTS_NAME);
                entitySet.setType(ET_PRODUCT_FQN);

                // navigation
                final CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
                navPropBinding.setTarget("Categories"); // the target entity set, where the navigation property points to
                navPropBinding.setPath("Category"); // the path from entity type to navigation property
                final List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<CsdlNavigationPropertyBinding>();
                navPropBindingList.add(navPropBinding);
                entitySet.setNavigationPropertyBindings(navPropBindingList);

            } else if (entitySetName.equals(ES_CATEGORIES_NAME)) {

                entitySet = new CsdlEntitySet();
                entitySet.setName(ES_CATEGORIES_NAME);
                entitySet.setType(ET_CATEGORY_FQN);

                // navigation
                final CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();
                navPropBinding.setTarget("Products"); // the target entity set, where the navigation property points to
                navPropBinding.setPath("Products"); // the path from entity type to navigation property
                final List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<CsdlNavigationPropertyBinding>();
                navPropBindingList.add(navPropBinding);
                entitySet.setNavigationPropertyBindings(navPropBindingList);
            }
        }

        return entitySet;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
        // create EntitySets
        final List<CsdlEntitySet> entitySets = new ArrayList<CsdlEntitySet>();
        entitySets.add(getEntitySet(CONTAINER, ES_PRODUCTS_NAME));
        entitySets.add(getEntitySet(CONTAINER, ES_CATEGORIES_NAME));

        // create EntityContainer
        final CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        return entityContainer;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {

        // create Schema
        final CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);

        // add EntityTypes
        final List<CsdlEntityType> entityTypes = new ArrayList<CsdlEntityType>();
        entityTypes.add(getEntityType(ET_PRODUCT_FQN));
        entityTypes.add(getEntityType(ET_CATEGORY_FQN));
        entityTypes.add(getEntityType(ET_SUPPLIER_FQN));
        schema.setEntityTypes(entityTypes);

        // add Complex Types
        final List<CsdlComplexType> complexTypes = new ArrayList<CsdlComplexType>();
        complexTypes.add(getComplexType(CT_ADDRESS_FQN));
        schema.setComplexTypes(complexTypes);

        // add EntityContainer
        schema.setEntityContainer(getEntityContainer());

        // finally
        final List<CsdlSchema> schemas = new ArrayList<CsdlSchema>();
        schemas.add(schema);

        return schemas;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(final FullQualifiedName entityContainerName) throws ODataException {
        // This method is invoked when displaying the Service Document at e.g. http://localhost:8080/DemoService/DemoService.svc
        if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
            final CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(CONTAINER);
            return entityContainerInfo;
        }

        return null;
    }

}
