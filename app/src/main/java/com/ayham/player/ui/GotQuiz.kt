package com.ayham.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.R

private val GotGold = Color(0xFFC9A227)
private val GotSteel = Color(0xFFC9D3DD)

/** سؤال واحد: العبارة، الخيارات، ورقم الإجابة الصحيحة (يبدأ من 0). */
private class GotQ(val quote: String, val options: List<String>, val answer: Int)

private val gotQuestions = listOf(
    GotQ("\"Winter is coming.\"", listOf("Ned Stark", "Robert Baratheon", "Tywin Lannister", "Jon Snow"), 0),
    GotQ("\"I am the blood of the dragon.\"", listOf("Cersei Lannister", "Daenerys Targaryen", "Sansa Stark", "Melisandre"), 1),
    GotQ("\"The things I do for love.\"", listOf("Jaime Lannister", "Bronn", "Theon Greyjoy", "Petyr Baelish"), 0),
    GotQ("\"Chaos isn't a pit. Chaos is a ladder.\"", listOf("Qyburn", "Varys", "Littlefinger", "Tyrion Lannister"), 1),
    GotQ("\"Valar morghulis.\"", listOf("Syrio Forel", "Jaqen H'ghar", "Gendry", "Bronn"), 1),
    GotQ("\"I drink and I know things.\"", listOf("Varys", "Tyrion Lannister", "Oberyn Martell", "Pycelle"), 1),
)

/** بطاقة Game of Thrones في الرئيسية للتلفاز: صورة Winterfell وشعار آل ستارك، والضغط يفتح الاختبار. */
@Composable
fun GotHome(vm: AppViewModel, small: Boolean, onOpen: () -> Unit) {
    val ar = vm.lang == "ar"
    val shape = RoundedCornerShape(18.dp)
    Focusable(shape = shape, focusScale = 1.04f, onClick = onOpen) { f ->
        Box(
            Modifier.width(if (small) 260.dp else 310.dp).height(if (small) 150.dp else 170.dp)
                .clip(shape)
                .border(if (f) 2.dp else 1.dp, if (f) Color.White else Color(0x66C9A227), shape),
        ) {
            Image(
                painter = painterResource(R.drawable.got_banner), contentDescription = null,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x00000000), Color(0xEE07090C)))))
            Column(
                Modifier.align(Alignment.BottomStart).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(
                        painter = painterResource(R.drawable.got_sigil), contentDescription = null,
                        modifier = Modifier.width(56.dp).height(32.dp).clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    RText("Game of Thrones", if (small) 17 else 21, Color.White, bold = true, maxLines = 1)
                }
                RText(
                    if (ar) "اختبر معرفتك بالعبارات" else "Test your knowledge",
                    14, GotGold, bold = true, maxLines = 1,
                )
            }
        }
    }
}

/** اختبار العبارات: سؤال "من قال هذه العبارة؟" بأربعة خيارات، يُدار كله بالريموت (الأسهم وOK وBack). */
@Composable
fun GotQuiz(vm: AppViewModel, onClose: () -> Unit) {
    val ar = vm.lang == "ar"
    val total = gotQuestions.size
    var index by remember { mutableIntStateOf(0) }
    var picked by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }
    BackHandler(onBack = onClose)

    val shape = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxSize().background(Color(0xE605020B)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(760.dp).clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF12161B), Color(0xFF07090C))))
                .border(1.dp, Color(0x66C9A227), shape)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                RText(if (ar) "اختبار Game of Thrones" else "Game of Thrones Quiz", 22, GotGold, bold = true, maxLines = 1)
                GotButton(if (ar) "إغلاق" else "Close", filled = false) { onClose() }
            }

            if (index < total) {
                val q = gotQuestions[index]
                val answered = picked >= 0
                BodyText("${index + 1} / $total", 14, Color(0xFF9AA5B1), maxLines = 1)
                RText(if (ar) "من قال هذه العبارة؟" else "Who said this line?", 18, GotSteel, bold = true, maxLines = 1)
                RText(q.quote, 30, Color.White, bold = true, maxLines = 3)

                q.options.forEachIndexed { i, opt ->
                    val isRight = i == q.answer
                    val tint: Color? = when {
                        !answered -> null
                        isRight -> Color(0xFF2E7D5B)
                        i == picked -> Color(0xFF8E2B2B)
                        else -> null
                    }
                    Focusable(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        focusScale = 1.02f,
                        onClick = {
                            if (!answered) {
                                picked = i
                                if (isRight) score++
                            }
                        },
                    ) { f ->
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(tint ?: if (f) Color(0x55C9D3DD) else Color(0x22FFFFFF))
                                .border(
                                    if (f) 2.dp else 1.dp,
                                    if (f) Color.White else Color(0x33C9D3DD),
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(horizontal = 22.dp, vertical = 12.dp),
                        ) { RText(opt, 18, Color.White, bold = true, maxLines = 1) }
                    }
                }

                if (answered) {
                    val ok = picked == q.answer
                    RText(
                        if (ok) (if (ar) "صحيح!" else "Correct!")
                        else (if (ar) "خطأ. الإجابة: " else "Wrong. Answer: ") + q.options[q.answer],
                        17, if (ok) Color(0xFF5EEAD4) else Color(0xFFFF8A8A), bold = true, maxLines = 2,
                    )
                    val last = index + 1 == total
                    GotButton(
                        if (last) (if (ar) "عرض النتيجة" else "See score") else (if (ar) "التالي" else "Next"),
                        filled = true,
                    ) { index++; picked = -1 }
                }
            } else {
                RText(if (ar) "نتيجتك" else "Your score", 18, GotSteel, bold = true, maxLines = 1)
                RText("$score / $total", 48, GotGold, bold = true, maxLines = 1)
                RText(
                    when {
                        score == total -> if (ar) "إجابات كاملة، أنت من أهل Winterfell!" else "Perfect. You're a true Stark!"
                        score >= total / 2 -> if (ar) "أحسنت، الشمال يفتخر بك" else "Well played, the North is proud"
                        else -> if (ar) "راجع بعض الحلقات وجرّب مرة ثانية" else "Rewatch a few episodes and try again"
                    },
                    18, Color.White, maxLines = 2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GotButton(if (ar) "إعادة" else "Play again", filled = true) {
                        index = 0; picked = -1; score = 0
                    }
                    GotButton(if (ar) "إغلاق" else "Close", filled = false) { onClose() }
                }
            }
        }
    }
}

/** زر موحد للاختبار: ذهبي للإجراء الأساسي، وشفاف للثانوي. */
@Composable
private fun GotButton(text: String, filled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Focusable(shape = shape, focusScale = 1.05f, onClick = onClick) { f ->
        Box(
            Modifier.clip(shape)
                .background(
                    when {
                        filled && f -> Color(0xFFE0B63A)
                        filled -> Color(0xFF8A7019)
                        f -> Color(0x66FFFFFF)
                        else -> Color(0x33FFFFFF)
                    },
                )
                .padding(horizontal = 24.dp, vertical = 10.dp),
        ) {
            RText(text, 17, if (filled) Color(0xFF07090C) else Color.White, bold = true, maxLines = 1)
        }
    }
}
