# RetailAssistant
Proof of concept to show DialogFlow in a retail environment

This application uses code from https://github.com/dialogflow/dialogflow-android-client and is released under Apache 2.0 in compliance with that library.

Device requirements to run this application:
- **Zebra mobile computer with an internal imager**
- GMS services (will not work on an AOSP device)
- Network connectivity
- Microphone
- Audio recording permission must be granted to the app
- Android KitKat or later

Tested with TC51 Nougat but should work with any Zebra device meeting the above requirements

# Conversation 
Try communicating with the app as follows:
- Ask for Help
- Ask for an item's price (e.g. "How much is this?")
- Ask for an item's stock level (e.g. "What is the stock level?")
- Other store availability (e.g. "Is the item available at our Dallas store?")
- When available (e.g. "When is a delivery due?")
- Where does this item go (e.g. "Where does this go?")


## Notes
- I have hardcoded the client key to communicate with my backend.  This is obviously not best security practice but this is a proof of concept demo
- The backend processing is hard coded.  The app is not actually looking up prices or stock levels, the returned values are dependant on the barcode scanned so will be consistent if you scan the same barcode multiple times but will differ from barcode to barcode.
- Scanned barcodes must be numeric
- The Android SDK depends on version 1 of the DialogFlow API.  You MUST configure your backend to use v1 of the API in order for this client SDK to work.
- The instructions for including the SDK from maven, as detailed in the [readme](https://github.com/dialogflow/dialogflow-android-client/blob/master/README.md) do not work for me, I therefore had to include the SDK in the same way as the sample app.

## Screenshots

![Screenshot 1](https://raw.githubusercontent.com/darryncampbell/RetailAssistant/master/screens/001.png)
![Screenshot 2](https://raw.githubusercontent.com/darryncampbell/RetailAssistant/master/screens/002.png)
![Screenshot 3](https://raw.githubusercontent.com/darryncampbell/RetailAssistant/master/screens/003.png)
![Screenshot 4](https://raw.githubusercontent.com/darryncampbell/RetailAssistant/master/screens/004.png)
