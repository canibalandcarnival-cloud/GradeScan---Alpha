package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.theme.GradeScanTheme
import com.example.ui.viewmodel.GradeScanViewModel
import com.example.util.ImageProcessor
import kotlinx.coroutines.launch

const val ROUTE_HOME = "home"
const val ROUTE_RESULTS = "results"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            GradeScanTheme {
                GradeScanApp()
            }
        }
    }
}

@Composable
fun GradeScanApp(
    viewModel: GradeScanViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var pendingCaptureUriString by rememberSaveable { mutableStateOf<String?>(null) }

    val navigateToResults = remember(navController) {
        {
            navController.navigate(ROUTE_RESULTS) {
                launchSingleTop = true
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onImageSelected(uri)
            navigateToResults()
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uri = pendingCaptureUriString?.let(Uri::parse)
        if (success && uri != null) {
            viewModel.onImageSelected(uri)
            navigateToResults()
        }
    }

    val launchCameraCapture = remember(context, takePictureLauncher) {
        {
            val captureUri = ImageProcessor.createCameraCaptureUri(context)
            pendingCaptureUriString = captureUri.toString()
            takePictureLauncher.launch(captureUri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            launchCameraCapture()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.camera_permission_denied)
                )
            }
        }
    }

    val onCapturePhotoRequested = remember(context, launchCameraCapture, cameraPermissionLauncher) {
        {
            val hasCameraPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (hasCameraPermission) {
                launchCameraCapture()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    val onUploadPhotoRequested = remember(photoPickerLauncher) {
        {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    // Trigger Android Share Sheet when .xmp export completes
    LaunchedEffect(uiState.pendingShareIntent) {
        val shareIntent = uiState.pendingShareIntent ?: return@LaunchedEffect
        viewModel.consumeShareIntent()
        context.startActivity(shareIntent)
    }

    // Surface export confirmation / errors in Snackbar
    LaunchedEffect(uiState.exportMessage) {
        val message = uiState.exportMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearExportMessage()
    }

    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME
    ) {
        composable(ROUTE_HOME) {
            HomeScreen(
                snackbarHostState = snackbarHostState,
                onUploadPhotoClick = onUploadPhotoRequested,
                onCapturePhotoClick = onCapturePhotoRequested
            )
        }

        composable(ROUTE_RESULTS) {
            ResultsScreen(
                state = uiState,
                snackbarHostState = snackbarHostState,
                onBackClick = {
                    viewModel.resetSession()
                    navController.popBackStack()
                },
                onNewPhotoClick = {
                    viewModel.resetSession()
                    navController.popBackStack()
                },
                onRetryClick = { viewModel.retryAnalysis() },
                onResetAiClick = { viewModel.resetToAiValues() },
                onChannelSelected = { channel -> viewModel.selectHistogramChannel(channel) },
                onUpdateProfile = { transform -> viewModel.updateActiveProfile(transform) },
                onExportClick = { viewModel.exportXmpPreset() }
            )
        }
    }
}
