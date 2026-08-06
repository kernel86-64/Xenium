package com.kernel64.xenium.util

import android.content.Context
import android.os.Build
import android.webkit.WebView
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLContext

object SystemInfoHelper {

    fun getOSVersion(): String {
        return "Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; ${Build.DISPLAY}"
    }

    fun getWebViewInfo(context: Context): String {
        return try {
            val webViewPackage = WebView.getCurrentWebViewPackage()
            val packageName = webViewPackage?.packageName ?: return "Unknown WebView"
            val versionName = webViewPackage.versionName ?: "Unknown Version"
            
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            val readableName = pm.getApplicationLabel(info).toString()
            
            "$readableName $versionName"
        } catch (e: Exception) {
            "Unknown WebView"
        }
    }

    fun getGPUModel(): String {
        return try {
            val egl = EGLContext.getEGL() as EGL10
            val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            if (!egl.eglInitialize(display, version)) {
                return "Unknown GPU (EGL init failed)"
            }

            val configAttribs = intArrayOf(
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
                EGL10.EGL_RENDERABLE_TYPE, 4 /* EGL_OPENGL_ES2_BIT */,
                EGL10.EGL_NONE
            )
            val configs = arrayOfNulls<javax.microedition.khronos.egl.EGLConfig>(1)
            val numConfigs = IntArray(1)
            egl.eglChooseConfig(display, configAttribs, configs, 1, numConfigs)
            val config = configs[0]

            if (config == null) {
                egl.eglTerminate(display)
                return "Unknown GPU (No config)"
            }

            val pbufferAttribs = intArrayOf(
                EGL10.EGL_WIDTH, 1,
                EGL10.EGL_HEIGHT, 1,
                EGL10.EGL_NONE
            )
            val pbuffer = egl.eglCreatePbufferSurface(display, config, pbufferAttribs)

            val contextAttribs = intArrayOf(
                0x3098 /* EGL_CONTEXT_CLIENT_VERSION */, 2,
                EGL10.EGL_NONE
            )
            val eglContext = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, contextAttribs)

            egl.eglMakeCurrent(display, pbuffer, pbuffer, eglContext)
            
            val renderer = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_RENDERER)
            val vendor = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_VENDOR)
            
            egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
            egl.eglDestroySurface(display, pbuffer)
            egl.eglDestroyContext(display, eglContext)
            egl.eglTerminate(display)
            
            if (vendor != null && renderer != null) {
                "$vendor $renderer"
            } else if (renderer != null) {
                renderer
            } else {
                "Unknown GPU"
            }
        } catch (e: Exception) {
            "Unknown GPU"
        }
    }
}
