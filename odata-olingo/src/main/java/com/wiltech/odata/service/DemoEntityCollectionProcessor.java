/*
 * (c) Midland Software Limited 2019
 * Name     : DemoEntityCollectionProcessor.java
 * Author   : ferraciolliw
 * Date     : 10 Jun 2019
 */
package com.wiltech.odata.service;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
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

/**
 * The type Demo entity collection processor. Class used to map metada and data for a request.
 */
public class DemoEntityCollectionProcessor implements EntityCollectionProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;

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
        final EntityCollection entitySet = getData(edmEntitySet);

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

    /**
     * We have not elaborated on fetching the actual data. In our tutorial, to keep the code as simple as possible, we use a little helper method that delivers some hardcoded entries. Since we are supposed to deliver the data inside an EntityCollection instance, we create the instance, ask it for the (initially empty) list of entities and add some new entities to it. We create the entities and their properties according to what we declared in our DemoEdmProvider class. So we have to take care to provide the correct names to the new property objects. If a client requests the response in ATOM format, each entity have to provide it`s own entity id. The method createId allows us to create an id in a convenient way.
     * @param edmEntitySet the edm entity set
     * @return the entity collection
     */
    private EntityCollection getData(final EdmEntitySet edmEntitySet) {

        final EntityCollection productsCollection = new EntityCollection();
        // check for which EdmEntitySet the data is requested
        if (DemoEdmProvider.ES_PRODUCTS_NAME.equals(edmEntitySet.getName())) {
            final List<Entity> productList = productsCollection.getEntities();

            // add some sample product entities
            final Entity e1 = new Entity()
                    .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 1))
                    .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Notebook Basic 15"))
                    .addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
                            "Notebook Basic, 1.7GHz - 15 XGA - 1024MB DDR2 SDRAM - 40GB"));
            e1.setId(createId("Products", 1));
            productList.add(e1);

            final Entity e2 = new Entity()
                    .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 2))
                    .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "1UMTS PDA"))
                    .addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
                            "Ultrafast 3G UMTS/HSDPA Pocket PC, supports GSM network"));
            e2.setId(createId("Products", 1));
            productList.add(e2);

            final Entity e3 = new Entity()
                    .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 3))
                    .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Ergo Screen"))
                    .addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
                            "19 Optimum Resolution 1024 x 768 @ 85Hz, resolution 1280 x 960"));
            e3.setId(createId("Products", 1));
            productList.add(e3);
        }

        return productsCollection;
    }

    /**
     * Create id uri.
     * @param entitySetName the entity set name
     * @param id the id
     * @return the uri
     */
    private URI createId(final String entitySetName, final Object id) {
        try {
            return new URI(entitySetName + "(" + String.valueOf(id) + ")");
        } catch (final URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }
}
