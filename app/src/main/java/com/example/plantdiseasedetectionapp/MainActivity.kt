
package com.example.plantdiseasedetectionapp

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.plantdiseasedetectionapp.ui.theme.PlantDiseaseDetectionAppTheme

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
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    // Creates a launcher to open the device's gallery for image selection
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            imageUri = uri
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
            if (imageUri != null) {
                // Decode the image URI to a Bitmap to display it
                val inputStream = context.contentResolver.openInputStream(imageUri!!)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                Image(
                    bitmap = bitmap.asImageBitmap(),
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
            Button(onClick = { /* TODO: Implement camera logic */ }) {
                Text("Take Photo")
            }

            Button(onClick = { galleryLauncher.launch("image/*") }) {
                Text("Upload from Gallery")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Analyze button is enabled only when an image is selected
            Button(
                onClick = {
                    // For now, we'll navigate with a dummy prediction.
                    // TODO: Replace with actual model inference.
                    navController.navigate("result/Potato___Early_blight")
                },
                enabled = imageUri != null
            ) {
                Text("Analyze")
            }
        }
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
