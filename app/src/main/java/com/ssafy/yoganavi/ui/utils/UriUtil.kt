package com.ssafy.yoganavi.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

fun getVideoPath(context: Context, uri: Uri): String {
    val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        cursor.getString(nameIndex)
    } ?: ""

    if (fileName.isBlank()) return ""
    val file = File(context.cacheDir, fileName)
    file.createNewFile()

    context.contentResolver.openInputStream(uri)?.use { inputStream ->
        FileOutputStream(file).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    } ?: return ""

    return file.path
}

suspend fun getImagePath(
    context: Context,
    uri: Uri
): Pair<String, String> = withContext(Dispatchers.IO) {
    val contentResolver = context.contentResolver

    // 파일 이름 가져오기
    val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        cursor.getString(nameIndex)
    } ?: ""
    if (fileName.isBlank()) return@withContext Pair("", "")

    // 임시 파일 생성
    val tempFile = File(context.cacheDir, fileName)
    tempFile.createNewFile()

    contentResolver.openInputStream(uri)?.use { inputStream ->
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    } ?: return@withContext Pair("", "")

    val originalPath = tempFile.absolutePath

    val bmp = BitmapFactory.decodeFile(originalPath) ?: return@withContext Pair("", "")
    val rotatedBitmap = getCorrectlyOrientedBitmap(originalPath, bmp)

    // 미니 이미지 생성 및 저장
    val miniBitmap = Bitmap.createScaledBitmap(
        rotatedBitmap,
        rotatedBitmap.width / 6,
        rotatedBitmap.height / 6,
        true
    )

    val miniWebpFile = File(context.filesDir, "${fileName.substringBeforeLast('.')}_mini.webp")
    try {
        FileOutputStream(miniWebpFile).use { outputStream ->
            miniBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext Pair("", "")
    } finally {
        miniBitmap.recycle()
        rotatedBitmap.recycle()
        bmp.recycle()
    }

    return@withContext Pair(originalPath, miniWebpFile.path)
}

fun getCorrectlyOrientedBitmap(filePath: String, bitmap: Bitmap): Bitmap {
    val exif = ExifInterface(filePath)
    val orientation =
        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    val matrix = android.graphics.Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}