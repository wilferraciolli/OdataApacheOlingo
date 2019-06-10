/*
 * (c) Midland Software Limited 2019
 * Name     : DemoEdmProvider.java
 * Author   : ferraciolliw
 * Date     : 10 Jun 2019
 */
package com.wiltech.odata.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;

/**
 * The type Demo edm provider. Entity Data model provider to provide metadata for the client.
 */
public class DemoEdmProvider extends CsdlAbstractEdmProvider {

    /**
     * The constant NAMESPACE.
     */
    // Service Namespace
    public static final String NAMESPACE = "OData.Demo";

    /**
     * The constant CONTAINER_NAME.
     */
    // EDM Container
    public static final String CONTAINER_NAME = "Container";
    /**
     * The constant CONTAINER.
     */
    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    /**
     * The constant ET_PRODUCT_NAME.
     */
    // Entity Types Names
    public static final String ET_PRODUCT_NAME = "Product";
    /**
     * The constant ET_PRODUCT_FQN.
     */
    public static final FullQualifiedName ET_PRODUCT_FQN = new FullQualifiedName(NAMESPACE, ET_PRODUCT_NAME);

    /**
     * The constant ES_PRODUCTS_NAME.
     */
    // Entity Set Names
    public static final String ES_PRODUCTS_NAME = "Products";

    /**
     * In our example service, we want to provide a list of products to users who call the OData service. The user of our service, for example an app-developer, may ask: What does such a "product" entry look like? How is it structured? Which information about a product is provided? For example, the name of it and which data types can be expected from these properties? Such information is provided by a CsdlEdmProvider (and for convenience we extend the CsdlAbstractEdmProvider).
     * In our example service, for modelling the CsdlEntityType, we have to provide the following metadata:
     * The name of the EntityType: “Product” The properties: name and type and additional info, e.g. “ID” of type “edm.int32” Which of the properties is the “key” property: a reference to the “ID” property.
     * @param entityTypeName
     * @return
     * @throws ODataException
     */
    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
        // this method is called for one of the EntityTypes that are configured in the Schema
        if (entityTypeName.equals(ET_PRODUCT_FQN)) {

            //create EntityType properties
            CsdlProperty id = new CsdlProperty().setName("ID").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty name = new CsdlProperty().setName("Name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty description = new CsdlProperty().setName("Description").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

            // create CsdlPropertyRef for Key element
            CsdlPropertyRef propertyRef = new CsdlPropertyRef();
            propertyRef.setName("ID");

            // configure EntityType
            CsdlEntityType entityType = new CsdlEntityType();
            entityType.setName(ET_PRODUCT_NAME);
            entityType.setProperties(Arrays.asList(id, name, description));
            entityType.setKey(Collections.singletonList(propertyRef));

            return entityType;
        }

        return null;
    }

    /**
     * The procedure for declaring the Entity Sets is similar. An EntitySet is a crucial resource, when an OData service is used to request data. In our example, we will invoke the following URL, which we expect to provide us a list of products:
     * http://localhost:8080/DemoService/DemoServlet.svc/Products
     * When declaring an EntitySet, we need to define the type of entries which are contained in the list, such as an CsdlEntityType. In our example, we set our previously created CsdlEntityType, which is referred by a FullQualifiedName.
     * @param entityContainer
     * @param entitySetName
     * @return
     * @throws ODataException
     */
    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {
        if (entityContainer.equals(CONTAINER)) {
            if (entitySetName.equals(ES_PRODUCTS_NAME)) {
                CsdlEntitySet entitySet = new CsdlEntitySet();
                entitySet.setName(ES_PRODUCTS_NAME);
                entitySet.setType(ET_PRODUCT_FQN);

                return entitySet;
            }
        }

        return null;
    }

    /**
     * In order to provide data, our OData service needs an EntityContainer that carries the EntitySets. In our example, we have only one EntitySet, so we create one EntityContainer and set our EntitySet.
     * @return
     * @throws ODataException
     */
    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {

        // create EntitySets
        List<CsdlEntitySet> entitySets = new ArrayList<CsdlEntitySet>();
        entitySets.add(getEntitySet(CONTAINER, ES_PRODUCTS_NAME));

        // create EntityContainer
        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        return entityContainer;
    }

    /**
     * Up to this point, we have declared the type of our data (CsdlEntityType) and our list (CsdlEntitySet), and we have put it into a container (CsdlEntityContainer). Now we are required to put all these elements into a CsdlSchema. While the model of an OData service can have several schemas, in most cases there will probably be only one schema. So, in our example, we create a list of schemas, where we add one new CsdlSchema object. The schema is configured with a Namespace, which serves to uniquely identify all elements. Then our elements are added to the Schema.
     * @return
     * @throws ODataException
     */
    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {
        // create Schema
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);

        // add EntityTypes
        List<CsdlEntityType> entityTypes = new ArrayList<CsdlEntityType>();
        entityTypes.add(getEntityType(ET_PRODUCT_FQN));
        schema.setEntityTypes(entityTypes);

        // add EntityContainer
        schema.setEntityContainer(getEntityContainer());

        // finally
        List<CsdlSchema> schemas = new ArrayList<CsdlSchema>();
        schemas.add(schema);

        return schemas;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {
        // This method is invoked when displaying the Service Document at e.g. http://localhost:8080/DemoService/DemoService.svc
        if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
            CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(CONTAINER);
            return entityContainerInfo;
        }

        return null;
    }

}
