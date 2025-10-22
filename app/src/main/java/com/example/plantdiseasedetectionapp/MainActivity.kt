
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
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

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
            val prediction = backStackEntry.arguments?.getString("prediction") ?: "No prediction"
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
    return try {
        val baseOptions = BaseOptions.builder().setNumThreads(4).build()
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(1)
            .build()
        val classifier = ImageClassifier.createFromFileAndOptions(
            context,
            "model.tflite",
            options
        )

        val image = TensorImage.fromBitmap(bitmap)

        val results = classifier.classify(image)

        results.firstOrNull()?.categories?.firstOrNull()?.label ?: "Analysis failed"
    } catch (e: Exception) {
        Log.e("AnalyzeImage", "Error analyzing image", e)
        "Error during analysis"
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
