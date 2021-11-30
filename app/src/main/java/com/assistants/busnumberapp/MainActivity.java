package com.assistants.busnumberapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.loader.content.Loader;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {


	BaseLoaderCallback baseLoaderCallback;
	CameraBridgeViewBase cameraBridgeViewBase;
	private static Net busNet;
	private static Net busNum;
	private static final int REQUEST_CODE = 0;
	private static boolean netPrepared = false;
	private static SensorManager mSensorManager;
	private static Sensor mOrientation;
	private static float[] accelerometerValues;
	public static int width;
	private static int x = 0, y = 1, z = 2;

	SensorManager sensorManager;
	Sensor sensorRot;

	SensorEventListener listenerLight = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			accelerometerValues = event.values;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.activity_main);

		cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.myCameraView);
		//cameraBridgeViewBase.setMaxFrameSize(cameraBridgeViewBase.getWidth(), cameraBridgeViewBase.getHeight());
		cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
		cameraBridgeViewBase.setCvCameraViewListener(this);
		baseLoaderCallback = new BaseLoaderCallback(this) {
			@Override
			public void onManagerConnected(int status) {
				super.onManagerConnected(status);
				switch(status){
					case BaseLoaderCallback.SUCCESS:
						cameraBridgeViewBase.enableView();
						break;
					default:
						super.onManagerConnected(status);
						break;
				}


			}
		};

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);


		getPermissions();

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorRot = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(listenerLight, sensorRot, SensorManager.SENSOR_DELAY_NORMAL);

		Downloading.link = this;

		if(Locale.getDefault().getLanguage().toLowerCase().startsWith("ru")) {

			SpeechGenerator.setLocate(new RUS(), this);

		} else {

			SpeechGenerator.setLocate(new ENG(), this);

		}
    }

	public static void pause(int mSec){
		try {
			Thread.sleep(mSec);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void getPermissions(){

		int permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

		if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{
							Manifest.permission.CAMERA
					},
					REQUEST_CODE);
			while (permissionStatus != PackageManager.PERMISSION_GRANTED){
				pause(100);
				permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
			}
		}

		pause(1000);
	}

//	@Override
//	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//		switch (requestCode) {
//			case REQUEST_CODE:
//				if (grantResults.length > 0
//						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//					// permission granted
//					();
//				} else {
//					// permission denied
//				}
//				return;
//		}
//	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		Downloading.checkFiles();

		if (!Downloading.notLoaded) {

			loadNets();

		} else {

			Downloading.startLoging();

		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(cameraBridgeViewBase!=null){
			cameraBridgeViewBase.disableView();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!OpenCVLoader.initDebug()){
			Toast.makeText(getApplicationContext(),"There's a proble, bruh!", Toast.LENGTH_SHORT).show();
		}

		else
		{
			baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
		}

	}

	@Override
	protected void onDestroy(){

		if (cameraBridgeViewBase!=null){
			cameraBridgeViewBase.disableView();
		}
		super.onDestroy();


	}



	@Override
	public void onCameraViewStopped() {

	}
	boolean isShown = false;
	@Override
	public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {


		Mat frame = inputFrame.rgba();
		sayToLog("Size of frame: (" + frame.size().width + ", " + frame.size().height + ");");




//		if(accelerometerValues[x] < 0) {
//
//
//			Core.rotate(frame, frame, );
//			Math.atan();
//			Imgproc.putText(frame, ".", new Point(10, 3), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 0, 0), 5);
//
//		} else {
//
//			Imgproc.putText(frame, ".", new Point(10, 3), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 5);
//		}

		if(!netPrepared) {
			return frame;
		} else {
			return calcNet(frame);
		}
	}

	public static void sayToLog(String msg){

		Log.d("VisionAssistant", msg);

	}

	public void loadNets(){
		Thread myThready = new Thread(new Runnable() {
			public void run() //Этот метод будет выполняться в побочном потоке
			{

				String cfgPathBuses = getFilesDir() + "/busNet.cfg";
				String weightsPathBuses = getFilesDir() + "/busNet.weights";
				String numsPath = getFilesDir() + "/BUSNUM.pb";

				busNet = Dnn.readNetFromDarknet(cfgPathBuses, weightsPathBuses);
				busNum = Dnn.readNetFromTensorflow(numsPath);

				SpeechGenerator.playGuide(SpeechGenerator.BEGIN);
				netPrepared = true;
			}
		});

		myThready.start();

	}
	private Mat calcNet(Mat frame){

		width = frame.cols();

		Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGBA2RGB);
		Mat blob = Dnn.blobFromImage(frame, 0.00392, new Size(416, 416), new Scalar(0, 0, 0),false, false);

		busNet.setInput(blob);

//		if(Math.abs(accelerometerValues[x]) < Math.abs(accelerometerValues[y])){
//			if(accelerometerValues[y] > 0){
//				Core.rotate(blob, blob, Core.ROTATE_90_CLOCKWISE);
//			}else {
//				Core.rotate(blob, blob, Core.ROTATE_90_COUNTERCLOCKWISE);
//			}
//		}else {
//			if(accelerometerValues[x] < 0){
//				Core.rotate(blob, blob, Core.ROTATE_180);
//			}
//		}

		java.util.List<Mat> result = new java.util.ArrayList<Mat>(2);

		List<String> outBlobNames = new java.util.ArrayList<>();
		outBlobNames.add(0, "yolo_16");
		outBlobNames.add(1, "yolo_23");

		busNet.forward(result, outBlobNames);


		float confThreshold = 0.15f;
		List<Integer> clsIds = new ArrayList<>();
		List<Float> confs = new ArrayList<>();
		List<Rect> rects = new ArrayList<>();
		for (int i = 0; i < result.size(); ++i) {

			Mat level = result.get(i);
			for (int j = 0; j < level.rows(); ++j) {
				Mat row = level.row(j);
				Mat scores = row.colRange(5, level.cols());
				Core.MinMaxLocResult mm = Core.minMaxLoc(scores);

				float confidence = (float) mm.maxVal;


				Point classIdPoint = mm.maxLoc;


				if (confidence > confThreshold) {
					int centerX = (int) (row.get(0, 0)[0] * frame.cols());
					int centerY = (int) (row.get(0, 1)[0] * frame.rows());
					int width = (int) (row.get(0, 2)[0] * frame.cols());
					int height = (int) (row.get(0, 3)[0] * frame.rows());
					int left = centerX - width / 2;
					int top = centerY - height / 2;

					clsIds.add((int) classIdPoint.x);
					confs.add((float) confidence);
					rects.add(new Rect(left, top, width, height));
				}
			}
		}
		int ArrayLength = confs.size();

		if (ArrayLength >= 1) {
			// Apply non-maximum suppression procedure.
			float nmsThresh = 0.2f;
			;
			MatOfFloat confidences = new MatOfFloat(Converters.vector_float_to_Mat(confs));


			Rect[] boxesArray = rects.toArray(new Rect[0]);

			MatOfRect boxes = new MatOfRect(boxesArray);

			MatOfInt indices = new MatOfInt();


			Dnn.NMSBoxes(boxes, confidences, confThreshold, nmsThresh, indices);


			// Draw result boxes:
			int[] ind = indices.toArray();
			for (int i = 0; i < ind.length; ++i) {
				int idx = ind[i];
				Rect box = boxesArray[idx];
				int idGuy = clsIds.get(idx);
				int confGuy = (int) (confs.get(idx) * 100);

				if (idGuy == 3) {


					int horGuy=(int)(((box.x + box.width)-box.x)*0.25);
					int verGuy=(int)(((box.y+box.height)-box.y)*0.25);

					int top =box.y - verGuy;
					int left = box.x - horGuy;
					int right =box.x+box.width    +horGuy;
					int bottom =box.y + box.height   +verGuy;

					Rect rectCropNum = new Rect(new Point(left, top), new Point(right, bottom));

					Mat numROI = new Mat(frame, rectCropNum);




					Mat imageBlobNums = Dnn.blobFromImage(numROI, 0.00392, new Size(54, 54), new Scalar(0, 0, 0), true, false);

					java.util.List<Mat> resultNums = new java.util.ArrayList<Mat>(5);

					List<java.lang.String> outBlobNamesNums = new java.util.ArrayList<>();
					outBlobNamesNums.add(0, "dLength/Softmax");
					outBlobNamesNums.add(1, "d1/Softmax");
					outBlobNamesNums.add(2, "d2/Softmax");
					outBlobNamesNums.add(3, "d3/Softmax");
					outBlobNamesNums.add(4, "d4/Softmax");

					busNum.setInput(imageBlobNums);
					busNum.forward(resultNums,outBlobNamesNums);

//            Core.MinMaxLocResult genScores = Core.minMaxLoc(genderMat);
//            float genConf = (float)genScores.maxVal;
//            Point genId = genScores.maxLoc;




					Core.MinMaxLocResult lengthScores = Core.minMaxLoc(resultNums.get(0));
					Point lengthPoint = lengthScores.maxLoc;
					int length = (int)lengthPoint.x;

					String realNums = "";

					for (int j = 1; j <= length+1; ++j){

						Core.MinMaxLocResult numScores = Core.minMaxLoc(resultNums.get(j));
						Point numPoint = numScores.maxLoc;
						int num = (int)numPoint.x;

						if (num!=10){

							realNums=realNums + num;

						}

					}


					Imgproc.putText(frame, realNums, new Point(left, top), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 130, 100), 3);
					Imgproc.rectangle(frame, new Point(left, top),new Point(right, bottom), new Scalar(255, 255, 0), 2);

					BusSounding.addNum(box, realNums);

				} else {

					if (idGuy==0) {
						Imgproc.rectangle(frame, box.tl(), box.br(), new Scalar(255, 0, 0), 2);
						Imgproc.putText(frame,  "closed " + confGuy + "%", box.tl(), Core.FONT_HERSHEY_SIMPLEX, .7, new Scalar(255, 0, 0), 2);
					}
					if (idGuy==1) {
						Imgproc.rectangle(frame, box.tl(), box.br(), new Scalar(0, 255, 0), 2);
						Imgproc.putText(frame, "open " + confGuy + "%", box.tl(), Core.FONT_HERSHEY_SIMPLEX, .7, new Scalar(0, 255, 0), 2);
					}
					if (idGuy==2) {
						Imgproc.rectangle(frame, box.tl(), box.br(), new Scalar(0, 0, 255), 2);
						Imgproc.putText(frame, "bus " + confGuy + "%", box.tl(), Core.FONT_HERSHEY_SIMPLEX, .7, new Scalar(0, 0, 255), 2);
					}

					BusSounding.addElement(box, idGuy);

				}
			}
			while (SpeechGenerator.isPlaying()) {

				pause(1000);

			}
			BusSounding.playBuses();

		}

		if(accelerometerValues[x] < 0) {
			Core.flip(frame, frame, -1);
		}

		return frame;

	}
}