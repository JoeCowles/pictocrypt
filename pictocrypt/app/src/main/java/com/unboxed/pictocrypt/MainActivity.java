package com.unboxed.pictocrypt;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    ActivityResultLauncher<Intent> cameraLauncher;
    private static final int PHOTO = 100;
    ImageView picture;
    InputImage inputImage;
    ImageLabeler labeler;
    TextView output;
    EditText filenameET;
    ImageView fileBtn;
    Button encryptBtn, decryptBtn;
    File filetoEncrypt;
    String key;
    EditText previewTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkCameraPerms();
        picture = findViewById(R.id.pictureView);
        output = findViewById(R.id.outputTv);
        filenameET = findViewById(R.id.filenameEt);
        fileBtn = findViewById(R.id.fileBtn);
        encryptBtn = findViewById(R.id.btn_encrypt);
        previewTv = findViewById(R.id.previewTv);
        decryptBtn = findViewById(R.id.btn_decrypt);
        picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePic();
            }
        });
        cameraLauncher=registerForActivityResult(new
                        ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Intent data = result.getData();
                        Bitmap pic = (Bitmap) data.getExtras().get("data");
                        inputImage = InputImage.fromBitmap(pic,0);
                        picture.setImageBitmap(pic);
                        proccessImage();
                    }
                });
        labeler= ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
        fileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                browseFiles();
            }
        });
        encryptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    createTxtFile(previewTv.getText().toString());
                    CryptoUtils.encrypt(key, filetoEncrypt, filetoEncrypt);
                    previewTv.setText(readFromFile(MainActivity.this, filetoEncrypt.getPath()));
                } catch (CryptoException e) {
                    e.printStackTrace();
                }
            }
        });
        decryptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    CryptoUtils.decrypt(key, filetoEncrypt, filetoEncrypt);
                } catch (CryptoException e) {
                    e.printStackTrace();
                }
                previewTv.setText(readFromFile(MainActivity.this, filetoEncrypt.getPath()));
            }
        });
    }
    public void takePic(){
        Intent camIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(camIntent);
    }

    //TODO: Clean up perms
    private boolean checkCameraPerms() {

        if (ContextCompat.checkSelfPermission(this.getBaseContext(),
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        100);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        100);
            }
        }
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1
            );

            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
            // app-defined int constant that should be quite unique

            return true;
        }
        return true;
    }
    private void proccessImage(){
        labeler.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
            @Override
            public void onSuccess(List<ImageLabel> imageLabels) {
                String result = imageLabels.get(0).getText();
                output.setText(result + "    ID#" + createKey(result));
                key = createKey(result);
                Log.d("Debugging", result);
            }
        });
    }

    private String createKey(String word){
        int sum =0;
        for(char c : word.toCharArray()){
            sum += (int) c;
        }
        sum*= 8;
        int[] binary = decimalToBinary(sum);
        for(int i = 0; i < binary.length-1; i++){

            binary[i] = binary[i+1];

        }
        String bin ="";
        for(int i : binary) {

            bin += i;

        }
        bin = Integer.parseInt(bin.substring(0,33),2) + "nowdan";

        byte bytes[] = new byte[16]; // 128 bits are converted to 16 bytes;

        for(int i = 0; i < 16; i++){

            bytes[i] = Byte.valueOf((byte) bin.toCharArray()[i]);

        }
        return bin;
    }
    public static int[] decimalToBinary(int num)
    {
        // Creating and assigning binary array size
        int[] binary = new int[35];
        int id = 0;

        // Number should be positive
        while (num > 0) {
            binary[id++] = num % 2;
            num = num / 2;
        }
        return binary;
    }
    public void browseFiles(){
        Intent browse = new Intent(Intent.ACTION_GET_CONTENT);
        browse.setType("file/*");
        startActivityForResult(browse, 101);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == 101){
            Log.d("DEBUGGING", data.getDataString());
            Toast.makeText(this.getApplicationContext(), data.getDataString(),Toast.LENGTH_LONG).show();

        }


        super.onActivityResult(requestCode, resultCode, data);
    }
    public String readTxtFile(File txtFile) throws FileNotFoundException {
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(txtFile));

            String line;

            while ((line = reader.readLine()) != null) {
                text.append(line + '\n');
            }
            reader.close();
        } catch (IOException e) {
            Log.e("C2c", "Error occured while reading text file!!" + e.toString());
        }
        return text.toString();
    }
    public File createTxtFile(String text){

        File file = new File(MainActivity.this.getFilesDir(), "text");
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        writeToFile(text, file.getName(), this);
        filetoEncrypt = file;
        return file;
    }
    private void writeToFile(String data,String filename, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(filename, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
    private String readFromFile(Context context, String filename) {

        String ret = "";

        try {
            FileInputStream inputStream = new FileInputStream(new File(filename));

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }
}