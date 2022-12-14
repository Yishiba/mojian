package com.mojian

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.mojian.baidu.BaiduUtils
import com.mojian.bean.FoodEntity
import com.mojian.ternsorflow.Classifier
import com.mojian.ternsorflow.TensorFlowImageClassifier
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledThreadPoolExecutor

/**
 * MainActivity
 * @author baishixian
 */
class FoodCamActivity : AppCompatActivity(), View.OnClickListener {
    private var executor: Executor? = null
    private var currentTakePhotoUri: Uri? = null
    private var foodEntity:FoodEntity? =null
//    private var result: TextView? = null
    private var pro: ProgressBar? = null
    private var classifier: Classifier? = null

    companion object {
        val TAG = FoodCamActivity::class.java.simpleName
        private const val OPEN_SETTING_REQUEST_COED = 110
        private const val TAKE_PHOTO_REQUEST_CODE = 120
        private const val PICTURE_REQUEST_CODE = 911
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

        /**
         * ?????????????????????
         * @param bitmap
         * @param size
         * @return
         * @throws IOException
         */
        @Throws(IOException::class)
        private fun getScaleBitmap(bitmap: Bitmap, size: Int): Bitmap {
            val width = bitmap.width
            val height = bitmap.height
            val scaleWidth = size.toFloat() / width
            val scaleHeight = size.toFloat() / height
            val matrix = Matrix()
            matrix.postScale(scaleWidth, scaleHeight)
            return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        }

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_cam)
        pro = findViewById(R.id.pro)
        // ??????????????????
        executor = ScheduledThreadPoolExecutor(1) { r ->
            val thread = Thread(r)
            thread.isDaemon = true
            thread.name = "ThreadPool-ImageClassifier"
            thread
        }

        if (classifier == null) {
            // ?????? Classifier
            classifier = TensorFlowImageClassifier.create(
                this@FoodCamActivity.assets,
                FoodCamActivity.MODEL_FILE,
                FoodCamActivity.LABEL_FILE,
                INPUT_SIZE,
                IMAGE_MEAN,
                IMAGE_STD,
                INPUT_NAME,
                OUTPUT_NAME
            )
        }

        takePhoto()
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
                    this@FoodCamActivity, arrayOf(
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
            R.id.iv_choose_picture -> choosePicture()
            R.id.iv_take_photo -> takePhoto()
            else -> {}
        }
    }

    /**
     * ?????????????????????????????????????????????
     */
    private fun choosePicture() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICTURE_REQUEST_CODE)
    }

    /**
     * ????????????????????????
     */
    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSIONS_REQUEST_CODE
            )
        } else {
            openSystemCamera()
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
            if (requestCode == PICTURE_REQUEST_CODE) {
                // ?????????????????????
                handleInputPhoto(data?.data)
            } else if (requestCode == OPEN_SETTING_REQUEST_COED) {
                requestMultiplePermissions()
            } else if (requestCode == TAKE_PHOTO_REQUEST_CODE) {
                // ??????????????????????????????????????????
                handleInputPhoto(currentTakePhotoUri)
            }
        }
    }

    /**
     * ????????????
     * @param imageUri
     */
    private fun handleInputPhoto(imageUri: Uri?) {
        pro?.visibility = View.VISIBLE
        createDialog()
        // ????????????
        Glide.with(this@FoodCamActivity).asBitmap().listener(object : RequestListener<Bitmap?> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any,
                target: Target<Bitmap?>,
                isFirstResource: Boolean
            ): Boolean {
                Log.d(TAG, "handleInputPhoto onLoadFailed")
                Toast.makeText(this@FoodCamActivity, "??????????????????", Toast.LENGTH_SHORT).show()
                return false
            }

            override fun onResourceReady(
                resource: Bitmap?,
                model: Any,
                target: Target<Bitmap?>,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean {
                Log.d(TAG, "handleInputPhoto onResourceReady")
//                startImageClassifierByBaidu(resource)
                startImageClassifier(resource!!)
                return false
            }
        }).load(imageUri).apply(RequestOptions.bitmapTransform(CircleCrop())).into(mImageView!!)

//        result!!.text = "Processing..."
    }




    /**
     * ????????????????????????
     * @param bitmap
     */
    private fun startImageClassifierByBaidu(bitmap: Bitmap?) {
        if(bitmap == null) return
        executor!!.execute {
            try {
                Log.i(TAG, Thread.currentThread().name + " startImageClassifier")
                val res = BaiduUtils.dish(bitmap)
                if(res == null || res.result_num == 0
                    || (res.result_num == 1 && (res.result[0].name == "??????" || !res.result[0].has_calorie))){
                    startImageClassifier(bitmap)
                    return@execute
                }

                Log.i(TAG, "startImageClassifier results: $res")
                runOnUiThread {
//                    val stringBuilder = StringBuilder()
//                    res.result.forEach{
//                        stringBuilder.append("" + it.name+", ????????????"+it.calorie + "\n")
//                    }
//
//                   result!!.text = stringBuilder.toString()
//
                    foodEntity = FoodEntity(
                        res.result[0].name,
                        res.result[0].calorie.toInt()
                    )

                    Log.d(TAG, "foodEntity = ${foodEntity?.name} ${foodEntity?.calorie}")
                    mTv1?.text = foodEntity?.name
                }
            } catch (e: IOException) {
                Log.e(TAG, "startImageClassifier getScaleBitmap " + e.message)
                e.printStackTrace()
            }
        }
    }

    /**
     * ????????????????????????
     * @param bitmap
     */
    private fun startImageClassifier(bitmap: Bitmap) {
        executor!!.execute {
            try {
                Log.i(TAG, Thread.currentThread().name + " startImageClassifier")
                val croppedBitmap = getScaleBitmap(bitmap, INPUT_SIZE)
                val results = classifier!!.recognizeImage(croppedBitmap)
                val mm = results[0].toString()
                val mm1 = mm[1]
                Log.i(TAG, "startImageClassifier results: $results")
                runOnUiThread { //                            result.setText(String.format("????????????: %s", results));
                    foodEntity = when(mm1){
                        '0'-> FoodEntity("??????", 45)
                        '1'-> FoodEntity("??????", 218)
                        '2'-> FoodEntity("??????", 188)
                        '3'-> FoodEntity("??????", 292)
                        '4'-> FoodEntity("??????", 307)
                        '5'-> FoodEntity("?????????", 127)
                        '6'-> FoodEntity("??????", 235)
                        '7'-> FoodEntity("??????", 286)
                        '8'-> FoodEntity("???", 47)
                        '9'-> FoodEntity("??????", 45)
                        else -> FoodEntity("??????", 0)


                    }



                    Log.d(TAG, "foodEntity2 = ${foodEntity?.name} ${foodEntity?.calorie}")
                    mTv1?.text = foodEntity?.name

                    showDialogYN()

                }
            } catch (e: IOException) {
                Log.e(TAG, "startImageClassifier getScaleBitmap " + e.message)
                e.printStackTrace()

                runOnUiThread {

                    foodEntity = FoodEntity("??????", 0)
                        Log.d(TAG, "foodEntity2 = ${foodEntity?.name} ${foodEntity?.calorie}")
                    mTv1?.text = foodEntity?.name

                    showDialogYN()
                }

            }
        }
    }


    private var mAlertDialog: AlertDialog? = null
    private var mImageView:ImageView? =null
    private var mTv1:TextView? =null
    private var mSpinner: Spinner? =null
    private var mRadioGroup: RadioGroup? =null

    private fun createDialog(){
        if (mAlertDialog != null&& this == mAlertDialog!!.context && mAlertDialog!!.isShowing ) {
            //??????????????????activity????????????????????????????????????
            return
        }
        if (mAlertDialog == null || this != mAlertDialog!!.context) {
            mAlertDialog = AlertDialog.Builder(this).create()//???????????????dialog
        }
        val view1 = View.inflate(this, R.layout.layout_add_food, null)//???ready
        view1.setOnClickListener(null)
        mAlertDialog!!.setView(view1)
        mImageView = view1.findViewById(R.id.food_pic)
        mTv1 = view1.findViewById(R.id.tv1)
        mSpinner = view1.findViewById(R.id.sp1)
        mRadioGroup = view1.findViewById(R.id.radioGroup)
        val mAddBtn =view1.findViewById<Button>(R.id.btnAdd)
        mAddBtn.setOnClickListener {
            Log.d(TAG, "eng = ${foodEntity?.name} ${foodEntity?.calorie}")
            val g = mSpinner?.selectedItem.toString().replace("g", "").toInt()
            Log.e(TAG, "$g  mSpinner?.selectedItem.toString()")


            val rb2:RadioButton = view1.findViewById(R.id.btn2)
            Log.d(TAG, "${rb2.text}")
            var type = 0
            if(rb2.isChecked){
                type=1
            } else {
                val rb3 :RadioButton = view1.findViewById(R.id.btn3)
                if(rb3.isChecked){
                    type=2
                }
            }

            Log.d(TAG, "type = ${type}")
            setData(g, type)

            mAlertDialog?.cancel()

            finish()
        }
    }

    private fun setData(ke:Int, type:Int){
        val tmp = Intent()
        if(foodEntity == null){
            foodEntity = FoodEntity("XXX", 0)
        }
        tmp.putExtra("food", foodEntity?.name)
        tmp.putExtra("eng", foodEntity?.calorie)
        tmp.putExtra("ke", ke)
        tmp.putExtra("type", type)
        setResult(MainActivity.RESULT_CODE, tmp)
    }

    private fun showDialogYN() {
        pro?.visibility = View.GONE
        if(mAlertDialog == null){
            createDialog()
        }
        mAlertDialog?.show()
    }
}