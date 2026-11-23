package com.postaltools.tr25.pdf

import android.content.Context
import android.graphics.Typeface

/** TeX Gyre Pagella from assets – a free Palatino design that matches Book Antiqua. */
object Fonts {
    var regular: Typeface? = null; private set
    var bold: Typeface? = null; private set
    var italic: Typeface? = null; private set

    fun load(ctx: Context) {
        fun f(n: String) = runCatching { Typeface.createFromAsset(ctx.assets, "fonts/$n") }.getOrNull()
        regular = f("texgyrepagella-regular.otf")
        bold = f("texgyrepagella-bold.otf")
        italic = f("texgyrepagella-italic.otf")
    }
}
