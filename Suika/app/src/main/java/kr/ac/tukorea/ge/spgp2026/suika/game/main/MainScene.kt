package kr.ac.tukorea.ge.spgp2026.suika.game.main

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.sqrt

class MainScene(gctx: GameContext) : Scene(gctx) {

    enum class State {
        PLAYING, PAUSED, GAME_OVER
    }

    enum class Layer {
        BACKGROUND, FRUIT, TOP_FRUIT, UI, COUNT
    }

    override val world = World(Layer.entries.toTypedArray())

    private var currentFruit: Fruit? = null
    private lateinit var scoreManager: ScoreManager
    private val gameOverDetector = GameOverDetector()

    var gameState = State.PLAYING
        private set

    // Paint 도구들
    private val wallPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }
    private val deadLinePaint = Paint().apply {
        color = Color.parseColor("#FF9800")
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val uiLabelPaint = Paint().apply {
        color = Color.BLACK
        textSize = 35f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val uiScorePaint = Paint().apply {
        color = Color.BLACK
        textSize = 45f
        textAlign = Paint.Align.CENTER
    }
    private val gameOverPaint = Paint().apply {
        color = Color.RED
        textSize = 100f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val pauseBtnPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val pauseBarPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val dimPaint = Paint().apply {
        color = Color.parseColor("#AA000000")
    }
    private val dialogPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val dialogBorderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val dialogTextPaint = Paint().apply {
        color = Color.BLACK
        textSize = 50f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    // 가상 해상도 좌표
    private val gameWidth = 900f
    private val gameHeight = 1600f
    private val playBoxTop = 250f
    private val playBoxLeft = 20f
    private val playBoxRight = 880f
    private val groundY = 1550f

    // 버튼 터치 영역
    private val pauseBtnRect = RectF(800f, 60f, 870f, 130f)
    private val resumeBtnRect = RectF(250f, 750f, 650f, 870f)
    private val lobbyBtnRect = RectF(250f, 920f, 650f, 1040f)

    private val touchPts = FloatArray(2)
    private val inverseMatrix = android.graphics.Matrix()

    init {
        scoreManager = ScoreManager(gctx.view.context)
        prepareNextFruit()
    }

    private fun prepareNextFruit() {
        if (gameOverDetector.isGameOver || gameState == State.PAUSED) return

        val grade = (0..2).random()
        val fruit = Fruit(grade)

        // [체크!] 과일에게 내 상태가 PAUSED인지 감시하는 함수를 쥐여줍니다.
        fruit.checkPaused = { this.gameState == State.PAUSED }

        fruit.setCenter(gameWidth / 2, playBoxTop)
        currentFruit = fruit
        world.add(fruit, Layer.TOP_FRUIT)
    }

    override fun onBackPressed(): Boolean {
        if (gameOverDetector.isGameOver) return false
        if (gameState == State.PLAYING) {
            gameState = State.PAUSED
            return true
        } else if (gameState == State.PAUSED) {
            gameState = State.PLAYING
            return true
        }
        return false
    }

    override fun update(gctx: GameContext) {
        super.update(gctx)
        if (gameState == State.PLAYING && !gameOverDetector.isGameOver) {
            solvePhysics()
            checkMerge()
            val fruits = world.objectsAt(Layer.FRUIT).filterIsInstance<Fruit>()
            gameOverDetector.update(fruits, playBoxTop, gctx.frameTime)
            if (gameOverDetector.isGameOver) {
                currentFruit = null
            }
        }
    }

    private fun solvePhysics() {
        val fruits = world.objectsAt(Layer.FRUIT).filterIsInstance<Fruit>()
        for (i in fruits.indices) {
            val f1 = fruits[i]
            if (f1.x - f1.radius < playBoxLeft) { f1.x = playBoxLeft + f1.radius; f1.dx *= -0.5f }
            if (f1.x + f1.radius > playBoxRight) { f1.x = playBoxRight - f1.radius; f1.dx *= -0.5f }
            if (f1.y + f1.radius > groundY) {
                f1.y = groundY - f1.radius
                f1.stopVertical()
                f1.dx *= 0.8f
            }
            if (f1.y - f1.radius < playBoxTop - 100f && f1.dy < 0) {
                f1.y = playBoxTop - 100f + f1.radius
                f1.dy *= -0.2f
            }
            for (j in i + 1 until fruits.size) {
                val f2 = fruits[j]
                val distX = f2.x - f1.x
                val distY = f2.y - f1.y
                val dist = sqrt(distX * distX + distY * distY)
                val minDist = f1.radius + f2.radius
                if (dist < minDist && dist > 0) {
                    val overlap = minDist - dist
                    val nx = distX / dist
                    val ny = distY / dist
                    f1.x -= nx * overlap * 0.5f
                    f1.y -= ny * overlap * 0.5f
                    f2.x += nx * overlap * 0.5f
                    f2.y += ny * overlap * 0.5f
                    val top = if (f1.y < f2.y) f1 else f2
                    val bottom = if (f1.y < f2.y) f2 else f1
                    val slideDir = if (top.x < bottom.x) -1f else 1f
                    if (top.dy > 0) {
                        val slideForce = top.dy * 0.3f
                        top.dx += slideDir * slideForce
                        top.dy *= 0.4f
                    }
                    f1.dx -= nx * 0.5f
                    f2.dx += nx * 0.5f
                }
            }
        }
    }

    private fun checkMerge() {
        val fruits = world.objectsAt(Layer.FRUIT).filterIsInstance<Fruit>()
        for (i in fruits.indices) {
            for (j in i + 1 until fruits.size) {
                val f1 = fruits[i]
                val f2 = fruits[j]
                if (f1.grade == f2.grade && f1.isCollidingWith(f2)) {
                    if (f1.grade < 6) {
                        val nextGrade = f1.grade + 1
                        val spawnX = (f1.x + f2.x) / 2
                        val spawnY = (f1.y + f2.y) / 2
                        world.remove(f1, Layer.FRUIT)
                        world.remove(f2, Layer.FRUIT)

                        val newFruit = Fruit(nextGrade)

                        // [체크!] 합성된 과일에게도 내 상태 감시 함수 연결!
                        newFruit.checkPaused = { this.gameState == State.PAUSED }

                        newFruit.setCenter(spawnX, spawnY)
                        newFruit.startPhysics()
                        world.add(newFruit, Layer.FRUIT)
                        scoreManager.addScoreForMerge(nextGrade)
                    }
                    return
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameOverDetector.isGameOver) return true

        val transform = gctx.metrics.transformMatrix
        if (transform.invert(inverseMatrix)) {
            touchPts[0] = event.x
            touchPts[1] = event.y
            inverseMatrix.mapPoints(touchPts)
        } else {
            touchPts[0] = event.x
            touchPts[1] = event.y
        }

        val tx = touchPts[0]
        val ty = touchPts[1]

        if (event.action == MotionEvent.ACTION_DOWN) {
            if (gameState == State.PAUSED) {
                if (resumeBtnRect.contains(tx, ty)) {
                    gameState = State.PLAYING
                    return true
                }
                if (lobbyBtnRect.contains(tx, ty)) {
                    (gctx.view.context as? android.app.Activity)?.finish()
                    return true
                }
                return true
            }
            if (gameState == State.PLAYING && pauseBtnRect.contains(tx, ty)) {
                gameState = State.PAUSED
                return true
            }
        }

        if (gameState == State.PLAYING) {
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    currentFruit?.let {
                        it.x = tx.coerceIn(playBoxLeft + it.radius, playBoxRight - it.radius)
                        it.y = playBoxTop
                    }
                }
                MotionEvent.ACTION_UP -> {
                    currentFruit?.let {
                        world.remove(it, Layer.TOP_FRUIT)
                        it.startPhysics()
                        world.add(it, Layer.FRUIT)
                        currentFruit = null
                        gctx.view.postDelayed({ prepareNextFruit() }, 1000)
                    }
                }
            }
        }
        return true
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(playBoxLeft, playBoxTop, playBoxRight, groundY, wallPaint)
        canvas.drawLine(playBoxLeft, playBoxTop, playBoxRight, playBoxTop, deadLinePaint)

        canvas.drawText("Best score", 180f, 80f, uiLabelPaint)
        canvas.drawText("${scoreManager.bestScore}", 180f, 145f, uiScorePaint)

        canvas.drawText("Score", 700f, 80f, uiLabelPaint)
        canvas.drawText("${scoreManager.currentScore}", 700f, 145f, uiScorePaint)

        canvas.drawRoundRect(pauseBtnRect, 15f, 15f, pauseBtnPaint)
        canvas.drawRect(823f, 83f, 832f, 107f, pauseBarPaint)
        canvas.drawRect(838f, 83f, 847f, 107f, pauseBarPaint)

        super.draw(canvas)

        if (gameOverDetector.isGameOver) {
            canvas.drawText("GAME OVER", gameWidth / 2, gameHeight / 2, gameOverPaint)
        }

        if (gameState == State.PAUSED) {
            canvas.drawRect(0f, 0f, gameWidth, gameHeight, dimPaint)
            val dialogRect = RectF(150f, 550f, 750f, 1150f)
            canvas.drawRoundRect(dialogRect, 30f, 30f, dialogPaint)
            canvas.drawRoundRect(dialogRect, 30f, 30f, dialogBorderPaint)
            canvas.drawText("일시정지", gameWidth / 2, 660f, dialogTextPaint)
            canvas.drawRoundRect(resumeBtnRect, 20f, 20f, dialogBorderPaint)
            canvas.drawText("돌아가기", gameWidth / 2, 825f, uiLabelPaint)
            canvas.drawRoundRect(lobbyBtnRect, 20f, 20f, dialogBorderPaint)
            canvas.drawText("로비 이동", gameWidth / 2, 995f, uiLabelPaint)
        }
    }

    override fun touchObjects(): List<IGameObject>? = null
}