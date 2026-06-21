package com.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var prefs: PrefsManager
    private val rootAvailableState = mutableStateOf(false)
    private val accessibilityEnabledState = mutableStateOf(false)
    private val useRootState = mutableStateOf(false)
    private val rootMethodState = mutableStateOf("keyevent")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = PrefsManager(this)

        // Load local preferences
        useRootState.value = prefs.useRoot
        rootMethodState.value = prefs.rootMethod

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFFF9F9FB) // Off-white clean bg
                ) { innerPadding ->
                    DashboardScreen(
                        modifier = Modifier.padding(innerPadding),
                        rootAvailable = rootAvailableState.value,
                        accessibilityEnabled = accessibilityEnabledState.value,
                        useRoot = useRootState.value,
                        rootMethod = rootMethodState.value,
                        onUseRootChange = { enabled ->
                            useRootState.value = enabled
                            prefs.useRoot = enabled
                        },
                        onRootMethodChange = { method ->
                            rootMethodState.value = method
                            prefs.rootMethod = method
                        },
                        onCheckRoot = { checkRootStatus() },
                        onOpenAccessibilitySettings = { openAccessibilitySettings() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkRootStatus()
        checkAccessibilityStatus()
    }

    private fun checkRootStatus() {
        // Run in thread to not block main thread
        Thread {
            val available = ShellUtils.isRootAvailable()
            Handler(Looper.getMainLooper()).post {
                rootAvailableState.value = available
            }
        }.start()
    }

    private fun checkAccessibilityStatus() {
        accessibilityEnabledState.value = isAccessibilityServiceEnabled(this, ScreenshotAccessibilityService::class.java)
    }

    private fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        val expectedComponentName = ComponentName(context, serviceClass)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledServiceComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledServiceComponent != null && enabledServiceComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Find 'Quick Tiles' under Installed Services and turn it ON", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open accessibility settings directly.", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    rootAvailable: Boolean,
    accessibilityEnabled: Boolean,
    useRoot: Boolean,
    rootMethod: String,
    onUseRootChange: (Boolean) -> Unit,
    onRootMethodChange: (String) -> Unit,
    onCheckRoot: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // TOP LOGO/BANNER HEADER (Modern Light Theme with Teal/Blue gradient accent)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFE0F2F1), Color(0xFFE8F5E9))
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tile_screenshot),
                        contentDescription = "App Icon",
                        tint = Color(0xFF00796B),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Quick Tiles",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF004D40)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "ScreenShot And Volume Panel Quick Settings",
                    fontSize = 14.sp,
                    color = Color(0xFF00796B),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // BLAZEFTL DESIGNER BADGE IN BOLD - Exactly as Requested
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF00796B),
                    modifier = Modifier.clip(CircleShape)
                ) {
                    Text(
                        text = "By BlazeFTL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // SERVICE STATUS SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "System Integrations",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F)
                )

                // 1. Root Status Indicator
                StatusRow(
                    label = "Root Access Status",
                    statusText = if (rootAvailable) "GRANTED / DETECTED" else "NOT DETECTED",
                    statusColor = if (rootAvailable) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                    icon = Icons.Default.CheckCircle,
                    onActionClick = onCheckRoot,
                    actionText = "Refresh"
                )

                Divider(color = Color(0xFFF1F1F4), thickness = 1.dp)

                // 2. Accessibility Status Indicator
                StatusRow(
                    label = "Accessibility Helper",
                    statusText = if (accessibilityEnabled) "ENABLED & ACTIVE" else "DISABLED (TAP TO FIX)",
                    statusColor = if (accessibilityEnabled) Color(0xFF2E7D32) else Color(0xFFE65100),
                    icon = if (accessibilityEnabled) Icons.Default.CheckCircle else Icons.Default.Warning,
                    onActionClick = onOpenAccessibilitySettings,
                    actionText = "Configure"
                )
            }
        }

        // SETTINGS CONTROL SECTION (Root Mode Toggle)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Quick Settings Configuration",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Prefer Root Capture",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1B1F)
                        )
                        Text(
                            text = "If enabled, uses root execution. Does not require the helper service to be active in background.",
                            fontSize = 12.sp,
                            color = Color(0xFF757575),
                            lineHeight = 16.sp
                        )
                    }
                    Switch(
                        checked = useRoot,
                        onCheckedChange = { onUseRootChange(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00796B)
                        )
                    )
                }

                AnimatedVisibility(
                    visible = useRoot,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Root Screenshot Capture Method:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRootMethodChange("keyevent") }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = rootMethod == "keyevent",
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00796B))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Simulated Power + Vol Down (KeyEvent 120)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1C1B1F)
                                )
                                Text(
                                    text = "Recommended. Fully triggers native system screenshot UI and effects.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRootMethodChange("screencap") }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = rootMethod == "screencap",
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00796B))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Direct Screencap Binary (CLI)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1C1B1F)
                                )
                                Text(
                                    text = "Captures the frame buffer directly and writes it to custom gallery storage.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                        }
                    }
                }
            }
        }

        // TILES QUICK TEST/PLAYGROUND
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Interactive Local Diagnostic Tests",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val coroutineScope = rememberCoroutineScope()
                    Button(
                        onClick = {
                            if (useRoot) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val success = if (rootMethod == "keyevent") {
                                            ShellUtils.runRootCommand("input keyevent 120")
                                        } else {
                                            val dirPath = "/sdcard/Pictures/Screenshots"
                                            ShellUtils.runRootCommand("mkdir -p $dirPath")
                                            val path = "$dirPath/Screenshot_${System.currentTimeMillis()}.png"
                                            ShellUtils.runRootCommand("screencap -p $path")
                                        }
                                        withContext(Dispatchers.Main) {
                                            if (success) {
                                                Toast.makeText(context, "Root snapshot triggered!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Root snapshot failed! Ensure root is granted.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } catch (t: Throwable) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } else {
                                if (ScreenshotAccessibilityService.isEnabled()) {
                                    ScreenshotAccessibilityService.takeScreenshot()
                                } else {
                                    Toast.makeText(context, "Please enable the Help service first!", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tile_screenshot),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Snapshot", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            audioManager.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004D40)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tile_volume),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Volume UI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // QUICK TILES INSTRUCTIONS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "How to add Quick Tiles",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F)
                )

                InstructionStep(
                    stepNum = "1",
                    text = "Pull down your notification panel twice to open the full Quick Settings."
                )
                InstructionStep(
                    stepNum = "2",
                    text = "Tap the Edit button (usually a pencil icon or three dots dynamic menu)."
                )
                InstructionStep(
                    stepNum = "3",
                    text = "Scroll down to discover 'Screenshot' and 'Volume' tiles available."
                )
                InstructionStep(
                    stepNum = "4",
                    text = "Drag-and-drop both tiles up into your prominent active layouts."
                )
            }
        }
    }
}

@Composable
fun StatusRow(
    label: String,
    statusText: String,
    statusColor: Color,
    icon: ImageVector,
    onActionClick: () -> Unit,
    actionText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.0f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF757575)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
            }
        }

        OutlinedButton(
            onClick = onActionClick,
            border = BorderStroke(1.dp, Color(0xFF00796B)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00796B)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            shape = RoundedCornerShape(30)
        ) {
            Text(actionText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InstructionStep(stepNum: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0F2F1)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNum,
                color = Color(0xFF00796B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFF424242),
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDashboard() {
    MyApplicationTheme {
        DashboardScreen(
            rootAvailable = true,
            accessibilityEnabled = false,
            useRoot = true,
            rootMethod = "keyevent",
            onUseRootChange = {},
            onRootMethodChange = {},
            onCheckRoot = {},
            onOpenAccessibilitySettings = {}
        )
    }
}
