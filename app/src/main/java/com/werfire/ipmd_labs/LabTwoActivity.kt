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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
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
import org.opencv.core.Core.convertScaleAbs
import org.opencv.core.Core.normalize
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class LabTwoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IPMD_labsTheme {
                if(OpenCVLoader.initDebug())
                    Log.d("Loaded", "YEP")

                CameraPreview(Modifier.fillMaxSize(), this@LabTwoActivity)

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

    var image by remember { mutableStateOf<Mat?>(null) }
    var imageInitial by remember { mutableStateOf<Mat?>(null) }
    var state by remember { mutableStateOf(1) }
    var areUseCasesBound by remember { mutableStateOf(false) }
    var kernelType by remember { mutableStateOf(KernelTypes.UNDEFINED) }
    var axis by remember { mutableStateOf(Axis.UNDEFINED) }

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

            if (image != null) {
                val bmp = Bitmap.createBitmap(
                    image!!.width(),
                    image!!.height(),
                    Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(image, bmp)

                bmp.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Result image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            when (state) {
                1 -> Button(
                    onClick = {
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {

                                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                    image = GetMatFromProxy(imageProxy)
                                    imageInitial = image!!.clone()
                                    imageProxy.close()
                                    Log.d("CaptureImage", "Image capture success")
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("CaptureImage", "Image capture failed", exception)
                                }
                            }
                        )
                        state++
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "Button icon",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                2 -> {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                image = Canny(image!!)
                                state = 5
                            }
                        ) {
                            Text(
                                text = "Canny",
                                fontSize = 16.sp
                            )
                        }
                        Button(
                            onClick = {
                                image = Harris(image!!)
                                state = 5
                            }
                        ) {
                            Text(
                                text = "Harris",
                                fontSize = 16.sp
                            )
                        }
                        Button(
                            onClick = {
                                image = Hough(image!!)
                                state = 5
                            }
                        ) {
                            Text(
                                text = "Hough",
                                fontSize = 16.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                kernelType = KernelTypes.SOBEL
                                state++
                            }
                        ) {
                            Text(
                                text = "Sobel",
                                fontSize = 13.sp
                            )
                        }
                        Button(
                            onClick = {
                                kernelType = KernelTypes.PREWITT
                                state++
                            }
                        ) {
                            Text(
                                text = "Prewitt",
                                fontSize = 13.sp
                            )
                        }
                        Button(
                            onClick = {
                                kernelType = KernelTypes.SCHARR
                                state++
                            }
                        ) {
                            Text(
                                text = "Scharr",
                                fontSize = 13.sp
                            )
                        }
                        Button(
                            onClick = {
                                kernelType = KernelTypes.CUSTOM
                                state = 4
                            }
                        ) {
                            Text(
                                text = "Custom",
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                3 -> {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                axis = Axis.X
                                state++
                            }
                        ) {
                            Text(
                                text = "X",
                                fontSize = 30.sp
                            )
                        }
                        Button(
                            onClick = {
                                axis = Axis.Y
                                state++
                            }
                        ) {
                            Text(
                                text = "Y",
                                fontSize = 30.sp
                            )
                        }
                    }
                }
                4 -> {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        val kernel = when (kernelType) {
                            KernelTypes.SOBEL -> {
                                if(axis == Axis.X)
                                    arrayOf(
                                        doubleArrayOf(-1.0, 0.0, 1.0),
                                        doubleArrayOf(-2.0, 0.0, 2.0),
                                        doubleArrayOf(-1.0, 0.0, 1.0)
                                    )
                                else
                                    arrayOf(
                                        doubleArrayOf(-1.0, -2.0, -1.0),
                                        doubleArrayOf(0.0, 0.0, 0.0),
                                        doubleArrayOf(1.0, 2.0, 1.0)
                                    )
                            }
                            KernelTypes.PREWITT -> {
                                if(axis == Axis.X)
                                    arrayOf(
                                        doubleArrayOf(-1.0, 0.0, 1.0),
                                        doubleArrayOf(-1.0, 0.0, 1.0),
                                        doubleArrayOf(-1.0, 0.0, 1.0)
                                    )
                                else
                                    arrayOf(
                                        doubleArrayOf(-1.0, -1.0, -1.0),
                                        doubleArrayOf(0.0, 0.0, 0.0),
                                        doubleArrayOf(1.0, 1.0, 1.0)
                                    )
                            }
                            KernelTypes.SCHARR -> {
                                if(axis == Axis.X)
                                    arrayOf(
                                        doubleArrayOf(-3.0, 0.0, 3.0),
                                        doubleArrayOf(-10.0, 0.0, 10.0),
                                        doubleArrayOf(-3.0, 0.0, 3.0)
                                    )
                                else
                                    arrayOf(
                                        doubleArrayOf(-3.0, -10.0, -3.0),
                                        doubleArrayOf(0.0, 0.0, 0.0),
                                        doubleArrayOf(3.0, 10.0, 3.0)
                                    )
                            }
                            KernelTypes.CUSTOM ->
                                arrayOf(
                                    doubleArrayOf(0.0, -1.0, 0.0),
                                    doubleArrayOf(-1.0, 4.0, -1.0),
                                    doubleArrayOf(0.0, -1.0, 0.0)
                                )
                            KernelTypes.UNDEFINED -> arrayOf(
                                doubleArrayOf(0.0)
                            )
                        }

                        Button(
                            onClick = {
                                image = convolveManual(image!!, kernel, 3)
                                state++
                            }
                        ) {
                            Text(
                                text = "manual",
                                fontSize = 16.sp
                            )
                        }
                        Button(
                            onClick = {
                                image = convolveFilter2D(image!!, kernel, 3)
                                state++
                            }
                        ) {
                            Text(
                                text = "filter2D",
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            if (state > 1) {
                Button(
                    onClick = {
                        kernelType = KernelTypes.UNDEFINED
                        axis = Axis.UNDEFINED

                        if(state == 2) {
                            image = null
                            state = 1
                        } else {
                            image = imageInitial!!.clone()
                            state = 2
                        }
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Button icon",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

private fun Canny(image: Mat): Mat {
    val edges = Mat(image.size(), CvType.CV_8UC1)
    Imgproc.Canny(image, edges, 50.0, 150.0)
    return edges
}

private fun Harris(image: Mat): Mat {
    val resized = Mat()
    val newSize = Size(image.cols() * 0.25, image.rows() * 0.25)
    Imgproc.resize(image, resized, newSize)

    val dest = Mat()
    val destNorm = Mat()
    val destNormScaled = Mat()

    Imgproc.cornerHarris(resized, dest, 2, 5, 0.04)

    normalize(dest, destNorm, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_32FC1)
    convertScaleAbs(destNorm, destNormScaled)

    // Drawing a circle around corners
    val threshold = 140
    for (j in 0 until dest.rows()) {
        for (i in 0 until dest.cols()) {
            if (destNorm.get(j, i)[0] > threshold) {
                //Log.d("VALUE", destNorm.get(j, i)[0].toString())
                Imgproc.circle(destNormScaled, Point(i.toDouble(), j.toDouble()), 5,  Scalar(255.0, 255.0, 255.0), 2)
            }
        }
    }

    return destNormScaled
}

private fun Hough(image: Mat): Mat {
    Imgproc.blur(image, image, Size(3.0, 3.0))
    val resized = Mat()
    val newSize = Size(image.cols() * 0.5, image.rows() * 0.5)
    Imgproc.resize(image, resized, newSize)

    val circles = Mat()

    Imgproc.HoughCircles(resized, circles, Imgproc.CV_HOUGH_GRADIENT, 1.0, resized.rows() / 15.0, 200.0, 100.0, 0, 0)

    if (circles.cols() > 0) {
        val data = FloatArray(circles.cols() * circles.rows() * circles.channels())
        circles.get(0, 0, data)

        for (i in data.indices step 3) {
            val center = Point(data[i].toDouble(), data[i + 1].toDouble())
            Imgproc.circle(resized, center, data[i + 2].toInt(), Scalar(255.0, 0.0, 0.0), 6)
        }
    }
    return resized
}

private fun convolveManual(image: Mat, kernel: Array<DoubleArray>, k: Int): Mat {
    val resized = Mat()
    val newSize = Size(image.cols() * 0.15, image.rows() * 0.15)
    Imgproc.resize(image, resized, newSize)

    val output = Mat.zeros(resized.size(), CvType.CV_32F)
    val halfK = k / 2
    for (i in halfK until resized.rows() - halfK) {
        for (j in halfK until resized.cols() - halfK) {
            var sum = 0.0
            for (m in -halfK..halfK) {
                for (n in -halfK..halfK) {
                    val pixel = resized.get(i + m, j + n)[0]
                    val coeff = kernel[halfK + m][halfK + n]
                    sum += pixel * coeff
                }
            }
            output.put(i, j, sum)
        }
    }
    convertScaleAbs(output, output)
    return output
}

private fun convolveFilter2D(image: Mat, kernel: Array<DoubleArray>, k: Int): Mat {
    val output = Mat.zeros(image.size(), CvType.CV_32F)
    val ddepth = -1
    val anchor = Point(-1.0, -1.0)
    val delta = 0.0
    val borderType = Core.BORDER_CONSTANT

    val kernelMat = Mat(k, k, CvType.CV_32F)
    for (i in 0 until k) {
        for (j in 0 until k) {
            kernelMat.put(i, j, kernel[i][j])
        }
    }

    Imgproc.filter2D(image, output, ddepth, kernelMat, anchor, delta, borderType)
    return output
}

private fun  GetMatFromProxy(image: ImageProxy): Mat {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC3)
    Utils.bitmapToMat(bitmap, mat)

    val grayMat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC1)
    Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)

    return grayMat
}

private enum class KernelTypes{
    UNDEFINED,
    SOBEL,
    PREWITT,
    SCHARR,
    CUSTOM
}

private enum class Axis{
    UNDEFINED,
    X,
    Y
}

