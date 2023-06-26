package com.werfire.ipmd_labs

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.werfire.ipmd_labs.ui.theme.IPMD_labsTheme
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat

class LabOneActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IPMD_labsTheme {
                if(OpenCVLoader.initDebug())
                    Log.d("Loaded", "YEP")

                CameraPreview(Modifier.fillMaxSize(), this@LabOneActivity)

            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CameraPreview(modifier: Modifier = Modifier,
                  lifecycleOwner: LifecycleOwner
) {
    val context = LocalContext.current
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted)
            Log.d("Camera_perm","PERMISSION GRANTED")
        else
            Log.d("Camera_perm","PERMISSION DENIED")
    }

    var image1 by remember { mutableStateOf<Mat?>(null) }
    var image2 by remember { mutableStateOf<Mat?>(null) }
    var state by remember { mutableStateOf(1) }
    var areUseCasesBound by remember { mutableStateOf(false) }

    if (!cameraPermission.status.isGranted) {
        LaunchedEffect(Unit) {
            launcher.launch(android.Manifest.permission.CAMERA)
        }
    } else {
        Box(modifier = modifier) {
            AndroidView({ PreviewView(context) }, modifier) {previewView ->
                if (!areUseCasesBound) {
                    val executor = ContextCompat.getMainExecutor(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = androidx.camera.core.Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            imageCapture,
                            preview
                        )
                        areUseCasesBound = true
                    }, executor)
                }
            }

            if (image2 != null) {
                val bmp = Bitmap.createBitmap(
                    image2!!.width(),
                    image2!!.height(),
                    Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(image2, bmp)

                bmp.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Blended image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Text(
                fontSize = 60.sp,
                text = when (state) {
                    1 -> "1"
                    2 -> "2"
                    3 -> "2"
                    4 -> "1+2"
                    else -> ""
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                color = Color.White
            )

            Button(
                onClick = {
                    when (state) {
                        1 -> {
                            imageCapture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {

                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        image1 = GetMatFromProxy(image)
                                        image.close()
                                        Log.d("CaptureImage", "Image capture success")
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("CaptureImage", "Image capture failed", exception)
                                    }
                                }
                            )
                            state++
                        }
                        2 -> {
                            imageCapture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        image2 = GetMatFromProxy(image)
                                        image.close()
                                        Log.d("CaptureImage", "Image capture success")
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("CaptureImage", "Image capture failed", exception)
                                    }
                                }
                            )
                            state++
                        }
                        3 -> {
                            image1.let { i1 ->
                                image2.let { i2 ->
                                    val result = Mat()
                                    Core.addWeighted(
                                        image1,
                                        0.5,
                                        image2,
                                        0.5,
                                        0.0,
                                        result
                                    )
                                    image2 = result
                                }
                            }
                            state++
                        }
                        4 -> {
                            // Reset the images and the state
                            image1 = null
                            image2 = null
                            state = 1
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(16.dp)
            ) {
                // Show different icons depending on the state
                Icon(
                    imageVector = when (state) {
                        1, 2 -> Icons.Default.Add
                        3 -> Icons.Default.Build
                        4 -> Icons.Default.Autorenew
                        else -> Icons.Default.Add
                    },
                    contentDescription = "Button icon",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

private fun  GetMatFromProxy(image: ImageProxy): Mat {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC3)
    Utils.bitmapToMat(bitmap, mat)
    return mat
}

