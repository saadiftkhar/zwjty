/*
 *
 * Created by Saad Iftikhar on 10/18/21, 5:19 PM
 * Copyright (c) 2021. All rights reserved
 *
 */

package com.gezapps.zwjtyapp.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatToggleButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gezapps.zwjtyapp.R
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*


fun <A : Activity> Activity.startNewActivity(activity: Class<A>) {
    Intent(this, activity).also {
        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(it)
    }
}

fun <A : Activity> Activity.openActivity(activity: Class<A>) {
    Intent(this, activity).also {
        startActivity(it)
    }
}

fun View.visible(isVisible: Boolean) {
    visibility = if (isVisible) View.VISIBLE else View.GONE
}

fun View.enable(enabled: Boolean) {
    isEnabled = enabled

}

fun Activity.makeCall(phoneNo: String) {
    if (!TextUtils.isEmpty(phoneNo)) {

        val callIntent = Intent(Intent.ACTION_DIAL)
        callIntent.data = Uri.parse("tel:$phoneNo")
        startActivity(Intent.createChooser(callIntent, "Make call..."))

    }
}

fun showSnackBar(view: View, message: String) {

    Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        .show()

}

fun Activity.showToast(message: String) {

    Toast.makeText(this, message, Toast.LENGTH_SHORT)
        .show()

}

// Convert string to uri
fun String?.asUri(): Uri? {
    return try {
        Uri.parse(this)
    } catch (e: Exception) {
        null
    }
}

// Open uri in browser
fun Uri?.openInBrowser(context: Context) {
    this ?: return // Do nothing if uri is null

    val browserIntent = Intent(Intent.ACTION_VIEW, this)
    ContextCompat.startActivity(context, browserIntent, null)
}

fun hideKeyboard(context: Activity) {
    val inputManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val view = context.currentFocus
    if (view != null) {
        inputManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

fun getFormattedDate(inputDate: String): String {
    val originalFormat = SimpleDateFormat("d/M/y", Locale.US)
    val targetFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    val finalDate = originalFormat.parse(inputDate)
    return targetFormat.format(finalDate)
}

fun Activity.isGalleryPermission(): Boolean {
    return if (ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100
        )
        false
    } else true
}


fun View.preventDoubleClick() {
    Log.e("Base", "Double Click Called")
    isClickable = false
    Handler(Looper.getMainLooper()).postDelayed({ isClickable = true }, 1000L)
}

val String.capitalizeWords
    get() = this.lowercase()
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { it.uppercaseChar() } }


fun AppCompatToggleButton.changeTextColor(color: Int) {
    this.setTextColor(ResourcesCompat.getColor(this.context.resources, color, null))
}

fun TextView.changeTextColor(color: Int) {
    this.setTextColor(ResourcesCompat.getColor(this.context.resources, color, null))
}

fun getView(id: Int, mContext: Context, parent: ViewGroup): View {
    return LayoutInflater.from(mContext)
        .inflate(id, parent, false)
}

fun CoroutineScope.launchPeriodicAsync(
    repeatMillis: Long,
    action: () -> Unit
) = this.async {
    if (repeatMillis > 0) {
        while (true) {
            action()
            delay(repeatMillis)
        }
    } else {
        action()
    }
}

fun isConnected(mContext: Context): Boolean {
    var result = false
    val connectivityManager = mContext.getSystemService(Context.CONNECTIVITY_SERVICE)
    if (connectivityManager is ConnectivityManager) {
        result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected ?: false
        }
    }
    return result
}


fun Activity.setStatusBarColor(color: Int, isLight: Boolean) {
    val window = this.window
    val background =
        ContextCompat.getDrawable(applicationContext, R.drawable.drawable_bg_status_bar)
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.statusBarColor =
        ContextCompat.getColor(applicationContext, color)
    window.setBackgroundDrawable(background)
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight

}




