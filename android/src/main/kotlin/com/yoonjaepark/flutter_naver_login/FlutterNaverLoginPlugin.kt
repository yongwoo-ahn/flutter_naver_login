package com.yoonjaepark.flutter_naver_login

import android.content.Context
import android.content.pm.PackageManager
import android.os.AsyncTask
import androidx.annotation.NonNull
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.NidOAuthLoginState
import com.navercorp.nid.oauth.OAuthLoginCallback
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutionException

/** FlutterNaverLoginPlugin */
class FlutterNaverLoginPlugin : FlutterPlugin,ActivityAware {
    private lateinit var channel: MethodChannel
    private lateinit var methodCallHandler: NaverMethodCallHandler;
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_naver_login")
            var methodCallHandler=NaverMethodCallHandler()
            methodCallHandler.setContext(registrar.context())
            channel.setMethodCallHandler(methodCallHandler)
            NaverMethodCallHandler.initSDK(registrar.context())
        }
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(
            flutterPluginBinding.getFlutterEngine().getDartExecutor(),
            "flutter_naver_login"
        )
        NaverMethodCallHandler.initSDK(flutterPluginBinding.getApplicationContext())
        this.methodCallHandler = NaverMethodCallHandler()
        this.methodCallHandler.setContext(flutterPluginBinding.getApplicationContext())
        this.channel.setMethodCallHandler(methodCallHandler)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        this.channel.setMethodCallHandler(null)
    }
    override fun onDetachedFromActivity() {}
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        this.methodCallHandler.setContext(binding.getActivity())
    }
    override fun onDetachedFromActivityForConfigChanges() {}
}

class NaverMethodCallHandler : MethodCallHandler {
    companion object{
        private var OAUTH_CLIENT_ID = "OAUTH_CLIENT_ID"
        private var OAUTH_CLIENT_SECRET = "OAUTH_CLIENT_SECRET"
        private var OAUTH_CLIENT_NAME = "OAUTH_CLIENT_NAME"

        @JvmStatic fun initSDK(context: Context){
            if (NaverIdLoginSDK.getState() != NidOAuthLoginState.NEED_INIT) {
                var packageName = context.packageName
                packageName.let {
                    var applicationInfo =
                        context.packageManager.getApplicationInfo(
                            it,
                            PackageManager.GET_META_DATA
                        )

                    var bundle = applicationInfo.metaData

                    if (bundle != null) {
                        OAUTH_CLIENT_ID = bundle.getString("com.naver.sdk.clientId")!!.toString()
                        OAUTH_CLIENT_SECRET =
                            bundle.getString("com.naver.sdk.clientSecret")!!.toString()
                        OAUTH_CLIENT_NAME = bundle.getString("com.naver.sdk.clientName")!!.toString()
                        NaverIdLoginSDK.initialize(
                            context,
                            OAUTH_CLIENT_ID,
                            OAUTH_CLIENT_SECRET,
                            OAUTH_CLIENT_NAME
                        )
                    }
                }
            }
        }
        @JvmStatic fun currentAccount(result: MethodChannel.Result) {
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
        @Throws(JSONException::class)
        @JvmStatic fun jsonObjectToMap(jObject: JSONObject): HashMap<String, String> {
            val map = HashMap<String, String>()
            val keys = jObject.keys()

            while (keys.hasNext()) {
                val key = keys.next() as String
                val value = jObject.getString(key)
                map[key] = value
            }
            return map
        }
        @JvmStatic fun errorResponse(result: MethodChannel.Result, status: String, description: String?) {
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

        @JvmStatic fun login(context: Context,result: MethodChannel.Result) {
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
            NaverIdLoginSDK.authenticate(context, mOAuthLoginHandler)
        }

        @JvmStatic fun logout(result: MethodChannel.Result) {
            NaverIdLoginSDK.logout()
            result.success(object : HashMap<String, Any>() {
                init {
                    put("status", "cancelledByUser")
                    put("isLogin", false)
                }
            })
        }

        @JvmStatic fun logoutAndDeleteToken(context: Context,result: MethodChannel.Result) {
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

            NidOAuthLogin().callDeleteTokenApi(
                context,
                mOAuthLoginHandler
            )
        }

        @JvmStatic fun refreshAccessTokenWithRefreshToken(context: Context,result: MethodChannel.Result) {
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
            NidOAuthLogin().callRefreshAccessTokenApi(
                context,
                mOAuthLoginHnadler
            )
        }
    }


    /** Plugin registration.  */
    private val METHOD_LOG_IN = "logIn"
    private val METHOD_LOG_OUT = "logOut"
    private val METHOD_LOG_OUT_DELETE_TOKEN = "logoutAndDeleteToken"
    private val METHOD_GET_ACCOUNT = "getCurrentAcount"

    private val METHOD_GET_TOKEN = "getCurrentAccessToken"
    private val METHOD_REFRESH_ACCESS_TOKEN_WITH_REFRESH_TOKEN =
        "refreshAccessTokenWithRefreshToken"

    private var context:Context?=null

    fun setContext(context: Context){
        this.context = context
    }
    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        if (call.method == METHOD_LOG_IN) {
            NaverMethodCallHandler.login(this.context!!,result)
        } else if (call.method == METHOD_LOG_OUT) {
            NaverMethodCallHandler.logout(result)
        } else if (call.method == METHOD_LOG_OUT_DELETE_TOKEN) {
            NaverMethodCallHandler.logoutAndDeleteToken(this.context!!,result)
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
            NaverMethodCallHandler.currentAccount(result)
        } else if (call.method == METHOD_REFRESH_ACCESS_TOKEN_WITH_REFRESH_TOKEN) {
            NaverMethodCallHandler.refreshAccessTokenWithRefreshToken(
                this.context!!,
                result
            )
        } else {
            result.notImplemented()
        }
    }
}
class ProfileTask : AsyncTask<String, Void, String>() {
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

