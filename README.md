# RetailAssistant
Proof of concept to show DialogFlow in a retail environment


Requires an internal imager
I have hardcoded the client key
requires Zebra device
Assign audio record permission
Needs GMS
Needs network connectivity
Barcodes processing is hard coded
Depends on Apache2 library
needs v1 of the api
Could not include api via gradle

Tested on TC51, should work on any Zebra GMS device with a microphone

Handled intents:
	Help
	Hello
	Price
	Stock
	Other store availability
	When available (next delivery)
	Where does this item go?


https://github.com/dialogflow/dialogflow-android-client
