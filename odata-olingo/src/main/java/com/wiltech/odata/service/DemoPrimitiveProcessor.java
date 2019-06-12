/*
 * (c) Midland Software Limited 2019
 * Name     : DemoPrimitiveProcessor.java
 * Author   : ferraciolliw
 * Date     : 11 Jun 2019
 */
package com.wiltech.odata.service;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.processor.PrimitiveProcessor;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.PrimitiveSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceProperty;

import com.wiltech.odata.data.Storage;

/**
 * The type Demo primitive processor.
 */
public class DemoPrimitiveProcessor implements PrimitiveProcessor {

    private OData odata;
    private final Storage storage;
    private ServiceMetadata serviceMetadata;

    public DemoPrimitiveProcessor(final Storage storage) {
        this.storage = storage;
    }

    public void init(final OData odata, final ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;

    }

    /*
     * In our example, the URL would be: http://localhost:8080/DemoService/DemoService.svc/Products(1)/Name
     * and the response:
     * {
     *
     * @odata.context: "$metadata#Products/Name",
     * value: "Notebook Basic 15"
     * }
     */
    public void readPrimitive(final ODataRequest request, final ODataResponse response,
            final UriInfo uriInfo, final ContentType responseFormat)
            throws ODataApplicationException, SerializerException {

        // 1. Retrieve info from URI
        // 1.1. retrieve the info about the requested entity set
        final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
        // Note: only in our example we can rely that the first segment is the EntitySet
        final UriResourceEntitySet uriEntityset = (UriResourceEntitySet) resourceParts.get(0);
        final EdmEntitySet edmEntitySet = uriEntityset.getEntitySet();
        // the key for the entity
        final List<UriParameter> keyPredicates = uriEntityset.getKeyPredicates();

        // 1.2. retrieve the requested (Edm) property
        // the last segment is the Property
        final UriResourceProperty uriProperty = (UriResourceProperty) resourceParts.get(resourceParts.size() - 1);
        final EdmProperty edmProperty = uriProperty.getProperty();
        final String edmPropertyName = edmProperty.getName();
        // in our example, we know we have only primitive types in our model
        final EdmPrimitiveType edmPropertyType = (EdmPrimitiveType) edmProperty.getType();

        // 2. retrieve data from backend
        // 2.1. retrieve the entity data, for which the property has to be read
        final Entity entity = storage.readEntityData(edmEntitySet, keyPredicates);
        if (entity == null) { // Bad request
            throw new ODataApplicationException("Entity not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        // 2.2. retrieve the property data from the entity
        final Property property = entity.getProperty(edmPropertyName);
        if (property == null) {
            throw new ODataApplicationException("Property not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        // 3. serialize
        final Object value = property.getValue();
        if (value != null) {
            // 3.1. configure the serializer
            final ODataSerializer serializer = odata.createSerializer(responseFormat);

            final ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).navOrPropertyPath(edmPropertyName).build();
            final PrimitiveSerializerOptions options = PrimitiveSerializerOptions.with().contextURL(contextUrl).build();
            // 3.2. serialize
            final SerializerResult serializerResult = serializer.primitive(serviceMetadata, edmPropertyType, property, options);
            final InputStream propertyStream = serializerResult.getContent();

            // 4. configure the response object
            response.setContent(propertyStream);
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
            response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
        } else {
            // in case there's no value for the property, we can skip the serialization
            response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
        }
    }

    /*
     * These processor methods are not handled in this tutorial
     */

    public void updatePrimitive(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo, final ContentType requestFormat,
            final ContentType responseFormat)
            throws ODataApplicationException, DeserializerException, SerializerException {
        throw new ODataApplicationException("Not supported.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }

    public void deletePrimitive(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo)
            throws ODataApplicationException {
        throw new ODataApplicationException("Not supported.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }
}
