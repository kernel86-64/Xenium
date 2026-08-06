package com.kernel64.xenium.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kernel64.xenium.R
import com.kernel64.xenium.util.SearchEngine
import com.kernel64.xenium.util.SettingsHelper
import com.kernel64.xenium.util.SystemInfoHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentSearchEngine by remember { mutableStateOf(SettingsHelper.getSearchEngine(context)) }
    var showSearchEngineDialog by remember { mutableStateOf(false) }

    var showUaDialog by remember { mutableStateOf(false) }
    var isUaSpoofingEnabled by remember { mutableStateOf(SettingsHelper.isCustomUaEnabled(context)) }
    var uaType by remember { mutableStateOf(SettingsHelper.getUaType(context)) }
    var customUaValue by remember { mutableStateOf(SettingsHelper.getCustomUaValue(context)) }
    var isSpoofVisibilityEnabled by remember { mutableStateOf(SettingsHelper.isSpoofVisibilityEnabled(context)) }

    val appVersion = remember { 
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    val osVersion by remember { mutableStateOf(SystemInfoHelper.getOSVersion()) }
    val webViewVersion by remember { mutableStateOf(SystemInfoHelper.getWebViewInfo(context)) }
    
    var gpuModel by remember { mutableStateOf("Loading...") }
    LaunchedEffect(Unit) {
        gpuModel = SystemInfoHelper.getGPUModel()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            // General Settings Section Header
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // General Settings Card Group
            SettingItem(
                title = stringResource(R.string.default_search_engine),
                subtitle = currentSearchEngine.title,
                position = ExpressiveItemPosition.TOP,
                onClick = { showSearchEngineDialog = true }
            )
            
            Spacer(modifier = Modifier.height(2.dp))

            SettingItem(
                title = stringResource(R.string.user_agent_spoofing),
                subtitle = stringResource(R.string.user_agent_desc),
                position = ExpressiveItemPosition.MIDDLE,
                onClick = { showUaDialog = true }
            )
            
            Spacer(modifier = Modifier.height(2.dp))

            SettingToggleItem(
                title = stringResource(R.string.spoof_visibility),
                subtitle = stringResource(R.string.spoof_visibility_desc),
                checked = isSpoofVisibilityEnabled,
                onCheckedChange = { 
                    isSpoofVisibilityEnabled = it
                    SettingsHelper.setSpoofVisibilityEnabled(context, it)
                },
                position = ExpressiveItemPosition.BOTTOM
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // About Section Header
            Text(
                text = stringResource(R.string.about_xenium),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            
            // About Items Card Group
            InfoItem(
                title = stringResource(R.string.app_version),
                value = appVersion,
                position = ExpressiveItemPosition.TOP
            )
            Spacer(modifier = Modifier.height(2.dp))
            InfoItem(
                title = stringResource(R.string.os_version),
                value = osVersion,
                position = ExpressiveItemPosition.MIDDLE
            )
            Spacer(modifier = Modifier.height(2.dp))
            InfoItem(
                title = stringResource(R.string.webview_version),
                value = webViewVersion,
                position = ExpressiveItemPosition.MIDDLE
            )
            Spacer(modifier = Modifier.height(2.dp))
            InfoItem(
                title = stringResource(R.string.gpu_model),
                value = gpuModel,
                position = ExpressiveItemPosition.BOTTOM
            )
        }
    }

    if (showSearchEngineDialog) {
        val engines = SearchEngine.values().toList()
        AlertDialog(
            onDismissRequest = { showSearchEngineDialog = false },
            title = { Text(stringResource(R.string.default_search_engine), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    engines.forEachIndexed { index, engine ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    SettingsHelper.setSearchEngine(context, engine)
                                    currentSearchEngine = engine
                                    showSearchEngineDialog = false
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = currentSearchEngine == engine,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = engine.title,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        if (index < engines.size - 1) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchEngineDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showUaDialog) {
        AlertDialog(
            onDismissRequest = { showUaDialog = false },
            title = { Text(stringResource(R.string.user_agent_spoofing), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isUaSpoofingEnabled = !isUaSpoofingEnabled
                                SettingsHelper.setCustomUaEnabled(context, isUaSpoofingEnabled)
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            stringResource(R.string.enable),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isUaSpoofingEnabled,
                            onCheckedChange = { 
                                isUaSpoofingEnabled = it
                                SettingsHelper.setCustomUaEnabled(context, it)
                            }
                        )
                    }
                    
                    if (isUaSpoofingEnabled) {
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        
                        // Predefined Option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    uaType = com.kernel64.xenium.util.UaType.PREDEFINED
                                    SettingsHelper.setUaType(context, uaType)
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = uaType == com.kernel64.xenium.util.UaType.PREDEFINED,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.predefined),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Custom Option
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    uaType = com.kernel64.xenium.util.UaType.CUSTOM
                                    SettingsHelper.setUaType(context, uaType)
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uaType == com.kernel64.xenium.util.UaType.CUSTOM,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    stringResource(R.string.custom),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            if (uaType == com.kernel64.xenium.util.UaType.CUSTOM) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = customUaValue,
                                    onValueChange = { 
                                        customUaValue = it
                                        SettingsHelper.setCustomUaValue(context, it)
                                    },
                                    label = { Text(stringResource(R.string.enter_custom_ua)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            },
            confirmButton = {
                TextButton(onClick = { showUaDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun SettingItem(
    title: String,
    subtitle: String,
    position: ExpressiveItemPosition,
    onClick: () -> Unit
) {
    Surface(
        shape = getItemShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoItem(
    title: String,
    value: String,
    position: ExpressiveItemPosition
) {
    Surface(
        shape = getItemShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    position: ExpressiveItemPosition
) {
    Surface(
        shape = getItemShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.material3.Switch(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
