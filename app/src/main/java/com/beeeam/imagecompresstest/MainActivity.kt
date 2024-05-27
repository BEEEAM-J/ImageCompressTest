package com.beeeam.imagecompresstest

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.databinding.DataBindingUtil
import com.beeeam.imagecompresstest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val pickMedia =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                uri?.let {
                    val imageFile = ImageUtil.uriToOptimizeImageFile(applicationContext, it)!!
                    binding.tvOriginalImage.text = this.getString(R.string.tv_original_size, getImageSize(contentResolver, uri))
                    binding.tvCompressedImage.text = this.getString(R.string.tv_compressed_size, imageFile.length() / (1024.0 * 1024.0))

                    val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    binding.ivSelectedImage.setImageBitmap(bitmap)
                }
            }

        binding.apply {
            tvOriginalImage.text = "Original: 0MB"
            tvCompressedImage.text = "Compress: 0MB"

            btnImageSelect.setOnClickListener {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }

    private fun getImageSize(contentResolver: ContentResolver, imageUri: Uri): Float {
        var imageSize: Long = 0
        val cursor = contentResolver.query(imageUri, null, null, null, null)
        cursor?.use {
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            if (it.moveToFirst()) {
                imageSize = it.getLong(sizeIndex)
            }
        }
        return (imageSize / 1048576).toFloat()
    }
}