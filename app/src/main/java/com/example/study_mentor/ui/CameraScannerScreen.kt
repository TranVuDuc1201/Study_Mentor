package com.example.study_mentor.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.study_mentor.data.ChatMessage
import com.example.study_mentor.ui.theme.BackgroundOffWhite
import com.example.study_mentor.ui.theme.PrimaryBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * Creates a temporary image file in the application cache and returns a content URI
 * provided by FileProvider for camera capture.
 */
fun createImageUri(context: Context): Uri {
    val tempFile = File.createTempFile(
        "IMG_${System.currentTimeMillis()}_",
        ".jpg",
        context.cacheDir
    ).apply {
        createNewFile()
    }
    return FileProvider.getUriForFile(
        context,
        "com.example.study_mentor.fileprovider",
        tempFile
    )
}

// ─── High-Performance Crop Frame State (Zero Recompositions) ────────────────

enum class DragHandle {
    NONE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER
}

/**
 * Encapsulates the crop frame coordinates using mutableFloatStateOf.
 * These state values are read exclusively in the Draw phase (drawWithContent)
 * and Layout phase (offset lambda), completely bypassing the Composition phase.
 * Result: 0 recompositions and 0 allocations during dragging gestures.
 */
class CropFrameState {
    var left by mutableFloatStateOf(0f)
    var top by mutableFloatStateOf(0f)
    var right by mutableFloatStateOf(0f)
    var bottom by mutableFloatStateOf(0f)
    var isInitialized by mutableStateOf(false)

    val width: Float get() = right - left
    val height: Float get() = bottom - top

    val rect: Rect get() = Rect(left, top, right, bottom)

    fun initialize(initLeft: Float, initTop: Float, initRight: Float, initBottom: Float) {
        if (!isInitialized) {
            left = initLeft
            top = initTop
            right = initRight
            bottom = initBottom
            isInitialized = true
        }
    }

    fun onDrag(
        handle: DragHandle,
        dragAmount: Offset,
        bounds: Rect,
        minWidth: Float,
        minHeight: Float
    ) {
        when (handle) {
            DragHandle.TOP_LEFT -> {
                left = (left + dragAmount.x).coerceIn(bounds.left, right - minWidth)
                top = (top + dragAmount.y).coerceIn(bounds.top, bottom - minHeight)
            }
            DragHandle.TOP_RIGHT -> {
                right = (right + dragAmount.x).coerceIn(left + minWidth, bounds.right)
                top = (top + dragAmount.y).coerceIn(bounds.top, bottom - minHeight)
            }
            DragHandle.BOTTOM_LEFT -> {
                left = (left + dragAmount.x).coerceIn(bounds.left, right - minWidth)
                bottom = (bottom + dragAmount.y).coerceIn(top + minHeight, bounds.bottom)
            }
            DragHandle.BOTTOM_RIGHT -> {
                right = (right + dragAmount.x).coerceIn(left + minWidth, bounds.right)
                bottom = (bottom + dragAmount.y).coerceIn(top + minHeight, bounds.bottom)
            }
            DragHandle.CENTER -> {
                val currentWidth = right - left
                val currentHeight = bottom - top
                val newLeft = (left + dragAmount.x).coerceIn(bounds.left, bounds.right - currentWidth)
                val newTop = (top + dragAmount.y).coerceIn(bounds.top, bounds.bottom - currentHeight)
                left = newLeft
                right = newLeft + currentWidth
                top = newTop
                bottom = newTop + currentHeight
            }
            DragHandle.NONE -> Unit
        }
    }
}

/**
 * Hit-test detection with a generous touch radius (> 48x48dp per Material guidelines).
 */
fun getDragHandle(
    offset: Offset,
    rect: Rect,
    touchRadius: Float
): DragHandle {
    val dTL = (offset - Offset(rect.left, rect.top)).getDistance()
    val dTR = (offset - Offset(rect.right, rect.top)).getDistance()
    val dBL = (offset - Offset(rect.left, rect.bottom)).getDistance()
    val dBR = (offset - Offset(rect.right, rect.bottom)).getDistance()

    val minCornerDist = minOf(dTL, dTR, dBL, dBR)
    if (minCornerDist <= touchRadius) {
        return when (minCornerDist) {
            dTL -> DragHandle.TOP_LEFT
            dTR -> DragHandle.TOP_RIGHT
            dBL -> DragHandle.BOTTOM_LEFT
            else -> DragHandle.BOTTOM_RIGHT
        }
    }

    if (rect.contains(offset)) {
        return DragHandle.CENTER
    }

    return DragHandle.NONE
}

// ─── Bitmap Cropping Utility ────────────────────────────────────────────────

/**
 * Accurately crops the captured photo bitmap based on the relative position of the
 * resizable crop frame inside the preview container, accounting for sensor orientation
 * and PreviewView's ScaleType.FILL_CENTER.
 */
fun cropCapturedImage(
    context: Context,
    rawPhotoFile: File,
    cropRect: Rect,
    previewWidth: Int,
    previewHeight: Int
): Uri {
    val originalBitmap = BitmapFactory.decodeFile(rawPhotoFile.absolutePath)
        ?: throw IllegalStateException("Failed to decode captured photo")

    val exif = ExifInterface(rawPhotoFile.absolutePath)
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )
    val rotationDegrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }

    val rotatedBitmap = if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(
            originalBitmap,
            0,
            0,
            originalBitmap.width,
            originalBitmap.height,
            matrix,
            true
        )
        if (rotated != originalBitmap) originalBitmap.recycle()
        rotated
    } else {
        originalBitmap
    }

    val bWidth = rotatedBitmap.width.toFloat()
    val bHeight = rotatedBitmap.height.toFloat()
    val pWidth = previewWidth.toFloat()
    val pHeight = previewHeight.toFloat()

    val scale = maxOf(pWidth / bWidth, pHeight / bHeight)
    val scaledBWidth = bWidth * scale
    val scaledBHeight = bHeight * scale

    val excessX = (scaledBWidth - pWidth) / 2f
    val excessY = (scaledBHeight - pHeight) / 2f

    val cropX = ((cropRect.left + excessX) / scale).toInt()
    val cropY = ((cropRect.top + excessY) / scale).toInt()
    val cropW = (cropRect.width / scale).toInt()
    val cropH = (cropRect.height / scale).toInt()

    val safeX = cropX.coerceIn(0, rotatedBitmap.width - 1)
    val safeY = cropY.coerceIn(0, rotatedBitmap.height - 1)
    val safeW = cropW.coerceIn(1, rotatedBitmap.width - safeX)
    val safeH = cropH.coerceIn(1, rotatedBitmap.height - safeY)

    val croppedBitmap = Bitmap.createBitmap(
        rotatedBitmap,
        safeX,
        safeY,
        safeW,
        safeH
    )
    if (croppedBitmap != rotatedBitmap) {
        rotatedBitmap.recycle()
    }

    val croppedFile = File.createTempFile(
        "CROP_MATH_${System.currentTimeMillis()}_",
        ".jpg",
        context.cacheDir
    )
    FileOutputStream(croppedFile).use { outStream ->
        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outStream)
    }
    croppedBitmap.recycle()

    return FileProvider.getUriForFile(
        context,
        "com.example.study_mentor.fileprovider",
        croppedFile
    )
}

// ─── Camera Preview Composable ──────────────────────────────────────────────

@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    onCameraReady: (Camera) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentCameraControl by remember { mutableStateOf<CameraControl?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(currentCameraControl) {
        previewView.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                val factory = previewView.meteringPointFactory
                val point = factory.createPoint(motionEvent.x, motionEvent.y)
                val action = FocusMeteringAction.Builder(point).build()
                try {
                    currentCameraControl?.startFocusAndMetering(action)
                } catch (e: Exception) {
                    Log.w("CameraPreview", "Focus failed: ${e.message}")
                }
                view.performClick()
            }
            true
        }
    }

    DisposableEffect(lifecycleOwner, cameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                currentCameraControl = camera.cameraControl
                onCameraReady(camera)
            } catch (e: Exception) {
                Log.e("CameraPreview", "Camera binding failed", e)
            }
        }, executor)

        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (e: Exception) {
                Log.e("CameraPreview", "Camera unbind error", e)
            }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

// ─── Optimized Resizable Viewfinder (Zero Recomposition on Drag) ────────────

@Composable
fun ResizableViewfinder(
    state: CropFrameState,
    bounds: Rect,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    // Touch detection area: 36dp radius (72dp diameter), exceeding the 48x48dp Material requirement
    val touchRadius = with(density) { 36.dp.toPx() }
    val minWidth = with(density) { 100.dp.toPx() }
    val minHeight = with(density) { 100.dp.toPx() }
    var currentHandle by remember { mutableStateOf(DragHandle.NONE) }
    val reusablePath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(bounds, minWidth, minHeight) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentHandle = getDragHandle(offset, state.rect, touchRadius)
                    },
                    onDragEnd = {
                        currentHandle = DragHandle.NONE
                    },
                    onDragCancel = {
                        currentHandle = DragHandle.NONE
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        state.onDrag(currentHandle, dragAmount, bounds, minWidth, minHeight)
                    }
                )
            }
            .drawWithContent {
                drawContent()

                if (!state.isInitialized) return@drawWithContent

                val l = state.left
                val t = state.top
                val r = state.right
                val b = state.bottom
                val w = r - l
                val h = b - t
                val cornerRadius = 12.dp.toPx()

                // 1. Dim the area outside the crop frame with Color.Black.copy(alpha = 0.6f)
                reusablePath.rewind()
                reusablePath.fillType = PathFillType.EvenOdd
                reusablePath.addRect(Rect(0f, 0f, size.width, size.height))
                reusablePath.addRoundRect(
                    RoundRect(
                        rect = Rect(l, t, r, b),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                )
                drawPath(path = reusablePath, color = Color.Black.copy(alpha = 0.6f))

                // 2. White outline stroke around the selection area
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(l, t),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    style = Stroke(width = 2.dp.toPx())
                )

                // 3. Photomath-style Corner Brackets (accents)
                val bracketLen = 22.dp.toPx()
                val bracketStroke = 4.dp.toPx()
                val bracketColor = Color.White

                // Top-Left bracket
                drawLine(bracketColor, Offset(l, t + bracketLen), Offset(l, t), bracketStroke)
                drawLine(bracketColor, Offset(l, t), Offset(l + bracketLen, t), bracketStroke)

                // Top-Right bracket
                drawLine(bracketColor, Offset(r - bracketLen, t), Offset(r, t), bracketStroke)
                drawLine(bracketColor, Offset(r, t), Offset(r, t + bracketLen), bracketStroke)

                // Bottom-Left bracket
                drawLine(bracketColor, Offset(l, b - bracketLen), Offset(l, b), bracketStroke)
                drawLine(bracketColor, Offset(l, b), Offset(l + bracketLen, b), bracketStroke)

                // Bottom-Right bracket
                drawLine(bracketColor, Offset(r - bracketLen, b), Offset(r, b), bracketStroke)
                drawLine(bracketColor, Offset(r, b), Offset(r, b - bracketLen), bracketStroke)

                // 4. Visual circular touch handle dots at all 4 corners
                val cornerDotRadius = 8.dp.toPx()
                val cornerInnerDotRadius = 4.dp.toPx()
                val corners = listOf(
                    Offset(l, t),
                    Offset(r, t),
                    Offset(l, b),
                    Offset(r, b)
                )

                corners.forEach { corner ->
                    drawCircle(
                        color = Color.White,
                        radius = cornerDotRadius,
                        center = corner
                    )
                    drawCircle(
                        color = PrimaryBlue,
                        radius = cornerInnerDotRadius,
                        center = corner
                    )
                }
            }
    ) {
        // Floating prompt guide pill - positioned in Layout phase via Modifier.offset to skip Recomposition
        if (state.isInitialized) {
            val pillHeightPx = with(density) { 36.dp.toPx() }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        val preferredY = state.bottom + 12.dp.toPx()
                        val targetY = if (preferredY + pillHeightPx > bounds.bottom) {
                            (state.top - pillHeightPx - 12.dp.toPx()).coerceAtLeast(bounds.top)
                        } else {
                            preferredY
                        }
                        IntOffset(0, targetY.roundToInt())
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Drag corners to resize • Drag center to move",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// ─── Permission Rationale Composable ────────────────────────────────────────

@Composable
fun CameraPermissionRationale(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 80.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Study Mentor needs camera access to scan and solve math problems directly from your notes or textbook.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Grant Camera Access",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            TextButton(
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                }
            ) {
                Text(
                    text = "Open App Settings",
                    color = Color(0xFF93C5FD),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ─── Main Screen Composable ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScannerScreen(
    onBackClick: () -> Unit,
    chatViewModel: ChatViewModel = viewModel(),
    initialQuestion: String = ""
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by chatViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    BackHandler {
        chatViewModel.startNewChat()
        onBackClick()
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var previewContainerSize by remember { mutableStateOf(IntSize.Zero) }
    val cropFrameState = remember { CropFrameState() }

    LaunchedEffect(previewContainerSize) {
        if (previewContainerSize.width > 0 && previewContainerSize.height > 0 && !cropFrameState.isInitialized) {
            val w = previewContainerSize.width.toFloat()
            val h = previewContainerSize.height.toFloat()

            val initWidth = (w * 0.82f).coerceIn(
                with(density) { 200.dp.toPx() },
                with(density) { 360.dp.toPx() }
            )
            val initHeight = with(density) { 200.dp.toPx() }
            val initLeft = (w - initWidth) / 2f
            val initTop = (h - initHeight) * 0.35f
            cropFrameState.initialize(initLeft, initTop, initLeft + initWidth, initTop + initHeight)
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    // ── One-Tap Capture, Crop & Solve Function ──────────────────────────────
    val captureAndSolve: () -> Unit = {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else if (!isCapturing && !uiState.isLoading) {
            isCapturing = true

            val rawPhotoFile = try {
                File.createTempFile("RAW_SCAN_${System.currentTimeMillis()}_", ".jpg", context.cacheDir)
            } catch (e: Exception) {
                Log.e("CameraScannerScreen", "Failed to create temp photo file", e)
                isCapturing = false
                Toast.makeText(context, "Failed to initialize image capture", Toast.LENGTH_SHORT).show()
                null
            }

            if (rawPhotoFile != null) {
                val outputOptions = ImageCapture.OutputFileOptions.Builder(rawPhotoFile).build()
                val mainExecutor = ContextCompat.getMainExecutor(context)

                // Snapshot the current crop rectangle at the exact moment of capture
                val currentCrop = cropFrameState.rect

                imageCapture.takePicture(
                    outputOptions,
                    mainExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val croppedUri = if (cropFrameState.isInitialized && previewContainerSize.width > 0 && previewContainerSize.height > 0) {
                                        cropCapturedImage(
                                            context = context,
                                            rawPhotoFile = rawPhotoFile,
                                            cropRect = currentCrop,
                                            previewWidth = previewContainerSize.width,
                                            previewHeight = previewContainerSize.height
                                        )
                                    } else {
                                        FileProvider.getUriForFile(
                                            context,
                                            "com.example.study_mentor.fileprovider",
                                            rawPhotoFile
                                        )
                                    }

                                    withContext(Dispatchers.Main) {
                                        isCapturing = false
                                        val prompt = uiState.inputText.trim()
                                        chatViewModel.sendImageAndPrompt(
                                            uri = croppedUri,
                                            context = context,
                                            prompt = if (prompt.isNotBlank()) prompt else null
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e("CameraScannerScreen", "Cropping failed", e)
                                    withContext(Dispatchers.Main) {
                                        isCapturing = false
                                        Toast.makeText(context, "Failed to process cropped image: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    try { rawPhotoFile.delete() } catch (_: Exception) {}
                                }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            isCapturing = false
                            Log.e("CameraScannerScreen", "Photo capture failed: ${exception.message}", exception)
                            Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        chatViewModel.captureRequest.collect {
            captureAndSolve()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        chatViewModel.onImageSelected(uri, context)
    }

    LaunchedEffect(initialQuestion) {
        if (initialQuestion.isNotBlank()) {
            chatViewModel.setInputText(initialQuestion)
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .navigationBarsPadding()
            .imePadding()
            .onSizeChanged { previewContainerSize = it }
    ) {
        if (hasCameraPermission) {
            CameraPreview(
                imageCapture = imageCapture,
                modifier = Modifier.fillMaxSize(),
                onCameraReady = { camera ->
                    cameraControl = camera.cameraControl
                }
            )

            if (uiState.messages.isEmpty()) {
                val currentBounds = if (previewContainerSize.width > 0 && previewContainerSize.height > 0) {
                    Rect(
                        0f,
                        with(density) { 70.dp.toPx() },
                        previewContainerSize.width.toFloat(),
                        previewContainerSize.height.toFloat() - with(density) { 180.dp.toPx() }
                    )
                } else {
                    Rect(0f, 0f, Float.MAX_VALUE, Float.MAX_VALUE)
                }

                ResizableViewfinder(
                    state = cropFrameState,
                    bounds = currentBounds,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                )
            }
        } else {
            CameraPermissionRationale(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        }

        // ── Top Bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    chatViewModel.startNewChat()
                    onBackClick()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Ask AI",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))

            if (hasCameraPermission && uiState.messages.isEmpty()) {
                IconButton(
                    onClick = {
                        isTorchOn = !isTorchOn
                        try {
                            cameraControl?.enableTorch(isTorchOn)
                        } catch (e: Exception) {
                            Log.w("CameraScannerScreen", "Torch toggle failed: ${e.message}")
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Torch",
                        tint = if (isTorchOn) Color(0xFFFFD700) else Color.White
                    )
                }
            }

            IconButton(
                onClick = {
                    chatViewModel.startNewChat()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "New Chat",
                    tint = Color.White
                )
            }
        }

        // ── Persistent Bottom Sheet ────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.messages.isNotEmpty() || uiState.isLoading) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 0.dp, max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.messages) { message ->
                            ChatBubble(message = message)
                        }

                        if (uiState.isLoading) {
                            item {
                                AiThinkingBubble()
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                if (uiState.messages.isEmpty()) {
                    Text(
                        text = "OR TYPE YOUR QUESTION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }

                if (uiState.selectedImageUri != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(70.dp)
                        ) {
                            AsyncImage(
                                model = uiState.selectedImageUri,
                                contentDescription = "Selected image preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp))
                            )

                            IconButton(
                                onClick = { chatViewModel.clearSelectedImage() },
                                modifier = Modifier
                                    .size(22.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear selected image",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                // ── Input Row ─────────────────────────────────────────────
                val canSend = (uiState.inputText.isNotBlank() || uiState.selectedImageUri != null) && !uiState.isLoading

                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = { chatViewModel.setInputText(it) },
                    placeholder = { 
                        Text(
                            if (uiState.selectedImageUri != null) "Ask about this photo or tap Send..." 
                            else "Ask something or tap Send to scan..."
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (uiState.messages.isEmpty() && uiState.selectedImageUri == null) {
                                captureAndSolve()
                            } else if (canSend) {
                                chatViewModel.sendMessage()
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.LightGray,
                        focusedContainerColor = BackgroundOffWhite,
                        unfocusedContainerColor = BackgroundOffWhite
                    ),
                    leadingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Attach image from gallery",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { captureAndSolve() },
                                modifier = Modifier.size(36.dp),
                                enabled = !isCapturing && !uiState.isLoading
                            ) {
                                if (isCapturing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = PrimaryBlue,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Capture photo with camera",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (uiState.messages.isEmpty() && uiState.selectedImageUri == null) {
                                    captureAndSolve()
                                } else if (canSend) {
                                    chatViewModel.sendMessage()
                                }
                            },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .background(
                                    if (canSend || uiState.messages.isEmpty()) PrimaryBlue else Color.LightGray,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )

                if (uiState.messages.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionPill("Solve step-by-step") {
                            val prompt = if (uiState.inputText.isNotBlank()) "Solve step-by-step: ${uiState.inputText}" else "Solve step-by-step"
                            chatViewModel.setInputText(prompt)
                            captureAndSolve()
                        }
                        ActionPill("Explain concept") {
                            val prompt = if (uiState.inputText.isNotBlank()) "Explain this concept simply: ${uiState.inputText}" else "Explain this concept simply"
                            chatViewModel.setInputText(prompt)
                            captureAndSolve()
                        }
                        ActionPill("Check my work") {
                            val prompt = if (uiState.inputText.isNotBlank()) "Check my work and correct any mistakes: ${uiState.inputText}" else "Check my work and correct any mistakes"
                            chatViewModel.setInputText(prompt)
                            captureAndSolve()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ─── Chat Bubble Composable ─────────────────────────────────────────────────

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) PrimaryBlue else Color(0xFFF1F5F9),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                color = if (isUser) Color.White else Color(0xFF1E293B),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

// ─── AI Thinking Indicator ──────────────────────────────────────────────────

@Composable
fun AiThinkingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp),
            color = Color(0xFFF1F5F9)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = PrimaryBlue,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "AI is thinking...",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ─── Action Pill Composable ─────────────────────────────────────────────────

@Composable
fun ActionPill(text: String, onClick: () -> Unit = {}) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFDBEAFE),
        modifier = Modifier.padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Text(
            text = text,
            color = PrimaryBlue,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
