package com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat

abstract class SwipeableActivity : BaseActivity() {

    private lateinit var gestureDetector: GestureDetectorCompat
    private val activityOrder = listOf(
        MainActivity::class.java,
        MarketAnalysisActivity::class.java,
        WeatherInfoActivity::class.java,
        ProfileActivity::class.java
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                        return true
                    }
                }
                return super.onFling(e1, e2, velocityX, velocityY)
            }
        })
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            gestureDetector.onTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    private fun onSwipeLeft() {
        val currentIndex = activityOrder.indexOf(this::class.java)
        if (currentIndex < activityOrder.size - 1) {
            val nextActivity = activityOrder[currentIndex + 1]
            startActivity(Intent(this, nextActivity).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            })
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun onSwipeRight() {
        val currentIndex = activityOrder.indexOf(this::class.java)
        if (currentIndex > 0) {
            val previousActivity = activityOrder[currentIndex - 1]
            startActivity(Intent(this, previousActivity).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            })
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}
