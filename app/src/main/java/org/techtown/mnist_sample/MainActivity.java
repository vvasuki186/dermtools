package org.techtown.mnist_sample;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseCustomLocalModel;
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelInterpreterOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.soundcloud.android.crop.Crop;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opencv.android.BaseLoaderCallback;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener{

    ImageButton imageButton;
    ImageView imageView1, imageView2, imageView3;
    ImageView iv;
    Button analysisButton, addButton;

    ArrayList<RectangleRange> ranges = new ArrayList<>();

    private final int GET_GALLERY_IMAGE = 200;

    int image_num = 0;
    int crop_num = 0;
    float[][][] bmpOutputs;
    Uri imageUri1, imageUri2, imageUri3;
    Uri cropUri1, cropUri2, cropUri3;

    Bitmap mask_bitmap;
    Bitmap resizedBitmap;

    Bitmap croppedBitmap1 = null, croppedBitmap2 = null, croppedBitmap3 = null;
    File croppedFile1, croppedFile2, croppedFile3;

    ArrayList<Bitmap> croppedBitmaps = new ArrayList<>();
    MainActivity mainActivity;

    FirebaseStorage storage = FirebaseStorage.getInstance();
    // Create a storage reference from our app
    StorageReference storageRef = storage.getReference();
    // Create a child reference
    // imagesRef now points to "images"
    StorageReference imagesRef = storageRef.child("images");
    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 상태바를 안보이도록 합니다.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mainActivity = this;

        // 화면 켜진 상태를 유지합니다.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }



        final FirebaseCustomRemoteModel remoteModel_unet =
                new FirebaseCustomRemoteModel.Builder("unet_basic_withopt66.tflite").build();

        final FirebaseCustomLocalModel localModel_unet = new FirebaseCustomLocalModel.Builder()
                .setAssetFilePath("unet_basic_withopt66.tflite")
                .build();

        final FirebaseCustomRemoteModel remoteModel_effnet =
                new FirebaseCustomRemoteModel.Builder("eff_net_basic.tflite").build();

        final FirebaseCustomLocalModel localModel_effnet = new FirebaseCustomLocalModel.Builder()
                .setAssetFilePath("eff_net_basic.tflite")
                .build();

        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
                .requireWifi()
                .build();

        /*FirebaseModelManager.getInstance().download(remoteModel_unet, conditions)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.e("asdf", "unet download sucess");
                    }
                });

        FirebaseModelManager.getInstance().download(remoteModel_effnet, conditions)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.e("asdf", "effnet download sucess");
                    }
                });*/



        setContentView(R.layout.activity_main);

        imageButton = findViewById(R.id.image_select_btn);
//        imageView = findViewById(R.id.image_view);
        imageView1 = findViewById(R.id.image_1);
        imageView2 = findViewById(R.id.image_2);
        imageView3 = findViewById(R.id.image_3);
        //cropButton = findViewById(R.id.crop_btn);
        analysisButton = findViewById(R.id.classify_btn);
        addButton = findViewById(R.id.add_btn);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(image_num<=2){
                    image_num ++;
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                    startActivityForResult(intent, GET_GALLERY_IMAGE);
                }else{
                    Toast.makeText(getApplicationContext(), "no more image", Toast.LENGTH_SHORT).show();
                }
            }
        });

        /*cropButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent, EFFICIENT_NET_IMAGE);
            }
        });*/

        analysisButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(croppedBitmap1 != null){
                    int[] bitWidth = new int[3];
                    int[] bitHeight = new int[3];


                    resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap1, 304, 304, true);

                    Bitmap[] combs = decompose(resizedBitmap);
                    croppedBitmap2 = combs[0];
                    croppedBitmap3 = combs[1];

                    bitWidth[0] = resizedBitmap.getWidth();
                    bitWidth[1] = resizedBitmap.getWidth();

                    bitWidth[2] = croppedBitmap3.getWidth();
                    bitHeight[0] = resizedBitmap.getHeight();
                    bitHeight[1] = croppedBitmap2.getHeight();
                    bitHeight[2] = croppedBitmap3.getHeight();
                    int[] coverImageIntArray1D1 = new int[bitWidth[0] * bitHeight[0]];
                    int[] coverImageIntArray1D2 = new int[bitWidth[1] * bitHeight[1]];
                    int[] coverImageIntArray1D3 = new int[bitWidth[2] * bitHeight[2]];
                    resizedBitmap.getPixels(coverImageIntArray1D1, 0, bitWidth[0], 0, 0, bitWidth[0], bitHeight[0]);
                    croppedBitmap2.getPixels(coverImageIntArray1D2, 0, bitWidth[1], 0, 0, bitWidth[1], bitHeight[1]);
                    croppedBitmap3.getPixels(coverImageIntArray1D3, 0, bitWidth[2], 0, 0, bitWidth[2], bitHeight[2]);

                    imageView2.setImageBitmap(croppedBitmap2);
                    imageView2.setVisibility(View.VISIBLE);
                    imageView3.setImageBitmap(croppedBitmap3);
                    imageView3.setVisibility(View.VISIBLE);



                    final float[][][][] input1 = new float[1][304][304][3];
                    final float[][][][] input2 = new float[1][304][304][3];
                    final float[][][][] input3 = new float[1][304][304][3];


                    /*float[][][][][] input = new float[3][1][304][304][3];
                    Map<Integer, Object> outputs = new HashMap<>();

                    float[][][][] output1 = new float[1][304][304][1];
                    outputs.put(0,output1);

                    ByteBuffer in1 = ByteBuffer.allocateDirect(3 * 304 * 304 * 3 * 4);

                    ByteBuffer[] in = new ByteBuffer[3];
                    in[0] = ByteBuffer.allocateDirect(304 * 304 * 3 * 4);
                    in[0].order(ByteOrder.nativeOrder());
                    in[1] = ByteBuffer.allocateDirect(304 * 304 * 3 * 4);
                    in[1].order(ByteOrder.nativeOrder());
                    in[2] = ByteBuffer.allocateDirect(304 * 304 * 3 * 4);
                    in[2].order(ByteOrder.nativeOrder());

                    in1.order(ByteOrder.nativeOrder());*/



                    if(bitHeight[0] == 304 && bitWidth[0] == 304 && bitHeight[1] == 304 && bitWidth[1] == 304 && bitHeight[2] == 304 &&bitWidth[2] == 304){
                        for(int i=0;i<304;i++){
                            for(int j=0;j<304;j++){
                                int rgb1 = coverImageIntArray1D1[i*304+j];
                                int rgb2 = coverImageIntArray1D2[i*304+j];
                                int rgb3 = coverImageIntArray1D3[i*304+j];
                                int R1 = Color.red(rgb1);
                                int G1 = Color.green(rgb1);
                                int B1 = Color.blue(rgb1);
                                int R2 = Color.red(rgb2);
                                int G2 = Color.green(rgb2);
                                int B2 = Color.blue(rgb2);
                                int R3 = Color.red(rgb3);
                                int G3 = Color.green(rgb3);
                                int B3 = Color.blue(rgb3);
                                input1[0][i][j][0] = (float) (R1 / 255.0);
                                input1[0][i][j][1] = (float) (G1 / 255.0);
                                input1[0][i][j][2] = (float) (B1 / 255.0);
                                input2[0][i][j][0] = (float) (R2 / 255.0);
                                input2[0][i][j][1] = (float) (G2 / 255.0);
                                input2[0][i][j][2] = (float) (B2 / 255.0);
                                input3[0][i][j][0] = (float) (R3 / 255.0);
                                input3[0][i][j][1] = (float) (G3 / 255.0);
                                input3[0][i][j][2] = (float) (B3 / 255.0);

                                /*in[0].putFloat((float) (R1 / 255.0));
                                in[0].putFloat((float) (G1 / 255.0));
                                in[0].putFloat((float) (B1 / 255.0));
                                in[1].putFloat((float) (R2 / 255.0));
                                in[1].putFloat((float) (G2 / 255.0));
                                in[1].putFloat((float) (B2 / 255.0));
                                in[2].putFloat((float) (R3 / 255.0));
                                in[2].putFloat((float) (G3 / 255.0));
                                in[2].putFloat((float) (B3 / 255.0));*/

                            }
                        }

                        FirebaseModelManager.getInstance().isModelDownloaded(remoteModel_unet)
                                .addOnSuccessListener(new OnSuccessListener<Boolean>() {
                                    @Override
                                    public void onSuccess(Boolean isDownloaded) {
                                        FirebaseModelInterpreterOptions options2;
                                        if (isDownloaded) {
                                            options2 = new FirebaseModelInterpreterOptions.Builder(remoteModel_unet).build();
                                        } else {
                                            options2 = new FirebaseModelInterpreterOptions.Builder(localModel_unet).build();
                                        }
                                        segment_and_classify(input1, input2, input3, options2);

                                    }
                                });



                        long startTime = System.currentTimeMillis();
                        long difference = System.currentTimeMillis() - startTime;
                        Log.e("DIFFERENCE", "Diff: " + String.valueOf(difference));
                        Log.e("asdf", "DONE");



                        //List<Rect> rect_list = new ArrayList<>();
                        /*rect_list.add(rect);
                        Log.e("length", "len: " + rect_list.size());

                        for(int it=0; it<rect_list.size(); it++)
                        {
                            if((rect & rect_list.get(it)).area() > 0)
                            {

                            }
                        }*/




                        /*File mypath = new File ("/document/raw:/storage/emulated/0/Download/", "masked_img.jpg");
                        FileOutputStream fos = null;
                        try{
                            fos = new FileOutputStream(mypath);
                            new_bit.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/


                    } else{
                        Toast.makeText(getApplicationContext(), "image is small", Toast.LENGTH_SHORT).show();
                    }


                }

            }


            private void segment_and_classify(float[][][][] input1,float[][][][] input2,float[][][][] input3,FirebaseModelInterpreterOptions options2) {
                try {

                    FirebaseModelInterpreter interpreter = FirebaseModelInterpreter.getInstance(options2);
                    FirebaseModelInputOutputOptions inputOutputOptions = null;
                    try {
                        inputOutputOptions =
                                new FirebaseModelInputOutputOptions.Builder()
                                        .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 304, 304, 3})
                                        .setInputFormat(1, FirebaseModelDataType.FLOAT32, new int[]{1, 304, 304, 3})
                                        .setInputFormat(2, FirebaseModelDataType.FLOAT32, new int[]{1, 304, 304, 3})
                                        .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 304, 304, 1})
                                        .build();
                        Log.e("asdf", "yes: 2");
                    } catch (FirebaseMLException e) {
                        e.printStackTrace();
                    }



                    FirebaseModelInputs inputs = null;
                    try {
                        inputs = new FirebaseModelInputs.Builder()
                                .add(input1)  // add() as many input arrays as your model requires
                                .add(input2)
                                .add(input3)
                                .build();
                        Log.e("asdf", "yes: 3");
                    } catch (FirebaseMLException e) {
                        e.printStackTrace();
                    }

                    interpreter.run(inputs, inputOutputOptions)
                            .addOnSuccessListener(
                                    new OnSuccessListener<FirebaseModelOutputs>() {
                                        @Override
                                        public void onSuccess(FirebaseModelOutputs result) {
                                            Log.e("asdf", "done masking");
                                            float[][][][] output2 = result.getOutput(0);
                                            mask_bitmap = Bitmap.createBitmap(304, 304, Bitmap.Config.ARGB_8888);
                                            for(int i=0;i<304;i++){
                                                for(int j=0;j<304;j++){
                                                    if(output2[0][i][j][0]>=0.10){          mask_bitmap.setPixel(j, i, Color.WHITE);           }
                                                    else                         {          mask_bitmap.setPixel(j, i, Color.BLACK);           }
                                                }
                                            }
                                            imageView3.setImageBitmap(mask_bitmap);

                                            init_efficientnet();
                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {        e.printStackTrace();                               }
                                    });
                } catch (FirebaseMLException e) {                                            e.printStackTrace();                               }

            }

            private void init_efficientnet() {
                int[] unet_result = new int[304 * 304];
                mask_bitmap.getPixels(unet_result, 0, 304, 0, 0, 304, 304);
                Mat contour_input = new Mat();
                Mat contour_input22 = new Mat();
                Utils.bitmapToMat(mask_bitmap, contour_input);

                Imgproc.cvtColor(contour_input, contour_input22, Imgproc.COLOR_BGR2GRAY);
                final List<MatOfPoint> contours = new ArrayList<>();
                Mat hierarchy = new Mat();

                Imgproc.findContours(contour_input22, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
                final Mat orig_img = new Mat();
                Utils.bitmapToMat(resizedBitmap, orig_img);

                final Bitmap[] bmps = new Bitmap[contours.size()];
                bmpOutputs = new float[contours.size()][1][18];


                FirebaseModelManager.getInstance().isModelDownloaded(remoteModel_effnet)
                        .addOnSuccessListener(new OnSuccessListener<Boolean>() {
                            @Override
                            public void onSuccess(Boolean isDownloaded) {
                                FirebaseModelInterpreterOptions options2;
                                if (isDownloaded) {
                                    options2 = new FirebaseModelInterpreterOptions.Builder(remoteModel_effnet).build();
                                } else {
                                    options2 = new FirebaseModelInterpreterOptions.Builder(localModel_effnet).build();
                                }
                                FirebaseModelInterpreter effnet_interpreter = null;
                                try {
                                    effnet_interpreter = FirebaseModelInterpreter.getInstance(options2);
                                } catch (FirebaseMLException e) {
                                    e.printStackTrace();
                                }
                                FirebaseModelInputOutputOptions inputOutputOptions = null;
                                try {
                                    inputOutputOptions =
                                            new FirebaseModelInputOutputOptions.Builder()
                                                    .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 224, 224, 3})
                                                    .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 18})
                                                    .build();
                                    Log.e("asdf", "yes22: 2");
                                } catch (FirebaseMLException e) {
                                    e.printStackTrace();
                                }

                                classify(contours, orig_img, bmps, effnet_interpreter, inputOutputOptions);



                            }
                        });
            }
        });

    }

    String res_all = "";
    Mat orig_changed;
    int counter = 0;

    private void classify(List<MatOfPoint> contours, Mat orig_img, Bitmap[] bmps,  FirebaseModelInterpreter effnet_interpreter, FirebaseModelInputOutputOptions inputOutputOptions) {

        String result = "";
        orig_changed = orig_img;

        for (int i=0; i<contours.size(); i++) {

            List<MatOfPoint> contour = new ArrayList<>();
            contour.add(contours.get(i));

            Rect rect = Imgproc.boundingRect(contour.get(0));

            int xx = rect.x;
            int yy = rect.y;
            int hh = rect.height;
            int ww = rect.width;

            if(rect.x >= 10){ rect.x-=10; }
            if(rect.y >= 10){ rect.y-=10; }

            if(rect.x + rect.width + 20 < orig_img.cols()){ rect.width+=20; }
            else{rect.width=orig_img.cols()-rect.x;}

            if(rect.y + rect.height + 20 < orig_img.rows()){ rect.height+=20; }
            else{rect.height=orig_img.rows()-rect.y;}

            final int x = rect.x;
            final int y = rect.y;
            final int height = rect.height;
            final int width = rect.width;
            if(height > 25 && width > 25 && height < 250)
            {



                Imgproc.drawContours(orig_img, contour, 0, new Scalar(0,255,0), 1);
                RectangleRange rectangleRange = new RectangleRange(yy, yy+hh, xx, xx+ww);
                ranges.add(rectangleRange);
                Imgproc.rectangle(orig_img, rect.tl(), rect.br(), new Scalar(255,0,0), 2);
                try{
                    ///////////TODO: 1. Crop rectangle from original image
                    Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri1);
                    Mat originImg = new Mat(originalBitmap.getWidth() ,originalBitmap.getHeight(), CvType.CV_8UC4);
                    Utils.bitmapToMat(originalBitmap, originImg);
                    Mat subImg = originImg.submat(rect);
                    Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(subImg, bmp);
                    imageView3.setImageBitmap(bmp);
                    ///////////TODO: 2. Resize cropped image to 224,224,3
                    bmps[i] = Bitmap.createScaledBitmap(bmp, 224, 224, true);
                    croppedBitmaps.add(bmps[i]);

                    ///////////TODO: 3. Input resized image into efficient-net




                    int[] temp_eff = new int[224*224];
                    bmps[i].getPixels(temp_eff, 0, 224, 0, 0, 224, 224);
                    float [][][][] effin = new float[1][224][224][3];
                    for(int a=0; a<224; a++)
                        for(int b=0; b<224; b++)
                        {
                            int rgb1 = temp_eff[a*224+b];
                            int R1 = Color.red(rgb1);
                            int G1 = Color.green(rgb1);
                            int B1 = Color.blue(rgb1);
                            effin[0][a][b][0] = (float) (R1);
                            effin[0][a][b][1] = (float) (G1);
                            effin[0][a][b][2] = (float) (B1);

                        }


                    FirebaseModelInputs inputs = new FirebaseModelInputs.Builder()
                            .add(effin)
                            .build();

                    effnet_interpreter.run(inputs, inputOutputOptions)
                            .addOnSuccessListener(
                                    new OnSuccessListener<FirebaseModelOutputs>() {
                                        @Override
                                        public void onSuccess(FirebaseModelOutputs result) {
                                            float[][] output = result.getOutput(0);
                                            bmpOutputs[counter] = result.getOutput(0);

                                            Log.e("adsf", "out: " + outputToString(output) + "  " + output[0][0] + "  " + output[0][5] + "     ");


                                            Character numb = (char)('A'+counter);
                                            counter++;

                                            String a = outputToString(output);
                                            res_all = res_all + numb + ": "+a + "\n";

                                            ///////////TODO: 4. Add textview to the center of cropped image
                                            String text = "" + numb;
                                            Point pnt = new Point(x + width/2 - 10, y + height/2 + 10);
                                            int fontFace = 2;
                                            double fontScale = 1.0;
                                            Imgproc.putText(orig_changed, text, pnt, fontFace, fontScale, Scalar.all(255));
                                            Bitmap new_bit = Bitmap.createBitmap(304, 304, Bitmap.Config.ARGB_8888);
                                            Utils.matToBitmap(orig_changed, new_bit);
                                            imageView2.setImageBitmap(new_bit);
                                            TextView textView = findViewById(R.id.textView);
                                            textView.setText(res_all);
                                            Log.d("efficientNet", res_all);

                                            ImageView imageView = findViewById(R.id.image_view);
//                        int width = imageView.getLayoutParams().width;
//                        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, width);
//                        imageView.setLayoutParams(layoutParams);
                                            imageView.setImageBitmap(new_bit);
                                            imageView.setVisibility(View.VISIBLE);

                                            iv = (ImageView)findViewById(R.id.image_view);
                                            if(iv!=null){
                                                iv.setOnTouchListener(mainActivity);
                                            }



                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {    e.printStackTrace();     }
                                    });







                } catch (Exception e){
                    e.printStackTrace();
                }

            }
        }



        imageView1.setVisibility(View.INVISIBLE);
        imageView2.setVisibility(View.INVISIBLE);
        imageView3.setVisibility(View.INVISIBLE);
    }


    private void cropImage(Uri photoUri) {
        /**
         *  갤러리에서 선택한 경우에는 tempFile 이 없으므로 새로 생성해줍니다.
         */
        try {
            switch (crop_num){
                case 1:
                    croppedFile1 = createImageFile();
                    break;
                case 2:
                    croppedFile2 = createImageFile();
                    break;
                case 3:
                    croppedFile3 = createImageFile();
                    break;
            }
        } catch (IOException e) {
            Toast.makeText(this, "이미지 처리 오류! 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            finish();
            e.printStackTrace();
        }

        //크롭 후 저장할 Uri
        switch (crop_num){
            case 1:
                cropUri1 = Uri.fromFile(croppedFile1);
                Crop.of(photoUri, cropUri1).withMaxSize(304, 304).start(this);
                break;
            case 2:
                cropUri2 = Uri.fromFile(croppedFile2);
                Crop.of(photoUri, cropUri2).withMaxSize(304, 304).start(this);
                break;
            case 3:
                cropUri3 = Uri.fromFile(croppedFile3);
                Crop.of(photoUri, cropUri3).withMaxSize(304, 304).start(this);
                break;
        }
//        Crop.of(photoUri, savingUri).withAspect(304, 304).start(this);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("HHmmss").format(new Date());
        String imageFileName = "croppedImage" + timeStamp + "_";

        File storageDir = new File(getFilesDir()+ "/sample_folder/");
        if (!storageDir.exists())
            try{
                storageDir.mkdirs();
            } catch (Exception e){
            }
        // 빈 파일 생성
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_GALLERY_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            if(image_num==1){
                imageUri1 = data.getData();
                imageView1.setImageURI(imageUri1);
//                Cursor cursor = getContentResolver().query(imageUri1, null, null, null, null );
//                cursor.moveToNext();
//                filename = cursor.getString( cursor.getColumnIndex( "_data" ) );
//                cursor.close();
                imageButton.setVisibility(View.GONE);
                imageView1.setVisibility(View.VISIBLE);
                crop_num = 1;
                cropImage(imageUri1);
            }
            else if(image_num==2){
                imageUri2 = data.getData();
                imageView2.setImageURI(imageUri2);
                imageView2.setVisibility(View.VISIBLE);
                crop_num = 2;
                cropImage(imageUri2);
            }
            else if(image_num==3){
                imageUri3 = data.getData();
                imageView3.setImageURI(imageUri3);
                imageView3.setVisibility(View.VISIBLE);
                crop_num = 3;
                cropImage(imageUri3);
            }
        }
        if (requestCode == Crop.REQUEST_CROP && resultCode == RESULT_OK) {
            Log.e("asdfasdf", "22HERE WITH: " + crop_num);
            switch (crop_num){
                case 1:
                    imageView1.setImageURI(cropUri1);
                    Log.d("imageString", cropUri1.toString());
                    break;
                case 2:
                    imageView2.setImageURI(cropUri2);
                    Log.d("imageString", cropUri2.toString());
                    break;
                case 3:
                    imageView3.setImageURI(cropUri3);
                    Log.d("imageString", cropUri3.toString());
                    break;
            }
            try{
                switch (crop_num){
                    case 1:
                        croppedBitmap1 = MediaStore.Images.Media.getBitmap(getContentResolver(), cropUri1);
                        uploadImage(croppedBitmap1, "img1");
                        break;
                    case 2:
                        croppedBitmap2 = MediaStore.Images.Media.getBitmap(getContentResolver(), cropUri2);
                        uploadImage(croppedBitmap2, "img2");
                        break;
                    case 3:
                        croppedBitmap3 = MediaStore.Images.Media.getBitmap(getContentResolver(), cropUri3);
                        uploadImage(croppedBitmap3, "img3");
                        break;

                }
            } catch (Exception e){
                Log.e("except", String.valueOf(e));

            }
        }
        /*if (requestCode == EFFICIENT_NET_IMAGE && resultCode == RESULT_OK){
            Uri enUri = data.getData();
            try{
                Bitmap enBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), enUri);
                Bitmap enResizeBitmap = Bitmap.createScaledBitmap(enBitmap, 224, 224, true);
                ImageView imageView = findViewById(R.id.image_view);
                imageView.setImageBitmap(enResizeBitmap);
                imageView.setVisibility(View.VISIBLE);
                imageButton.setVisibility(View.GONE);
                EfficientNet efficientNet = new EfficientNet(enResizeBitmap, this.getApplicationContext());
                efficientNet.RunModel();
                float[][] output = efficientNet.getOutput();
                if(output != null){
                    String outputString = new String();
                    for(int i=0;i<17;i++) {
                        outputString = outputString + output[0][i] +", ";
                    }
                    outputString = outputString + output[0][17];
                    TextView textView = findViewById(R.id.textView);
                    textView.setText(outputString);
                }
            } catch (Exception e){
                Log.d("exception", e.toString());
            }
        }*/
        if(requestCode==1){
            if(resultCode==RESULT_OK){
                String result = data.getStringExtra("result");
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap[] decompose(Bitmap croppedBitmap) {
        int[] coverImageIntArray1D = new int[304 * 304];
        Bitmap resizedBitmap = null;
        resizedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 304, 304, true);

        resizedBitmap.getPixels(coverImageIntArray1D, 0, 304, 0, 0, 304, 304);

        float[][][] input = new float[304][304][3];

        for (int i = 0; i < 304; i++) {
            for (int j = 0; j < 304; j++) {
                int rgb = coverImageIntArray1D[i * 304 + j];
                int R = Color.red(rgb);
                int G = Color.green(rgb);
                int B = Color.blue(rgb);
                input[i][j][0] = R;
                input[i][j][1] = G;
                input[i][j][2] = B;
            }
        }

        float[][] C = new float[3][304*304];

        int counter = 0;
        for(int i=0; i<304; i++)
            for(int j=0; j<304; j++) {
                C[0][counter] = (float) -Math.log(input[i][j][0]/255.0);
                C[1][counter] = (float) -Math.log(input[i][j][1]/255.0);
                C[2][counter] = (float) -Math.log(input[i][j][2]/255.0);
                counter += 1;
            }


        float[][] C_add = new float[2][3];
        float[][] C_b = new float[2][1];

        C_add[0][0] = (float) 49.498073;
        C_add[0][1] = (float) -26.59762992;
        C_add[0][2] = (float) 15.80793166;
        C_add[1][0] = (float) -23.99799413;
        C_add[1][1] = (float) 21.06695593;
        C_add[1][2] = (float) -1.91282112;

        C_b[0][0] = (float) 1.1601;
        C_b[1][0] = (float) 2.8347;

        float[][] q = new float[3][92416];
        for(int i=0; i<2; i++)
            for(int j=0; j<304*304; j++)
            {
                q[i][j] = C_add[i][0]*C[0][j];
                q[i][j] += C_add[i][1]*C[1][j];
                q[i][j] += C_add[i][2]*C[2][j];
            }

        for (int i=0; i<2; i++)
            for(int j=0; j<304*304; j++)
            {
                q[i][j] = q[i][j] - C_b[i][0];
            }

        float[][] p = new float[3][2];

        p[0][0] = (float) 0.0246;
        p[0][1] = (float) 0.0;
        p[1][0] = (float) 0.0316;
        p[1][1] = (float) 0.0;
        p[2][0] = (float) 0.0394;
        p[2][1] = (float) 0.0;

        float[][] I1 = new float[92416][3];

        for(int i=0; i<92416; i++)
            for(int j=0; j<3; j++)
            {
                I1[i][j] = (float) Math.exp(-(p[j][0] * q[0][i]));

                if(I1[i][j] < 0.0){    I1[i][j] = 0;   }
                if(I1[i][j] > 1.0){    I1[i][j] = 1;   }

            }

        Bitmap bitmap_mela = Bitmap.createBitmap(304, 304, Bitmap.Config.ARGB_8888);

        for(int i=0; i<304; i++)
            for(int j=0; j<304; j++)
            {
                bitmap_mela.setPixel(j, i, Color.rgb(I1[304*i + j][0], I1[304*i + j][1], I1[304*i + j][2]));
            }

        float[][] pp = new float[3][2];

        pp[0][0] = (float) 0.0;
        pp[0][1] = (float) 0.0193;
        pp[1][0] = (float) 0.0;
        pp[1][1] = (float) 0.0755;
        pp[2][0] = (float) 0.0;
        pp[2][1] = (float) 0.0666;

        float[][] I2 = new float[92416][3];

        for(int i=0; i<92416; i++)
            for(int j=0; j<3; j++)
            {
                I2[i][j] = (float) Math.exp(-(pp[j][1] * q[1][i]));
                if(I2[i][j] < 0.0){    I2[i][j] = 0;   }
                if(I2[i][j] > 1.0){    I2[i][j] = 1;   }
            }

        Bitmap bitmap_hemo = Bitmap.createBitmap(304, 304, Bitmap.Config.ARGB_8888);

        for(int i=0; i<304; i++)
            for(int j=0; j<304; j++)
            {
                bitmap_hemo.setPixel(j, i, Color.rgb(I2[304*i + j][0], I2[304*i + j][1], I2[304*i + j][2]));
            }
        Bitmap[] combined = new Bitmap[2];
        combined[0] = bitmap_hemo;
        combined[1] = bitmap_mela;
        return combined;
    }

    void uploadImage(Bitmap bitmap, String filename){
        StorageReference imageRef = storageRef.child("images/"+filename +".jpg");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = imageRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
            }
        });
    }

    String outputToString(float[][] op){
        ArrayList<EfficientOuput> eo = new ArrayList<>();

        String[] outputNames = {"Acne", "Actinic", "Atopic", "Bullous", "Cellulitis", "Eczema", "Exanthems", "Herpes",
                "Hives", "Light Disease", "Lupus", "Contact Dermititis", "Psoriasis", "Scabies",
                "Systemic", "Tinea", "Vasculitis", "Warts"};


        for(int i=0;i<18;i++){
            eo.add(new EfficientOuput(outputNames[i], op[0][i]));
        }

        Collections.sort(eo);



        String result = "";
        for(int i=0;i<4;i++){
            float data = eo.get(i).getData();
            data = data*100;
            String strData = new String();
            if(data>=1) strData = String.format("%.0f", data);
            else if(data>=0.1) strData = String.format("%.1f", data);
            else if(data>=0.01) strData = String.format("%.2f", data);
            else strData = String.format("%.3f", data);
            result = result + eo.get(i).getName() + ": " +strData+"%, ";
        }
        float data = eo.get(4).getData();
        data = data*100;
        String strData = new String();
        if(data>=1) strData = String.format("%.0f", data);
        else if(data>=0.1) strData = String.format("%.1f", data);
        else if(data>=0.01) strData = String.format("%.2f", data);
        else strData = String.format("%.3f", data);
        result = result + eo.get(4).getName() + ": " +strData+"%";

        return result;
    }

    String outputToData(float[][] op){
        ArrayList<EfficientOuput> eo = new ArrayList<>();

        String[] outputNames = {"Acne", "Actinic", "Atopic", "Bullous", "Cellulitis", "Eczema", "Exanthems", "Herpes",
                "Hives", "Light Disease", "Lupus", "Contact Dermititis", "Psoriasis", "Scabies",
                "Systemic", "Tinea", "Vasculitis", "Warts"};


        for(int i=0;i<18;i++){
            eo.add(new EfficientOuput(outputNames[i], op[0][i]));
        }

        Collections.sort(eo);

        String name = eo.get(0).getName();
        Float confidence = eo.get(0).getData()*100;

        String result = "Name: "+name + "\nConfidence: ";
        if(confidence>=1) result = result + String.format("%.0f", confidence);
        else if(confidence>=0.1) result = result + String.format("%.1f", confidence);
        else result = result + String.format("%.2f", confidence);

        return result;
    }




    public boolean onTouch(View v, MotionEvent ev){
        boolean handledHere = false;

        final int action = ev.getAction();

        final int evX = (int)ev.getX();
        final int evY = (int)ev.getY();

        ImageView imageView = (ImageView)v.findViewById(R.id.image_view);

        final int imSize = imageView.getLayoutParams().width;
        final int changeSize = 304;
        if(imageView == null) return false;

        switch(action){
            case MotionEvent.ACTION_UP:
                int changeX = evX * changeSize / imSize;
                int changeY = evY * changeSize / imSize;
                int inNum = -1;
                for(int i=0;i<ranges.size();i++){
                    if(ranges.get(i).isIn(changeX, changeY)){
                        inNum = i;
                        break;
                    }
                }
                if(inNum==-1){
                    Log.d("asdfasdf", "not inside contour   : " + ranges.size());
                }
                else{
                    Character numb = (char)('A'+inNum);
                    Toast.makeText(getApplicationContext(), numb+" ", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, PopupActivity.class);
                    intent.putExtra("image", croppedBitmaps.get(inNum));
                    intent.putExtra("numb", numb+"");
                    intent.putExtra("data", outputToData(bmpOutputs[inNum]));
                    startActivityForResult(intent, 1);
                }
                break;
        }
        return true;
    }
}