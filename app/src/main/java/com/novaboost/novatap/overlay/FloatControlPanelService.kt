package com.novaboost.novatap.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.novaboost.novatap.ui.MainViewModel
import com.novaboost.novatap.data.model.TapPoint
import com.novaboost.novatap.data.model.TapZone
import com.novaboost.novatap.data.model.SwipeCoordinates
import com.novaboost.novatap.ui.theme.NovaTapTheme
import kotlin.math.*
import kotlinx.coroutines.launch

class FloatControlPanelService : Service() {

    private var windowManager: WindowManager? = null

    // Window instances
    private var panelComposeView: ComposeView? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private var tapVisualizerView: ComposeView? = null

    private val targetViews = mutableListOf<View>()
    private var swipeLineComposeView: ComposeView? = null
    private var timingDialogView: View? = null

    private var currentMode = "single" // "single", "multi", "area", "swipe"
    private var isWorkspacePlaying = false
    private var isExpanded = true

    // Floater Panel floating offset
    private var panelX = 150
    private var panelY = 300

    private var snapToGrid = false

    private val serviceLifecycleOwner = MyServiceLifecycleOwner()
    private val serviceViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = ViewModelStore()
    }

    private val viewModel: MainViewModel? get() = MainViewModel.instance

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        setTheme(com.novaboost.novatap.R.style.Theme_MyApplication)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Dynamically center the control panel on the left edge of the screen
        val displayMetrics = resources.displayMetrics
        panelX = 0
        panelY = (displayMetrics.heightPixels / 2) - dpToPx(120f)

        try {
            serviceLifecycleOwner.performRestore(null)
            serviceLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            serviceLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            serviceLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        MainViewModel.instance?.isOverlayWorkspaceActive = true

        MainViewModel.instance?.onAutomationActiveChanged = { active ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                isWorkspacePlaying = active
                recreateOverlayWindows()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("mode")?.let {
            currentMode = it
        }
        recreateOverlayWindows()
        return START_STICKY
    }

    private fun dpToPx(dp: Float): Int {
        val density = resources.displayMetrics.density
        return (dp * density).roundToInt()
    }

    private fun createComposeView(content: @Composable () -> Unit): ComposeView {
        val themedContext = android.view.ContextThemeWrapper(this, com.novaboost.novatap.R.style.Theme_MyApplication)
        return ComposeView(themedContext).apply {
            setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(serviceLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(serviceLifecycleOwner)
            setViewTreeViewModelStoreOwner(serviceViewModelStoreOwner)

            setContent {
                val themeMode = MainViewModel.instance?.selectedTheme ?: "dark"
                val isDarkTheme = when (themeMode) {
                    "dark" -> true
                    "light" -> false
                    else -> isSystemInDarkTheme()
                }
                NovaTapTheme(darkTheme = isDarkTheme) {
                    content()
                }
            }
        }
    }

    private fun createParams(width: Int, height: Int, x: Int, y: Int, touchable: Boolean = true): WindowManager.LayoutParams {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (!touchable) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }

        return WindowManager.LayoutParams(
            width,
            height,
            layoutType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
    }

    private fun removeAllOverlayWindows() {
        panelComposeView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        panelComposeView = null

        targetViews.forEach { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        targetViews.clear()

        swipeLineComposeView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        swipeLineComposeView = null

        tapVisualizerView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        tapVisualizerView = null
    }

    private fun recreateOverlayWindows() {
        try {
            removeAllOverlayWindows()

            val viewModel = MainViewModel.instance ?: return
            isWorkspacePlaying = viewModel.isAutomationActive

            // 1. Create Floating Control Panel View
            val panelWidth = dpToPx(56f)
            val panelHeight = WindowManager.LayoutParams.WRAP_CONTENT

            panelParams = createParams(panelWidth, panelHeight, panelX, panelY, touchable = true)

            panelComposeView = createComposeView {
                CompactControlPanelCard(
                    isRu = viewModel.selectedLanguage == "ru",
                    isPlaying = isWorkspacePlaying,
                    onTogglePlay = {
                        isWorkspacePlaying = !isWorkspacePlaying
                        if (isWorkspacePlaying) {
                            when (currentMode) {
                                "single" -> viewModel.startSingleTapAutomation()
                                "multi" -> viewModel.startMultiTapAutomation()
                                "area" -> viewModel.startAreaTapAutomation()
                                "swipe" -> viewModel.startSwipeAutomation()
                            }
                        } else {
                            viewModel.stopAutomation()
                        }
                        recreateOverlayWindows()
                    },
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        isExpanded = it
                        recreateOverlayWindows()
                    },
                    onClose = {
                        viewModel.stopAutomation()
                        stopSelf()
                    },
                    onOpenSettings = {
                        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        }
                    },
                    onDrag = { dx, dy ->
                        panelX += dx.roundToInt()
                        panelY += dy.roundToInt()
                        panelParams?.let { p ->
                            p.x = panelX
                            p.y = panelY
                            panelComposeView?.let { v -> windowManager?.updateViewLayout(v, p) }
                        }
                    },
                    currentMode = currentMode,
                    viewModel = viewModel,
                    snapToGrid = snapToGrid,
                    onToggleSnap = {
                        snapToGrid = it
                    },
                    onAddMultiPoint = {
                        val newPoint = viewModel.addMultiTapPoint(400f, 800f)
                        recreateOverlayWindows()
                        if (newPoint != null) {
                            showTimingDialog(newPoint) { delay, hold ->
                                viewModel.updateMultiTapPointTimings(newPoint.id, delay, hold)
                            }
                        }
                    },
                    onAddAreaZone = {
                        viewModel.addAreaTapZone(isAllowed = true, isRect = true, x = 450f, y = 700f)
                        recreateOverlayWindows()
                    },
                    onToggleDiagnostics = {
                        recreateOverlayWindows()
                    }
                )
            }

            try {
                windowManager?.addView(panelComposeView, panelParams)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Draw over other apps permission required: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
                stopSelf()
                return
            }

            // 2. Add Mode-Specific Drag Targets (ONLY when NOT actively playing)
            // To ensure 100% touch reliability and bypass Android 12+ "Block untrusted touches" protection,
            // we remove all target coordinate handle overlays from the screen during active playback.
            // This leaves the screen 100% clean and allows gestures to execute with absolute security and zero obstruction.
            if (!isWorkspacePlaying) {
                when (currentMode) {
                    "single" -> addSingleTargetWindow(viewModel)
                    "multi" -> addMultiTargetWindows(viewModel)
                    "area" -> addAreaTargetWindows(viewModel)
                    "swipe" -> addSwipeTargetWindows(viewModel)
                }
            }

                // 3. Add Full-Screen Tap Visualizer Overlay.
                // In compatibility mode (Xiaomi/Redmi), hide it during active automation to reduce blocked-tap risk.
                val shouldShowTapVisualizer = (viewModel.showTapRipples || viewModel.showDiagnostics) &&
                    !(viewModel.compatibilityMode && isWorkspacePlaying)
                if (shouldShowTapVisualizer) {
                val visualizerParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    },
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = 0
                    y = 0
                }

                tapVisualizerView = createComposeView {
                    TapVisualizerOverlay(viewModel)
                }

                try {
                    windowManager?.addView(tapVisualizerView, visualizerParams)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            val logger = MainViewModel.instance
            logger?.executionLog?.value = "CRASH IN SERVICE: ${t.localizedMessage}\n${t.stackTrace.take(4).joinToString("\n")}"
            Toast.makeText(this, "Crash in Service: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun addSingleTargetWindow(viewModel: MainViewModel) {
        val preset = viewModel.activeSingleTapPreset
        var px = preset.pointsJson.toFloatOrNull() ?: 500f
        var py = preset.zonesJson.toFloatOrNull() ?: 1000f

        val targetSize = dpToPx(68f)
        val params = createParams(
            targetSize,
            targetSize,
            (px - targetSize / 2f).roundToInt(),
            (py - targetSize / 2f).roundToInt(),
            touchable = !isWorkspacePlaying
        )

        var singleView: View? = null
        singleView = createComposeView {
            DraggableTargetPointWindow(
                index = 1,
                initialX = px,
                initialY = py,
                snapToGrid = snapToGrid,
                isPlaying = isWorkspacePlaying,
                onPositionChanged = { nx, ny ->
                    px = nx
                    py = ny
                    viewModel.activeSingleTapPreset = viewModel.activeSingleTapPreset.copy(
                        pointsJson = nx.roundToInt().toString(),
                        zonesJson = ny.roundToInt().toString()
                    )
                    params.x = (nx - targetSize / 2f).roundToInt()
                    params.y = (ny - targetSize / 2f).roundToInt()
                    try {
                        singleView?.let { windowManager?.updateViewLayout(it, params) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            )
        }

        try {
            windowManager?.addView(singleView, params)
            targetViews.add(singleView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addMultiTargetWindows(viewModel: MainViewModel) {
        val points = viewModel.multiTapPoints
        points.forEachIndexed { index, point ->
            var px = point.x
            var py = point.y
            val pointId = point.id

            val targetSize = dpToPx(68f)
            val params = createParams(
                targetSize,
                targetSize,
                (px - targetSize / 2f).roundToInt(),
                (py - targetSize / 2f).roundToInt(),
                touchable = !isWorkspacePlaying
            )

            var multiView: View? = null
            multiView = createComposeView {
                DraggableTargetPointWindow(
                    index = index + 1,
                    initialX = px,
                    initialY = py,
                    snapToGrid = snapToGrid,
                    isPlaying = isWorkspacePlaying,
                    onPositionChanged = { nx, ny ->
                        px = nx
                        py = ny
                        viewModel.updateMultiTapPoint(pointId, nx, ny)
                        
                        params.x = (nx - targetSize / 2f).roundToInt()
                        params.y = (ny - targetSize / 2f).roundToInt()
                        try {
                            multiView?.let { windowManager?.updateViewLayout(it, params) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onDelete = {
                        viewModel.deleteMultiTapPoint(pointId)
                        recreateOverlayWindows()
                    },
                    onDuplicate = {
                        viewModel.duplicateMultiTapPoint(pointId)
                        recreateOverlayWindows()
                    },
                    onEdit = {
                        showTimingDialog(point) { delay, hold ->
                            viewModel.updateMultiTapPointTimings(pointId, delay, hold)
                        }
                    }
                )
            }

            try {
                windowManager?.addView(multiView, params)
                targetViews.add(multiView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun addAreaTargetWindows(viewModel: MainViewModel) {
        val zones = viewModel.areaTapZones
        zones.forEachIndexed { index, zone ->
            var px = zone.x
            var py = zone.y
            val zoneId = zone.id

            val paddingPx = dpToPx(24f)
            val baseW = if (zone.isRect) zone.width else (zone.radius * 2)
            val baseH = if (zone.isRect) zone.height else (zone.radius * 2)

            val winW = dpToPx(baseW) + paddingPx
            val winH = dpToPx(baseH) + paddingPx

            val params = createParams(
                winW,
                winH,
                (px - winW / 2f).roundToInt(),
                (py - winH / 2f).roundToInt(),
                touchable = !isWorkspacePlaying
            )

            var areaView: View? = null
            areaView = createComposeView {
                DraggableTargetZoneWindow(
                    index = index + 1,
                    zone = zone,
                    snapToGrid = snapToGrid,
                    isPlaying = isWorkspacePlaying,
                    onPositionChanged = { nx, ny ->
                        px = nx
                        py = ny
                        viewModel.updateAreaTapZone(zoneId, nx, ny, zone.width, zone.height, zone.radius)
                        
                        params.x = (nx - winW / 2f).roundToInt()
                        params.y = (ny - winH / 2f).roundToInt()
                        try {
                            areaView?.let { windowManager?.updateViewLayout(it, params) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    onResize = { nw, nh, nr ->
                        viewModel.updateAreaTapZone(zoneId, px, py, nw, nh, nr)
                        recreateOverlayWindows()
                    },
                    onDelete = {
                        viewModel.deleteAreaTapZone(zoneId)
                        recreateOverlayWindows()
                    },
                    onDuplicate = {
                        viewModel.duplicateAreaTapZone(zoneId)
                        recreateOverlayWindows()
                    },
                    onToggleAllowed = {
                        val currentList = viewModel.areaTapZones.toMutableList()
                        val zIndex = currentList.indexOfFirst { it.id == zoneId }
                        if (zIndex != -1) {
                            currentList[zIndex] = currentList[zIndex].copy(isAllowed = !zone.isAllowed)
                            viewModel.areaTapZones = currentList
                        }
                        recreateOverlayWindows()
                    }
                )
            }

            try {
                windowManager?.addView(areaView, params)
                targetViews.add(areaView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun addSwipeTargetWindows(viewModel: MainViewModel) {
        val coords = viewModel.swipeCoordinates
        var sx = coords.startX
        var sy = coords.startY
        var ex = coords.endX
        var ey = coords.endY

        val targetSize = dpToPx(68f)

        // Create Start Anchor Window (S)
        val startParams = createParams(
            targetSize,
            targetSize,
            (sx - targetSize / 2f).roundToInt(),
            (sy - targetSize / 2f).roundToInt(),
            touchable = !isWorkspacePlaying
        )

        var startView: View? = null
        startView = createComposeView {
            DraggableAnchorWidget(
                label = "S",
                x = sx,
                y = sy,
                snapToGrid = snapToGrid,
                isPlaying = isWorkspacePlaying,
                color = Color(0xFFF57C00),
                onDrag = { nx, ny ->
                    sx = nx
                    sy = ny
                    viewModel.swipeCoordinates = viewModel.swipeCoordinates.copy(startX = nx, startY = ny)
                    
                    startParams.x = (nx - targetSize / 2f).roundToInt()
                    startParams.y = (ny - targetSize / 2f).roundToInt()
                    try {
                        startView?.let { windowManager?.updateViewLayout(it, startParams) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    swipeLineComposeView?.invalidate()
                }
            )
        }

        // Create End Anchor Window (E)
        val endParams = createParams(
            targetSize,
            targetSize,
            (ex - targetSize / 2f).roundToInt(),
            (ey - targetSize / 2f).roundToInt(),
            touchable = !isWorkspacePlaying
        )

        var endView: View? = null
        endView = createComposeView {
            DraggableAnchorWidget(
                label = "E",
                x = ex,
                y = ey,
                snapToGrid = snapToGrid,
                isPlaying = isWorkspacePlaying,
                color = Color(0xFFFFB74D),
                onDrag = { nx, ny ->
                    ex = nx
                    ey = ny
                    viewModel.swipeCoordinates = viewModel.swipeCoordinates.copy(endX = nx, endY = ny)
                    
                    endParams.x = (nx - targetSize / 2f).roundToInt()
                    endParams.y = (ny - targetSize / 2f).roundToInt()
                    try {
                        endView?.let { windowManager?.updateViewLayout(it, endParams) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    swipeLineComposeView?.invalidate()
                }
            )
        }

        // Create Connecting Line Window (MATCH_PARENT, completely untargetable!)
        val lineParams = createParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            0,
            0,
            touchable = false
        ).apply {
            gravity = Gravity.FILL
        }

        swipeLineComposeView = createComposeView {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val coordsState = viewModel.swipeCoordinates
                val csx = coordsState.startX
                val csy = coordsState.startY
                val cex = coordsState.endX
                val cey = coordsState.endY

                drawLine(
                    color = Color(0xFFFFB74D),
                    start = Offset(csx, csy),
                    end = Offset(cex, cey),
                    strokeWidth = 6f
                )
                val angle = atan2(cey - csy, cex - csx)
                val arrowLength = 30f
                val arrowX1 = cex - arrowLength * cos(angle - Math.PI / 6).toFloat()
                val arrowY1 = cey - arrowLength * sin(angle - Math.PI / 6).toFloat()
                val arrowX2 = cex - arrowLength * cos(angle + Math.PI / 6).toFloat()
                val arrowY2 = cey - arrowLength * sin(angle + Math.PI / 6).toFloat()

                drawLine(Color(0xFFFFB74D), Offset(cex, cey), Offset(arrowX1, arrowY1), strokeWidth = 6f)
                drawLine(Color(0xFFFFB74D), Offset(cex, cey), Offset(arrowX2, arrowY2), strokeWidth = 6f)
            }
        }

        try {
            windowManager?.addView(startView, startParams)
            targetViews.add(startView)

            windowManager?.addView(endView, endParams)
            targetViews.add(endView)

            // Show swipe line only when not actively playing to prevent full-screen overlay during playback
            if (!isWorkspacePlaying) {
                windowManager?.addView(swipeLineComposeView, lineParams)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MainViewModel.instance?.isOverlayWorkspaceActive = false
        if (MainViewModel.instance?.onAutomationActiveChanged != null) {
            MainViewModel.instance?.onAutomationActiveChanged = null
        }
        removeAllOverlayWindows()
        try {
            serviceLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            serviceLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            serviceLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showTimingDialog(point: com.novaboost.novatap.data.model.TapPoint, onSave: (Long, Long) -> Unit) {
        val vm = viewModel ?: return
        if (timingDialogView != null) {
            try {
                windowManager?.removeView(timingDialogView)
            } catch (e: Exception) {}
            timingDialogView = null
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            dimAmount = 0.5f
            gravity = Gravity.CENTER
        }

        timingDialogView = createComposeView {
            var delayStr by remember { mutableStateOf(point.delayMs.toString()) }
            var holdStr by remember { mutableStateOf(point.holdMs.toString()) }
            val isRu = vm.selectedLanguage == "ru"

            Card(
                modifier = Modifier
                    .width(300.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val idx = vm.multiTapPoints.indexOfFirst { it.id == point.id }
                    Text(
                        text = if (isRu) "Настройки Точки #${idx + 1}" else "Point #${idx + 1} Settings",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    OutlinedTextField(
                        value = delayStr,
                        onValueChange = { delayStr = it },
                        label = { Text(if (isRu) "Пауза (мс)" else "Pause (ms)", color = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    OutlinedTextField(
                        value = holdStr,
                        onValueChange = { holdStr = it },
                        label = { Text(if (isRu) "Удержание (мс)" else "Hold (ms)", color = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                try {
                                    windowManager?.removeView(timingDialogView)
                                } catch (e: Exception) {}
                                timingDialogView = null
                            }
                        ) {
                            Text(if (isRu) "Отмена" else "Cancel", color = Color(0xFFEF4444))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val delay = delayStr.toLongOrNull()?.coerceAtLeast(MainViewModel.MIN_INTERVAL_MS) ?: MainViewModel.MIN_INTERVAL_MS
                                val hold = holdStr.toLongOrNull()?.coerceAtLeast(MainViewModel.MIN_HOLD_MS) ?: MainViewModel.MIN_HOLD_MS
                                onSave(delay, hold)
                                try {
                                    windowManager?.removeView(timingDialogView)
                                } catch (e: Exception) {}
                                timingDialogView = null
                                recreateOverlayWindows()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                        ) {
                            Text(if (isRu) "Сохранить" else "Save", color = Color.Black)
                        }
                    }
                }
            }
        }

        try {
            windowManager?.addView(timingDialogView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun CompactControlPanelCard(
    isRu: Boolean,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    isExpanded: Boolean,
    onToggleExpand: (Boolean) -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    currentMode: String,
    viewModel: MainViewModel,
    snapToGrid: Boolean,
    onToggleSnap: (Boolean) -> Unit,
    onAddMultiPoint: () -> Unit = {},
    onAddAreaZone: () -> Unit = {},
    onToggleDiagnostics: () -> Unit = {}
) {
    val dragHandleHeight = 38.dp

    Card(
        modifier = Modifier
            .width(64.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .pointerInput(isPlaying) {
                    if (isPlaying) {
                        detectTapGestures(onTap = { onTogglePlay() })
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. DEDICATED DRAG HANDLE BOX (the exact touch target requested by the user!)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dragHandleHeight)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to move panel",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Play / Pause Button (Always present)
            IconButton(
                onClick = onTogglePlay,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isPlaying) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                ),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Toggle Expand/Collapse Button (Chevron pointing Up or Down)
            IconButton(
                onClick = { onToggleExpand(!isExpanded) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle size",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 4. Expanded menu items arranged vertically
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 8.dp))
                Spacer(modifier = Modifier.height(8.dp))

                // Settings Button
                IconButton(
                    onClick = onOpenSettings,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Close Button
                IconButton(
                    onClick = onClose,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close overlay",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Snap to Grid Button
                IconButton(
                    onClick = { onToggleSnap(!snapToGrid) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (snapToGrid) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Snap Grid",
                        tint = if (snapToGrid) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Diagnostic Toggle Button
                IconButton(
                    onClick = {
                        viewModel.showDiagnostics = !viewModel.showDiagnostics
                        onToggleDiagnostics()
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (viewModel.showDiagnostics) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Toggle Diagnostics",
                        tint = if (viewModel.showDiagnostics) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Mode-Specific actions (Add point/zone)
                if (!isPlaying) {
                    if (currentMode == "multi") {
                        Spacer(modifier = Modifier.height(6.dp))
                        IconButton(
                            onClick = onAddMultiPoint,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddLocation,
                                contentDescription = "Add Point",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (currentMode == "area") {
                        Spacer(modifier = Modifier.height(6.dp))
                        IconButton(
                            onClick = onAddAreaZone,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AcUnit,
                                contentDescription = "Add Smart Zone",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun DraggableTargetPointWindow(
    index: Int,
    initialX: Float,
    initialY: Float,
    snapToGrid: Boolean,
    isPlaying: Boolean,
    onPositionChanged: (Float, Float) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    var rx by remember { mutableStateOf(initialX) }
    var ry by remember { mutableStateOf(initialY) }
    var isSelected by remember { mutableStateOf(false) }

    LaunchedEffect(initialX, initialY) {
        rx = initialX
        ry = initialY
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(snapToGrid, isPlaying) {
                if (isPlaying) return@pointerInput
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val freshX = rx + dragAmount.x
                    val freshY = ry + dragAmount.y
                    val finalX = if (snapToGrid) (freshX / 50f).roundToInt() * 50f else freshX
                    val finalY = if (snapToGrid) (freshY / 50f).roundToInt() * 50f else freshY
                    rx = finalX
                    ry = finalY
                    onPositionChanged(finalX, finalY)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(52.dp)
                .clip(CircleShape)
                .clickable(enabled = !isPlaying) { isSelected = !isSelected }
                .border(
                    width = 2.dp,
                    color = if (isSelected) Color(0xFF00E5FF) else Color(0x7700E5FF),
                    shape = CircleShape
                )
                .background(Color(0xFF0F172A).copy(alpha = if (isPlaying) 0.4f else 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            // Draw standard targets/crosshairs on the point of simulated touch
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 1.5f
                val crosshairColor = Color(0xAA00E5FF)
                // horizontal crosshair
                drawLine(crosshairColor, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = strokeW)
                // vertical crosshair
                drawLine(crosshairColor, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = strokeW)
                
                // Outer subtle thin target ring
                drawCircle(color = Color(0x5500E5FF), radius = size.width / 2.8f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
                
                // Center touch indicator dot (standard red in target clickers)
                drawCircle(color = Color(0xFFE11D48), radius = 5f, center = center)
            }
            Text(
                text = index.toString(),
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontSize = 17.sp,
                textAlign = TextAlign.Center
            )
        }

        if (isSelected && !isPlaying) {
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(20.dp)
                        .background(Color.Red, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }

            if (onDuplicate != null) {
                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .background(Color(0xFF10B981), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }

            if (onEdit != null) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(20.dp)
                        .background(Color(0xFF00E5FF), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Edit", tint = Color.Black, modifier = Modifier.size(10.dp))
                }
            }
        }
    }
}

@Composable
fun DraggableTargetZoneWindow(
    index: Int,
    zone: TapZone,
    snapToGrid: Boolean,
    isPlaying: Boolean,
    onPositionChanged: (Float, Float) -> Unit,
    onResize: (Float, Float, Float) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onToggleAllowed: () -> Unit
) {
    var rx by remember { mutableStateOf(zone.x) }
    var ry by remember { mutableStateOf(zone.y) }
    var w by remember { mutableStateOf(zone.width) }
    var h by remember { mutableStateOf(zone.height) }
    var r by remember { mutableStateOf(zone.radius) }
    var isSelected by remember { mutableStateOf(false) }

    LaunchedEffect(zone) {
        rx = zone.x
        ry = zone.y
        w = zone.width
        h = zone.height
        r = zone.radius
    }

    val activeColor = if (zone.isAllowed) Color(0xFF10B981) else Color(0xFFEF4444)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .clip(if (zone.isRect) RoundedCornerShape(12.dp) else CircleShape)
            .clickable(enabled = !isPlaying) { isSelected = !isSelected }
            .pointerInput(snapToGrid, isPlaying) {
                if (isPlaying) return@pointerInput
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val freshX = rx + dragAmount.x
                    val freshY = ry + dragAmount.y
                    val finalX = if (snapToGrid) (freshX / 50f).roundToInt() * 50f else freshX
                    val finalY = if (snapToGrid) (freshY / 50f).roundToInt() * 50f else freshY
                    rx = finalX
                    ry = finalY
                    onPositionChanged(finalX, finalY)
                }
            }
            .border(
                width = if (isSelected) 3.dp else 1.5.dp,
                color = if (isSelected) Color.Cyan else activeColor,
                shape = if (zone.isRect) RoundedCornerShape(12.dp) else CircleShape
            )
            .background(activeColor.copy(alpha = if (isPlaying) 0.08f else 0.15f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Z$index",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 12.sp
                )
                Text(
                    text = if (zone.isAllowed) "Allowed" else "Blocked",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isSelected && !isPlaying) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(20.dp)
                    .background(Color.Red, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(10.dp))
            }

            IconButton(
                onClick = onDuplicate,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .background(Color(0xFF10B981), CircleShape)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(10.dp))
            }

            IconButton(
                onClick = onToggleAllowed,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(20.dp)
                    .background(Color.DarkGray, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = "Toggle Allowed", tint = Color.White, modifier = Modifier.size(10.dp))
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .background(Color.Cyan, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            if (zone.isRect) {
                                val nw = (w + dragAmount.x).coerceIn(100f, 600f)
                                val nh = (h + dragAmount.y).coerceIn(100f, 600f)
                                w = nw
                                h = nh
                                onResize(nw, nh, r)
                            } else {
                                val nr = (r + dragAmount.x).coerceIn(40f, 300f)
                                r = nr
                                onResize(w, h, nr)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.AspectRatio, contentDescription = "Resize", tint = Color.Black, modifier = Modifier.size(10.dp))
            }
        }
    }
}

@Composable
fun DraggableAnchorWidget(
    label: String,
    x: Float,
    y: Float,
    snapToGrid: Boolean,
    isPlaying: Boolean,
    color: Color,
    onDrag: (Float, Float) -> Unit
) {
    var ox by remember { mutableStateOf(x) }
    var oy by remember { mutableStateOf(y) }

    LaunchedEffect(x, y) {
        ox = x
        oy = y
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(snapToGrid, isPlaying) {
                if (isPlaying) return@pointerInput
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val freshX = ox + dragAmount.x
                    val freshY = oy + dragAmount.y
                    val finalX = if (snapToGrid) (freshX / 50f).roundToInt() * 50f else freshX
                    val finalY = if (snapToGrid) (freshY / 50f).roundToInt() * 50f else freshY
                    ox = finalX
                    oy = finalY
                    onDrag(finalX, finalY)
                }
            }
            .padding(8.dp)
            .clip(CircleShape)
            .border(2.dp, Color.White, CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Black,
            color = Color.White,
            fontSize = 15.sp
        )
    }
}

private class MyServiceLifecycleOwner : androidx.lifecycle.LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    fun performRestore(savedState: android.os.Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }
}

@Composable
fun TapVisualizerOverlay(viewModel: MainViewModel) {
    val events by viewModel.visualTapEvents.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        events.forEach { event ->
            key(event.id) {
                TapRippleEffect(x = event.x, y = event.y)
            }
        }

        if (viewModel.showDiagnostics) {
            val attempted by viewModel.diagnosticAttemptedCount.collectAsState()
            val dispatched by viewModel.diagnosticDispatchedCount.collectAsState()
            val dropped by viewModel.diagnosticDroppedCount.collectAsState()
            val latency by viewModel.diagnosticLatencyMs.collectAsState()
            val lastStatus by viewModel.diagnosticLastStatus.collectAsState()
            val isThrottled by viewModel.diagnosticIsThrottled.collectAsState()

            val serviceActive = com.novaboost.novatap.accessibility.NovaTapAccessibilityService.instance != null
            val successRate = if (attempted > 0) (dispatched.toFloat() / attempted * 100).toInt() else 100

            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .width(300.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF20F172A)), // Deep premium slate-900 (95% opaque)
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0x2694A3B8)) // 15% opacity slate border
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Diagnostics Icon",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TAP DIAGNOSTICS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        
                        // Live pulsing indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // OS Service Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Accessibility Service:",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (serviceActive) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (serviceActive) "ACTIVE" else "INACTIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (serviceActive) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Attempted / Dispatched / Dropped
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "ATTEMPTED", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                            Text(text = "$attempted", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column {
                            Text(text = "DISPATCHED", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                            Text(text = "$dispatched", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        Column {
                            Text(text = "DROPPED", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "$dropped",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dropped > 0) Color(0xFFEF4444) else Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Success Rate Progress Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Delivery Success Rate:", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                        Text(
                            text = "$successRate%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                successRate == 100 -> Color(0xFF10B981)
                                successRate >= 90 -> Color(0xFFFBBF24)
                                else -> Color(0xFFEF4444)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Custom progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color(0xFF334155), RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(successRate / 100f)
                                .fillMaxHeight()
                                .background(
                                    color = when {
                                        successRate == 100 -> Color(0xFF10B981)
                                        successRate >= 90 -> Color(0xFFFBBF24)
                                        else -> Color(0xFFEF4444)
                                    },
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Latency / Response Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Last Dispatch Time:", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                        Text(
                            text = "${latency}ms",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (latency > 150) Color(0xFFFBBF24) else Color(0xFF34D399)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Last Event Log Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Last Event:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = lastStatus,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0),
                                maxLines = 1
                            )
                        }
                    }

                    // Throttling warning/tip
                    if (isThrottled || dropped > 0 || !serviceActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x26EF4444), RoundedCornerShape(8.dp)) // 15% opacity red background
                                .border(1.dp, Color(0x4DEF4444), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = if (!serviceActive) {
                                    "⚠️ ACCESSIBILITY INACTIVE: Enable the NovaTap service in system settings or click actions will only be simulated."
                                } else {
                                    "⚠️ SYSTEM LAG WARNING: OS is slow delivering gestures. Inputs are being queued or dropped."
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFCA5A5),
                                lineHeight = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "💡 Tip: Increase Interval / Hold Ms delays, or close heavy background apps to lower CPU load.",
                                fontSize = 9.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TapRippleEffect(x: Float, y: Float) {
    val scaleAnim = remember { androidx.compose.animation.core.Animatable(0.15f) }
    val alphaAnim = remember { androidx.compose.animation.core.Animatable(0.9f) }

    LaunchedEffect(Unit) {
        launch {
            scaleAnim.animateTo(
                targetValue = 1.0f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 350,
                    easing = androidx.compose.animation.core.LinearOutSlowInEasing
                )
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 0.0f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 350,
                    easing = androidx.compose.animation.core.LinearEasing
                )
            )
        }
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val xDp = with(density) { x.toDp() }
    val yDp = with(density) { y.toDp() }

    Box(
        modifier = Modifier
            .offset(x = xDp - 24.dp, y = yDp - 24.dp)
            .size(48.dp)
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                alpha = alphaAnim.value
            }
            .background(
                color = Color(0xFF10B981).copy(alpha = 0.35f),
                shape = CircleShape
            )
            .border(
                width = 2.5.dp,
                color = Color(0xFF10B981),
                shape = CircleShape
            )
    )
}
