package com.zhicheng.myfingerdemo

import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.security.KeyStoreException
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.zhicheng.myfingerdemo.databinding.ActivitySecondeFingerBinding
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey


class SecondeFingerActivity : AppCompatActivity(), View.OnClickListener {

    //region 指纹识别
    private val KEY_STORE_ALIAS = "KEY_STORE_ALIAS"

    private var mBiometricPrompt: BiometricPrompt? = null
    private var mBiometricPromptInfo: PromptInfo? = null
    private var mSecretKey: SecretKey? = null
    private var mCipher: Cipher? = null
    private var mConfirmDeviceCredentialLauncher //确认设备凭据,处理Cipher.init抛出的UserNotAuthenticatedException,异常消息:user not authenticated
            : ActivityResultLauncher<Intent>? = null
    private var mBiometricLauncher //提示添加生物识别
            : ActivityResultLauncher<Intent>? = null

    lateinit var binding: ActivitySecondeFingerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_seconde_finger)

        mBiometricLauncher = registerForActivityResult<Intent, ActivityResult>(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            try {
                if (RESULT_OK == result.resultCode) { //设置生物识别成功
                    initBiometricPrompt()
                } else {
                    Toast.makeText(this, "请使用用户名或手机号与密码登录.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }


        mConfirmDeviceCredentialLauncher = registerForActivityResult<Intent, ActivityResult>(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            try {
                if (RESULT_OK == result.resultCode) {
                    mCipher!!.init(Cipher.ENCRYPT_MODE, mSecretKey) //密钥要求30秒验证完成,否则密钥失效
                    mBiometricPrompt!!.authenticate(
                        mBiometricPromptInfo!!,
                        BiometricPrompt.CryptoObject(mCipher!!)
                    )
                } else {
                    Toast.makeText(this, "请使用用户名或手机号与密码登录.", Toast.LENGTH_LONG).show()
                }
            } catch (e: java.lang.Exception) {
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }


        //region TODO:检查是否支持生物识别
        val biometricManager: BiometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> do {
                Toast.makeText(this, "您还未注册生物识别信息,请输入屏幕解锁密码后在系统中注册您的生物识别信息.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                intent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BiometricManager.Authenticators.BIOMETRIC_STRONG)
               // mBiometricLauncher!!.launch(intent)

                val packageManager = packageManager
                if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
                    println("还未设置指纹？")
//                    val intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL)
//                    startActivity(intent)
                } else {
                    println("设备不支持生物识别功能")
                    // 设备不支持生物识别功能
                }



            } while (false)

            BiometricManager.BIOMETRIC_SUCCESS -> do {
                initBiometricPrompt()
            } while (false)

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE, BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {}
        }
    }


    //region TODO:生物识别
    private fun initBiometricPrompt() {
        mBiometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@SecondeFingerActivity, String.format("%s.%s", errString, "请使用用户名或手机号与密码登录."), Toast.LENGTH_LONG).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                try {
                    val data = "要加密的信息".toByteArray(StandardCharsets.UTF_8)
                    val cipher = result.cryptoObject!!.cipher
                    val encode = cipher!!.doFinal(data)
                    println("001我们获取的手机指纹加密信息是？$data")
                    println("002我们获取的手机指纹加密信息是？$cipher")
                    println("003我们获取的手机指纹加密信息是？$encode")
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@SecondeFingerActivity, e.message, Toast.LENGTH_LONG).show()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@SecondeFingerActivity, String.format("指纹验证失败.%s", "请使用用户名或手机号与密码登录."), Toast.LENGTH_LONG).show()
            }
        })
        binding.btnOpenFinger.isEnabled = true
        binding.btnOpenFinger.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        try {
            when (v) {
                binding.btnOpenFinger -> onBiometric()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
    }


    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        UnrecoverableKeyException::class,
        CertificateException::class,
        KeyStoreException::class,
        IOException::class,
        InvalidKeyException::class,
        InvalidAlgorithmParameterException::class,
        NoSuchProviderException::class
    )
    private fun onBiometric() {
        generateSecretKey()
        mCipher = getCipher()
        mSecretKey = getSecretKey()
        mBiometricPromptInfo = PromptInfo.Builder()
            .setTitle("指纹登录")
            .setSubtitle("使用您在Android系统中已经登记的指纹登录本系统")
            .setNegativeButtonText("取消")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        try {
            mCipher!!.init(Cipher.ENCRYPT_MODE, mSecretKey) //这里可能会报user not authenticated异常
            mBiometricPrompt!!.authenticate(mBiometricPromptInfo!!, BiometricPrompt.CryptoObject(mCipher!!))
        } catch (e: UserNotAuthenticatedException) {
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            val intent = keyguardManager.createConfirmDeviceCredentialIntent(null, null) //创建设备凭据
            mConfirmDeviceCredentialLauncher!!.launch(intent)
        }
    }

    @Throws(
        KeyStoreException::class,
        CertificateException::class,
        IOException::class,
        NoSuchAlgorithmException::class,
        UnrecoverableKeyException::class
    )
    private fun getSecretKey(): SecretKey? {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null)
        return keyStore.getKey(KEY_STORE_ALIAS, null) as SecretKey
    }

    @Throws(NoSuchPaddingException::class, NoSuchAlgorithmException::class)
    private fun getCipher(): Cipher? {
        return Cipher.getInstance(
            KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7
        )
    }

    //region TODO:生物识别
    @Throws(NoSuchAlgorithmException::class, NoSuchProviderException::class, InvalidAlgorithmParameterException::class)
    private fun generateSecretKey() {
        val keyGenParameterSpecBuilder = KeyGenParameterSpec.Builder(
            KEY_STORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true) // Invalidate the keys if the user has registered a new biometric
            // credential, such as a new fingerprint. Can call this method only
            // on Android 7.0 (API level 24) or higher. The variable
            // "invalidatedByBiometricEnrollment" is true by default.
            .setInvalidatedByBiometricEnrollment(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) keyGenParameterSpecBuilder.setUserAuthenticationParameters(
            30,
            KeyProperties.AUTH_BIOMETRIC_STRONG
        ) //设置在成功对用户进行身份验证后授权使用此密钥的持续时间（秒）和授权类型
        else keyGenParameterSpecBuilder.setUserAuthenticationValidityDurationSeconds(30)
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(keyGenParameterSpecBuilder.build())
        val secretKey = keyGenerator.generateKey()
    }
}