/*
 * (c) Midland Software Limited 2019
 * Name     : Util.java
 * Author   : ferraciolliw
 * Date     : 11 Jun 2019
 */
package com.wiltech.odata.utils;

import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

public class Util {

    public static Entity findEntity(final EdmEntityType edmEntityType, final EntityCollection entitySet,
            final List<UriParameter> keyParams) {

        final List<Entity> entityList = entitySet.getEntities();

        // loop over all entities in order to find that one that matches
        // all keys in request e.g. contacts(ContactID=1, CompanyID=1)
        for (final Entity entity : entityList) {
            final boolean foundEntity = entityMatchesAllKeys(edmEntityType, entity, keyParams);
            if (foundEntity) {
                return entity;
            }
        }

        return null;
    }

    public static boolean entityMatchesAllKeys(final EdmEntityType edmEntityType, final Entity rt_entity,
            final List<UriParameter> keyParams) {

        // loop over all keys
        for (final UriParameter key : keyParams) {
            // key
            final String keyName = key.getName();
            final String keyText = key.getText();

            // note: below line doesn't consider: keyProp can be part of a complexType in V4
            // in such case, it would be required to access it via getKeyPropertyRef()
            // but since this isn't the case in our model, we ignore it in our implementation
            final EdmProperty edmKeyProperty = (EdmProperty) edmEntityType.getProperty(keyName);
            // Edm: we need this info for the comparison below
            final Boolean isNullable = edmKeyProperty.isNullable();
            final Integer maxLength = edmKeyProperty.getMaxLength();
            final Integer precision = edmKeyProperty.getPrecision();
            final Boolean isUnicode = edmKeyProperty.isUnicode();
            final Integer scale = edmKeyProperty.getScale();
            // get the EdmType in order to compare
            final EdmType edmType = edmKeyProperty.getType();
            // if(EdmType instanceof EdmPrimitiveType) // do we need this?
            final EdmPrimitiveType edmPrimitiveType = (EdmPrimitiveType) edmType;

            // Runtime data: the value of the current entity
            // don't need to check for null, this is done in FWK
            final Object valueObject = rt_entity.getProperty(keyName).getValue();
            // TODO if the property is a complex type

            // now need to compare the valueObject with the keyText String
            // this is done using the type.valueToString
            String valueAsString = null;
            try {
                valueAsString = edmPrimitiveType.valueToString(valueObject, isNullable,
                        maxLength, precision, scale, isUnicode);
            } catch (final EdmPrimitiveTypeException e) {
                return false; // TODO proper Exception handling
            }

            if (valueAsString == null) {
                return false;
            }

            final boolean matches = valueAsString.equals(keyText);
            if (matches) {
                // if the given key value is found in the current entity, continue with the next key
                continue;
            } else {
                // if any of the key properties is not found in the entity, we don't need to search further
                return false;
            }
        }

        return true;
    }

    /**
     * Example:
     * For the following navigation: DemoService.svc/Categories(1)/Products
     * we need the EdmEntitySet for the navigation property "Products"
     * This is defined as follows in the metadata:
     * <code>
     * <EntitySet Name="Categories" EntityType="OData.Demo.Category">
     * <NavigationPropertyBinding Path="Products" Target="Products"/>
     * </EntitySet>
     * </code>
     * The "Target" attribute specifies the target EntitySet
     * Therefore we need the startEntitySet "Categories" in order to retrieve the target EntitySet "Products"
     */
    public static EdmEntitySet getNavigationTargetEntitySet(final EdmEntitySet startEdmEntitySet,
            final EdmNavigationProperty edmNavigationProperty)
            throws ODataApplicationException {

        EdmEntitySet navigationTargetEntitySet = null;

        final String navPropName = edmNavigationProperty.getName();
        final EdmBindingTarget edmBindingTarget = startEdmEntitySet.getRelatedBindingTarget(navPropName);
        if (edmBindingTarget == null) {
            throw new ODataApplicationException("Not supported.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        if (edmBindingTarget instanceof EdmEntitySet) {
            navigationTargetEntitySet = (EdmEntitySet) edmBindingTarget;
        } else {
            throw new ODataApplicationException("Not supported.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        return navigationTargetEntitySet;
    }

}
