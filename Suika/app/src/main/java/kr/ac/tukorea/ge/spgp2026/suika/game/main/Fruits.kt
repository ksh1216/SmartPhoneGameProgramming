package kr.ac.tukorea.ge.spgp2026.suika.game.main

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Fruit(
    val grade: Int
) : IGameObject {

    var checkPaused: (() -> Boolean)? = null

    var x = 0f
    var y = 0f
    val radius = 40f + (grade * 30f)

    var dx = 0f
    var dy = 0f

    // --- [회전 기능 핵심 변수 추가] ---
    var angle = 0f // 과일의 현재 회전 각도 (단위: 도/Degree)

    private val gravity = 0.8f
    private val friction = 0.96f
    private var isPhysicsEnabled = false

    var bitmap: Bitmap? = null
    private val dstRect = RectF()
    private val bitmapPaint = Paint().apply { isAntiAlias = true }

    fun setCenter(cx: Float, cy: Float) {
        x = cx
        y = cy
    }

    fun startPhysics() { isPhysicsEnabled = true }
    fun stopVertical() { dy = 0f }

    override fun update(gctx: GameContext) {
        if (checkPaused?.invoke() == true) return

        if (isPhysicsEnabled) {
            dy += gravity
            dx *= friction
            dy *= friction
            x += dx
            y += dy

            // --- [회전 물리 주입] ---
            // 과일이 좌우로 움직이는 속도(dx)에 비례해서 각도를 변화시킵니다.
            // 반지름(radius)이 클수록(큰 과일일수록) 무거우므로 회전 속도를 살짝 조절해 줍니다.
            val rotationFactor = 2.0f
            angle += (dx / radius) * (180f / Math.PI.toFloat()) * rotationFactor

            // 각도가 너무 커져서 오버플로우가 나지 않도록 360도 주기로 제한해 줍니다.
            if (angle > 360f) angle -= 360f
            if (angle < -360f) angle += 360f
        }
    }

    override fun draw(canvas: Canvas) {
        val bmp = bitmap
        if (bmp != null) {
            // 이미지를 그리기 전에 도화지(Canvas) 자체를 과일 중심점을 기준으로 회전시킵니다.
            canvas.save() // 현재 도화지 상태 저장

            // 과일의 중심 좌표(x, y)를 기준으로 현재 계산된 angle만큼 도화지를 돌립니다.
            canvas.rotate(angle, x, y)

            // 회전된 도화지 위에 비트맵 이미지를 그립니다.
            dstRect.set(x - radius, y - radius, x + radius, y + radius)
            canvas.drawBitmap(bmp, null, dstRect, bitmapPaint)

            canvas.restore() // 다음 오브젝트들을 위해 도화지 회전 상태를 원래대로 복구
        } else {
            val fallbackPaint = Paint().apply {
                color = android.graphics.Color.GRAY
                style = Paint.Style.FILL
            }
            canvas.drawCircle(x, y, radius, fallbackPaint)
        }
    }

    fun isCollidingWith(other: Fruit): Boolean {
        val dx = x - other.x
        val dy = y - other.y
        return (dx * dx + dy * dy) <= (radius + other.radius) * (radius + other.radius)
    }
}