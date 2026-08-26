package com.northline.browser

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : AppCompatActivity() {
    private val tabCenterRequest = 410
    private val settingsRequest = 411
    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var progress: ProgressBar
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var homePanel: View
    private lateinit var recentContainer: LinearLayout
    private lateinit var bookmarkButton: ImageButton
    private lateinit var pageTitle: TextView
    private lateinit var tabBadge: TextView
    private lateinit var tabContainer: FrameLayout
    private lateinit var greetingText: TextView
    private lateinit var homeHeadline: TextView
    private lateinit var quickGrid: GridLayout

    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private var currentHome = true
    private var privateSession = false
    private var desktopMode = false
    private var searchEngine = "Google"
    private var restoreSession = true
    private val tabs = mutableListOf<TabState>()
    private var activeTab = 0
    private val nextTabId = AtomicInteger(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<android.view.View>(R.id.customizeHomeButton).setOnClickListener { showCustomize() }
        bindViews()
        createInitialTab()
        setupControls()
        setupHomeCards()
        loadPreferences()
        applyHomePreferences()
        showHome()
    }

    private fun bindViews() {
        address = findViewById(R.id.address)
        progress = findViewById(R.id.progress)
        swipe = findViewById(R.id.swipeRefresh)
        homePanel = findViewById(R.id.homePanel)
        recentContainer = findViewById(R.id.recentContainer)
        bookmarkButton = findViewById(R.id.bookmarkButton)
        pageTitle = findViewById(R.id.pageTitle)
        tabBadge = findViewById(R.id.tabBadge)
        tabContainer = findViewById(R.id.tabContainer)
        greetingText = findViewById(R.id.greetingText)
        homeHeadline = findViewById(R.id.homeHeadline)
        quickGrid = findViewById(R.id.quickGrid)
    }

    private fun createInitialTab() { addTab(false) }

    private fun addTab(openHome: Boolean = true) {
        val view = WebView(this)
        tabContainer.addView(view, FrameLayout.LayoutParams(-1, -1))
        setupWebView(view)
        tabs.add(TabState(nextTabId.getAndIncrement(), view))
        switchTab(tabs.lastIndex, openHome)
    }

    private fun setupWebView(view: WebView) {
        with(view.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            useWideViewPort = true
            loadWithOverviewMode = false
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = true
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            blockNetworkImage = false
            safeBrowsingEnabled = true
            userAgentString = if (desktopMode) desktopUserAgent(userAgentString) else userAgentString
        }
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean {
                val u = request.url.toString()
                if (u.startsWith("http://") || u.startsWith("https://")) { v.loadUrl(u); return true }
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) } catch (_: Exception) { }
                return true
            }
            override fun onPageStarted(v: WebView, url: String, favicon: Bitmap?) {
                if (v == web) { currentHome = false; homePanel.visibility = View.GONE; swipe.visibility = View.VISIBLE; address.setText(url); updateBookmarkIcon(url) }
            }
            override fun onPageFinished(v: WebView, url: String) {
                val tab = tabs.firstOrNull { it.webView == v } ?: return
                tab.url = url; tab.title = v.title?.takeIf { it.isNotBlank() } ?: hostName(url)
                if (v == web) {
                    currentHome = false; address.setText(url); pageTitle.text = tab.title; swipe.isRefreshing = false; updateBookmarkIcon(url)
                    if (!privateSession) saveHistory(url, tab.title)
                }
            }
        }
        view.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(v: WebView, p: Int) { if (v == web) { progress.visibility = if (p in 1..99) View.VISIBLE else View.GONE; progress.progress = p } }
            override fun onReceivedTitle(v: WebView, title: String?) { tabs.firstOrNull { it.webView == v }?.title = title?.takeIf { it.isNotBlank() } ?: "New tab"; if (v == web && !currentHome) pageTitle.text = tabs[activeTab].title }
        }
        view.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mimeType)
                    addRequestHeader("User-Agent", userAgent)
                    CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
                    setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                    setDescription("Downloading with NORTHLINE Browser")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType))
                }
                (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this, "Unable to start download", Toast.LENGTH_SHORT).show() }
        })
    }

    private fun switchTab(index: Int, home: Boolean = false) {
        if (tabs.isEmpty()) return
        activeTab = index.coerceIn(0, tabs.lastIndex)
        web = tabs[activeTab].webView
        for ((i, tab) in tabs.withIndex()) tab.webView.visibility = if (i == activeTab) View.VISIBLE else View.GONE
        updateTabBadge()
        if (home) showHome() else {
            val tab = tabs[activeTab]
            if (tab.url.isBlank()) showHome() else openUrl(tab.url)
        }
    }

    private fun closeTab(index: Int) {
        if (tabs.size == 1) { showHome(); return }
        val removed = tabs.removeAt(index.coerceIn(0, tabs.lastIndex))
        tabContainer.removeView(removed.webView); removed.webView.destroy()
        activeTab = (activeTab.coerceAtMost(tabs.lastIndex))
        switchTab(activeTab)
    }

    private fun setupControls() {
        address.setOnEditorActionListener { _, _, _ -> navigate(); true }
        address.setOnKeyListener { _, keyCode, event -> if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) { navigate(); true } else false }
        findViewById<ImageButton>(R.id.back).setOnClickListener { if (!currentHome && web.canGoBack()) web.goBack() else showHome() }
        findViewById<ImageButton>(R.id.forward).setOnClickListener { if (!currentHome && web.canGoForward()) web.goForward() }
        findViewById<ImageButton>(R.id.home).setOnClickListener { showHome() }
        findViewById<ImageButton>(R.id.reload).setOnClickListener { if (currentHome) refreshRecent() else web.reload() }
        findViewById<View>(R.id.tabs).setOnClickListener { showTabs() }
        findViewById<ImageButton>(R.id.menuButton).setOnClickListener { showMenu() }
        bookmarkButton.setOnClickListener { toggleBookmark() }
        swipe.setOnRefreshListener { if (!currentHome) web.reload() else swipe.isRefreshing = false }
    }

    private fun setupHomeCards() {
        findViewById<View>(R.id.openNorthlineCard).setOnClickListener { openUrl(NORTHLINE_URL) }
        findViewById<View>(R.id.searchCard).setOnClickListener { focusSearch() }
        findViewById<View>(R.id.googleQuick).setOnClickListener { openUrl("https://www.google.com/") }
        findViewById<View>(R.id.youtubeQuick).setOnClickListener { openUrl("https://www.youtube.com/") }
        findViewById<View>(R.id.githubQuick).setOnClickListener { openUrl("https://github.com/") }
        findViewById<View>(R.id.northlineQuick).setOnClickListener { openUrl(NORTHLINE_URL) }
        findViewById<View>(R.id.addQuick).setOnClickListener { focusSearch() }
        findViewById<View>(R.id.historyLink).setOnClickListener { showHistory() }
    }

    private fun showHome() {
        applyHomePreferences()
        currentHome = true; web.stopLoading(); swipe.visibility = View.GONE; homePanel.visibility = View.VISIBLE; address.setText("")
        pageTitle.text = if (privateSession) "Private browsing" else "Your internet, your way"; updateBookmarkIcon(null); refreshRecent()
    }

    private fun openUrl(url: String) { currentHome = false; homePanel.visibility = View.GONE; swipe.visibility = View.VISIBLE; web.loadUrl(url) }
    private fun navigate() {
        val text = address.text.toString().trim(); if (text.isEmpty()) { showHome(); return }
        val url = when {
            text.startsWith("https://", true) || text.startsWith("http://", true) -> text
            text.contains(".") && !text.contains(" ") -> "https://$text"
            else -> searchUrl(text)
        }
        hideKeyboard(); openUrl(url)
    }
    private fun searchUrl(query: String): String {
        val q = URLEncoder.encode(query, "UTF-8")
        return when (searchEngine) {
            "Bing" -> "https://www.bing.com/search?q=$q"
            "DuckDuckGo" -> "https://duckduckgo.com/?q=$q"
            else -> "https://www.google.com/search?q=$q"
        }
    }

    private fun loadPreferences() {
        searchEngine = prefs.getString(KEY_SEARCH_ENGINE, "Google") ?: "Google"
        restoreSession = prefs.getBoolean(KEY_RESTORE_SESSION, true)
        desktopMode = prefs.getBoolean(KEY_DESKTOP_MODE, false)
        applyHomePreferences()
    }

    private fun applyHomePreferences() {
        greetingText.text = prefs.getString("home_greeting", "Welcome back!") ?: "Welcome back!"
        quickGrid.visibility = if (prefs.getBoolean("show_quick", true)) View.VISIBLE else View.GONE
        recentContainer.parent.let { }
        val showRecent = prefs.getBoolean("show_recent", true)
        recentContainer.visibility = if (showRecent) View.VISIBLE else View.GONE
        findViewById<View>(R.id.historyLink).visibility = if (showRecent) View.VISIBLE else View.GONE
        val theme = prefs.getString("home_theme", "Obsidian")
        val color = when(theme){"Midnight" -> android.graphics.Color.rgb(8,18,32); "Crimson" -> android.graphics.Color.rgb(28,8,12); else -> android.graphics.Color.rgb(15,15,17)}
        homePanel.setBackgroundColor(color)
        if (prefs.getInt("quick_layout",0)==1) { quickGrid.columnCount=4 } else quickGrid.columnCount=2
        addCustomShortcuts()
    }

    private fun addCustomShortcuts() {
        // Remove only previously injected custom views.
        for (i in quickGrid.childCount-1 downTo 0) if (quickGrid.getChildAt(i).tag == "custom") quickGrid.removeViewAt(i)
        val raw = prefs.getString("custom_shortcuts", "") ?: ""
        raw.lineSequence().filter { it.contains("|") }.take(8).forEach { line ->
            val parts=line.split("|",limit=2); if(parts.size==2){
                val v=TextView(this).apply { tag="custom"; text="＋   ${parts[0]}"; gravity=android.view.Gravity.CENTER; textSize=15f; setTextColor(getColor(R.color.north_primary)); setBackgroundResource(R.drawable.home_card); setPadding(dp(6),0,dp(6),0); setOnClickListener{openUrl(parts[1])} }
                quickGrid.addView(v, GridLayout.LayoutParams().apply { width=0; height=dp(60); columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); setMargins(dp(5),dp(5),dp(5),dp(5)) })
            }
        }
    }

    private fun focusSearch() { address.requestFocus(); address.setText(""); (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(address, InputMethodManager.SHOW_IMPLICIT) }
    private fun hideKeyboard() { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(address.windowToken, 0); address.clearFocus() }

    private fun saveHistory(url: String, title: String) { if (url.startsWith("about:") || url.startsWith("data:")) return; val old = historyEntries().filterNot { it.url == url }.toMutableList(); old.add(0, Entry(title, url)); writeEntries(KEY_HISTORY, old) }
    private fun historyEntries(): List<Entry> = readEntries(KEY_HISTORY)
    private fun bookmarks(): MutableList<Entry> = readEntries(KEY_BOOKMARKS).toMutableList()
    private fun readEntries(key: String): List<Entry> { val raw = prefs.getString(key, "") ?: ""; return raw.lineSequence().mapNotNull { line -> val p = line.split("\t", limit = 2); if (p.size == 2 && p[1].isNotBlank()) Entry(p[0], p[1]) else null }.toList() }
    private fun writeEntries(key: String, items: List<Entry>) { prefs.edit().putString(key, items.joinToString("\n") { clean(it.title) + "\t" + clean(it.url) }).apply() }
    private fun clean(v: String) = v.replace("\n", " ").replace("\r", " ").replace("\t", " ")

    private fun refreshRecent() { recentContainer.removeAllViews(); val items = historyEntries().take(5); if (items.isEmpty()) addRecent("Nothing here yet", "Your recently visited pages will appear here", null) else items.forEach { addRecent(it.title, it.url, it.url) } }
    private fun addRecent(title: String, subtitle: String, url: String?) { val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(14), dp(18), dp(14)); setBackgroundResource(R.drawable.home_card); val lp = LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(10); layoutParams = lp; isClickable = url != null }
        row.addView(TextView(this).apply { text = title.take(54); textSize = 16f; setTextColor(getColor(R.color.north_text)); maxLines = 1 }); row.addView(TextView(this).apply { text = subtitle; textSize = 12f; setTextColor(getColor(R.color.north_text_secondary)); maxLines = 1 }); if (url != null) row.setOnClickListener { openUrl(url) }; recentContainer.addView(row) }

    private fun toggleBookmark() { if (currentHome) return; val url = web.url ?: return; val set = bookmarks(); val i = set.indexOfFirst { it.url == url }; if (i >= 0) { set.removeAt(i); Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show() } else { set.add(0, Entry(web.title?.takeIf { it.isNotBlank() } ?: hostName(url), url)); Toast.makeText(this, "Bookmark saved", Toast.LENGTH_SHORT).show() }; writeEntries(KEY_BOOKMARKS, set); updateBookmarkIcon(url) }
    private fun updateBookmarkIcon(url: String?) { val saved = url != null && bookmarks().any { it.url == url }; bookmarkButton.setImageResource(if (saved) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off) }

    private fun showTabs() {
        startActivityForResult(Intent(this, TabCenterActivity::class.java).apply {
            putStringArrayListExtra("titles", ArrayList(tabs.map { it.title }))
            putStringArrayListExtra("urls", ArrayList(tabs.map { it.url }))
            putExtra("active", activeTab)
        }, tabCenterRequest)
    }
    private fun updateTabBadge() { tabBadge.text = tabs.size.toString() }

    private fun showHistory() { val items = historyEntries(); if (items.isEmpty()) { toast("No pages visited yet"); return }; val labels = items.map { "${it.title}\n${it.url}" }.toTypedArray(); AlertDialog.Builder(this).setTitle("History").setItems(labels) { _, w -> openUrl(items[w].url) }.setNeutralButton("Clear") { _, _ -> prefs.edit().remove(KEY_HISTORY).apply(); refreshRecent() }.setNegativeButton("Close", null).show() }
    private fun showBookmarks() { val items = bookmarks(); if (items.isEmpty()) { toast("No bookmarks yet"); return }; val labels = items.map { "${it.title}\n${it.url}" }.toTypedArray(); AlertDialog.Builder(this).setTitle("Bookmarks").setItems(labels) { _, w -> openUrl(items[w].url) }.setNeutralButton("Clear all") { _, _ -> prefs.edit().remove(KEY_BOOKMARKS).apply() }.setNegativeButton("Close", null).show() }

    private fun findInPage() {
        if (currentHome) { toast("Open a page first"); return }
        val input = EditText(this).apply { hint = "Find in page"; setSingleLine(true) }
        AlertDialog.Builder(this).setTitle("Find in page").setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Find") { _, _ -> web.findAllAsync(input.text.toString()) }.show()
    }

    private fun showSearchEnginePicker() {
        val engines = arrayOf("Google", "Bing", "DuckDuckGo")
        AlertDialog.Builder(this).setTitle("Search engine").setSingleChoiceItems(engines, engines.indexOf(searchEngine)) { d, which ->
            searchEngine = engines[which]
            prefs.edit().putString(KEY_SEARCH_ENGINE, searchEngine).apply()
            d.dismiss(); toast("Search engine: $searchEngine")
        }.show()
    }

    private fun showMenu() {
        val privateLabel = if (privateSession) "Exit private session" else "New private session"
        val desktopLabel = if (desktopMode) "Desktop site: ON" else "Desktop site: OFF"
        val options = arrayOf("New tab", privateLabel, "Bookmarks", "History", "Find in page", desktopLabel, "Share page", "Downloads", "Customize start page", "Settings", "Clear browsing data")
        AlertDialog.Builder(this).setTitle("NORTHLINE BROWSER").setItems(options) { _, w -> when (w) { 0 -> addTab(true); 1 -> togglePrivateSession(); 2 -> showBookmarks(); 3 -> showHistory(); 4 -> findInPage(); 5 -> toggleDesktopMode(); 6 -> sharePage(); 7 -> openDownloads(); 8 -> showCustomize(); 9 -> showSettings(); 10 -> clearBrowsingData() } }.show()
    }
    private fun showCustomize() { startActivity(Intent(this, CustomizeActivity::class.java)) }
    private fun showSettings() {
        startActivityForResult(Intent(this, SettingsActivity::class.java), settingsRequest)
    }
    private fun togglePrivateSession() { privateSession = !privateSession; if (privateSession) { CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush() }; toast(if (privateSession) "Private browsing enabled" else "Private browsing disabled"); showHome() }
    private fun toggleDesktopMode() { desktopMode = !desktopMode; prefs.edit().putBoolean(KEY_DESKTOP_MODE, desktopMode).apply(); tabs.forEach { t -> t.webView.settings.userAgentString = if (desktopMode) desktopUserAgent(t.webView.settings.userAgentString) else defaultUserAgent(t.webView.settings.userAgentString) }; toast(if (desktopMode) "Desktop mode enabled" else "Mobile mode enabled"); if (!currentHome) web.reload() }
    private fun desktopUserAgent(current: String) = current.replace(" Mobile", "")
    private fun defaultUserAgent(current: String) = if (current.contains(" Mobile")) current else "$current Mobile"
    private fun sharePage() { val url = web.url; if (currentHome || url.isNullOrBlank()) { toast("Open a page first"); return }; startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url) }, "Share page")) }
    private fun openDownloads() { try { startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)) } catch (_: Exception) { toast("Downloads are managed by Android") } }
    private fun clearBrowsingData() { AlertDialog.Builder(this).setTitle("Clear browsing data?").setMessage("This clears history, bookmarks, cache and cookies.").setNegativeButton("Cancel", null).setPositiveButton("Clear") { _, _ -> prefs.edit().clear().apply(); tabs.forEach { it.webView.clearHistory(); it.webView.clearCache(true) }; CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush(); refreshRecent(); toast("Browsing data cleared") }.show() }
    private fun hostName(url: String) = url.removePrefix("https://").removePrefix("http://").substringBefore('/').ifBlank { "Page" }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == tabCenterRequest) {
            when (resultCode) {
                RESULT_OK -> addTab(true)
                RESULT_FIRST_USER -> data?.getIntExtra("selected", activeTab)?.let { if (it in tabs.indices) switchTab(it) }
            }
        }
        if (requestCode == settingsRequest) loadPreferences()
    }
    override fun onBackPressed() { if (currentHome) { super.onBackPressed(); return }; if (web.canGoBack()) web.goBack() else showHome() }
    override fun onDestroy() { tabs.forEach { it.webView.stopLoading(); it.webView.destroy() }; super.onDestroy() }
    private data class Entry(val title: String, val url: String)
    private data class TabState(val id: Int, val webView: WebView, var title: String = "New tab", var url: String = "")
    companion object { private const val PREFS = "northline_browser"; private const val KEY_HISTORY = "history_v4"; private const val KEY_BOOKMARKS = "bookmarks_v4"; private const val KEY_SEARCH_ENGINE = "search_engine"; private const val KEY_RESTORE_SESSION = "restore_session"; private const val KEY_DESKTOP_MODE = "desktop_mode"; private const val NORTHLINE_URL = "https://northline.pages.dev/" }
}
