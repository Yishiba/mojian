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
         * 对图片进行缩放
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
        // 初始化线程池
        executor = ScheduledThreadPoolExecutor(1) { r ->
            val thread = Thread(r)
            thread.isDaemon = true
            thread.name = "ThreadPool-ImageClassifier"
            thread
        }

        if (classifier == null) {
            // 创建 Classifier
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
        // 防止拍照后无法返回当前 activity 时数据丢失
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
     * 请求存储和相机权限
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
                    // 系统弹窗提示授权
                    showNeedStoragePermissionDialog()
                } else {
                    // 已经被禁止的状态，比如用户在权限对话框中选择了"不再显示”，需要自己弹窗解释
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
     * 显示缺失权限提示，可再次请求动态权限
     */
    private fun showNeedStoragePermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("权限获取提示")
            .setMessage("必须要有存储权限才能获取到图片")
            .setNegativeButton("取消") { dialog, which -> dialog.cancel() }
            .setPositiveButton("确定") { dialog, which ->
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
     * 显示权限被拒提示，只能进入设置手动改
     */
    private fun showMissingStoragePermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("权限获取失败")
            .setMessage("必须要有存储权限才能正常运行")
            .setNegativeButton("取消") { dialog, which -> finish() }
            .setPositiveButton("去设置") { dialog, which -> startAppSettings() }
            .setCancelable(false)
            .show()
    }

    private fun showNeedCameraPermissionDialog() {
        AlertDialog.Builder(this)
            .setMessage("摄像头权限被关闭，请开启权限后重试")
            .setPositiveButton("确定") { dialog, which -> dialog.dismiss() }
            .create().show()
    }

    /**
     * 启动应用的设置进行授权
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
     * 选择一张图片并裁剪获得一个小图
     */
    private fun choosePicture() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, PICTURE_REQUEST_CODE)
    }

    /**
     * 使用系统相机拍照
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
     * 打开系统相机
     */
    private fun openSystemCamera() {
        //调用系统相机
        val takePhotoIntent = Intent()
        takePhotoIntent.action = MediaStore.ACTION_IMAGE_CAPTURE

        //这句作用是如果没有相机则该应用不会闪退，要是不加这句则当系统没有相机应用的时候该应用会闪退
        if (takePhotoIntent.resolveActivity(packageManager) == null) {
            Toast.makeText(this, "当前系统没有可用的相机应用", Toast.LENGTH_SHORT).show()
            return
        }
        val fileName = "TF_" + System.currentTimeMillis() + ".jpg"
        val photoFile = File(FileUtil.getPhotoCacheFolder(), fileName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //通过FileProvider创建一个content类型的Uri
            currentTakePhotoUri =
                FileProvider.getUriForFile(this, "com.mojian.fileprovider", photoFile)
            //对目标应用临时授权该 Uri 所代表的文件
            takePhotoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            currentTakePhotoUri = Uri.fromFile(photoFile)
        }

        //将拍照结果保存至 outputFile 的Uri中，不保留在相册中
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentTakePhotoUri)
        startActivityForResult(takePhotoIntent, TAKE_PHOTO_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == PICTURE_REQUEST_CODE) {
                // 处理选择的图片
                handleInputPhoto(data?.data)
            } else if (requestCode == OPEN_SETTING_REQUEST_COED) {
                requestMultiplePermissions()
            } else if (requestCode == TAKE_PHOTO_REQUEST_CODE) {
                // 如果拍照成功，加载图片并识别
                handleInputPhoto(currentTakePhotoUri)
            }
        }
    }

    /**
     * 处理图片
     * @param imageUri
     */
    private fun handleInputPhoto(imageUri: Uri?) {
        pro?.visibility = View.VISIBLE
        createDialog()
        // 加载图片
        Glide.with(this@FoodCamActivity).asBitmap().listener(object : RequestListener<Bitmap?> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any,
                target: Target<Bitmap?>,
                isFirstResource: Boolean
            ): Boolean {
                Log.d(TAG, "handleInputPhoto onLoadFailed")
                Toast.makeText(this@FoodCamActivity, "图片加载失败", Toast.LENGTH_SHORT).show()
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
     * 开始图片识别匹配
     * @param bitmap
     */
    private fun startImageClassifierByBaidu(bitmap: Bitmap?) {
        if(bitmap == null) return
        executor!!.execute {
            try {
                Log.i(TAG, Thread.currentThread().name + " startImageClassifier")
                val res = BaiduUtils.dish(bitmap)
                if(res == null || res.result_num == 0
                    || (res.result_num == 1 && (res.result[0].name == "非菜" || !res.result[0].has_calorie))){
                    startImageClassifier(bitmap)
                    return@execute
                }

                Log.i(TAG, "startImageClassifier results: $res")
                runOnUiThread {
//                    val stringBuilder = StringBuilder()
//                    res.result.forEach{
//                        stringBuilder.append("" + it.name+", 卡路里："+it.calorie + "\n")
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
     * 开始图片识别匹配
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
                runOnUiThread { //                            result.setText(String.format("识别结果: %s", results));
                    foodEntity = when(mm1){
                        '0'-> FoodEntity("可乐", 45)
                        '1'-> FoodEntity("饺子", 218)
                        '2'-> FoodEntity("炒饭", 188)
                        '3'-> FoodEntity("汉堡", 292)
                        '4'-> FoodEntity("热狗", 307)
                        '5'-> FoodEntity("冰淇淋", 127)
                        '6'-> FoodEntity("披萨", 235)
                        '7'-> FoodEntity("面条", 286)
                        '8'-> FoodEntity("粥", 47)
                        '9'-> FoodEntity("雪碧", 45)
                        else -> FoodEntity("未知", 0)


                    }



                    Log.d(TAG, "foodEntity2 = ${foodEntity?.name} ${foodEntity?.calorie}")
                    mTv1?.text = foodEntity?.name

                    showDialogYN()

                }
            } catch (e: IOException) {
                Log.e(TAG, "startImageClassifier getScaleBitmap " + e.message)
                e.printStackTrace()

                runOnUiThread {

                    foodEntity = FoodEntity("未知", 0)
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
            //表示在同一个activity，已经显示了，就不再显示
            return
        }
        if (mAlertDialog == null || this != mAlertDialog!!.context) {
            mAlertDialog = AlertDialog.Builder(this).create()//背景透明的dialog
        }
        val view1 = View.inflate(this, R.layout.layout_add_food, null)//有ready
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