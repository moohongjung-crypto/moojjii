package com.mdtoobsidian

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile

object VaultCopier {
    private const val PREFS = "prefs"
    private const val KEY_VAULT = "vault_uri"

    fun vaultUri(ctx: Context): Uri? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_VAULT, null)?.let(Uri::parse)

    fun saveVault(ctx: Context, uri: Uri) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_VAULT, uri.toString()).apply()
    }

    /** 선택된 파일들만 볼트 폴더로 복사. 같은 이름이 있으면 덮어씀. 성공 개수 반환. */
    fun copy(ctx: Context, uris: List<Uri>): Int {
        val tree = vaultUri(ctx) ?: throw IllegalStateException("볼트 폴더가 설정되지 않았습니다")
        val dir = DocumentFile.fromTreeUri(ctx, tree) ?: throw IllegalStateException("볼트 폴더를 열 수 없습니다")
        var ok = 0
        for (uri in uris) {
            val name = displayName(ctx, uri) ?: "note-${System.currentTimeMillis()}.md"
            dir.findFile(name)?.delete()
            // octet-stream: 시스템이 확장자를 멋대로 덧붙이지 않게 함
            val out = dir.createFile("application/octet-stream", name) ?: continue
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                ctx.contentResolver.openOutputStream(out.uri, "wt")?.use { input.copyTo(it) }
            }
            ok++
        }
        return ok
    }

    /** 파일 없이 텍스트만 공유된 경우 새 md 파일로 저장 */
    fun saveText(ctx: Context, text: String): Int {
        val tree = vaultUri(ctx) ?: throw IllegalStateException("볼트 폴더가 설정되지 않았습니다")
        val dir = DocumentFile.fromTreeUri(ctx, tree) ?: throw IllegalStateException("볼트 폴더를 열 수 없습니다")
        val out = dir.createFile("application/octet-stream", "shared-${System.currentTimeMillis()}.md") ?: return 0
        ctx.contentResolver.openOutputStream(out.uri, "wt")?.use { it.write(text.toByteArray()) }
        return 1
    }

    private fun displayName(ctx: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) return it.getString(0)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}
