package com.northline.browser

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class CustomizeActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("northline_browser", Context.MODE_PRIVATE) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Customize Start Page"
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20),dp(20),dp(20),dp(32)); setBackgroundColor(Color.rgb(12,12,14)) }
        scroll.addView(box)
        fun label(t:String)=TextView(this).apply { text=t; textSize=13f; setTextColor(Color.LTGRAY); letterSpacing=.10f }
        fun title(t:String)=TextView(this).apply { text=t; textSize=28f; setTextColor(Color.WHITE); setPadding(0,dp(8),0,dp(8)) }
        box.addView(label("MAKE NORTHLINE YOURS")); box.addView(title("Customize Start Page"))
        box.addView(TextView(this).apply { text="Personalize your greeting, layout, colors and shortcuts."; textSize=15f; setTextColor(Color.LTGRAY) })
        val greeting=EditText(this).apply { hint="Greeting text"; setText(prefs.getString("home_greeting","Welcome back!")); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
        box.addView(greeting,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(20)})
        val recent=Switch(this).apply { text="Show recently visited"; setTextColor(Color.WHITE); isChecked=prefs.getBoolean("show_recent",true) }
        val quick=Switch(this).apply { text="Show quick access"; setTextColor(Color.WHITE); isChecked=prefs.getBoolean("show_quick",true) }
        box.addView(recent); box.addView(quick)
        val layoutBtn=Button(this).apply { text=if(prefs.getInt("quick_layout",0)==0) "Quick access: 2 columns" else "Quick access: compact" }
        layoutBtn.setOnClickListener { AlertDialog.Builder(this@CustomizeActivity).setTitle("Quick access layout").setSingleChoiceItems(arrayOf("2 columns","4 compact shortcuts"),prefs.getInt("quick_layout",0)){d,w->prefs.edit().putInt("quick_layout",w).apply();layoutBtn.text=if(w==0)"Quick access: 2 columns" else "Quick access: compact";d.dismiss()}.show() }
        box.addView(layoutBtn)
        val themeBtn=Button(this).apply { text="Start page theme: ${prefs.getString("home_theme","Obsidian")}" }
        themeBtn.setOnClickListener { val themes=arrayOf("Obsidian","Midnight","Crimson"); AlertDialog.Builder(this@CustomizeActivity).setTitle("Start page theme").setSingleChoiceItems(themes,themes.indexOf(prefs.getString("home_theme","Obsidian"))){d,w->prefs.edit().putString("home_theme",themes[w]).apply();themeBtn.text="Start page theme: ${themes[w]}";d.dismiss()}.show() }
        box.addView(themeBtn)
        val add=Button(this).apply{text="＋ Add custom shortcut"}
        add.setOnClickListener { addShortcut() }; box.addView(add)
        val clear=Button(this).apply{text="Clear custom shortcuts"}; clear.setOnClickListener{prefs.edit().remove("custom_shortcuts").apply();Toast.makeText(this@CustomizeActivity,"Custom shortcuts cleared",Toast.LENGTH_SHORT).show()}; box.addView(clear)
        val save=Button(this).apply{text="Save changes";gravity=Gravity.CENTER}
        save.setOnClickListener{prefs.edit().putString("home_greeting",greeting.text.toString().ifBlank{"Welcome back!"}).putBoolean("show_recent",recent.isChecked).putBoolean("show_quick",quick.isChecked).apply();Toast.makeText(this@CustomizeActivity,"Start page updated",Toast.LENGTH_SHORT).show();finish()}
        box.addView(save,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(16)})
        setContentView(scroll)
    }
    private fun addShortcut(){ val wrap=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),0,dp(20),0)}; val name=EditText(this).apply{hint="Shortcut name"}; val url=EditText(this).apply{hint="https://example.com"};wrap.addView(name);wrap.addView(url); AlertDialog.Builder(this).setTitle("Add shortcut").setView(wrap).setNegativeButton("Cancel",null).setPositiveButton("Add"){_,_->val n=name.text.toString().trim();var u=url.text.toString().trim();if(n.isNotBlank()&&u.isNotBlank()){if(!u.startsWith("http"))u="https://$u";val old=prefs.getString("custom_shortcuts","")?:"";prefs.edit().putString("custom_shortcuts",old+"\n"+n.replace("|"," ")+"|"+u.replace("|"," ")).apply();Toast.makeText(this,"Shortcut added",Toast.LENGTH_SHORT).show()}}.show() }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
