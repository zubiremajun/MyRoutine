package com.myroutine.app
import android.content.Context

object Store {
    private fun p(c: Context) = c.getSharedPreferences("rc", Context.MODE_PRIVATE)
    fun json(c: Context): String? = p(c).getString("data", null)
    fun save(c: Context, s: String) = p(c).edit().putString("data", s).apply()
    fun banner(c: Context): String? = p(c).getString("banner", null)
    fun saveBanner(c: Context, s: String) {
        if (s.isBlank()) p(c).edit().remove("banner").apply()
        else p(c).edit().putString("banner", s).apply()
    }
    fun oldCount(c: Context) = p(c).getInt("count", 0)
    fun setCount(c: Context, n: Int) = p(c).edit().putInt("count", n).apply()
}
