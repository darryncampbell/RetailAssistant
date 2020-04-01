# Transitioning to DialogFlow API v2

**Per Google's announcement, as of ~~March 31st 2020~~ May 31st, 2020 v1 of the DialogFlow API will be shutdown.**  The DialogFlow API comprises a front-end client library and a back-end agent, both of which have to be on the same version of the API.

**Whilst not implemented in this project, I did produce a proof of concept for using Android with DialogFlow V2 [here](https://github.com/darryncampbell/AndroidV2DialogFlow)

The [RetailAssistant](https://github.com/darryncampbell/RetailAssistant) application was written to use v1 of the DialogFlow API and therefore will not work after March 31st 2020.  **There is no Android client library for v2 of the DialogFlow API** and whilst there is a Java library, it is clearly stated in the client library docs that this Java library does not support Android. 

![Java SDK Android support](https://raw.githubusercontent.com/darryncampbell/RetailAssistant/master/screens/v2_java_support_newer.png)

The v1 Android client libraries [are listed as deprecated on GitHub](https://github.com/dialogflow/dialogflow-android-client) and developers are (presumably erronously) recommend to migrate to the [Java client library](https://cloud.google.com/dialogflow/docs/reference/libraries/java).  

It is therefore non-trivial to update the Retail Assistant to use v2 of the DialogFlow API, the most sensible route forward for anybody in my position is as follows:

- Create a v2 agent in the DialogFlow console
- Modify your Android application to use the [REST Dialogflow API](https://cloud.google.com/dialogflow/docs/reference/rest/v2-overview)
  - *It is an open question (to me) how to authenticate against this API on Android*
  - Remove the existing v1 Dialogflow Android SDK
  - Make use of the [Android speech recognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer) feature to convert user voice to text.
  - Send the recognized speech via the REST API and interpret the response
  - Send any subsequent scan data via the REST API  

Further information on transitioning from v1 to v2 of the API are [provided by Google](https://dialogflow.com/docs/reference/v1-v2-migration-guide)
