package com.example.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.VeriLensApplication
import com.example.feature.verification.capture.ScreenCaptureActivity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class VeriLensOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingBubbleView: View? = null
    private var bubbleLayoutParams: WindowManager.LayoutParams? = null

    private var bottomSheetView: View? = null
    private var bottomSheetLayoutParams: WindowManager.LayoutParams? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isDragging = false
    private var isLongPressDetected = false

    // Opacity animation runnable
    private val idleFadeRunnable = Runnable {
        floatingBubbleView?.animate()
            ?.alpha(0.68f)
            ?.setDuration(350)
            ?.start()
    }

    companion object {
        private const val TAG = "VeriLensOverlay"
        const val ACTION_START = "com.example.overlay.ACTION_START"
        const val ACTION_STOP = "com.example.overlay.ACTION_STOP"
        const val EXTRA_OPEN_SHEET = "extra_open_overlay_sheet"
        const val BUBBLE_IDLE_DELAY_MS = 2500L
        const val LONG_PRESS_DURATION_MS = 650L

        @Volatile
        var isOverlayActive: Boolean = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Initializing WindowManager")
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get WindowManager service", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Starting service (action=${intent?.action})")

        if (intent?.action == ACTION_STOP) {
            Log.d(TAG, "ACTION_STOP received, stopping foreground and service")
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping foreground", e)
            }
            stopSelf()
            return START_NOT_STICKY
        }

        // Verify overlay permission using Settings.canDrawOverlays()
        val hasOverlayPermission = Settings.canDrawOverlays(this)
        Log.d(TAG, "Permission granted: $hasOverlayPermission")
        if (!hasOverlayPermission) {
            Log.w(TAG, "Overlay permission not granted. Stopping service.")
            showErrorToast("Overlay permission is required for floating assistant")
            stopSelf()
            return START_NOT_STICKY
        }

        // Build and display foreground notification
        Log.d(TAG, "Creating notification")
        val notification = buildForegroundNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    VeriLensApplication.OVERLAY_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(
                    VeriLensApplication.OVERLAY_NOTIFICATION_ID,
                    notification
                )
            }
            Log.d(TAG, "Foreground service started with TYPE_SPECIAL_USE")
        } catch (e: Exception) {
            Log.e(TAG, "SecurityException or error in startForeground: ${e.message}", e)
            showErrorToast("Could not start floating assistant notification")
            stopSelf()
            return START_NOT_STICKY
        }

        isOverlayActive = true

        // Create overlay and attach to WindowManager
        initFloatingBubble()

        return START_STICKY
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, VeriLensOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, VeriLensApplication.OVERLAY_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_service_name))
            .setContentText("VeriLens Floating Assistant is active above other apps")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_overlay_close, "Stop Assistant", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initFloatingBubble() {
        if (floatingBubbleView != null) {
            Log.d(TAG, "Floating bubble view already exists, skipping creation")
            return
        }

        Log.d(TAG, "Creating overlay")
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val size = (58 * resources.displayMetrics.density).toInt()
        val screenHeight = resources.displayMetrics.heightPixels

        bubbleLayoutParams = WindowManager.LayoutParams(
            size,
            size,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (14 * resources.displayMetrics.density).toInt()
            y = (screenHeight * 0.35f).toInt()
        }

        val themedContext = ContextThemeWrapper(this, R.style.Theme_VeriLens_Transparent)
        val inflater = LayoutInflater.from(themedContext)
        val bubble: View
        try {
            bubble = inflater.inflate(R.layout.layout_floating_bubble, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inflate layout_floating_bubble", e)
            showErrorToast("Failed to initialize overlay layout")
            stopSelf()
            return
        }

        // Setup touch, drag, long-press, and snap listener
        bubble.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private val touchSlop = 10 * resources.displayMetrics.density

            private val longPressRunnable = Runnable {
                if (!isDragging) {
                    isLongPressDetected = true
                    triggerHapticFeedback()
                    Toast.makeText(
                        this@VeriLensOverlayService,
                        "VeriLens Floating Assistant closed",
                        Toast.LENGTH_SHORT
                    ).show()
                    stopSelf()
                }
            }

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val params = bubbleLayoutParams ?: return false

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mainHandler.removeCallbacks(idleFadeRunnable)
                        bubble.alpha = 1.0f

                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        isLongPressDetected = false

                        mainHandler.postDelayed(longPressRunnable, LONG_PRESS_DURATION_MS)
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY

                        if (abs(deltaX) > touchSlop || abs(deltaY) > touchSlop) {
                            if (!isDragging) {
                                isDragging = true
                                mainHandler.removeCallbacks(longPressRunnable)
                            }
                            params.x = initialX + deltaX.toInt()
                            params.y = initialY + deltaY.toInt()

                            try {
                                if (bubble.isAttachedToWindow) {
                                    windowManager?.updateViewLayout(bubble, params)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error updating bubble layout", e)
                            }
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        mainHandler.removeCallbacks(longPressRunnable)

                        if (isLongPressDetected) {
                            return true
                        }

                        val deltaX = abs(event.rawX - initialTouchX)
                        val deltaY = abs(event.rawY - initialTouchY)

                        if (!isDragging && deltaX < touchSlop && deltaY < touchSlop) {
                            showOverlayBottomSheet()
                        } else {
                            snapToNearestEdge(params.x)
                        }

                        scheduleIdleFade()
                        return true
                    }
                }
                return false
            }
        })

        floatingBubbleView = bubble

        Log.d(TAG, "Adding view")
        try {
            windowManager?.addView(floatingBubbleView, bubbleLayoutParams)
            Log.d(TAG, "Overlay ready")
            scheduleIdleFade()
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "BadTokenException adding view to WindowManager: ${e.message}", e)
            showErrorToast("Could not display floating assistant")
            stopSelf()
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException adding view to WindowManager: ${e.message}", e)
            showErrorToast("Overlay permission error")
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error adding view to WindowManager: ${e.message}", e)
            showErrorToast("Could not initialize floating assistant")
            stopSelf()
        }
    }

    private fun scheduleIdleFade() {
        mainHandler.removeCallbacks(idleFadeRunnable)
        mainHandler.postDelayed(idleFadeRunnable, BUBBLE_IDLE_DELAY_MS)
    }

    private fun snapToNearestEdge(currentX: Int) {
        val wm = windowManager ?: return
        val displaySize = Point()
        @Suppress("DEPRECATION")
        wm.defaultDisplay?.getSize(displaySize)
        val screenWidth = displaySize.x
        val bubbleWidth = floatingBubbleView?.width ?: (58 * resources.displayMetrics.density).toInt()
        val margin = (12 * resources.displayMetrics.density).toInt()

        val targetX = if (currentX + bubbleWidth / 2 < screenWidth / 2) {
            margin
        } else {
            screenWidth - bubbleWidth - margin
        }

        val params = bubbleLayoutParams ?: return
        val animator = ValueAnimator.ofInt(params.x, targetX)
        animator.duration = 260
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            params.x = animation.animatedValue as Int
            floatingBubbleView?.let { view ->
                if (view.isAttachedToWindow) {
                    try {
                        wm.updateViewLayout(view, params)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error animating snap", e)
                    }
                }
            }
        }
        animator.start()
    }

    /**
     * Displays a system-wide Material Design bottom sheet on top of any active application.
     */
    private fun showOverlayBottomSheet() {
        if (bottomSheetView != null) return

        floatingBubbleView?.visibility = View.INVISIBLE

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bottomSheetLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        val themedContext = ContextThemeWrapper(this, R.style.Theme_VeriLens_Transparent)
        val inflater = LayoutInflater.from(themedContext)
        val sheetView: View
        try {
            sheetView = inflater.inflate(R.layout.layout_system_overlay_bottom_sheet, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inflate bottom sheet layout", e)
            floatingBubbleView?.visibility = View.VISIBLE
            return
        }

        val scrim = sheetView.findViewById<View>(R.id.overlay_root_scrim)
        val container = sheetView.findViewById<View>(R.id.overlay_sheet_container)
        val btnClose = sheetView.findViewById<View>(R.id.btn_overlay_close)
        val btnHeaderClose = sheetView.findViewById<View>(R.id.btn_header_close)

        val itemCaptureScreenshot = sheetView.findViewById<View>(R.id.item_capture_screenshot)
        val itemUploadScreenshot = sheetView.findViewById<View>(R.id.item_upload_screenshot)
        val itemPasteText = sheetView.findViewById<View>(R.id.item_paste_text)
        val itemPasteLink = sheetView.findViewById<View>(R.id.item_paste_link)

        scrim?.setOnClickListener { dismissOverlayBottomSheet() }
        btnClose?.setOnClickListener { dismissOverlayBottomSheet() }
        btnHeaderClose?.setOnClickListener { dismissOverlayBottomSheet() }

        itemCaptureScreenshot?.setOnClickListener {
            dismissOverlayBottomSheet()
            val captureIntent = Intent(this, ScreenCaptureActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(captureIntent)
        }

        itemUploadScreenshot?.setOnClickListener {
            dismissOverlayBottomSheet()
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_ACTION, MainActivity.ACTION_UPLOAD_SCREENSHOT)
            }
            startActivity(mainIntent)
        }

        itemPasteText?.setOnClickListener {
            dismissOverlayBottomSheet()
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_ACTION, MainActivity.ACTION_PASTE_TEXT)
            }
            startActivity(mainIntent)
        }

        itemPasteLink?.setOnClickListener {
            dismissOverlayBottomSheet()
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_ACTION, MainActivity.ACTION_PASTE_LINK)
            }
            startActivity(mainIntent)
        }

        container?.post {
            container.translationY = container.height.toFloat()
            container.animate()
                .translationY(0f)
                .setDuration(280)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        bottomSheetView = sheetView
        try {
            windowManager?.addView(bottomSheetView, bottomSheetLayoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach bottom sheet view", e)
            floatingBubbleView?.visibility = View.VISIBLE
        }
    }

    private fun dismissOverlayBottomSheet() {
        val sheet = bottomSheetView ?: return
        val container = sheet.findViewById<View>(R.id.overlay_sheet_container)

        container?.animate()
            ?.translationY(container.height.toFloat())
            ?.setDuration(200)
            ?.withEndAction {
                safelyRemoveSheetView(sheet)
            }
            ?.start() ?: safelyRemoveSheetView(sheet)
    }

    private fun safelyRemoveSheetView(sheet: View) {
        try {
            if (sheet.isAttachedToWindow) {
                windowManager?.removeView(sheet)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error removing bottom sheet view", e)
        }
        bottomSheetView = null
        floatingBubbleView?.visibility = View.VISIBLE
        scheduleIdleFade()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val params = bubbleLayoutParams ?: return
        val wm = windowManager ?: return
        val displaySize = Point()
        @Suppress("DEPRECATION")
        wm.defaultDisplay?.getSize(displaySize)
        val screenWidth = displaySize.x
        val screenHeight = displaySize.y
        val bubbleSize = (58 * resources.displayMetrics.density).toInt()

        params.x = max(12, min(params.x, screenWidth - bubbleSize - 12))
        params.y = max(12, min(params.y, screenHeight - bubbleSize - 24))

        floatingBubbleView?.let { view ->
            if (view.isAttachedToWindow) {
                try {
                    wm.updateViewLayout(view, params)
                    snapToNearestEdge(params.x)
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating configuration layout", e)
                }
            }
        }
    }

    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic feedback unavailable", e)
        }
    }

    private fun showErrorToast(message: String) {
        try {
            mainHandler.post {
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show toast", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Overlay service being destroyed, cleaning up views")
        isOverlayActive = false
        mainHandler.removeCallbacks(idleFadeRunnable)

        bottomSheetView?.let {
            if (it.isAttachedToWindow) {
                try {
                    windowManager?.removeView(it)
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing bottom sheet on destroy", e)
                }
            }
        }
        bottomSheetView = null

        floatingBubbleView?.let {
            if (it.isAttachedToWindow) {
                try {
                    windowManager?.removeView(it)
                } catch (e: Exception) {
                    Log.w(TAG, "Error removing floating bubble on destroy", e)
                }
            }
        }
        floatingBubbleView = null
    }
}
