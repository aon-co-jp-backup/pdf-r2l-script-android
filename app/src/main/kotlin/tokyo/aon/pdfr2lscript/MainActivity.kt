package tokyo.aon.pdfr2lscript

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView

/** PDFを選び、綴じ方向を変えて別名保存する画面(Storage Access Frameworkで読み書きする)。 */
class MainActivity : Activity() {
    private val hasMode = false
    private var src: Uri? = null
    private var srcName: String = ""
    private lateinit var fileLabel: TextView
    private lateinit var status: TextView
    private lateinit var convertButton: Button
    private var rtl: RadioButton? = null
    private var pendingMode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        fun lp() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        root.addView(TextView(this).apply { text = "PDF R2L Script"; textSize = 22f; setTypeface(typeface, Typeface.BOLD) }, lp())
        root.addView(Button(this).apply {
            text = "PDFを選択 / Choose PDF"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/pdf"
                }, REQ_PICK)
            }
        }, lp())
        fileLabel = TextView(this).apply { text = "(未選択 / none)" }
        root.addView(fileLabel, lp())
        if (hasMode) {
            val group = RadioGroup(this)
            rtl = RadioButton(this).apply { text = "右綴じ (R2L)"; id = 1001; isChecked = true }
            group.addView(rtl)
            group.addView(RadioButton(this).apply { text = "左綴じ (L2R)"; id = 1002 })
            root.addView(group, lp())
        } else {
            root.addView(TextView(this).apply { text = "右綴じ(R2L)・見開き右始まり(TwoColumnRight)にします" }, lp())
        }
        convertButton = Button(this).apply {
            text = "変換して保存… / Convert and save"
            isEnabled = false
            setOnClickListener {
                pendingMode = if (hasMode && rtl?.isChecked == false) 1 else 0
                val stem = srcName.removeSuffix(".pdf").removeSuffix(".PDF")
                val suffix = if (pendingMode == 1) "l2r" else "r2l"
                startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_TITLE, "$stem-$suffix.pdf")
                }, REQ_SAVE)
            }
        }
        root.addView(convertButton, lp())
        status = TextView(this)
        root.addView(status, lp())
        root.addView(Button(this).apply {
            text = "ライセンス / License"
            setOnClickListener { showLicense() }
        }, lp())
        setContentView(ScrollView(this).apply {
            addView(root)
            // Android 15 の画面端までの描画で、ステータスバー/ナビゲーションバーに隠れないよう余白を足す
            setOnApplyWindowInsetsListener { _, ins ->
                @Suppress("DEPRECATION")
                root.setPadding(pad, ins.systemWindowInsetTop + pad, pad, ins.systemWindowInsetBottom + pad)
                ins
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null) return
        when (requestCode) {
            REQ_PICK -> {
                src = uri
                srcName = displayName(uri)
                fileLabel.text = srcName
                convertButton.isEnabled = true
            }
            REQ_SAVE -> save(uri)
        }
    }

    private fun displayName(uri: Uri): String =
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "document.pdf"

    private fun save(dest: Uri) {
        val from = src ?: return
        val mode = pendingMode
        status.text = "変換中… / Converting…"
        Thread {
            val msg = try {
                val input = contentResolver.openInputStream(from)!!.use { it.readBytes() }
                val out = Native.convert(input, mode)
                contentResolver.openOutputStream(dest, "wt")!!.use { it.write(out) }
                "完了 / Done: $dest"
            } catch (e: Throwable) {
                "エラー / Error: ${e.message}"
            }
            runOnUiThread { status.text = msg }
        }.start()
    }

    private fun showLicense() {
        val text = assets.open("LICENSES.txt").bufferedReader(Charsets.UTF_8).use { it.readText() }
            .trimStart('﻿')
        val view = TextView(this).apply {
            this.text = text
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            val p = (12 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }
        AlertDialog.Builder(this)
            .setTitle("ライセンス / License")
            .setView(ScrollView(this).apply { addView(view) })
            .setPositiveButton("OK", null)
            .show()
    }

    companion object {
        private const val REQ_PICK = 1
        private const val REQ_SAVE = 2
    }
}
