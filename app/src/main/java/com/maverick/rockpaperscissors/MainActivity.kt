package com.maverick.rockpaperscissors

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.media.ExifInterface
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.maverick.rockpaperscissors.databinding.ActivityMainBinding
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
        const val REQUEST_IMAGE_CAPTURE: Int = 1
        private const val MAX_FONT_SIZE = 96F
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var currentPhotoPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.w(TAG, "onCreate")

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        if (savedInstanceState != null) {
            currentPhotoPath = savedInstanceState.getString("currentPhotoPath", "")
            Log.w(TAG, "restoring $currentPhotoPath")
        }

        binding.buttonCaptureImage.setOnClickListener {
            dispatchTakePictureIntent()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.w(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.w(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.w(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.w(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "onDestroy")
    }

    override fun onSaveInstanceState(outState: Bundle) {

        Log.w(TAG, "onSaveInstanceState")

        super.onSaveInstanceState(outState)
        if (this::currentPhotoPath.isInitialized) {
            Log.w(TAG, "saving $currentPhotoPath")
            outState.putString("currentPhotoPath", currentPhotoPath)
        } else {
            Log.w(TAG, "onSaveInstanceState: currentPhotoPath not initialized")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.w(TAG, "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {

            // avoid zero imageView issue
            binding.imageView.post {
                binding.tvPlaceholder.visibility = View.GONE
                binding.informationCard.visibility = View.VISIBLE
                val bitmap = getCapturedImage()
                binding.imageView.setImageBitmap(bitmap)
                runObjectDetection(bitmap)
            }
        }
    }

    private fun runObjectDetection(bitmap: Bitmap) {
        //  Step 0: resize
        Log.w("resize", "${bitmap.width}, ${bitmap.height}")
        val scale = max(640f / bitmap.width, 640f / bitmap.height)
        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            false
        )
        Log.w("resize", "${resizedBitmap.width}, ${resizedBitmap.height}")

        // Step 1: Create TFLite's TensorImage object
        val image = TensorImage.fromBitmap(resizedBitmap)

        // Step 2: Initialize the detector object
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
            .setScoreThreshold(.5f)
            .build()

        val detector = ObjectDetector.createFromFileAndOptions(
            this,
            "my_efficientdet_lite0_tuned_metadata.tflite",
            options
        )

        // Step 3: Feed given image to the detector
        val results = detector.detect(image)

        val gestures = results.map {
            RockPaperScissors.Gesture.fromName(it.categories.first().label)
        }
        val gameResult = RockPaperScissors.getResults(gestures)
        val gameSummary = RockPaperScissors.Result.summary(gameResult)
        binding.winCountText.text = "0"
        binding.loseCountText.text = "0"
        binding.tieCountText.text = "0"
        for (i in gameSummary.keys) {
            when (i) {
                RockPaperScissors.Result.WIN -> binding.winCountText
                RockPaperScissors.Result.LOSE -> binding.loseCountText
                RockPaperScissors.Result.TIE -> binding.tieCountText
            }.text = "${gameSummary[i]}"
        }
        Log.w(TAG, "runObjectDetection: $gameResult, $gameSummary")

        // Step 4: Parse the detection result and show it
        val resultToDisplay = mutableListOf<DetectionResult>()
        for (i in 0 until results.size) {
            // Get the top-1 category and craft the display text
            val category = results[i].categories.first()
            val text = "${category.label}, ${category.score.times(100).toInt()}%"

            // Create a data object to display the detection result
            resultToDisplay.add(DetectionResult(results[i].boundingBox, text, gameResult[i]))
        }
        // Draw the detection result on the bitmap and show it.
        val imgWithResult = drawDetectionResult(resizedBitmap, resultToDisplay)
        runOnUiThread {
            binding.imageView.setImageBitmap(imgWithResult)
        }
    }

    private fun drawDetectionResult(
        bitmap: Bitmap,
        detectionResults: List<DetectionResult>
    ): Bitmap {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        detectionResults.forEach {
            // draw bounding box
            pen.color = when (it.gameResult) {
                RockPaperScissors.Result.WIN -> Color.RED
                RockPaperScissors.Result.LOSE -> Color.BLUE
                RockPaperScissors.Result.TIE -> Color.LTGRAY
            }
            pen.strokeWidth = 8F
            pen.style = Paint.Style.STROKE
            val box = it.boundingBox
            canvas.drawRect(box, pen)


            val tagSize = Rect(0, 0, 0, 0)

            // calculate the right font size
            pen.style = Paint.Style.FILL_AND_STROKE
            pen.color = Color.YELLOW
            pen.strokeWidth = 2F

            pen.textSize = MAX_FONT_SIZE
            pen.getTextBounds(it.text, 0, it.text.length, tagSize)
            val fontSize: Float = pen.textSize * box.width() / tagSize.width()

            // adjust the font size so texts are inside the bounding box
            if (fontSize < pen.textSize) pen.textSize = fontSize

            var margin = (box.width() - tagSize.width()) / 2.0F
            if (margin < 0F) margin = 0F
            canvas.drawText(
                it.text, box.left + margin,
                box.top + tagSize.height().times(1F), pen
            )
        }
        return outputBitmap
    }

    data class DetectionResult(
        val boundingBox: RectF,
        val text: String,
        val gameResult: RockPaperScissors.Result
    )

    private fun getCapturedImage(): Bitmap {
        // Get the dimensions of the View
        val targetW: Int = binding.imageView.width
        val targetH: Int = binding.imageView.height
        Log.w(TAG, "getCapturedImage: w: $targetW h: $targetH")

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            BitmapFactory.decodeFile(currentPhotoPath, this)


            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = max(1, min(photoW / targetW, photoH / targetH))

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inMutable = true
        }
        val exifInterface = ExifInterface(currentPhotoPath)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        val bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions)
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                rotateImage(bitmap, 90f)
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                rotateImage(bitmap, 180f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                rotateImage(bitmap, 270f)
            }
            else -> {
                bitmap
            }
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)

        val photoFile = createImageFile()
        val photoURI = FileProvider.getUriForFile(
            this,
            "com.maverick.rockpaperscissors.fileprovider",
            photoFile
        )
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "IMG_${timeStamp}_",
            ".jpg",
            storageDirectory
        ).apply {
            currentPhotoPath = absolutePath
        }
    }


}