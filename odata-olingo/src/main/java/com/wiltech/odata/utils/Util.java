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
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

/**
 * The type Util.
 */
public class Util {

    /**
     * Gets edm entity set.
     * @param uriInfo the uri info
     * @return the edm entity set
     * @throws ODataApplicationException the o data application exception
     */
    public static EdmEntitySet getEdmEntitySet(final UriInfoResource uriInfo) throws ODataApplicationException {

        final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        // To get the entity set we have to interpret all URI segments
        if (!(resourcePaths.get(0) instanceof UriResourceEntitySet)) {
            throw new ODataApplicationException("Invalid resource type for first segment.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }

        final UriResourceEntitySet uriResource = (UriResourceEntitySet) resourcePaths.get(0);

        return uriResource.getEntitySet();
    }

    /**
     * Find entity entity.
     * @param edmEntityType the edm entity type
     * @param rt_entitySet the rt entity set
     * @param keyParams the key params
     * @return the entity
     * @throws ODataApplicationException the o data application exception
     */
    public static Entity findEntity(final EdmEntityType edmEntityType,
            final EntityCollection rt_entitySet, final List<UriParameter> keyParams)
            throws ODataApplicationException {

        final List<Entity> entityList = rt_entitySet.getEntities();

        // loop over all entities in order to find that one that matches all keys in request
        // an example could be e.g. contacts(ContactID=1, CompanyID=1)
        for (final Entity rt_entity : entityList) {
            final boolean foundEntity = entityMatchesAllKeys(edmEntityType, rt_entity, keyParams);
            if (foundEntity) {
                return rt_entity;
            }
        }

        return null;
    }

    /**
     * Entity matches all keys boolean.
     * @param edmEntityType the edm entity type
     * @param rt_entity the rt entity
     * @param keyParams the key params
     * @return the boolean
     * @throws ODataApplicationException the o data application exception
     */
    public static boolean entityMatchesAllKeys(final EdmEntityType edmEntityType, final Entity rt_entity, final List<UriParameter> keyParams)
            throws ODataApplicationException {

        // loop over all keys
        for (final UriParameter key : keyParams) {
            // key
            final String keyName = key.getName();
            final String keyText = key.getText();

            // Edm: we need this info for the comparison below
            final EdmProperty edmKeyProperty = (EdmProperty) edmEntityType.getProperty(keyName);
            final Boolean isNullable = edmKeyProperty.isNullable();
            final Integer maxLength = edmKeyProperty.getMaxLength();
            final Integer precision = edmKeyProperty.getPrecision();
            final Boolean isUnicode = edmKeyProperty.isUnicode();
            final Integer scale = edmKeyProperty.getScale();
            // get the EdmType in order to compare
            final EdmType edmType = edmKeyProperty.getType();
            // Key properties must be instance of primitive type
            final EdmPrimitiveType edmPrimitiveType = (EdmPrimitiveType) edmType;

            // Runtime data: the value of the current entity
            final Object valueObject = rt_entity.getProperty(keyName).getValue(); // null-check is done in FWK

            // now need to compare the valueObject with the keyText String
            // this is done using the type.valueToString //
            String valueAsString = null;
            try {
                valueAsString = edmPrimitiveType.valueToString(valueObject, isNullable, maxLength,
                        precision, scale, isUnicode);
            } catch (final EdmPrimitiveTypeException e) {
                throw new ODataApplicationException("Failed to retrieve String value",
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH, e);
            }

            if (valueAsString == null) {
                return false;
            }

            final boolean matches = valueAsString.equals(keyText);
            if (!matches) {
                // if any of the key properties is not found in the entity, we don't need to search further
                return false;
            }
        }

        return true;
    }
}
