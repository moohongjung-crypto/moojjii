package com.mdtoobsidian

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

/** 공유 메뉴로 받은 파일을 화면 없이 바로 볼트로 복사 */
class ShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (VaultCopier.vaultUri(this) == null) {
                Toast.makeText(this, "먼저 앱을 열어 볼트 폴더를 선택하세요", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                val uris = mutableListOf<Uri>()
                if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let(uris::addAll)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(uris::add)
                }
                val n = if (uris.isNotEmpty()) VaultCopier.copy(this, uris)
                else intent.getStringExtra(Intent.EXTRA_TEXT)?.let { VaultCopier.saveText(this, it) } ?: 0
                Toast.makeText(this, "옵시디언 볼트로 ${n}개 파일 전송 완료", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
        finish()
    }
}
