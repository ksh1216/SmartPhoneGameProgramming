package kr.ac.tukorea.ge.spgp2026.suika.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kr.ac.tukorea.ge.spgp2026.suika.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    // ViewBinding을 사용하여 XML 레이아웃과 연결합니다.
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 레이아웃 설정
        setContentView(binding.root)

        // 추가적인 초기화가 필요하다면 여기에 작성합니다.
    }

    // XML에서 android:onClick="onBtnStartGame"으로 설정된 함수입니다.
    fun onBtnStartGame(view: View) {
        startGameActivity()
    }

    private fun startGameActivity() {
        // 실제 게임이 구동되는 Activity(CookieRunActivity)를 실행합니다.
        val intent = Intent(this, SuikaActivity::class.java)
        startActivity(intent)
    }
}