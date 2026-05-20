package kr.ac.tukorea.ge.spgp2026.suika.game.main

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.sqrt

class MainScene(gctx: GameContext) : Scene(gctx) {

    enum class Layer {
        BACKGROUND, FRUIT, TOP_FRUIT, UI, COUNT
    }

    override val world = World(Layer.entries.toTypedArray())

    private var currentFruit: Fruit? = null

    // 분리된 매니저 클래스들 객체 생성
    private lateinit var scoreManager: ScoreManager
    private val gameOverDetector = GameOverDetector() // [변경] 독립 클래스로 장착

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

    // 화면 크기 및 플레이 박스 경계 정의
    private val gameWidth = 900f
    private val gameHeight = 1600f

    private val playBoxTop = 250f     // 데드라인 기준선
    private val playBoxLeft = 20f
    private val playBoxRight = 880f
    private val groundY = 1550f

    init {
        scoreManager = ScoreManager(gctx.view.context)
        prepareNextFruit()
    }

    private fun prepareNextFruit() {
        // [변경] Detector 객체에게 상태를 물어봅니다.
        if (gameOverDetector.isGameOver) return

        val grade = (0..2).random()
        val fruit = Fruit(grade)

        fruit.setCenter(gameWidth / 2, playBoxTop)
        currentFruit = fruit
        world.add(fruit, Layer.TOP_FRUIT)
    }

    override fun update(gctx: GameContext) {
        super.update(gctx)

        // [변경] 플레이 중일 때만 물리 연산 및 합성을 수행
        if (!gameOverDetector.isGameOver) {
            solvePhysics()
            checkMerge()

            // [변경] 과일 리스트를 추출하여 전담 디텍터 클래스에게 상시 검사를 맡깁니다.
            val fruits = world.objectsAt(Layer.FRUIT).filterIsInstance<Fruit>()
            gameOverDetector.update(fruits, playBoxTop, gctx.frameTime)

            // 방금 검사로 인해 게임오버로 전환되었다면 조작 과일 제거
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
        // [변경] Detector에게 게임 오버 상태를 확인받아 입력을 통제합니다.
        if (gameOverDetector.isGameOver) return true

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                currentFruit?.let {
                    it.x = event.x.coerceIn(playBoxLeft + it.radius, playBoxRight - it.radius)
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
        return true
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(playBoxLeft, playBoxTop, playBoxRight, groundY, wallPaint)
        canvas.drawLine(playBoxLeft, playBoxTop, playBoxRight, playBoxTop, deadLinePaint)

        canvas.drawText("Best score", 180f, 80f, uiLabelPaint)
        canvas.drawText("${scoreManager.bestScore}", 180f, 145f, uiScorePaint)

        canvas.drawText("Score", 650f, 80f, uiLabelPaint)
        canvas.drawText("${scoreManager.currentScore}", 650f, 145f, uiScorePaint)

        super.draw(canvas)

        // [변경] Detector의 상태 값에 맞춰 게임오버 텍스트 출력
        if (gameOverDetector.isGameOver) {
            canvas.drawText("GAME OVER", gameWidth / 2, gameHeight / 2, gameOverPaint)
        }
    }

    override fun touchObjects(): List<IGameObject>? = null
}