/*
 * (c) Midland Software Limited 2019
 * Name     : DemoEntityProcessor.java
 * Author   : ferraciolliw
 * Date     : 11 Jun 2019
 */
package com.wiltech.odata.service;

import java.io.InputStream;
import java.util.List;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

import com.wiltech.odata.data.Storage;
import com.wiltech.odata.utils.Util;

/**
 * The type Demo entity processor. This class will be called when odata is queried by id. Eg http://localhost:8080/DemoService/DemoService.svc/Products(3)
 */
public class DemoEntityProcessor implements EntityProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;
    private final Storage storage;

    /**
     * Instantiates a new Demo entity processor.
     * @param storage the storage
     */
    public DemoEntityProcessor(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Initiate component. Here we are initialized by the Framework to pass the context objects to us
     * @param odata
     * @param serviceMetadata
     */
    public void init(final OData odata, final ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    /**
     * Read entity. For given ids
     * @param request
     * @param response
     * @param uriInfo
     * @param responseFormat
     * @throws ODataApplicationException
     * @throws ODataLibraryException
     */
    public void readEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo, final ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        // 1. retrieve the Entity Type
        final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        // Note: only in our example we can assume that the first segment is the EntitySet
        final UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        final EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        // 2. retrieve the data from backend
        final List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        final Entity entity = storage.readEntityData(edmEntitySet, keyPredicates);

        // 3. serialize
        final EdmEntityType entityType = edmEntitySet.getEntityType();

        final ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();
        // expand and select currently not supported
        final EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl).build();

        final ODataSerializer serializer = odata.createSerializer(responseFormat);
        final SerializerResult serializerResult = serializer.entity(serviceMetadata, entityType, entity, options);
        final InputStream entityStream = serializerResult.getContent();

        //4. configure the response object
        response.setContent(entityStream);
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    public void createEntity(
            final ODataRequest request, final ODataResponse response, final UriInfo uriInfo, final ContentType requestFormat,
            final ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        // 1. Retrieve the entity type from the URI
        final EdmEntitySet edmEntitySet = Util.getEdmEntitySet(uriInfo);
        final EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // 2. create the data in backend
        // 2.1. retrieve the payload from the POST request for the entity to create and deserialize it
        final InputStream requestInputStream = request.getBody();
        final ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
        final DeserializerResult result = deserializer.entity(requestInputStream, edmEntityType);
        final Entity requestEntity = result.getEntity();
        // 2.2 do the creation in backend, which returns the newly created entity
        final Entity createdEntity = storage.createEntityData(edmEntitySet, requestEntity);

        // 3. serialize the response (we have to return the created entity)
        final ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();
        // expand and select currently not supported
        final EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl).build();

        final ODataSerializer serializer = this.odata.createSerializer(responseFormat);
        final SerializerResult serializedResponse = serializer.entity(serviceMetadata, edmEntityType, createdEntity, options);

        //4. configure the response object
        response.setContent(serializedResponse.getContent());
        response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    public void updateEntity(
            final ODataRequest request, final ODataResponse response, final UriInfo uriInfo, final ContentType requestFormat,
            final ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        // 1. Retrieve the entity set which belongs to the requested entity
        final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        // Note: only in our example we can assume that the first segment is the EntitySet
        final UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        final EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
        final EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // 2. update the data in backend
        // 2.1. retrieve the payload from the PUT request for the entity to be updated
        final InputStream requestInputStream = request.getBody();
        final ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
        final DeserializerResult result = deserializer.entity(requestInputStream, edmEntityType);
        final Entity requestEntity = result.getEntity();
        // 2.2 do the modification in backend
        final List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        // Note that this updateEntity()-method is invoked for both PUT or PATCH operations
        final HttpMethod httpMethod = request.getMethod();
        storage.updateEntityData(edmEntitySet, keyPredicates, requestEntity, httpMethod);

        //3. configure the response object
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    public void deleteEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo)
            throws ODataApplicationException, ODataLibraryException {

        // 1. Retrieve the entity set which belongs to the requested entity
        final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        // Note: only in our example we can assume that the first segment is the EntitySet
        final UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        final EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        // 2. delete the data in backend
        final List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        storage.deleteEntityData(edmEntitySet, keyPredicates);

        //3. configure the response object
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }
}
