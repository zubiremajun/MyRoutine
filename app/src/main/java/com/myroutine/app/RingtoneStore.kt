package com.myroutine.app
import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object RingtoneStore {
    private fun dir(c: Context): File {
        val d = File(c.filesDir, "ringtones"); d.mkdirs(); return d
    }
    private fun indexFile(c: Context) = File(dir(c), "index.json")

    private fun readIndex(c: Context): JSONArray =
        try { JSONArray(indexFile(c).readText()) } catch (e: Exception) { JSONArray() }

    private fun writeIndex(c: Context, arr: JSONArray) { indexFile(c).writeText(arr.toString()) }

    fun list(c: Context): String = readIndex(c).toString()

    fun save(c: Context, name: String, dataUrl: String): String? {
        return try {
            val comma = dataUrl.indexOf(',')
            val b64 = if (comma >= 0) dataUrl.substring(comma + 1) else dataUrl
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            if (bytes.size > 8 * 1024 * 1024) return null
            val ext = name.substringAfterLast('.', "mp3").lowercase().filter { it.isLetterOrDigit() }.ifBlank { "mp3" }
            val id = "t${System.currentTimeMillis()}.$ext"
            File(dir(c), id).writeBytes(bytes)
            val arr = readIndex(c)
            val row = JSONObject().put("id", id).put("name", name)
            arr.put(row)
            writeIndex(c, arr)
            id
        } catch (e: Exception) { null }
    }

    fun path(c: Context, id: String): String? {
        val f = File(dir(c), id.replace("..", "").replace("/", ""))
        return if (f.exists()) f.absolutePath else null
    }

    fun delete(c: Context, id: String) {
        val safe = id.replace("..", "").replace("/", "")
        File(dir(c), safe).delete()
        val arr = readIndex(c); val out = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("id") != safe) out.put(o)
        }
        writeIndex(c, out)
    }
}
