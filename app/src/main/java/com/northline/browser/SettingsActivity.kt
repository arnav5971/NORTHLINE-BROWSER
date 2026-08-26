SettingsActivity.kt

package com.northline.browser

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val p = getSharedPreferences(
            "northline_browser",
            MODE_PRIVATE
        )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(Color.rgb(12, 12, 14))
        }

        root.addView(
            TextView(this).apply {
                text = "Settings"
                textSize = 28f
                setTextColor(Color.WHITE)
            }
        )

        root.addView(
            TextView(this).apply {
                text = "Control your NORTHLINE experience"
                textSize = 14f
                setTextColor(Color.LTGRAY)
                setPadding(0, 0, 0, dp(20))
            }
        )

        val engines = arrayOf(
            "Google",
            "Bing",
            "DuckDuckGo"
        )

        val engine = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@SettingsActivity,
                android.R.layout.simple_spinner_dropdown_item,
                engines
            )

            setSelection(
                engines.indexOf(
                    p.getString("search_engine", "Google")
                )
            )
        }

        addRow(root, "Search engine", engine)

        val desktop = Switch(this).apply {
            text = "Desktop mode"
            setTextColor(Color.WHITE)

            isChecked = p.getBoolean(
                "desktop_mode",
                false
            )

            setOnCheckedChangeListener { _, b ->
                p.edit()
                    .putBoolean("desktop_mode", b)
                    .apply()
            }
        }

        root.addView(desktop)

        val restore = Switch(this).apply {
            text = "Restore session"
            setTextColor(Color.WHITE)

            isChecked = p.getBoolean(
                "restore_session",
                true
            )

            setOnCheckedChangeListener { _, b ->
                p.edit()
                    .putBoolean("restore_session", b)
                    .apply()
            }
        }

        root.addView(restore)

        root.addView(
            Button(this).apply {
                text = "Save settings"

                setOnClickListener {
                    p.edit()
                        .putString(
                            "search_engine",
                            engines[engine.selectedItemPosition]
                        )
                        .apply()

                    finish()
                }
            }
        )

        setContentView(root)
    }

    private fun addRow(
        root: LinearLayout,
        label: String,
        view: android.view.View
    ) {
        root.addView(
            TextView(this).apply {
                text = label
                setTextColor(Color.LTGRAY)
                textSize = 13f
            }
        )

        root.addView(
            view,
            LinearLayout.LayoutParams(
                -1,
                -2
            ).apply {
                bottomMargin = dp(18)
            }
        )
    }

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density).toInt()
    }
}
