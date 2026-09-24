package com.example
 
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.camera.CameraPermissionScreen
import com.example.camera.CameraScreen
import com.example.camera.CameraViewModel
import com.example.camera.DeviceStabilityManager
import com.example.camera.PhotoPreviewScreen
import com.example.ui.theme.CameraBlack
import com.example.ui.theme.MyApplicationTheme

/**
 * Actividad principal de la aplicación de cámara.
 * Gestiona el ciclo de vida, la verificación de permisos (Cámara y Micrófono) y la navegación
 * entre el visor principal (Foto y Vídeo) y la vista previa de capturas.
 */
class MainActivity : ComponentActivity() {

    private val cameraViewModel: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                // Gestor de estabilidad del dispositivo mediante acelerómetro
                val stabilityManager = remember { DeviceStabilityManager(context) }

                DisposableEffect(lifecycleOwner) {
                    lifecycleOwner.lifecycle.addObserver(stabilityManager)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(stabilityManager)
                    }
                }

                // Recolectar estado de estabilidad física y actualizar el ViewModel
                val isDeviceSteady by stabilityManager.isDeviceSteady.collectAsStateWithLifecycle()
                LaunchedEffect(isDeviceSteady) {
                    cameraViewModel.setDeviceSteady(isDeviceSteady)
                }

                // Estado de verificación del permiso de cámara
                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                // Launcher para solicitar permiso de micrófono al grabar vídeos
                val audioPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    cameraViewModel.setHasAudioPermission(isGranted)
                }

                // Re-verificar permisos al reanudar la app (por ejemplo tras volver de Ajustes)
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val camGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            hasCameraPermission = camGranted

                            val audioGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            cameraViewModel.setHasAudioPermission(audioGranted)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // Chequeo inicial del permiso de audio y detección de hardware gráfico (Vulkan 1.1 / OpenGL ES)
                LaunchedEffect(Unit) {
                    val audioGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    cameraViewModel.setHasAudioPermission(audioGranted)
                    cameraViewModel.detectGraphicsHardware(context)
                }

                val uiState by cameraViewModel.uiState.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CameraBlack),
                    color = CameraBlack
                ) {
                    when {
                        // 1. Si no tiene permiso de cámara, mostrar pantalla informativa de permisos
                        !hasCameraPermission -> {
                            CameraPermissionScreen(
                                onPermissionGranted = { hasCameraPermission = true }
                            )
                        }

                        // 2. Si hay una captura seleccionada para ver, mostrar visor a pantalla completa
                        uiState.selectedPhotoForPreview != null -> {
                            PhotoPreviewScreen(
                                photoUri = uiState.selectedPhotoForPreview!!,
                                onBack = { cameraViewModel.closePhotoPreview() },
                                onDeletePhoto = { uri ->
                                    cameraViewModel.deletePhoto(context, uri)
                                }
                            )
                        }

                        // 3. Pantalla independiente de Calibración de Color (Anti-Colores Lavados y Pasteles)
                        uiState.isColorCalibrationOpen -> {
                            com.example.camera.ColorCalibrationScreen(
                                uiState = uiState,
                                onBack = { cameraViewModel.openColorCalibration(false) },
                                onToggleAntiWashedMode = { cameraViewModel.toggleAntiWashedMode() },
                                onSelectColorProfile = { profile -> cameraViewModel.setColorProfile(profile) },
                                onSelectExposureEv = { ev -> cameraViewModel.setExposureEv(ev) },
                                onResetDefaults = { cameraViewModel.resetColorCalibrationToDefaults() }
                            )
                        }

                        // 4. Pantalla independiente de Configuración General
                        uiState.isSettingsOpen -> {
                            com.example.camera.SettingsScreen(
                                uiState = uiState,
                                onBack = { cameraViewModel.openSettings(false) },
                                onOpenColorCalibration = { cameraViewModel.openColorCalibration(true) },
                                onToggleGrid = { cameraViewModel.toggleGrid() },
                                onSelectAspectRatio = { ratio -> cameraViewModel.setAspectRatio(ratio) },
                                onSelectGraphicsBackend = { backend -> cameraViewModel.setSelectedGraphicsBackend(backend) },
                                onSetBeautyIntensity = { intensity -> cameraViewModel.setBeautyFilterIntensity(intensity) }
                            )
                        }

                        // 5. Pantalla principal de la cámara (Fotografía y Grabación de Vídeo)
                        else -> {
                            CameraScreen(
                                uiState = uiState,
                                onSetCaptureMode = { mode -> cameraViewModel.setCaptureMode(mode) },
                                onToggleFlash = { cameraViewModel.toggleFlashMode() },
                                onToggleLens = { cameraViewModel.toggleLensFacing() },
                                onToggleGrid = { cameraViewModel.toggleGrid() },
                                onToggleMaxMegapixels = { cameraViewModel.toggleMaxMegapixels() },
                                onZoomChanged = { zoom -> cameraViewModel.setZoomRatio(zoom) },
                                onCaptureStarted = { cameraViewModel.onCaptureStarted() },
                                onPhotoCaptured = { uri, path ->
                                    cameraViewModel.onPhotoCaptured(uri, path)
                                },
                                onCaptureError = { errorMsg ->
                                    cameraViewModel.onCaptureError(errorMsg)
                                },
                                onVideoRecordingStarted = { cameraViewModel.onVideoRecordingStarted() },
                                onVideoDurationUpdate = { seconds ->
                                    cameraViewModel.onVideoDurationUpdate(seconds)
                                },
                                onVideoSaved = { uri, path ->
                                    cameraViewModel.onVideoSaved(uri, path)
                                },
                                onVideoError = { errorMsg ->
                                    cameraViewModel.onVideoError(errorMsg)
                                },
                                onSelectQuality = { quality -> cameraViewModel.setVideoQuality(quality) },
                                onSelectFps = { fps -> cameraViewModel.setVideoFps(fps) },
                                onToggleAudio = { cameraViewModel.toggleAudioRecording() },
                                onToggleHdr = { cameraViewModel.toggleHdrVideo() },
                                onOpenVideoSettings = { open -> cameraViewModel.openVideoSettings(open) },
                                onCapabilitiesDetected = { caps ->
                                    cameraViewModel.setVideoCapabilities(caps)
                                },
                                onPhotoCapabilitiesDetected = { photoCaps ->
                                    cameraViewModel.setPhotoCapabilities(photoCaps)
                                },
                                onThumbnailClick = { uri ->
                                    cameraViewModel.openPhotoPreview(uri)
                                },
                                onClearUserMessage = { cameraViewModel.clearUserMessage() },
                                onRequestAudioPermission = {
                                    val isAudioGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!isAudioGranted) {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                onZoomLimitsDetected = { minZoom, maxZoom ->
                                    cameraViewModel.setZoomLimits(minZoom, maxZoom)
                                },
                                onOpenSettings = { cameraViewModel.openSettings(true) },
                                onExposureLimitsDetected = { min, max, step, isSupported ->
                                    cameraViewModel.setExposureLimits(min, max, step, isSupported)
                                },
                                onOpenHighResInfoDialog = { open ->
                                    cameraViewModel.setHighResInfoDialogOpen(open)
                                },
                                onToggleBeautyFilter = {
                                    cameraViewModel.toggleBeautyFilter()
                                },
                                onSelectAspectRatio = { ratio ->
                                    cameraViewModel.setAspectRatio(ratio)
                                },
                                onToggleAspectRatioSelector = {
                                    cameraViewModel.toggleAspectRatioSelector()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
