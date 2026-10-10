package com.mdtoobsidian

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { textSize = 15f }
        val pickVault = Button(this).apply {
            text = "1. 옵시디언 볼트 폴더 선택 (처음 한 번)"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION), REQ_VAULT)
            }
        }
        val pickFiles = Button(this).apply {
            text = "2. 파일 선택해서 보내기"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }, REQ_FILES)
            }
        }
        val help = TextView(this).apply {
            text = "\n팁: 파일 관리자에서 파일을 길게 눌러 [공유] → [옵시디언으로 보내기]를 선택해도 됩니다."
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            addView(status); addView(pickVault); addView(pickFiles); addView(help)
        })
        refresh()
    }

    private fun refresh() {
        status.text = "볼트 폴더: " + (VaultCopier.vaultUri(this)?.lastPathSegment ?: "미설정")
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        when (requestCode) {
            REQ_VAULT -> {
                val uri = data.data ?: return
                contentResolver.takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                VaultCopier.saveVault(this, uri)
                refresh()
            }
            REQ_FILES -> {
                val uris = mutableListOf<Uri>()
                data.clipData?.let { c -> for (i in 0 until c.itemCount) uris.add(c.getItemAt(i).uri) }
                    ?: data.data?.let(uris::add)
                try {
                    Toast.makeText(this, "${VaultCopier.copy(this, uris)}개 파일 전송 완료", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object { const val REQ_VAULT = 1; const val REQ_FILES = 2 }
}
