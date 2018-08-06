package com.darryncampbell.retailassistant;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.StatusData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.api.AIServiceException;
import ai.api.RequestExtras;
import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.android.GsonFactory;
import ai.api.model.AIContext;
import ai.api.model.AIError;
import ai.api.model.AIEvent;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;
import ai.api.model.Status;
import ai.api.ui.AIButton;

public class MainActivity extends AppCompatActivity implements AIButton.AIButtonListener, EMDKManager.EMDKListener, Scanner.DataListener, Scanner.StatusListener {

    //  NOTE: The API on which this application depends is using V1 (Legacy) of the Dataflow API.
    //  If you configure the DialogFlow cloud component to use V2 (default) then you will get strange errors during fulfillment
    //  Based on dialogflow-android-client, https://github.com/dialogflow/dialogflow-android-client, released under Apache 2
    //  Mirrored here: https://github.com/darryncampbell/dialogflow-android-client
    public static final String ACCESS_TOKEN = "6f15d63c55c344008d84bd31b4bcd221";  // Retail assistant
    public static final String TAG = "RetailAssistant";
    private static final int REQUEST_AUDIO_PERMISSIONS_ID = 33;

    //  Constants copied from dialogflow sample app
    private AIButton aiButton;
    private AIDataService aiDataService; //  used by scanner
    private TextView resultTextView;
    private Gson gson = GsonFactory.getGson();

    //  Constants for EMDK
    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;
    private String statusString = "";
    private TextView textViewStatus = null;
    private boolean bContinuousMode = false;
    private static final String PLEASE_SCAN_BARCODE  = "Please Scan Barcode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  Dialogflow initialisation
        TTS.init(getApplicationContext());
        resultTextView = (TextView) findViewById(R.id.resultTextView);
        aiButton = (AIButton) findViewById(R.id.micButton);
        final AIConfiguration config = new AIConfiguration(ACCESS_TOKEN,
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        config.setRecognizerStartSound(getResources().openRawResourceFd(R.raw.test_start));
        config.setRecognizerStopSound(getResources().openRawResourceFd(R.raw.test_stop));
        config.setRecognizerCancelSound(getResources().openRawResourceFd(R.raw.test_cancel));
        aiButton.initialize(config);
        aiButton.setResultsListener(this);
        aiDataService = new AIDataService(this, config);

        //  EMDK initialisation
        textViewStatus = (TextView)findViewById(R.id.textViewStatus);
        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            textViewStatus.setText("Status: " + "EMDKManager object request failed!");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAudioRecordPermission();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //  From dialogflow sample
        // use this method to disconnect from speech recognition service
        // Not destroying the SpeechRecognition object in onPause method would block other apps from using SpeechRecognition service
        aiButton.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //  From dialogflow sample
        // use this method to reinit connection to recognition service
        aiButton.resume();
    }

    @Override
    public void onResult(final AIResponse response) {
        //  Largely copied from dialogflow sample
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onResult");
                //  Output the whole json to the UI
                //resultTextView.setText(gson.toJson(response));
                Log.i(TAG, "Received success response");
                // this is example how to get different parts of result object
                final Status status = response.getStatus();
                Log.i(TAG, "Status code: " + status.getCode());
                Log.i(TAG, "Status type: " + status.getErrorType());
                final Result result = response.getResult();
                Log.i(TAG, "Resolved query: " + result.getResolvedQuery());
                Log.i(TAG, "Action: " + result.getAction());
                final String speech = result.getFulfillment().getSpeech();
                Log.i(TAG, "Speech: " + speech);
                resultTextView.setText(speech);
                TTS.speak(speech);
                //  Modified dialogflow sample, initiate a scan if requested
                if (speech != null && speech.equalsIgnoreCase(PLEASE_SCAN_BARCODE))
                {
                    //  We are being asked to scan a barcode
                    startScan();
                }
                else
                {
                    //  Something other than being asked to scan a barcode, stop any pending read
                    stopScan();
                }

                final Metadata metadata = result.getMetadata();
                if (metadata != null) {
                    Log.i(TAG, "Intent id: " + metadata.getIntentId());
                    Log.i(TAG, "Intent name: " + metadata.getIntentName());
                }

                final HashMap<String, JsonElement> params = result.getParameters();
                if (params != null && !params.isEmpty()) {
                    Log.i(TAG, "Parameters: ");
                    for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
                        Log.i(TAG, String.format("%s: %s", entry.getKey(), entry.getValue().toString()));
                    }
                }
            }

        });
    }

    @Override
    public void onError(final AIError error) {
        //  copied from dialogflow sample
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onError");
                resultTextView.setText(error.toString());
            }
        });
    }

    @Override
    public void onCancelled() {
        //  copied from dialogflow sample
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onCancelled");
                resultTextView.setText("");
            }
        });
    }

    //  Largely copied from dialogflow sample.  Send a string to the dialogflow engine (this will
    //  be a scanned barcode)
    private void sendDataToDialogFlow(String query)
    {
        //  Not worrying about different languages or contexts
        final String queryString = query;
        final String eventString = null;
        final String contextString = "";
        if (TextUtils.isEmpty(queryString) && TextUtils.isEmpty(eventString)) {
            onError(new AIError("Query should not be empty"));
            return;
        }
        final AsyncTask<String, Void, AIResponse> task = new AsyncTask<String, Void, AIResponse>() {

            private AIError aiError;
            @Override
            protected AIResponse doInBackground(final String... params) {
                final AIRequest request = new AIRequest();
                String query = params[0];
                String event = params[1];

                if (!TextUtils.isEmpty(query))
                    request.setQuery(query);
                if (!TextUtils.isEmpty(event))
                    request.setEvent(new AIEvent(event));
                final String contextString = params[2];
                RequestExtras requestExtras = null;
                if (!TextUtils.isEmpty(contextString)) {
                    final List<AIContext> contexts = Collections.singletonList(new AIContext(contextString));
                    requestExtras = new RequestExtras(contexts, null);
                }

                try {
                    return aiDataService.request(request, requestExtras);
                } catch (final AIServiceException e) {
                    aiError = new AIError(e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(final AIResponse response) {
                if (response != null) {
                    onResult(response);
                } else {
                    onError(aiError);
                }
            }
        };

        task.execute(queryString, eventString, contextString);
    }

    protected void checkAudioRecordPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_AUDIO_PERMISSIONS_ID);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSIONS_ID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    //  EMDK Overrides
    @Override
    public void onOpened(EMDKManager emdkManager) {
        //  Largely copied from BarcodeSample1
        this.emdkManager = emdkManager;
        barcodeManager = (BarcodeManager) emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
    }

    @Override
    public void onClosed() {
        //  Largely copied from BarcodeSample1
        if (emdkManager != null) {
            // Remove connection listener
            if (barcodeManager != null){
                barcodeManager = null;
            }
            // Release all the resources
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        //  Largely copied from BarcodeSample1
        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList<ScanDataCollection.ScanData> scanData = scanDataCollection.getScanData();
            for(ScanDataCollection.ScanData data : scanData) {
                String dataString =  data.getData();
                //  Send the scan data to the DialogFlow
                sendDataToDialogFlow(dataString);
            }
        }
    }

    @Override
    public void onStatus(StatusData statusData) {
        //  Largely copied from BarcodeSample1
        StatusData.ScannerStates state = statusData.getState();
        switch(state) {
            case IDLE:
                statusString = statusData.getFriendlyName()+" is enabled and idle...";
                new AsyncStatusUpdate().execute(statusString);
                if (bContinuousMode) {
                    try {
                        // An attempt to use the scanner continuously and rapidly (with a delay < 100 ms between scans)
                        // may cause the scanner to pause momentarily before resuming the scanning.
                        // Hence add some delay (>= 100ms) before submitting the next read.
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        scanner.read();
                    } catch (ScannerException e) {
                        statusString = e.getMessage();
                        new AsyncStatusUpdate().execute(statusString);
                    }
                }
                //new AsyncUiControlUpdate().execute(true);
                break;
            case WAITING:
                statusString = "Scanner is waiting for trigger press...";
                new AsyncStatusUpdate().execute(statusString);
                //new AsyncUiControlUpdate().execute(false);
                break;
            case SCANNING:
                statusString = "Scanning...";
                new AsyncStatusUpdate().execute(statusString);
                //new AsyncUiControlUpdate().execute(false);
                break;
            case DISABLED:
                statusString = statusData.getFriendlyName()+" is disabled.";
                new AsyncStatusUpdate().execute(statusString);
                //new AsyncUiControlUpdate().execute(true);
                break;
            case ERROR:
                statusString = "An error has occurred.";
                new AsyncStatusUpdate().execute(statusString);
                //new AsyncUiControlUpdate().execute(true);
                break;
            default:
                break;
        }
    }

    private class AsyncStatusUpdate extends AsyncTask<String, Void, String> {
        //  Largely copied from BarcodeSample1
        @Override
        protected String doInBackground(String... params) {
            return params[0];
        }
        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "Scanner Status: " + result);
            textViewStatus.setText("Status: " + result);
        }
    }

    private void initScanner() {
        //  Largely copied from BarcodeSample1
        if (scanner == null) {
            //  This is a simple app, assume requires an internal imager
            scanner = barcodeManager.getDevice(BarcodeManager.DeviceIdentifier.INTERNAL_IMAGER1);
            if (scanner != null) {
                scanner.addDataListener(this);
                scanner.addStatusListener(this);
                scanner.triggerType = Scanner.TriggerType.SOFT_ONCE;
                try {
                    scanner.enable();
                } catch (ScannerException e) {
                    textViewStatus.setText("Status: " + e.getMessage());
                }
            }else{
                textViewStatus.setText("Status: " + "Failed to initialize the scanner device.");
            }
        }
    }

    private void startScan() {
        //  Largely copied from BarcodeSample1

        if(scanner == null) {
            initScanner();
        }
        if (scanner != null) {
            try {
                if(scanner.isEnabled())
                {
                    // Submit a new read.
                    bContinuousMode = true;
                    scanner.triggerType = Scanner.TriggerType.SOFT_ONCE;
                    scanner.read();
                    //new AsyncUiControlUpdate().execute(false);
                }
                else
                {
                    textViewStatus.setText("Status: Scanner is not enabled");
                }

            } catch (ScannerException e) {
                textViewStatus.setText("Status: " + e.getMessage());
            }
        }
    }

    private void stopScan() {
        //  Largely copied from BarcodeSample1
        if (scanner != null) {
            try {
                // Reset continuous flag
                bContinuousMode = false;
                // Cancel the pending read.
                scanner.cancelRead();
                //new AsyncUiControlUpdate().execute(true);
            } catch (ScannerException e) {
                textViewStatus.setText("Status: " + e.getMessage());
            }
        }
    }
}
