package com.mojian

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.MessageQueue.IdleHandler
import android.provider.MediaStore
import android.provider.Settings
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledThreadPoolExecutor

/**
 * MainActivity
 * @author baishixian
 */
class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var executor: Executor? = null
    private var currentTakePhotoUri: Uri? = null
    private var pieChart: PieChart? = null

    protected var tfRegular: Typeface? = null
    protected var tfLight: Typeface? = null

    companion object {
        val TAG = MainActivity::class.java.simpleName
        private const val OPEN_SETTING_REQUEST_COED = 110
        private const val TAKE_PHOTO_REQUEST_CODE = 120
        private const val PICTURE_REQUEST_CODE = 911
        public const val RESULT_CODE = 912
        private const val PERMISSIONS_REQUEST = 108
        private const val CAMERA_PERMISSIONS_REQUEST_CODE = 119
        private const val CURRENT_TAKE_PHOTO_URI = "currentTakePhotoUri"
        private const val INPUT_SIZE = 299 //224
        private const val IMAGE_MEAN = 128 //117
        private const val IMAGE_STD = 128f
        private const val INPUT_NAME = "Mul"
        private const val OUTPUT_NAME = "final_result"
        private const val MODEL_FILE = "file:///android_asset/model/optimized_graph.pb"
        private const val LABEL_FILE = "file:///android_asset/model/retrained_labels.txt"
        private const val PACKAGE_URL_SCHEME = "package:"



        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isTaskRoot) {
            finish()
        }
        setContentView(R.layout.activity_detail)
        pieChart = findViewById(R.id.pie_chart)
        findViewById<View>(R.id.iv_take_photo).setOnClickListener(this)
        // ???????????????????????? CPU ???????????????UI?????????????????????????????????????????????
        Looper.myQueue().addIdleHandler(idleHandler)

        tfRegular = Typeface.createFromAsset(assets, "OpenSans-Regular.ttf")
        tfLight = Typeface.createFromAsset(assets, "OpenSans-Light.ttf")

        initChart(pieChart!!)
    }

    private fun initChart(chart:PieChart){

        chart.setUsePercentValues(true)
        chart.getDescription().setEnabled(false)
        chart.setExtraOffsets(5f, 10f, 5f, 5f)

        chart.setDragDecelerationFrictionCoef(0.95f)

        chart.setCenterTextTypeface(tfLight)
        chart.setCenterText(generateCenterSpannableText())

        chart.setDrawHoleEnabled(true)
        chart.setHoleColor(Color.WHITE)

        chart.setTransparentCircleColor(Color.WHITE)
        chart.setTransparentCircleAlpha(110)

        chart.setHoleRadius(58f)
        chart.setTransparentCircleRadius(61f)

        chart.setDrawCenterText(true)

        chart.setRotationAngle(0f)
        // enable rotation of the chart by touch
        // enable rotation of the chart by touch
        chart.setRotationEnabled(true)
        chart.setHighlightPerTapEnabled(true)

        // chart.setUnit(" ???");
        // chart.setDrawUnitsInChart(true);

        // add a selection listener

        // chart.setUnit(" ???");
        // chart.setDrawUnitsInChart(true);

        // add a selection listener
//        chart.setOnChartValueSelectedListener(this)

        chart.animateY(1400, Easing.EaseInOutQuad)
        // chart.spin(2000, 0, 360);

        // chart.spin(2000, 0, 360);
        val l: Legend = chart.getLegend()
        l.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        l.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        l.orientation = Legend.LegendOrientation.VERTICAL
        l.setDrawInside(false)
        l.xEntrySpace = 7f
        l.yEntrySpace = 0f
        l.yOffset = 0f

        // entry label styling

        // entry label styling
        chart.setEntryLabelColor(Color.WHITE)
        chart.setEntryLabelTypeface(tfRegular)
        chart.setEntryLabelTextSize(12f)
    }

    private fun generateCenterSpannableText(): SpannableString? {
        val s = SpannableString("MPAndroidChart \n developed by Philipp Jahoda")
        s.setSpan(RelativeSizeSpan(1.7f), 0, 14, 0)
        s.setSpan(StyleSpan(Typeface.NORMAL), 14, s.length - 15, 0)
        s.setSpan(ForegroundColorSpan(Color.GRAY), 14, s.length - 15, 0)
        s.setSpan(RelativeSizeSpan(.8f), 14, s.length - 15, 0)
        s.setSpan(StyleSpan(Typeface.ITALIC), s.length - 14, s.length, 0)
        s.setSpan(ForegroundColorSpan(ColorTemplate.getHoloBlue()), s.length - 14, s.length, 0)
        return s
    }


    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        // ????????????????????????????????? activity ???????????????
        savedInstanceState.putParcelable(CURRENT_TAKE_PHOTO_URI, currentTakePhotoUri)
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState != null) {
            currentTakePhotoUri = savedInstanceState.getParcelable(CURRENT_TAKE_PHOTO_URI)
        }
    }

    /**
     * ????????????????????????????????????????????????????????????????????????????????????
     */
    var idleHandler = IdleHandler {

        // ??????????????????
        executor = ScheduledThreadPoolExecutor(1) { r ->
            val thread = Thread(r)
            thread.isDaemon = true
            thread.name = "ThreadPool-ImageClassifier"
            thread
        }

        // ????????????
        requestMultiplePermissions()
        false
    }

    /**
     * ???????????????????????????
     */
    private fun requestMultiplePermissions() {
        val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        val cameraPermission = Manifest.permission.CAMERA
        val hasStoragePermission = ActivityCompat.checkSelfPermission(this, storagePermission)
        val hasCameraPermission = ActivityCompat.checkSelfPermission(this, cameraPermission)
        val permissions: MutableList<String> = ArrayList()
        if (hasStoragePermission != PackageManager.PERMISSION_GRANTED) {
            permissions.add(storagePermission)
        }
        if (hasCameraPermission != PackageManager.PERMISSION_GRANTED) {
            permissions.add(cameraPermission)
        }
        if (!permissions.isEmpty()) {
            val params = permissions.toTypedArray()
            ActivityCompat.requestPermissions(this, params, PERMISSIONS_REQUEST)
        }
    }




    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (Manifest.permission.WRITE_EXTERNAL_STORAGE == permissions[0] && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                //permission denied ????????????????????????????????????????????? (storagePermission )
                // Should we show an explanation?
                // ???app?????????????????????????????????????????????shouldShowRequestPermissionRationale() ??????false
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    // ????????????????????????
                    showNeedStoragePermissionDialog()
                } else {
                    // ?????????????????????????????????????????????????????????????????????"??????????????????????????????????????????
                    showMissingStoragePermissionDialog()
                }
            }
        } else if (requestCode == CAMERA_PERMISSIONS_REQUEST_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showNeedCameraPermissionDialog()
            } else {
                openSystemCamera()
            }
        }
    }



    /**
     * ??????????????????????????????????????????????????????
     */
    private fun showNeedStoragePermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("??????????????????")
            .setMessage("?????????????????????????????????????????????")
            .setNegativeButton("??????") { dialog, which -> dialog.cancel() }
            .setPositiveButton("??????") { dialog, which ->
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), PERMISSIONS_REQUEST
                )
            }
            .setCancelable(false)
            .show()
    }

    /**
     * ??????????????????????????????????????????????????????
     */
    private fun showMissingStoragePermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("??????????????????")
            .setMessage("??????????????????????????????????????????")
            .setNegativeButton("??????") { dialog, which -> finish() }
            .setPositiveButton("?????????") { dialog, which -> startAppSettings() }
            .setCancelable(false)
            .show()
    }

    private fun showNeedCameraPermissionDialog() {
        AlertDialog.Builder(this)
            .setMessage("???????????????????????????????????????????????????")
            .setPositiveButton("??????") { dialog, which -> dialog.dismiss() }
            .create().show()
    }

    /**
     * ?????????????????????????????????
     */
    private fun startAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse(PACKAGE_URL_SCHEME + packageName)
        startActivityForResult(intent, OPEN_SETTING_REQUEST_COED)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.iv_take_photo -> {
                startActivityForResult(Intent(this, FoodCamActivity::class.java), RESULT_CODE)
            }
            else -> {}
        }
    }



    /**
     * ??????????????????
     */
    private fun openSystemCamera() {
        //??????????????????
        val takePhotoIntent = Intent()
        takePhotoIntent.action = MediaStore.ACTION_IMAGE_CAPTURE

        //???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        if (takePhotoIntent.resolveActivity(packageManager) == null) {
            Toast.makeText(this, "???????????????????????????????????????", Toast.LENGTH_SHORT).show()
            return
        }
        val fileName = "TF_" + System.currentTimeMillis() + ".jpg"
        val photoFile = File(FileUtil.getPhotoCacheFolder(), fileName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //??????FileProvider????????????content?????????Uri
            currentTakePhotoUri =
                FileProvider.getUriForFile(this, "com.mojian.fileprovider", photoFile)
            //?????????????????????????????? Uri ??????????????????
            takePhotoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            currentTakePhotoUri = Uri.fromFile(photoFile)
        }

        //???????????????????????? outputFile ???Uri???????????????????????????
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentTakePhotoUri)
        startActivityForResult(takePhotoIntent, TAKE_PHOTO_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == OPEN_SETTING_REQUEST_COED) {
                requestMultiplePermissions()
            }
        }

        if(resultCode == RESULT_CODE){
            val food = data?.getStringExtra("food")
            val ca = data?.getIntExtra("eng", 0)
            val ke = data?.getIntExtra("ke", 0)
            val type = data?.getIntExtra("type", 0)
            Log.d(TAG, "${data?.extras} food = $food  ca= $ca $ke  $type")
        }
    }

}