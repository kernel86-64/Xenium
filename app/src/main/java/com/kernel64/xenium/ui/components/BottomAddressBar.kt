package com.kernel64.xenium.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kernel64.xenium.R
import com.kernel64.xenium.model.WebTab

@Composable
fun BottomAddressBar(
    tab: WebTab?,
    onNavigateToUrl: (String) -> Unit,
    onBackClicked: () -> Unit,
    onForwardClicked: () -> Unit,
    onRefreshClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onHistoryClicked: () -> Unit,
    onDesktopSiteToggled: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(tab?.url ?: "", selection = TextRange.Zero)) }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Synchronize current active tab URL into address bar text when not editing
    LaunchedEffect(tab?.url, isFocused) {
        if (!isFocused) {
            val url = tab?.url ?: ""
            textFieldValue = TextFieldValue(text = url, selection = TextRange.Zero)
        }
    }

    // Clear focus and text selection after page finishes loading
    LaunchedEffect(tab?.isLoading) {
        if (tab?.isLoading == false) {
            focusManager.clearFocus()
            val url = tab.url ?: ""
            textFieldValue = TextFieldValue(text = url, selection = TextRange.Zero)
        }
    }

    // Select all text in address bar when field gains focus (e.g. user taps on it)
    LaunchedEffect(isFocused) {
        if (isFocused) {
            val text = textFieldValue.text
            if (text.isNotEmpty()) {
                textFieldValue = textFieldValue.copy(
                    selection = TextRange(0, text.length)
                )
            }
        }
    }

    val currentUrl = tab?.url ?: ""
    val isSecure = currentUrl.startsWith("https://", ignoreCase = true) || currentUrl.startsWith("file://", ignoreCase = true)
    val isHttp = currentUrl.startsWith("http://", ignoreCase = true)
    val isLoading = tab?.isLoading == true
    val canBack = tab?.canGoBack == true
    val canForward = tab?.canGoForward == true

    val customTextSelectionColors = TextSelectionColors(
        handleColor = Color.White,
        backgroundColor = Color.White.copy(alpha = 0.35f)
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Loading Progress Line
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { (tab?.progress ?: 0) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            } else {
                Spacer(modifier = Modifier.height(3.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Back Button
                IconButton(
                    onClick = onBackClicked,
                    enabled = canBack,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = if (canBack) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                // Forward Button
                IconButton(
                    onClick = onForwardClicked,
                    enabled = canForward,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.forward),
                        tint = if (canForward) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                // Main Address Input Pill Box (Material 3 Expressive Pill shape with precise text sizing)
                CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    val text = textFieldValue.text
                                    textFieldValue = textFieldValue.copy(
                                        selection = TextRange(0, text.length)
                                    )
                                } else {
                                    val text = textFieldValue.text
                                    textFieldValue = textFieldValue.copy(
                                        selection = TextRange.Zero
                                    )
                                }
                            },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                textFieldValue = textFieldValue.copy(selection = TextRange.Zero)
                                onNavigateToUrl(textFieldValue.text)
                            }
                        ),
                        decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(21.dp))
                                .background(
                                    if (isFocused) MaterialTheme.colorScheme.surfaceContainerLowest
                                    else MaterialTheme.colorScheme.surfaceContainer
                                )
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isFocused) {
                                if (isSecure) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = stringResource(R.string.security_secure),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else if (isHttp) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = stringResource(R.string.security_not_secure),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(R.string.search_or_type_url),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search_or_type_url),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (textFieldValue.text.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.search_or_type_url),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                innerTextField()
                            }

                            if (textFieldValue.text.isNotEmpty() && isFocused) {
                                IconButton(
                                    onClick = { textFieldValue = TextFieldValue("", selection = TextRange.Zero) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.clear),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (isFocused) {
                                IconButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        textFieldValue = textFieldValue.copy(selection = TextRange.Zero)
                                        onNavigateToUrl(textFieldValue.text)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = stringResource(R.string.go),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }

                // Menu Button
                Box {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { 
                            focusManager.clearFocus()
                            menuExpanded = true 
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.menu),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(modifier = Modifier.width(220.dp)) {
                            // Top item: Refresh / Stop
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.refresh),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isLoading) Icons.Default.Stop else Icons.Default.Refresh,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    if (isLoading) onStopClicked() else onRefreshClicked()
                                    menuExpanded = false
                                }
                            )

                            Divider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            // Middle item: Desktop site
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.desktop_site),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                trailingIcon = {
                                    Checkbox(
                                        checked = tab?.isDesktopMode == true,
                                        onCheckedChange = null
                                    )
                                },
                                onClick = {
                                    onDesktopSiteToggled()
                                    menuExpanded = false
                                }
                            )

                            Divider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            // History item
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.history),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    onHistoryClicked()
                                    menuExpanded = false
                                }
                            )

                            Divider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            // Bottom item: Settings
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.settings),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    onSettingsClicked()
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
