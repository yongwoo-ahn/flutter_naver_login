package com.yoonjaepark.flutter_naver_login

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.AsyncTask
import androidx.annotation.NonNull
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutionException

/** FlutterNaverLoginPlugin */
class FlutterNaverLoginPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    /** Plugin registration.  */
    private val METHOD_INITIALIZE_SDK = "initializeSDK"
    private val METHOD_LOG_IN = "logIn"
    private val METHOD_LOG_OUT = "logOut"
    private val METHOD_LOG_OUT_DELETE_TOKEN = "logoutAndDeleteToken"
    private val METHOD_GET_ACCOUNT = "getCurrentAcount"

    private val METHOD_GET_TOKEN = "getCurrentAccessToken"
    private val METHOD_REFRESH_ACCESS_TOKEN_WITH_REFRESH_TOKEN =
        "refreshAccessTokenWithRefreshToken"

    /**
     * 네이버 개발자 등록한 client 정보를 넣어준다.
     */
    private var OAUTH_CLIENT_ID = "OAUTH_CLIENT_ID"
    private var OAUTH_CLIENT_SECRET = "OAUTH_CLIENT_SECRET"
    private var OAUTH_CLIENT_NAME = "OAUTH_CLIENT_NAME"
    public var registrar: Registrar? = null

    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private var currentActivity: Activity?=null

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_plugin_name")
            channel.setMethodCallHandler(FlutterNaverLoginPlugin())
        }
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(
            flutterPluginBinding.getFlutterEngine().getDartExecutor(),
            "flutter_naver_login"
        )
        channel.setMethodCallHandler(this);
        context = flutterPluginBinding.applicationContext

        initSDK(context);
    }

    public fun initSDK(applicationContext: Context) {
        var packageName = applicationContext.packageName
        packageName.let {
            var applicationInfo =
                applicationContext.packageManager.getApplicationInfo(
                    it,
                    PackageManager.GET_META_DATA
                )

            var bundle = applicationInfo.metaData

            if (bundle != null) {
                OAUTH_CLIENT_ID = bundle.getString("com.naver.sdk.clientId").toString()
                OAUTH_CLIENT_SECRET = bundle.getString("com.naver.sdk.clientSecret").toString()
                OAUTH_CLIENT_NAME = bundle.getString("com.naver.sdk.clientName").toString()
                NaverIdLoginSDK.initialize(
                    applicationContext,
                    OAUTH_CLIENT_ID,
                    OAUTH_CLIENT_SECRET,
                    OAUTH_CLIENT_NAME
                )
            }
        }
    }

    override fun onDetachedFromActivity() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        currentActivity = binding.activity
    }
    override fun onDetachedFromActivityForConfigChanges() {}

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == METHOD_LOG_IN) {
            this.login(result)
        } else if (call.method == METHOD_LOG_OUT) {
            this.logout(result)
        } else if (call.method == METHOD_LOG_OUT_DELETE_TOKEN) {
            this.logoutAndDeleteToken(result)
        } else if (call.method == METHOD_INITIALIZE_SDK) {
            this.initializeSDK(result)
        } else if (call.method == METHOD_GET_TOKEN) {
            result.success(object : HashMap<String, String>() {
                init {
                    put("status", "getToken")
                    NaverIdLoginSDK.getAccessToken()?.let { put("accessToken", it) }
                    NaverIdLoginSDK.getRefreshToken()?.let { put("refreshToken", it) }
                    put("expiresAt", NaverIdLoginSDK.getExpiresAt().toString())
                    NaverIdLoginSDK.getTokenType()?.let { put("tokenType", it) }
                }
            })
        } else if (call.method == METHOD_GET_ACCOUNT) {
            this.currentAccount(result)
        } else if (call.method == METHOD_REFRESH_ACCESS_TOKEN_WITH_REFRESH_TOKEN) {
            this.refreshAccessTokenWithRefreshToken(
                result
            )
        } else {
            result.notImplemented()
        }
    }

    fun currentAccount(result: Result) {
        val accessToken = NaverIdLoginSDK.getAccessToken()
        if (accessToken == null) {
            val errorCode = NaverIdLoginSDK.getLastErrorCode().code
            val errorDesc = NaverIdLoginSDK.getLastErrorDescription()
            errorResponse(result, errorCode, errorDesc)
        } else {
            val task = ProfileTask()
            try {
                val res = task.execute(accessToken).get()
                val obj = JSONObject(res)
                var resultProfile = jsonObjectToMap(obj.getJSONObject("response"))
                resultProfile["status"] = "loggedIn"
                result.success(resultProfile)
            } catch (e: InterruptedException) {
                e.printStackTrace()
                errorResponse(result, "InterruptedException", e.toString())
            } catch (e: ExecutionException) {
                e.printStackTrace()
                errorResponse(result, "ExecutionException", e.toString())
            } catch (e: JSONException) {
                e.printStackTrace()
                errorResponse(result, "JSONException", e.toString())
            }
        }
    }

    fun errorResponse(result: Result, status: String, description: String?) {
        var descriptionString = description
        if (description == null) {
            descriptionString = "NULL"
        }
        result.success(object : HashMap<String, String>() {
            init {
                put("status", "error")
                put("errorMessage", "errorCode:$status, errorDesc:$descriptionString")
            }
        })
    }

    private fun initializeSDK(result: Result) {
        result.success(null)
    }

    private fun login(result: Result) {
        val mOAuthLoginHandler = object : OAuthLoginCallback {
            override fun onSuccess() {
                currentAccount(result)
            }

            override fun onFailure(httpStatus: Int, message: String) {
                val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                val errorDesc = NaverIdLoginSDK.getLastErrorDescription()
                errorResponse(result, errorCode, errorDesc)
            }

            override fun onError(errorCode: Int, message: String) {
                onFailure(errorCode, message)
            }
        }
        NaverIdLoginSDK.authenticate(currentActivity!!, mOAuthLoginHandler);
    }

    fun logout(result: Result) {
        NaverIdLoginSDK.logout()
        result.success(object : HashMap<String, Any>() {
            init {
                put("status", "cancelledByUser")
                put("isLogin", false)
            }
        })
    }

    fun logoutAndDeleteToken(result: Result) {
        val mOAuthLoginHandler = object : OAuthLoginCallback {
            override fun onSuccess() {
                result.success(object : HashMap<String, Any>() {
                    init {
                        put("status", "cancelledByUser")
                        put("isLogin", false)
                    }
                })
            }

            override fun onFailure(httpStatus: Int, message: String) {
                // 서버에서 token 삭제에 실패했어도 클라이언트에 있는 token 은 삭제되어 로그아웃된 상태이다
                // 실패했어도 클라이언트 상에 token 정보가 없기 때문에 추가적으로 해줄 수 있는 것은 없음
                val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                val errorDesc = NaverIdLoginSDK.getLastErrorDescription()
                errorResponse(result, errorCode, errorDesc)
            }

            override fun onError(errorCode: Int, message: String) {
                onFailure(errorCode, message)
            }
        }

        NidOAuthLogin().callDeleteTokenApi(currentActivity!!, mOAuthLoginHandler)
    }

    fun refreshAccessTokenWithRefreshToken(result: Result) {
        val mOAuthLoginHnadler = object : OAuthLoginCallback {
            override fun onSuccess() {
                result.success(true)
            }

            override fun onFailure(httpStatus: Int, message: String) {
                val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                val errorDesc = NaverIdLoginSDK.getLastErrorDescription()
                errorResponse(result, errorCode, errorDesc)
            }

            override fun onError(errorCode: Int, message: String) {
                onFailure(errorCode, message)
            }
        }
        NidOAuthLogin().callRefreshAccessTokenApi(currentActivity!!, mOAuthLoginHnadler)
    }

    internal inner class ProfileTask : AsyncTask<String, Void, String>() {
        var result: String = "";
        override fun doInBackground(vararg arg: String): String {
            val token = arg[0]// 네이버 로그인 접근 토큰;
            val header = "Bearer $token" // Bearer 다음에 공백 추가
            try {
                val apiURL = "https://openapi.naver.com/v1/nid/me"
                val url = URL(apiURL)
                val con = url.openConnection() as HttpURLConnection
                con.requestMethod = "GET"
                con.setRequestProperty("Authorization", header)
                val responseCode = con.responseCode
                val br: BufferedReader
                if (responseCode == 200) { // 정상 호출
                    br = BufferedReader(InputStreamReader(con.inputStream))
                } else {  // 에러 발생
                    br = BufferedReader(InputStreamReader(con.errorStream))
                }
                val response = StringBuffer()
                val allText = br.use(BufferedReader::readText)
                result = allText
                br.close()
                println(response.toString())
            } catch (e: Exception) {
                println(e)
            }

            //result 값은 JSONObject 형태로 넘어옵니다.
            return result
        }

        override fun onPostExecute(s: String) {

        }
    }

    @Throws(JSONException::class)
    fun jsonObjectToMap(jObject: JSONObject): HashMap<String, String> {
        val map = HashMap<String, String>()
        val keys = jObject.keys()

        while (keys.hasNext()) {
            val key = keys.next() as String
            val value = jObject.getString(key)
            map[key] = value
        }
        return map
    }
}
