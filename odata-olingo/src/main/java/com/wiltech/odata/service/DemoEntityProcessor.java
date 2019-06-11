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
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

import com.wiltech.odata.data.Storage;

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
     * Initiate component
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

    }

    public void updateEntity(
            final ODataRequest request, final ODataResponse response, final UriInfo uriInfo, final ContentType requestFormat,
            final ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

    }

    public void deleteEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo)
            throws ODataApplicationException, ODataLibraryException {

    }
}
