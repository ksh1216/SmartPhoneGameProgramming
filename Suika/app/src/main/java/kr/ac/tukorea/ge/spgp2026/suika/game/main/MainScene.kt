package kr.ac.tukorea.ge.spgp2026.suika.game.main

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kr.ac.tukorea.ge.spgp2026.suika.R
import kotlin.math.sqrt

class MainScene(gctx: GameContext) : Scene(gctx) {

    enum class State {
        PLAYING, PAUSED, GAME_OVER
    }

    enum class Layer {
        BACKGROUND, FRUIT, TOP_FRUIT, UI, COUNT
    }

    // --- [UI 디자인용 Paint 추가] ---
    private val bestScoreBoxPaint = Paint().apply {
        color = Color.parseColor("#FFE0B2") // 부드러운 살구색
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val currentScoreBoxPaint = Paint().apply {
        color = Color.parseColor("#E1BEE7") // 부드러운 연보라색
    }
    private val uiBoxBorderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    // --- [UI 박스 위치 정의] ---
    private val bestScoreBoxRect = RectF(30f, 40f, 330f, 180f)    // 왼쪽 최고점수 박스
    private val currentScoreBoxRect = RectF(540f, 40f, 780f, 180f)

    override val world = World(Layer.entries.toTypedArray())

    private var currentFruit: Fruit? = null
    private lateinit var scoreManager: ScoreManager
    private val gameOverDetector = GameOverDetector()

    var gameState = State.PLAYING
        private set

    // 과일 리소스 매핑 및 캐싱 배열
    private val fruitResIds = intArrayOf(
        R.drawable.fruit_0, // grade 0
        R.drawable.fruit_1, // grade 1
        R.drawable.fruit_2, // grade 2
        R.drawable.fruit_3, // grade 3
        R.drawable.fruit_4, // grade 4
        R.drawable.fruit_5, // grade 5
        R.drawable.fruit_6  // grade 6
    )
    private val fruitBitmaps = arrayOfNulls<Bitmap>(7)

    // UI 터치 소모 플래그
    private var touchConsumedByUI = false

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

    // 가상 해상도 좌표 (★테스트를 위해 데드라인을 1200f로 낮춤)
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

        // 과일 비트맵 로딩 부분...
        val res = gctx.view.context.resources
        for (i in fruitResIds.indices) {
            fruitBitmaps[i] = BitmapFactory.decodeResource(res, fruitResIds[i])
        }

        // [★ 해결책] 게임 시작할 때 효과음들을 미리 사운드풀에 로드하라고 쿡 찔러둡니다.
        // (볼륨을 0으로 주어 로드만 시키는 영리한 꼼수입니다.)
        gctx.res.sound.playEffect(R.raw.mergesound)
        gctx.res.sound.playEffect(R.raw.gameoversound)

        // 게임 시작 BGM 무한 반복 재생
        gctx.res.sound.playMusic(R.raw.backgroundsound)

        prepareNextFruit()
    }

    private fun prepareNextFruit() {
        if (gameState == State.GAME_OVER || gameOverDetector.isGameOver) return
        if (gameState == State.PAUSED) return

        val grade = (0..2).random()
        val fruit = Fruit(grade)

        fruit.checkPaused = { this.gameState == State.PAUSED }
        fruit.bitmap = fruitBitmaps[grade]

        fruit.setCenter(gameWidth / 2, playBoxTop)

        currentFruit = fruit
        world.add(fruit, Layer.TOP_FRUIT)
    }

    override fun onBackPressed(): Boolean {
        if (gameState == State.GAME_OVER || gameOverDetector.isGameOver) return false
        if (gameState == State.PLAYING) {
            gameState = State.PAUSED
            gctx.res.sound.pauseMusic()
            return true
        } else if (gameState == State.PAUSED) {
            gameState = State.PLAYING
            gctx.res.sound.resumeMusic()
            if (currentFruit == null) {
                prepareNextFruit()
            }
            return true
        }
        return false
    }

    override fun update(gctx: GameContext) {
        super.update(gctx)

        if (gameState == State.PLAYING) {
            solvePhysics()
            checkMerge()

            val fruits = world.objectsAt(Layer.FRUIT).filterIsInstance<Fruit>()

            // [복구] 무조건 2개 조건문 대신, 원래의 똑똑한 gameOverDetector 감시 카메라를 다시 가동합니다.
            gameOverDetector.update(fruits, playBoxTop, gctx.frameTime)

            if (gameOverDetector.isGameOver) {
                currentFruit = null
                gameState = State.GAME_OVER

                gctx.res.sound.stopMusic()
                gctx.res.sound.playEffect(R.raw.gameoversound)
            }
        }
    }

    private fun solvePhysics() {
        val fruits = world.objectsAt(Layer.FRUIT).filterIsInstance<Fruit>()
        for (i in fruits.indices) {
            val f1 = fruits[i]

            // 삭제 대기 중인 가짜 과일은 물리 연산 패스
            if (f1.dx == 9999f) continue

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
                if (f2.dx == 9999f) continue

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
                    // [버그 방어] 이미 삭제 대기 중인 프레임의 과일이라면 무시 (효과음 연사 원천 차단)
                    if (f1.dx == 9999f || f2.dx == 9999f) continue

                    if (f1.grade < 6) {
                        val nextGrade = f1.grade + 1
                        val spawnX = (f1.x + f2.x) / 2
                        val spawnY = (f1.y + f2.y) / 2

                        // 지연 삭제 도중 중복 체크를 막기 위한 임시 고스트 플래그 설정
                        f1.dx = 9999f
                        f2.dx = 9999f

                        // [효과음 주입] 합성 소리 깔끔하게 딱 1번만 출력!
                        gctx.res.sound.playEffect(R.raw.mergesound)

                        world.remove(f1, Layer.FRUIT)
                        world.remove(f2, Layer.FRUIT)

                        val newFruit = Fruit(nextGrade)
                        newFruit.checkPaused = { this.gameState == State.PAUSED }
                        newFruit.bitmap = fruitBitmaps[nextGrade]

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
        if (gameState == State.GAME_OVER || gameOverDetector.isGameOver) return true

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
            touchConsumedByUI = false

            if (gameState == State.PAUSED) {
                if (resumeBtnRect.contains(tx, ty)) {
                    gameState = State.PLAYING
                    gctx.res.sound.resumeMusic()
                    if (currentFruit == null) {
                        prepareNextFruit()
                    }
                    touchConsumedByUI = true
                    return true
                }
                if (lobbyBtnRect.contains(tx, ty)) {
                    (gctx.view.context as? android.app.Activity)?.finish()
                    touchConsumedByUI = true
                    return true
                }
                touchConsumedByUI = true
                return true
            }
            if (gameState == State.PLAYING && pauseBtnRect.contains(tx, ty)) {
                gameState = State.PAUSED
                gctx.res.sound.pauseMusic()
                touchConsumedByUI = true
                return true
            }
        }

        // 조준선 터치 유효 범위를 낮아진 데드라인(1200f)보다 위인 1100f에 맞춤
        if (gameState == State.PLAYING && !touchConsumedByUI) {
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

        if (event.action == MotionEvent.ACTION_UP) {
            touchConsumedByUI = false
        }

        return true
    }

    override fun draw(canvas: Canvas) {
        // 배경색 채우기
        canvas.drawColor(Color.parseColor("#C8E6C9"))

        // 최고 점수 박스 및 테두리 (좌측 30f ~ 330f 유지)
        canvas.drawRoundRect(bestScoreBoxRect, 30f, 30f, bestScoreBoxPaint)
        canvas.drawRoundRect(bestScoreBoxRect, 30f, 30f, uiBoxBorderPaint)

        // 현재 점수 박스 및 테두리 (새롭게 다이어트된 540f ~ 780f 구역)
        canvas.drawRoundRect(currentScoreBoxRect, 30f, 30f, currentScoreBoxPaint)
        canvas.drawRoundRect(currentScoreBoxRect, 30f, 30f, uiBoxBorderPaint)

        // 최고 점수 텍스트 (좌측 180f 중심)
        canvas.drawText("Best score", 180f, 95f, uiLabelPaint)
        canvas.drawText("${scoreManager.bestScore}", 180f, 155f, uiScorePaint)

        // 현재 점수 텍스트 (줄어든 박스의 딱 한가운데인 660f에 정갈하게 꽂힙니다)
        canvas.drawText("Score", 660f, 95f, uiLabelPaint)
        canvas.drawText("${scoreManager.currentScore}", 660f, 155f, uiScorePaint)

        // ---------------------------------------------------------
        // 아래 기존 구조 유지
        canvas.drawRect(playBoxLeft, playBoxTop, playBoxRight, groundY, wallPaint)
        canvas.drawLine(playBoxLeft, playBoxTop, playBoxRight, playBoxTop, deadLinePaint)

        // 일시정지 버튼 (800f~870f 구역)
        canvas.drawRoundRect(pauseBtnRect, 15f, 15f, pauseBtnPaint)
        canvas.drawRect(823f, 83f, 832f, 107f, pauseBarPaint)
        canvas.drawRect(838f, 83f, 847f, 107f, pauseBarPaint)

        super.draw(canvas)

        // 동기화된 GAME_OVER 문구 렌더링
        if (gameState == State.GAME_OVER) {
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