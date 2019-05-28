package com.android.dump

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.ArrayMap
import android.util.Log
import dalvik.system.DexClassLoader
import java.io.*
import java.lang.Exception
import java.lang.ref.WeakReference
import java.util.zip.ZipInputStream

class dumpCreate: Activity(){
    lateinit var apkFileName: String
    lateinit var libsPath: String

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        val odex: File = this.getDir("payload_odex", Context.MODE_PRIVATE)
        val libs: File = this.getDir("payload_libs", Context.MODE_PRIVATE)

        val odexPath = odex.absolutePath
        libsPath = libs.absolutePath
        apkFileName = odexPath + "/payload.apk"
        val dexFile = File(apkFileName)

        Log.d("dump", "apk size: " + dexFile.length())
        Log.d("dump","te4")
        try {
            if (!dexFile.exists()) {
                dexFile.createNewFile()

                val dexData: ByteArray = readDexFileFromApk()//get classes.dex file from sourceDir which contains oriApk and dumpDex
                Log.d("dump","dexData size: "+dexData.size)
                splitPayloadFromDex(dexData)//create original Apk file and move libs in apk<zip> into libsPath

                val currentActivityThread = invoke_static_method("android.app.ActivityThread",
                    "currentActivityThread",
                    arrayOf<Class<*>>(),
                    arrayOf<Any>())
                val packageName = this.packageName
                val mPackages = get_field_object("android.app.ActivityThread",
                    currentActivityThread,
                    "mPackages") as ArrayMap<*,*>
                val wr = mPackages.get(packageName) as WeakReference<*>

                val dLoader = DexClassLoader(apkFileName,
                    odexPath, //ori dex
                    libsPath, //ori libs
                    get_field_object("android.app.LoadedApk",wr.get(),"mClassLoader") as ClassLoader)

                set_field_object("android.app.LoadedApk",
                    "mClassLoader",
                    wr.get(),
                    dLoader)

                Log.d("dump","classLoader: "+dLoader)

                val actObj = dLoader.loadClass("com.android.zither.Gos")
                Log.d("dump","actObj: "+actObj)
            }
        }catch (e: Exception){
            Log.d("dump",e.toString())
            e.printStackTrace()
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lateinit var appClassName: String
        try {
            val ai: ApplicationInfo = this.packageManager.getApplicationInfo(this.packageName,PackageManager.GET_META_DATA)
            val bundle = ai.metaData
            if(bundle != null && bundle.containsKey("APP_CLASS_NAME"))
                appClassName = bundle.getString("APP_CLASS_NAME")!!
            else{
                Log.d("dump","meta class name null")
                return
            }
            Log.d("dump","meta data "+appClassName)
            val currentActivityThread = invoke_static_method("android.app.ActivityThread",
                "currentActivityThread",
                arrayOf<Class<*>>(),
                arrayOf<Any>())
            val mBoundApplication = get_field_object("android.app.ActivityThread",
                currentActivityThread,
                "mBoundApplication")
            val loadedApkInfo = get_field_object("android.app.ActivityThread\$AppBindData",
                mBoundApplication,
                "info")

            set_field_object("android.app.LoadedApk",
                "mApplication",
                loadedApkInfo,
                null)//now fake mapplication is null

            val oldApplication = get_field_object("android.app.ActivityThread",
                currentActivityThread,
                "mInitialApplication") as Any
            val mAllApplications: ArrayList<Application> = get_field_object("android.app.ActivityThread",
                currentActivityThread,
                "mAllApplications") as ArrayList<Application>


            mAllApplications.remove(oldApplication)
            val appInfoInLoadedApk = get_field_object("android.app.LoadedApk",
                loadedApkInfo,
                "mApplicationInfo") as ApplicationInfo
            val appInfoInBindData  = get_field_object("android.app.ActivityThread\$AppBindData",
                mBoundApplication,
                "appInfo") as ApplicationInfo

            appInfoInLoadedApk.className = appClassName
            appInfoInBindData.className = appClassName

            val app = invoke_method("android.app.LoadedApk",
                "makeApplication",
                loadedApkInfo,
                arrayOf<Class<*>>(Boolean::class.java,Instrumentation::class.java),
                arrayOf<Any?>(false,null)) as Application

            set_field_object("android.app.ActivityThread",
                "mInitialApplication",
                currentActivityThread,
                app)

            /*
            val mProviderMap = get_field_object("android.app.ActivityThread",
                currentActivityThread,
                "mProviderMap") as ArrayMap<*,*>

            val it = mProviderMap.values.iterator()
            while(it.hasNext()){
                val providerClientRecord = it.next()
                val localProvider = get_field_object("android.app.ActivityThread\$ProviderClientRecord",
                    providerClientRecord,
                    "mLocalProvider")

                set_field_object("android.content.ContentProvider",
                    "mContext",
                    localProvider,
                    app)
            }*/
            Log.d("dump","app: "+app)
            val p = packageManager
            val componentName = ComponentName(
                this,
                dumpCreate::class.java
            ) // activity which is first time open in manifiest file which is declare as <category android:name="android.intent.category.LAUNCHER" />
            p.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            app.onCreate()

        }catch (e: Exception){
            Log.d("dump",e.toString())
            e.printStackTrace()
        }


    }

    fun splitPayloadFromDex(dexData: ByteArray){
        val ablen = dexData.size
        val dexLen = ByteArray(4)

        System.arraycopy(dexData,ablen-4,dexLen,0,4)
        var fixLen  = 0
        fixLen = fixLen.and(dexLen[3].toUByte().toInt())
        fixLen = fixLen.shl(8).or(dexLen[2].toUByte().toInt())
        fixLen = fixLen.shl(8).or(dexLen[1].toUByte().toInt())
        fixLen = fixLen.shl(8).or(dexLen[0].toUByte().toInt())

        Log.d("dump",fixLen.toString())
        val oriApkLen: Int = fixLen

        val oriApk = ByteArray(oriApkLen)
        System.arraycopy(dexData,ablen-4-oriApkLen,oriApk,0,oriApkLen)

        val oriApkFile = File(apkFileName)
        val localFileOutputStream = FileOutputStream(oriApkFile)
        localFileOutputStream.write(oriApk)
        localFileOutputStream.close()

        val localZipInputStream = ZipInputStream(BufferedInputStream(FileInputStream(oriApkFile)))

        while(true){
            val localZipEntry = localZipInputStream.nextEntry
            if(localZipEntry == null){
                localZipInputStream.close()
                break
            }
            val fileName = localZipEntry.name
            if(fileName.startsWith("lib") && fileName.endsWith("so")){
                val storeFile = File(libsPath+"/"+fileName.substring(fileName.lastIndexOf('/')))
                storeFile.createNewFile()
                val fos = FileOutputStream(storeFile)
                val arrayofBytes = ByteArray(1024)

                while(true){
                    val readNum = localZipInputStream.read(arrayofBytes)
                    if(readNum == -1)
                        break
                    fos.write(arrayofBytes,0,readNum)
                }
                fos.flush()
                fos.close()
            }
            localZipInputStream.closeEntry()
        }
        localZipInputStream.close()
    }

    fun readDexFileFromApk(): ByteArray{
        val dexBytesArrayOutputStream = ByteArrayOutputStream()
        val localZipInputStream = ZipInputStream(BufferedInputStream(FileInputStream(this.applicationInfo.sourceDir)))

        while(true){
            val localZipEntry = localZipInputStream.nextEntry
            if(localZipEntry == null){
                localZipInputStream.close()
                break
            }
            if(localZipEntry.name.equals("classes.dex")){
                val arrayofBytes = ByteArray(1024)
                while(true){
                    val readNum = localZipInputStream.read(arrayofBytes)
                    if(readNum == -1)
                        break
                    dexBytesArrayOutputStream.write(arrayofBytes,0,readNum)
                }
            }
            localZipInputStream.closeEntry()
        }
        localZipInputStream.close()
        return dexBytesArrayOutputStream.toByteArray()
    }
}

