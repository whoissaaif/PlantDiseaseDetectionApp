
package com.example.plantdiseasedetectionapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.plantdiseasedetectionapp.ui.theme.PlantDiseaseDetectionAppTheme
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlantDiseaseDetectionAppTheme {
                PlantGuardApp()
            }
        }
    }
}

@Composable
fun PlantGuardApp() {
    val navController = rememberNavController()
    // Sets up the navigation between different screens in the app
    NavHost(navController = navController, startDestination = "upload") {
        composable("upload") {
            UploadScreen(navController = navController)
        }
        composable("history") {
            HistoryScreen(navController = navController)
        }
        composable("result/{prediction}") { backStackEntry ->
            val prediction = backStackEntry.arguments?.getString("prediction")?.replace("_", " ") ?: "No prediction"
            ResultScreen(navController = navController, prediction = prediction)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(navController: NavController) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                bitmap = BitmapFactory.decodeStream(inputStream)
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { newBitmap: Bitmap? ->
            bitmap = newBitmap
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                cameraLauncher.launch()
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plant Guard") },
                actions = {
                    // History button to navigate to the history screen
                    IconButton(onClick = { navController.navigate("history") }) {
                        Icon(Icons.Filled.History, contentDescription = "History")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Display the selected image or a placeholder text
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .height(300.dp)
                        .fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("No image selected.")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Buttons for user actions
            Button(onClick = {
                when (PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) -> {
                        cameraLauncher.launch()
                    }
                    else -> {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }) {
                Text("Take Photo")
            }

            Button(onClick = { galleryLauncher.launch("image/*") }) {
                Text("Upload from Gallery")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Analyze button is enabled only when an image is selected
            Button(
                onClick = {
                    bitmap?.let { bm ->
                        val prediction = analyzeImage(context, bm)
                        navController.navigate("result/$prediction")
                    }
                },
                enabled = bitmap != null
            ) {
                Text("Analyze")
            }
        }
    }
}

fun analyzeImage(context: Context, bitmap: Bitmap): String {
    try {
        val modelName = "model.tflite"

        // Load TFLite model
        val assetFileDescriptor = context.assets.openFd(modelName)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        val interpreter = Interpreter(modelBuffer)

        // Manually load labels from labels.txt
        val labels = context.assets.open("labels.txt").bufferedReader().useLines { it.toList() }

        // Get model input and output details
        val inputTensor = interpreter.getInputTensor(0)
        val outputTensor = interpreter.getOutputTensor(0)
        val inputShape = inputTensor.shape()
        val inputHeight = inputShape[1]
        val inputWidth = inputShape[2]

        // Preprocess the image
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputHeight, inputWidth, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        var tensorImage = TensorImage(inputTensor.dataType())
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // Run inference
        val outputBuffer = Array(1) { FloatArray(outputTensor.shape()[1]) }
        interpreter.run(tensorImage.buffer, outputBuffer)

        // Process the output
        val probabilities = outputBuffer[0]
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1

        return if (maxIndex != -1 && maxIndex < labels.size) {
            labels[maxIndex]
        } else {
            "Prediction failed"
        }

    } catch (e: Exception) {
        Log.e("AnalyzeImage", "Error analyzing image", e)
        return e.message ?: "An unknown error occurred"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnosis History") },
                navigationIcon = {
                    // Back button to return to the previous screen
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("History will be displayed here.")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UploadScreenPreview() {
    PlantDiseaseDetectionAppTheme {
        // A preview of the UploadScreen for design purposes
        val navController = rememberNavController()
        UploadScreen(navController)
    }
}
