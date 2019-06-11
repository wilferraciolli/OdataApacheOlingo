This tutorial can be found here
https://www.baeldung.com/odata

At the core of the OData protocol is the concept of an Entity Data Model – or EDM for short. The EDM describes the data exposed by an OData provider through a metadata document containing a number of meta-entities:

Entity type and its properties (e.g. Person, Customer, Order, etc) and keys
Relationships between entities
Complex types used to describe structured types embedded into entities (say, an address type which is part of a Customer type)
Entity Sets, which aggregate entities of a given type
The spec mandates that this metadata document must be available at the standard location $metadata at the root URL used to access the service. For instance, if we have an OData service available at http://example.org/odata.svc/, then its metadata document will be available at http://example.org/odata.svc/$metadata.

The returned document contains a bunch of XML describing the schemas supported by this server:


To deploy this app
build with maven and deploy on an EE app srver
then go the the follwowing address
`http://localhost:8080/DemoService/DemoService.svc/$metadata`

The url to get a list of products
`http://localhost:8080/DemoService/DemoService.svc/Products?$format=application/json;odata.metadata=minimal`
