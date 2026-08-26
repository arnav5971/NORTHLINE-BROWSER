package com.northline.browser

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class TabCenterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20),dp(20),dp(20),dp(20)); setBackgroundColor(Color.rgb(12,12,14)) }
        val top = LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
        top.addView(TextView(this).apply { text="‹"; textSize=40f; setTextColor(Color.WHITE); setOnClickListener{finish()} }, LinearLayout.LayoutParams(dp(48),dp(56)))
        top.addView(TextView(this).apply { text="Tabs"; textSize=26f; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0,dp(56),1f))
        val add=Button(this).apply { text="＋ New tab"; setOnClickListener{ setResult(Activity.RESULT_OK); finish() } }
        top.addView(add)
        root.addView(top)
        val subtitle=TextView(this).apply { text="Your open pages"; textSize=14f; setTextColor(Color.LTGRAY); setPadding(dp(4),0,0,dp(16)) }; root.addView(subtitle)
        val scroll=ScrollView(this); val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}; scroll.addView(list); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val titles=intent.getStringArrayListExtra("titles")?: arrayListOf("New tab")
        val urls=intent.getStringArrayListExtra("urls")?: arrayListOf("")
        val active=intent.getIntExtra("active",0)
        titles.indices.forEach { i ->
            val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(18),dp(18),dp(18)); setBackgroundColor(if(i==active) Color.rgb(45,18,22) else Color.rgb(28,28,32)); setOnClickListener{ intent.putExtra("selected",i); setResult(Activity.RESULT_FIRST_USER,intent); finish() } }
            card.addView(TextView(this).apply { text=titles[i].ifBlank{"New tab"}; textSize=18f; setTextColor(Color.WHITE); maxLines=1 })
            card.addView(TextView(this).apply { text=urls.getOrElse(i){"Start page"}.ifBlank{"Start page"}; textSize=13f; setTextColor(Color.LTGRAY); maxLines=1 })
            list.addView(card,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)})
        }
        setContentView(root)
    }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
