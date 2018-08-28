/*
Amar Bessedik
Class CSC588
Fall 2016
Final project: Detect Faces and Emotions in Crowdsourced Pictures
Description: This project aims for detecting faces and emotions in crowdsourced pictures. To achieve the goal,
             a user needs to provide a picture from their local smartphone directories or by taking a picture.
             Once a picture is provided, and after some local processing, the picture will be sent into
             Microsoft's Face API/Emotion API. If any faces are present, results of detected faces or detected emotions
             will be sent back to the client for display.
*/
package com.example.amar.facemotion;
import java.io.*;
import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.text.method.ScrollingMovementMethod;
import android.view.*;
import android.widget.*;
import android.provider.*;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.microsoft.projectoxford.emotion.*;
import com.microsoft.projectoxford.emotion.contract.*;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import android.graphics.Bitmap;

/**
 * Created by Amar on 10/25/2016.
 */

public class EmotionActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST = 1200;
    private final int PICK_IMAGE = 1;//Only one image is picked at a time
    protected Bitmap bitmap;// A copy of an image will be converted into a bitmap for processing
    protected Button browsePhotoButton;// Browse for pictures in local directories
    protected Button takePictureButton;// Take a picture button
    protected Button goBackButton; // Go back to face detection screen button
    private ImageView emotionImageView; // Container to display the loaded image
    private TextView resultsTextView; // For  emotions results display
    private ProgressDialog emotionDetectionProgressDialog; // Show detection progress
    protected List<RecognizeResult> result = null; // List of scores and positions of faces in a picture
    //Key to access Emotion API
    private EmotionServiceClient emotionServiceClient = new EmotionServiceRestClient("dad2de94aee4433d82c6b0a794e3c9c4");
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion);
        //Click Listener
        browsePhotoButton = (Button) findViewById(R.id.browsePhotoButton);
        browsePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resultsTextView.setText("");
                Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
                gallIntent.setType("image/*");
                startActivityForResult(Intent.createChooser(gallIntent, "Select Picture"), PICK_IMAGE);
            }
        });

        //Go Back to main Activity (Face Detection Activity) click listener
        goBackButton = (Button)findViewById(R.id.back_button);
        goBackButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(EmotionActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        //Show progress in detection
        emotionDetectionProgressDialog = new ProgressDialog(this);

        resultsTextView = (TextView)findViewById(R.id.resultsTextView);
        resultsTextView.setMovementMethod(new ScrollingMovementMethod());

        this.emotionImageView = (ImageView)this.findViewById(R.id.emotionImageView);
        //Click listener for picture taking
        takePictureButton = (Button) this.findViewById(R.id.takePictureButton);
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        });

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }//end onCreate


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        DetectFacialEmotions detectFacialEmotions = new DetectFacialEmotions();
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                emotionImageView = (ImageView) findViewById(R.id.emotionImageView);
                emotionImageView.setImageBitmap(bitmap);

                detectFacialEmotions.execute();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            try {
                bitmap = (Bitmap) data.getExtras().get("data");
                emotionImageView.setImageBitmap(bitmap);

                detectFacialEmotions.execute();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Emotion Activity") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    protected class DetectFacialEmotions extends AsyncTask<String, String, List<RecognizeResult>> {
        @Override
        protected List<RecognizeResult> doInBackground(String... args) {
            try {
                // Put the image into an input stream for detection.
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

                // Record emotions using the position of the faces in the image.
                result = emotionServiceClient.recognizeImage(inputStream);

                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            emotionDetectionProgressDialog.show();
        }
        @Override
        protected void onProgressUpdate(String[] progress) {
            emotionDetectionProgressDialog.setMessage(progress[0]);
        }


        @Override
        protected void onPostExecute(List<RecognizeResult> faces) {
            //For grammatical use: if 1 face use "IS" otherwise use "ARE"
            //Example: 1 face is detected ; 2 faces are detected
            String verb = "ARE";
            //For grammatical use: If more than 1 face use the plural "S" otherwise omit the "S"
            //Example: 1 face ; 2 face(s)
            String singularOrPlural = "S";
            if(faces.size() == 1){
                verb = "IS";
                singularOrPlural = "";
            }

            int faceNumber = 0;
            if(faces.size() > 0) {
                String[] EMOTIONS = {"ANGER", "CONTEMPT", "DISGUST", "FEAR", "HAPPINESS", "NEUTRAL", "SADNESS", "SURPRISE"};

                resultsTextView.append("\tTHERE "+verb+" "+faces.size()+" DETECTED FACE"+singularOrPlural+":\n\n");
                for (RecognizeResult face : faces) {
                    double[] SCORES = {face.scores.anger, face.scores.contempt,
                                       face.scores.disgust, face.scores.fear,
                                       face.scores.happiness, face.scores.neutral,
                                       face.scores.sadness, face.scores.surprise};

                    int[] POSITIONS = {face.faceRectangle.left,
                                       face.faceRectangle.top,
                                       face.faceRectangle.width,
                                       face.faceRectangle.height};

                    //Increment the number of faces by 1 each time
                    faceNumber++;
                    resultsTextView.append(String.format(Locale.US, "\t FACE #" + faceNumber + " POSITION: %d, %d, %d, %d\n\n", POSITIONS[0], POSITIONS[1], POSITIONS[2], POSITIONS[3]));

                    for (int i = 0; i < EMOTIONS.length; i++) {

                        resultsTextView.append(String.format(Locale.US, "\t\t%s", EMOTIONS[i] + " : "));
                        resultsTextView.append(String.format(Locale.US, "%1$.6f\n", SCORES[i]));
                    }
                    resultsTextView.append("\n");
                }
            }else{
                resultsTextView.append("THERE ARE NO DETECTED FACES!");
            }
            emotionDetectionProgressDialog.dismiss();
        }

    }//end DetectFacialEmotions class

}//end EmotionActivity
