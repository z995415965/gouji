// TutorialActivity.kt: Tutorial activity with usage instructions
package com.example.goujicardcounter.ui

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.goujicardcounter.R

class TutorialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)

        supportActionBar?.title = "使用教程"

        val webView: WebView = findViewById(R.id.webViewTutorial)
        webView.webClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        // Load tutorial HTML
        val tutorialHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: sans-serif; padding: 16px; line-height: 1.6; }
                    h1 { color: #1976D2; }
                    h2 { color: #424242; margin-top: 24px; }
                    .step { background: #F5F5F5; padding: 12px; border-radius: 8px; margin: 12px 0; }
                    .warning { background: #FFF3E0; padding: 12px; border-radius: 8px; border-left: 4px solid #FF9800; }
                </style>
            </head>
            <body>
                <h1>🃏 够级记牌器使用教程</h1>
                
                <h2>📱 首次使用设置</h2>
                <div class="step">
                    <strong>1. 开启悬浮窗权限</strong><br>
                    点击首页"设置"按钮，按照提示开启悬浮窗权限。这是记牌器显示的必要条件。
                </div>
                
                <div class="step">
                    <strong>2. 开启通知权限</strong><br>
                    允许APP发送通知，以便在后台运行时显示服务状态。
                </div>
                
                <div class="step">
                    <strong>3. 关闭电池优化（推荐）</strong><br>
                    在设置中关闭电池优化，确保记牌器能在后台稳定运行不被系统杀死。
                </div>

                <h2>🎮 开始记牌</h2>
                <div class="step">
                    <strong>1. 选择牌局类型</strong><br>
                    在主界面选择5副牌或6副牌模式。
                </div>
                
                <div class="step">
                    <strong>2. 点击"开始记牌"</strong><br>
                    系统会请求录屏权限，请允许。录屏用于捕获游戏画面。
                </div>
                
                <div class="step">
                    <strong>3. 将游戏画面置于前台</strong><br>
                    切换到够级游戏APP，让游戏画面完全显示在屏幕上。
                </div>
                
                <div class="step">
                    <strong>4. 观察悬浮球和记牌面板</strong><br>
                    屏幕会出现一个可拖动的悬浮球，点击可展开记牌面板查看剩余牌数。
                </div>

                <h2>⚙️ 高级设置</h2>
                <div class="step">
                    <strong>OCR识别阈值</strong><br>
                    默认0.75，如果识别不准确可适当降低。太低会增加误识别。
                </div>
                
                <div class="step">
                    <strong>识别间隔</strong><br>
                    默认2秒，可根据手机性能调整。性能好的手机可以设更短。
                </div>

                <h2>🔄 重置牌局</h2>
                <div class="warning">
                    <strong>注意：</strong>每局开始前请手动点击"重置"按钮，清空上一局的记录。
                </div>

                <h2>💡 常见问题</h2>
                <div class="step">
                    <strong>Q: 识别不准确怎么办？</strong><br>
                    A: 确保游戏画面清晰，光线充足。可以尝试调整OCR识别阈值。
                </div>
                
                <div class="step">
                    <strong>Q: 后台运行被杀死了？</strong><br>
                    A: 请在手机设置中将本APP加入白名单，关闭电池优化。
                </div>
                
                <div class="step">
                    <strong>Q: 悬浮窗不见了？</strong><br>
                    A: 检查悬浮窗权限是否被撤销，在设置中重新授权。
                </div>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, tutorialHtml, "text/html", "UTF-8", null)
    }
}
