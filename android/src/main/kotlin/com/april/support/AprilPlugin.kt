package com.april.support

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Process
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlin.system.exitProcess

/** AprilPlugin */
class AprilPlugin : FlutterPlugin, ActivityAware, MethodCallHandler {
    private var activity: Activity? = null
    private lateinit var channel: MethodChannel

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(
                flutterPluginBinding.binaryMessenger,
                "April.FlutterDevelopSupport.MethodChannelName",
        )
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            //回到桌面
            "backToDesktop" -> {
                activity?.moveTaskToBack(false)
                result.success(activity != null)
            }
            //关闭应用
            "closeApplication" -> {
                Process.killProcess(Process.myPid())
                exitProcess(0)
            }
            //重启应用
            "restartApplication" -> {
                result.success(activity != null)
                activity?.let { activity ->
                    activity.baseContext.packageManager
                            .getLaunchIntentForPackage(activity.baseContext.packageName)
                            ?.let {
                                it.addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                                or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                )
                                activity.baseContext.startActivity(it)
                            }
                    activity.overridePendingTransition(0, 0)
                    Process.killProcess(Process.myPid())
                    exitProcess(0)
                }
            }
            //获取能够接收该隐式跳转链的 APP 包名
            "canLaunchComponentName" -> {
                if (activity == null) {
                    result.error("The host Activity is destroyed", null, null)
                } else {
                    val componentName: ComponentName? = Intent(Intent.ACTION_VIEW).also {
                        it.data = Uri.parse(call.argument<String>("url") ?: "")
                    }.resolveActivity(activity!!.packageManager)
                    result.success(mapOf(
                            "packageName" to componentName?.packageName,
                            "className" to componentName?.className,
                    ))
                }
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }
}