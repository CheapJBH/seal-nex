package com.junkfood.seal.ui.page.settings.network

import android.webkit.WebView as AndroidWebView
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.CookieProfile
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.SealDialog
import com.junkfood.seal.util.COOKIES
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.FileUtil.getCookiesFile
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 内置浏览器页面：让用户登录视频平台（YouTube / B站 等），
 * 登录后点击"导出 Cookies"，将 WebView 中的 Cookies 导出为
 * yt-dlp 可用的 Netscape 格式，保存为 [CookieProfile]，供下载时启用 Cookies 选项使用。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookieBrowserPage(
    onNavigateBack: () -> Unit = {},
    onCookiesExported: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var exportStatus by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }

    var nativeWebView by remember { mutableStateOf<AndroidWebView?>(null) }
    val webViewState = rememberWebViewState("https://www.youtube.com/")

    fun loadWebUrl(input: String) {
        if (input.isNotEmpty()) {
            if (!input.startsWith("http")) {
                nativeWebView?.loadUrl("https://$input")
            } else {
                nativeWebView?.loadUrl(input)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部导航栏
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { nativeWebView?.goBack() }) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = null,
                            enabled = nativeWebView?.canGoBack == true,
                        )
                    }
                    IconButton(onClick = { nativeWebView?.goForward() }) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowForward,
                            contentDescription = null,
                            enabled = nativeWebView?.canGoForward == true,
                        )
                    }
                    IconButton(onClick = { nativeWebView?.reload() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                        )
                    }
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        label = { Text(stringResource(R.string.video_url)) },
                    )
                    IconButton(onClick = { loadWebUrl(url) }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                        )
                    }
                }
            }
        }

        // WebView 内容区域
        WebView(
            state = webViewState,
            client = AccompanistWebViewClient(),
            chromeClient = object : AccompanistWebChromeClient() {},
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                AndroidWebView(context).apply {
                    nativeWebView = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                }
            },
        )

        // 底部状态栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // 左侧：状态文本
            Text(
                text = exportStatus.ifEmpty { stringResource(R.string.loading) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // 右侧：按钮
            Button(
                onClick = { showExportDialog = true },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Cookie,
                    contentDescription = null,
                )
                Text(text = stringResource(R.string.export_cookies))
            }
        }
    }

    // 导出 Cookies 确认对话框
    if (showExportDialog) {
        SealDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.export_cookies)) },
            icon = { Icon(Icons.Outlined.Cookie, null) },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { showExportDialog = false }) {
                    Text(text = stringResource(androidx.appcompat.R.string.abc_action_mode_done))
                }
            },
            confirmButton = {
                ConfirmButton(
                    text = stringResource(R.string.export),
                ) {
                    scope.launch(Dispatchers.IO) {
                        val cookiesContent =
                            DownloadUtil.getCookiesContentFromDatabase().getOrNull()
                        if (cookiesContent != null && cookiesContent.isNotEmpty()) {
                            // 写入 cookies 文件
                            FileUtil.writeContentToFile(cookiesContent, context.getCookiesFile())
                            // 启用 cookies 选项
                            COOKIES.updateBoolean(true)
                            // 保存为 CookieProfile
                            val profile = CookieProfile(
                                id = 0,
                                url = nativeWebView?.url ?: "https://",
                                content = cookiesContent,
                            )
                            DatabaseUtil.insertCookieProfile(profile)
                            exportStatus = "Cookies 已导出（${nativeWebView?.url}）"
                            showExportDialog = false
                            onCookiesExported()
                        } else {
                            exportStatus = "未找到 Cookies，请先在页面中登录"
                        }
                    }
                }
            },
            text = {
                Text(
                    text = stringResource(R.string.export_cookies_desc),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
    }
}
