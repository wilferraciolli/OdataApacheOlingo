This tutorial can be found here
https://olingo.apache.org/doc/odata4/tutorials/read/tutorial_read.html

It is to provide a full server-client to work with odata using apache olingo v4

At the end of this tutorial, you will have written an OData service and you will be able to invoke the following URL in a browser:

`http://localhost:8080/DemoService/DemoService.svc/Products`

And the browser will display the following collection of data:

```
{
   "@odata.context": "$metadata#Products",
   "value": [
     {
       "ID": 1,
       "Name": "Notebook Basic 15",
       "Description": "Notebook Basic, 1.7GHz - 15 XGA - 1024MB DDR2 SDRAM - 40GB"
     },
     {
       "ID": 2,
       "Name": "1UMTS PDA",
       "Description": "Ultrafast 3G UMTS/HSDPA Pocket PC, supports GSM network"
     },
     {
       "ID": 3,
       "Name": "Ergo Screen",
       "Description": "17 Optimum Resolution 1024 x 768 @ 85Hz, resolution 1280 x 960"
     }
   ]
 }
 ```
 
 To be able to implement odata the following is needed
 	creating a service
 	Declare metadata for the service
 	Handle service requests
 
 
#Implementation
According to the OData specification, an OData service has to declare its structure in the so-called Metadata Document. This document defines the contract, such that the user of the service knows which requests can be executed, the structure of the result and how the service can be navigated.

The Metadata Document can be invoked via the following URI:

`<serviceroot>/$metadata`

Furthermore, OData specifies the usage of the so-called Service Document Here, the user can see which Entity Collections are offered by an OData service.

The service document can be invoked via the following URI:

`<serviceroot>/`

The information that is given by these 2 URIs, has to be implemented in the service code. Olingo provides an API for it and we will use it in the implementation of our CsdlEdmProvider.

###Data model implementation
In our simple example, we implement the minimum amount of methods, required to run a meaningful OData service. These are:

`getEntityType()` Here we declare the EntityType “Product” and a few of its properties

`getEntitySet()` Here we state that the list of products can be called via the EntitySet “Products”

`getEntityContainer()` Here we provide a Container element that is necessary to host the EntitySet.

`getSchemas()` The Schema is the root element to carry the elements.

`getEntityContainerInfo()` Information about the EntityContainer to be displayed in the Service Document


##vocabulary
EDM=Entity Data model
