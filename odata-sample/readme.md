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
