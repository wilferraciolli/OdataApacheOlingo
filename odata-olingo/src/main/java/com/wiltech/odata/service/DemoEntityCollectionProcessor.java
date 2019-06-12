/*
 * (c) Midland Software Limited 2019
 * Name     : DemoEntityCollectionProcessor.java
 * Author   : ferraciolliw
 * Date     : 10 Jun 2019
 */
package com.wiltech.odata.service;

import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;

import com.wiltech.odata.data.Storage;
import com.wiltech.odata.utils.Util;

/**
 * The type Demo entity collection processor. Class used to map metadata and data for a request.
 */
public class DemoEntityCollectionProcessor implements EntityCollectionProcessor {

    private OData odata;
    private ServiceMetadata srvMetadata;
    // our database-mock
    private final Storage storage;

    public DemoEntityCollectionProcessor(final Storage storage) {
        this.storage = storage;
    }

    public void init(final OData odata, final ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.srvMetadata = serviceMetadata;
    }

    /*
     * This method is invoked when a collection of entities has to be read.
     * In our example, this can be either a "normal" read operation, or a navigation:
     *
     * Example for "normal" read entity set operation:
     * http://localhost:8080/DemoService/DemoService.svc/Categories
     *
     * Example for navigation
     * http://localhost:8080/DemoService/DemoService.svc/Categories(3)/Products
     */
    public void readEntityCollection(final ODataRequest request, final ODataResponse response,
            final UriInfo uriInfo, final ContentType responseFormat)
            throws ODataApplicationException, SerializerException {

        EdmEntitySet responseEdmEntitySet = null; // we'll need this to build the ContextURL
        EntityCollection responseEntityCollection = null; // we'll need this to set the response body
        EdmEntityType responseEdmEntityType = null;

        // 1st retrieve the requested EntitySet from the uriInfo (representation of the parsed URI)
        final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
        final int segmentCount = resourceParts.size();

        final UriResource uriResource = resourceParts.get(0); // in our example, the first segment is the EntitySet
        if (!(uriResource instanceof UriResourceEntitySet)) {
            throw new ODataApplicationException("Only EntitySet is supported",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        final UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriResource;
        final EdmEntitySet startEdmEntitySet = uriResourceEntitySet.getEntitySet();

        if (segmentCount == 1) { // this is the case for: DemoService/DemoService.svc/Categories
            responseEdmEntitySet = startEdmEntitySet; // the response body is built from the first (and only) entitySet

            // 2nd: fetch the data from backend for this requested EntitySetName and deliver as EntitySet
            responseEntityCollection = storage.readEntitySetData(startEdmEntitySet);
        } else if (segmentCount == 2) { // in case of navigation: DemoService.svc/Categories(3)/Products

            final UriResource lastSegment = resourceParts.get(1); // in our example we don't support more complex URIs
            if (lastSegment instanceof UriResourceNavigation) {
                final UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
                final EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
                final EdmEntityType targetEntityType = edmNavigationProperty.getType();
                if (!edmNavigationProperty.containsTarget()) {
                    // from Categories(1) to Products
                    responseEdmEntitySet = Util.getNavigationTargetEntitySet(startEdmEntitySet, edmNavigationProperty);
                } else {
                    responseEdmEntitySet = startEdmEntitySet;
                    responseEdmEntityType = targetEntityType;
                }

                // 2nd: fetch the data from backend
                // first fetch the entity where the first segment of the URI points to
                final List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
                // e.g. for Categories(3)/Products we have to find the single entity: Category with ID 3
                final Entity sourceEntity = storage.readEntityData(startEdmEntitySet, keyPredicates);
                // error handling for e.g. DemoService.svc/Categories(99)/Products
                if (sourceEntity == null) {
                    throw new ODataApplicationException("Entity not found.",
                            HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
                }
                // then fetch the entity collection where the entity navigates to
                // note: we don't need to check uriResourceNavigation.isCollection(),
                // because we are the EntityCollectionProcessor
                responseEntityCollection = storage.getRelatedEntityCollection(sourceEntity, targetEntityType);
            }
        } else { // this would be the case for e.g. Products(1)/Category/Products
            throw new ODataApplicationException("Not supported",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        ContextURL contextUrl = null;
        EdmEntityType edmEntityType = null;
        // 3rd: create and configure a serializer
        if (isContNav(uriInfo)) {
            contextUrl = ContextURL.with().entitySetOrSingletonOrType(request.getRawODataPath()).build();
            edmEntityType = responseEdmEntityType;
        } else {
            contextUrl = ContextURL.with().entitySet(responseEdmEntitySet).build();
            edmEntityType = responseEdmEntitySet.getEntityType();
        }
        final String id = request.getRawBaseUri() + "/" + responseEdmEntitySet.getName();
        final EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with()
                .contextURL(contextUrl).id(id).build();

        final ODataSerializer serializer = odata.createSerializer(responseFormat);
        final SerializerResult serializerResult = serializer.entityCollection(this.srvMetadata, edmEntityType,
                responseEntityCollection, opts);

        // 4th: configure the response object: set the body, headers and status code
        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    private boolean isContNav(final UriInfo uriInfo) {
        final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
        for (final UriResource resourcePart : resourceParts) {
            if (resourcePart instanceof UriResourceNavigation) {
                final UriResourceNavigation navResource = (UriResourceNavigation) resourcePart;
                if (navResource.getProperty().containsTarget()) {
                    return true;
                }
            }
        }
        return false;
    }
}
