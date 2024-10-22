package com.gezapps.zwjtyapp.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.gezapps.zwjtyapp.R
import com.gezapps.zwjtyapp.databinding.ActivityMainBinding
import com.gezapps.zwjtyapp.util.Constants
import com.gezapps.zwjtyapp.util.isConnected
import com.gezapps.zwjtyapp.util.setStatusBarColor
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    // Context
    private lateinit var mContext: AppCompatActivity

    // Data binding
    private lateinit var binding: ActivityMainBinding

    // Admob
    private lateinit var adRequest: AdRequest
    private var mInterstitialAd: InterstitialAd? = null


    private var fString: ValueCallback<Array<Uri>>? = null
    private var camPath: String? = null
    private val getPackageManager by lazy { packageManager }
    lateinit var results: Array<Uri>

    private val myArl = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_CANCELED) {
            fString?.onReceiveValue(null)
            return@registerForActivityResult
        }

        if (it.resultCode == Activity.RESULT_OK) {
            var clipData: ClipData? = null
            var stringData: String? = null

            try {
                clipData = it.data?.clipData
                stringData = it.data?.dataString
            } catch (e: Exception) {
                clipData = null
                stringData = null
            }

            if (clipData == null && stringData == null && camPath != null) {
                results = arrayOf(Uri.parse(camPath))
            } else {
                if (clipData != null) {
                    val newSelectedFiles = clipData.itemCount
//                    results = arrayOf(1)
                    for (i in 0 until clipData.itemCount) {
                        results[0] = clipData.getItemAt(i).uri
                    }
                } else {
                    try {
                        assert(it.data != null)
                        val camPhoto: Bitmap = it.data?.extras?.get("data") as Bitmap
                        val bytes: ByteArrayOutputStream = ByteArrayOutputStream()
                        camPhoto.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
                        stringData = MediaStore.Images.Media.insertImage(contentResolver, camPhoto, null,
                            null)
                    } catch (e: Exception) { }
                    results = arrayOf(Uri.parse(stringData))
                }
            }

            fString?.onReceiveValue(results)
            fString = null

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this
        binding = DataBindingUtil.setContentView(mContext, R.layout.activity_main)
        setStatusBarColor(R.color.color_application, false)

        if (isConnected(mContext)) {
            setupWebView()
        } else showAlertDialog()

        // Swipe refresh layout
        binding.swipeRefreshLayout.setOnRefreshListener { reloadWebsite() }

    }

    /*****************************************************************************************************************************
     *                                                           Override methods
     *****************************************************************************************************************************/

    override fun onBackPressed() {
        if (binding.webView.isFocused && binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            AlertDialog.Builder(mContext)
                .setTitle("Exit")
                .setMessage("Are you sure. You want to close this app?")
                .setPositiveButton("Yes") { _, _ -> finish() }
                .setNegativeButton("No", null)
                .show()
        }
    }

    /*****************************************************************************************************************************
     *                                                           Calling methods
     *****************************************************************************************************************************/


    private fun setupWebView() {

        binding.swipeRefreshLayout.isRefreshing = true
        adRequest = AdRequest.Builder().build()
        binding.bannerAdView.loadAd(adRequest) // banner ad
        setupInitAd()                          // interstitial ad


        // WebView
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.allowFileAccess = true
        binding.webView.settings.allowContentAccess = true
        binding.webView.settings.allowFileAccessFromFileURLs = true
        binding.webView.settings.allowUniversalAccessFromFileURLs = true
        binding.webView.settings.useWideViewPort = true
        binding.webView.settings.setSupportZoom(true)
        binding.webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        binding.webView.loadUrl(Constants.WEBSITE_URL)
        binding.webView.webViewClient = CustomWebViewClient(binding)
        setWebChromeClient()

    }

    private fun setupInitAd() {
        MobileAds.initialize(mContext) {}
        InterstitialAd.load(mContext,
            mContext.getString(R.string.admob_interstitial_id),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    setAdCallbackListener()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mInterstitialAd = null
                }
            })
    }

    private fun setAdCallbackListener() {

        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {}
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {}
            override fun onAdShowedFullScreenContent() {
                mInterstitialAd = null
            }
        }

        // Show ad
        if (mInterstitialAd != null)
            mInterstitialAd?.show(mContext)

    }

    private fun showAlertDialog() {

        AlertDialog.Builder(mContext)
            .setCancelable(false)
            .setTitle("No internet connection available")
            .setMessage("Please Check you're Mobile data or Wifi network.")
            .setPositiveButton("Ok") { _, _ -> finish() }
            .setNegativeButton("Retry") { _, _ -> reloadWebsite() }
            .show()

    }


    private fun setWebChromeClient() {
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (filePathCallback != null) {
                    fString = filePathCallback

                    var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    if (takePictureIntent?.resolveActivity(getPackageManager) != null) {
                        var photoFile: File? = null
                        try {
                            photoFile = createImageFile()
                            takePictureIntent.putExtra("PhotoPath", camPath)
                        } catch (ex: IOException) {
                            Log.d("PictureIntent", "onShowFileChooser: $ex")
                        }

                        if (photoFile != null) {
                            camPath = "file:" + photoFile.absolutePath
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                        } else takePictureIntent = null

                    }

                    val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                    contentSelectionIntent.type = "image/*"
                    val intentArray: Array<Intent?> = arrayOf(takePictureIntent)
                    val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                    myArl.launch(chooserIntent)
                }




                return true
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val filename: String = SimpleDateFormat("yyyy_mm_ss").format(Date())
        val newName = "file_" + filename + "_"
        val sdDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(newName, ".jpg", sdDirectory)
    }

    private fun reloadWebsite() {
        binding.swipeRefreshLayout.isRefreshing = true
        binding.webView.reload()
    }

    private fun openChromeCustomTab(url: String) {
        val intentBuilder = CustomTabsIntent.Builder()
        val params = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(mContext, R.color.purple_500))
            .setSecondaryToolbarColor(ContextCompat.getColor(mContext, R.color.white))
            .build()
        intentBuilder.setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, params)
        val customTabsIntent = intentBuilder.build()
        customTabsIntent.launchUrl(mContext, Uri.parse(url))
    }


    /*****************************************************************************************************************************
     *                                                           Calling class
     *****************************************************************************************************************************/

    private class CustomWebViewClient(val binding: ActivityMainBinding) : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            try {
                println("url_called:::$url")
                if (url.startsWith("https://m.facebook.com")
                    || url.startsWith("https://api.whatsapp.com")
                    || url.startsWith("https://place here instagram link might be start with m.instagram.com or instagram.com")
                ) {
//                    openChromeCustomTab(url)
                } else {
                    view.loadUrl(url)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }


}