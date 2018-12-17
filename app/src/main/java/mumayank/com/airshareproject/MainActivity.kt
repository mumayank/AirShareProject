package mumayank.com.airshareproject

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        send.setOnClickListener {
            startActivity(Intent(this, SendActivity::class.java))
        }

        receive.setOnClickListener {
            startActivity(Intent(this, ReceiveActivity::class.java))
        }
    }
}
