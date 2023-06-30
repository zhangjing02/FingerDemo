package com.zhicheng.myfingerdemo

import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.zhicheng.myfingerdemo.databinding.ActivityFingerBinding
import java.nio.charset.StandardCharsets


class FingerActivity : AppCompatActivity() , View.OnClickListener{

    //region 生物识别
    private var mBiometricPrompt: BiometricPrompt? = null
    private var mBiometricLauncher //提示添加生物识别
            : ActivityResultLauncher<Intent>? = null

    lateinit var binding:ActivityFingerBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_finger)

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


        //region TODO:检查是否支持生物识别
        val biometricManager: androidx.biometric.BiometricManager = androidx.biometric.BiometricManager.from(this)
        when (biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> do {
                Toast.makeText(this, "您还未注册生物识别信息,请输入屏幕解锁密码后在系统中注册您的生物识别信息.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                intent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BiometricManager.Authenticators.BIOMETRIC_STRONG)
                mBiometricLauncher!!.launch(intent)
            } while (false)

            BiometricManager.BIOMETRIC_SUCCESS -> do {
                initBiometricPrompt()
            } while (false)

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE, BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {}
        }
        //endregion

    }


    //region TODO:生物识别
    private fun initBiometricPrompt() {
        mBiometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { //验证时发生异常
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@FingerActivity, String.format("%s.%s", errString, "请使用用户名或手机号与密码登录."), Toast.LENGTH_LONG).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { //验证成功
                super.onAuthenticationSucceeded(result)
                try {
//                    val data = "要加密的信息".toByteArray(StandardCharsets.UTF_8)
//                    val cipher = result.cryptoObject!!.cipher
//                    val encode = cipher!!.doFinal(data)
//                    println("cipher---我们验证指纹成功与否${cipher}")
//                    println("encode---我们验证指纹成功与否${encode}")
                    println("data--我们验证指纹成功")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        println("presentationSession---我们验证指纹成功与否${result.cryptoObject?.presentationSession}")
                    }
                    /*
                    密钥在互联网上传播是不安全的,因此直接采用android Cipher加解密数据后与服务器通信不太安全.你可以采用以下几个步骤:
                    1.需要使用生物识别登录服务器或在服务器上验证信息前,先从服务器获取一个随机字符串和RAS公钥
                    2.生物识别验证成功后将随机字符串用RSA公钥加密后将密文发送到服务器
                    3.服务器使用自己保存的RAS私钥解密密文后,比较随机字符串是否相同,相同则予以通过
                    */
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@FingerActivity, e.message, Toast.LENGTH_LONG).show()
                }
            }

            override fun onAuthenticationFailed() { //验证指纹失败,如失败次数超过5次,则点击button时系统会提示稍后再试,我们不需要任何处理
                super.onAuthenticationFailed()
                Toast.makeText(this@FingerActivity, String.format("指纹验证失败.%s", "请使用用户名或手机号与密码登录."), Toast.LENGTH_LONG).show()
            }
        })
        binding.btnOpenFinger.isEnabled = true
        binding.btnOpenFinger.setOnClickListener(this)
    }


    //region TODO:生物识别
    private fun onBiometric() {
        val biometricPromptInfo = PromptInfo.Builder()
            .setTitle("指纹登录")
            .setSubtitle("使用您在Android系统中已经登记的指纹登录本系统")
            .setNegativeButtonText("取消")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        mBiometricPrompt!!.authenticate(biometricPromptInfo)
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
}



