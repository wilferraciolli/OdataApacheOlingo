/*
 * (c) Midland Software Limited 2019
 * Name     : DemoEntityProcessor.java
 * Author   : ferraciolliw
 * Date     : 11 Jun 2019
 */
package com.wiltech.odata.service;

import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.ContextURL.Suffix;
import org.apache.olingo.commons.api.data.Entity;
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
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
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
 * The type Demo entity processor. This class will be called when odata is queried by id. Eg http://localhost:8080/DemoService/DemoService.svc/Products(3)
 */
public class DemoEntityProcessor implements EntityProcessor {

    private OData odata;
    private ServiceMetadata srvMetadata;
    private final Storage storage;

    public DemoEntityProcessor(final Storage storage) {
        this.storage = storage;
    }

    public void init(final OData odata, final ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.srvMetadata = serviceMetadata;
    }

    /**
     * This method is invoked when a single entity has to be read.
     * In our example, this can be either a "normal" read operation, or a navigation:
     * Example for "normal" read operation:
     * http://localhost:8080/DemoService/DemoService.svc/Products(1)
     * Example for navigation
     * http://localhost:8080/DemoService/DemoService.svc/Products(1)/Category
     */
    public void readEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo, final ContentType responseFormat)
            throws ODataApplicationException, SerializerException {

        EdmEntityType responseEdmEntityType = null; // we'll need this to build the ContextURL
        Entity responseEntity = null; // required for serialization of the response body
        EdmEntitySet responseEdmEntitySet = null; // we need this for building the contextUrl

        // 1st step: retrieve the requested Entity: can be "normal" read operation, or navigation (to-one)
        final List<UriResource> resourceParts = uriInfo.getUriResourceParts();
        final int segmentCount = resourceParts.size();

        final UriResource uriResource = resourceParts.get(0); // in our example, the first segment is the EntitySet
        if (!(uriResource instanceof UriResourceEntitySet)) {
            throw new ODataApplicationException("Only EntitySet is supported",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }

        final UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriResource;
        final EdmEntitySet startEdmEntitySet = uriResourceEntitySet.getEntitySet();

        // Analyze the URI segments
        if (segmentCount == 1) { // no navigation
            responseEdmEntityType = startEdmEntitySet.getEntityType();
            responseEdmEntitySet = startEdmEntitySet; // since we have only one segment

            // 2. step: retrieve the data from backend
            final List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
            responseEntity = storage.readEntityData(startEdmEntitySet, keyPredicates);
        } else if (segmentCount == 2) { // navigation
            final UriResource navSegment = resourceParts.get(1); // in our example we don't support more complex URIs
            if (navSegment instanceof UriResourceNavigation) {
                final UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) navSegment;
                final EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
                responseEdmEntityType = edmNavigationProperty.getType();
                if (!edmNavigationProperty.containsTarget()) {
                    // contextURL displays the last segment
                    responseEdmEntitySet = Util.getNavigationTargetEntitySet(startEdmEntitySet, edmNavigationProperty);
                } else {
                    responseEdmEntitySet = startEdmEntitySet;
                }

                // 2nd: fetch the data from backend.
                // e.g. for the URI: Products(1)/Category we have to find the correct Category entity
                final List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
                // e.g. for Products(1)/Category we have to find first the Products(1)
                final Entity sourceEntity = storage.readEntityData(startEdmEntitySet, keyPredicates);

                // now we have to check if the navigation is
                // a) to-one: e.g. Products(1)/Category
                // b) to-many with key: e.g. Categories(3)/Products(5)
                // the key for nav is used in this case: Categories(3)/Products(5)
                final List<UriParameter> navKeyPredicates = uriResourceNavigation.getKeyPredicates();

                if (navKeyPredicates.isEmpty()) { // e.g. DemoService.svc/Products(1)/Category
                    responseEntity = storage.getRelatedEntity(sourceEntity, responseEdmEntityType);
                } else { // e.g. DemoService.svc/Categories(3)/Products(5)
                    responseEntity = storage.getRelatedEntity(sourceEntity, responseEdmEntityType, navKeyPredicates);
                }
            }
        } else {
            // this would be the case for e.g. Products(1)/Category/Products(1)/Category
            throw new ODataApplicationException("Not supported", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        if (responseEntity == null) {
            // this is the case for e.g. DemoService.svc/Categories(4) or DemoService.svc/Categories(3)/Products(999)
            throw new ODataApplicationException("Nothing found.", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
        }

        // 3. serialize
        ContextURL contextUrl = null;
        if (isContNav(uriInfo)) {
            contextUrl = ContextURL.with().entitySetOrSingletonOrType(request.getRawODataPath()).
                    suffix(Suffix.ENTITY).build();
        } else {
            contextUrl = ContextURL.with().entitySet(responseEdmEntitySet).suffix(Suffix.ENTITY).build();
        }

        final EntitySerializerOptions opts = EntitySerializerOptions.with().contextURL(contextUrl).build();

        final ODataSerializer serializer = this.odata.createSerializer(responseFormat);
        final SerializerResult serializerResult = serializer.entity(this.srvMetadata,
                responseEdmEntityType, responseEntity, opts);

        // 4. configure the response object
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

    /*
     * These processor methods are not handled in this tutorial
     */

    public void createEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
            final ContentType requestFormat, final ContentType responseFormat)
            throws ODataApplicationException, DeserializerException, SerializerException {
        throw new ODataApplicationException("Not supported.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }

    public void updateEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo,
            final ContentType requestFormat, final ContentType responseFormat)
            throws ODataApplicationException, DeserializerException, SerializerException {
        throw new ODataApplicationException("Not supported.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }

    public void deleteEntity(final ODataRequest request, final ODataResponse response, final UriInfo uriInfo)
            throws ODataApplicationException {
        throw new ODataApplicationException("Not supported.", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
    }
}
