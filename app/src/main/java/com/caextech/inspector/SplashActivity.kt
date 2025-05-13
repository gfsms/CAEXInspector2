package com.caextech.inspector

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val animationDuration = 500L
    private val checkDelay = 300L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        startSplashAnimation()
    }

    private fun startSplashAnimation() {
        val items = listOf(
            Pair(findViewById<LinearLayout>(R.id.gear_item), findViewById<CheckBox>(R.id.gear_checkbox)),
            Pair(findViewById<LinearLayout>(R.id.meter_item), findViewById<CheckBox>(R.id.meter_checkbox)),
            Pair(findViewById<LinearLayout>(R.id.suspension_item), findViewById<CheckBox>(R.id.suspension_checkbox))
        )

        var delay = 500L // Initial delay

        items.forEachIndexed { index, (itemLayout, checkbox) ->
            handler.postDelayed({
                // Fade in the item
                val fadeIn = ObjectAnimator.ofFloat(itemLayout, "alpha", 0f, 1f)
                fadeIn.duration = animationDuration
                fadeIn.start()

                // Check the checkbox after item appears
                handler.postDelayed({
                    checkbox.isChecked = true

                    // If this is the last item, navigate to main activity
                    if (index == items.size - 1) {
                        handler.postDelayed({
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }, 1000)
                    }
                }, checkDelay)

            }, delay)

            delay += 800L // Delay between each item
        }
    }
}