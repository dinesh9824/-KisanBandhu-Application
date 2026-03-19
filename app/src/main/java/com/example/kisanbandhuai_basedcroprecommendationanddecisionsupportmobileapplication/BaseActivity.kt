package he

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication.LocaleHelper

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.updateBaseContextLocale(newBase))
    }
}