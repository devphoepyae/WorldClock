package com.worldclock.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worldclock.app.R
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Renders the adaptive app icon to a PNG so the monochrome mark can be
 * reviewed, and asserts the vector actually inflates (which also validates the
 * path data).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class AppIconTest {

    @Test
    fun launcherIcon_renders() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val foreground = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
        assertNotNull("launcher foreground drawable must inflate", foreground)

        val sizePx = 432 // 108 dp at 4x
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(ContextCompat.getColor(context, R.color.ic_launcher_background))

        // A circular launcher mask shows the central 72 dp of the 108 dp canvas.
        val maskRadius = sizePx * 36f / 108f
        val clip = Path().apply {
            addCircle(sizePx / 2f, sizePx / 2f, maskRadius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clip)
        foreground!!.setBounds(0, 0, sizePx, sizePx)
        foreground.draw(canvas)
        canvas.restore()

        val output = File("build/screenshots/app_icon.png")
        output.parentFile?.mkdirs()
        FileOutputStream(output).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
