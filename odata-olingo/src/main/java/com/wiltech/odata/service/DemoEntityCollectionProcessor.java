/*
 * (c) Midland Software Limited 2019
 * Name     : DemoEntityCollectionProcessor.java
 * Author   : ferraciolliw
 * Date     : 10 Jun 2019
 */
package com.wiltech.odata.service;

import java.io.InputStream;
import java.util.List;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.EntityCollection;
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
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

import com.wiltech.odata.data.Storage;

/**
 * The type Demo entity collection processor. Class used to map metadata and data for a request.
 */
public class DemoEntityCollectionProcessor implements EntityCollectionProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;
    private final Storage storage;

    public DemoEntityCollectionProcessor(final Storage storage) {
        this.storage = storage;
    }

    /**
     * This method is common to all processor interfaces. The Olingo framework initializes the processor with an instance of the OData object.
     * According to the Javadoc, this object is the “Root object for serving factory tasks…” We will need it later, so we store it as member variable.
     * @param oData
     * @param serviceMetadata
     */
    public void init(final OData oData, final ServiceMetadata serviceMetadata) {
        this.odata = oData;
        this.serviceMetadata = serviceMetadata;
    }

    /**
     * The EntityCollectionProcessor exposes only one method: readEntityCollection(...)
     * Here we have to understand that this readEntityCollection(...)-method is invoked, when the OData service is called with an HTTP GET operation for an entity collection.
     * The readEntityCollection(...) method is used to “read” the data in the backend (this can be e.g. a database) and to deliver it to the user who calls the OData service.
     * The method signature:
     * The “request” parameter contains raw HTTP information. It is typically used for creation scenario, where a request body is sent along with the request.
     * With the second parameter, the “response” object is passed to our method in order to carry the response data. So here we have to set the response body, along with status code and content-type header.
     * The third parameter, the “uriInfo”, contains information about the relevant part of the URL. This means, the segments starting after the service name.
     * Example: If the user calls the following URL: http://localhost:8080/DemoService/DemoService.svc/Products The readEntityCollection(...) method is invoked and the uriInfo object contains one segment: “Products”
     * @param oDataRequest
     * @param oDataResponse
     * @param uriInfo
     * @param responseFormat
     * @throws ODataApplicationException
     * @throws ODataLibraryException
     */
    public void readEntityCollection(final ODataRequest oDataRequest, final ODataResponse oDataResponse, final UriInfo uriInfo,
            final ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        // 1st we have retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)
        final List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        final UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths
                .get(0); // in our example, the first segment is the EntitySet
        final EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        // 2nd: fetch the data from backend for this requested EntitySetName
        // it has to be delivered as EntitySet object
        final EntityCollection entitySet = storage.readEntitySetData(edmEntitySet);

        // 3rd: create a serializer based on the requested format (json)
        final ODataSerializer serializer = odata.createSerializer(responseFormat);

        // 4th: Now serialize the content: transform from the EntitySet object to InputStream
        final EdmEntityType edmEntityType = edmEntitySet.getEntityType();
        final ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

        final String id = oDataRequest.getRawBaseUri() + "/" + edmEntitySet.getName();
        final EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().id(id).contextURL(contextUrl).build();
        final SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, entitySet, opts);
        final InputStream serializedContent = serializerResult.getContent();

        // Finally: configure the response object: set the body, headers and status code
        oDataResponse.setContent(serializedContent);
        oDataResponse.setStatusCode(HttpStatusCode.OK.getStatusCode());
        oDataResponse.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }
}
