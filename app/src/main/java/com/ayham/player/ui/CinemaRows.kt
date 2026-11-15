package com.ayham.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayham.player.data.Item

/** صف «الأعلى 10»: أرقام كبيرة مفرّغة خلف البوسترات بأسلوب المنصات العالمية. */
@Composable
fun RankRow(
    title: String, items: List<Item>, posterH: Dp, hpad: Dp,
    onFocus: (Item) -> Unit = {}, first: Boolean = false, onOpen: (Item) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        RText(title, 17, C.text, bold = true, maxLines = 1, modifier = Modifier.padding(start = hpad, top = 6.dp, end = hpad))
        LazyRow(
            Modifier.fillMaxWidth().height(posterH + 22.dp),
            contentPadding = PaddingValues(horizontal = hpad - 4.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(items.take(10), key = { _, it -> "rk" + it.key }) { i, it ->
                Row(verticalAlignment = Alignment.Bottom) {
                    androidx.compose.material3.Text(
                        text = "${i + 1}",
                        modifier = Modifier.offset(x = if (i == 9) 30.dp else 22.dp, y = 8.dp),
                        style = TextStyle(
                            fontFamily = AppFont,
                            fontSize = (posterH.value * 0.78f).sp,
                            fontWeight = FontWeight.Black,
                            color = tc(0x33140A28),
                            drawStyle = Stroke(width = 7f),
                        ),
                        maxLines = 1,
                    )
                    PosterCard(
                        it,
                        Modifier.onFocusChanged { f -> if (f.hasFocus) onFocus(it) }.then(if (first && i == 0) Modifier.autoFocus() else Modifier),
                        w = posterH * 0.68f, h = posterH, focusScale = 1.1f,
                    ) { onOpen(it) }
                }
            }
        }
    }
}
