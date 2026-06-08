package kr.ac.tukorea.ge.spgp2026.suika.game.main

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Fruit(
    val grade: Int
) : IGameObject {

    // [버그 해결] MainScene에서 넘겨받을 일시정지 체크용 감시 함수입니다.
    // 이 방식은 패키지 간의 간섭이나 참조 오류를 100% 원천 차단합니다.
    var checkPaused: (() -> Boolean)? = null

    private val paint = Paint().apply {
        isAntiAlias = true
        color = getGradeColor(grade)
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    var x = 0f
    var y = 0f
    val radius = 40f + (grade * 30f)

    // 외부(MainScene)에서 충돌 시 속도를 제어할 수 있도록 public 유지
    var dx = 0f
    var dy = 0f

    private val gravity = 0.8f   // 기존 1.2f에서 감소시켜 부드러운 낙하 유도
    private val friction = 0.96f // 마찰력을 살짝 줄여 더 잘 미끄러지게 함
    private var isPhysicsEnabled = false

    private fun getGradeColor(grade: Int): Int {
        return when (grade) {
            0 -> Color.parseColor("#FFCDD2")
            1 -> Color.parseColor("#F48FB1")
            2 -> Color.parseColor("#CE93D8")
            3 -> Color.parseColor("#FFE082")
            4 -> Color.parseColor("#FFAB91")
            5 -> Color.parseColor("#EF5350")
            6 -> Color.parseColor("#4CAF50")
            else -> Color.WHITE
        }
    }

    fun setCenter(cx: Float, cy: Float) {
        x = cx
        y = cy
    }

    fun startPhysics() {
        isPhysicsEnabled = true
    }

    fun stopVertical() {
        dy = 0f
    }

    override fun update(gctx: GameContext) {
        // [버그 해결] 일시정지 체크 함수가 등록되어 있고, 현재 씬이 일시정지(PAUSED) 상태라면
        // 중력 가속 및 이동 계산을 일절 하지 않고 그 자리에 그대로 얼려버립니다.
        if (checkPaused?.invoke() == true) {
            return
        }

        // --- 기존 Fruit 클래스의 오리지널 물리/이동 코드 작동 ---
        if (isPhysicsEnabled) {
            // 교수님 프레임워크의 고정 시간(gctx.frameTime) 대신
            // 기존 작성해두셨던 부드러운 수치 연산 흐름을 그대로 유지합니다.
            dy += gravity
            dx *= friction
            dy *= friction

            x += dx
            y += dy
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, radius, paint)
        canvas.drawCircle(x, y, radius, strokePaint)
    }

    fun isCollidingWith(other: Fruit): Boolean {
        val dx = x - other.x
        val dy = y - other.y
        return (dx * dx + dy * dy) <= (radius + other.radius) * (radius + other.radius)
    }
}