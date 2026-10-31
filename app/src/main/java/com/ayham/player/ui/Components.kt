package com.ayham.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ayham.player.data.Item
import com.ayham.player.data.Kind
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Img(
    url: String?,
    modifier: Modifier = Modifier,
    scale: ContentScale = ContentScale.Crop,
    w: Int = 200,
    h: Int = 300,
) {
    if (url.isNullOrBlank()) return
    val ctx = LocalContext.current
    val req = remember(url, w, h) {
        ImageRequest.Builder(ctx).data(url).size(w, h).crossfade(false).allowRgb565(true).build()
    }
    AsyncImage(model = req, contentDescription = null, modifier = modifier, contentScale = scale)
}

/** يطلب التركيز تلقائيًا عند ظهور العنصر. */
@Composable
fun Modifier.autoFocus(): Modifier {
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(80)
        runCatching { fr.requestFocus() }
    }
    return this.focusRequester(fr)
}

@Composable
fun PosterCard(
    item: Item,
    modifier: Modifier = Modifier,
    w: Dp = Dp.Unspecified,
    h: Dp = Dp.Unspecified,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val compact = isCompact()
    val ww = if (w != Dp.Unspecified) w else if (compact) 118.dp else 128.dp
    val hh = if (h != Dp.Unspecified) h else if (compact) 177.dp else 192.dp
    Focusable(modifier = modifier.width(ww).height(hh), shape = shape, onClick = onClick) { _ ->
        Box(Modifier.fillMaxSize().clip(shape).background(C.surface)) {
            if (item.image.isBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RText(item.name.take(1), 44, C.surfaceHi, bold = true)
                }
            }
            Img(item.image, Modifier.fillMaxSize())
            // طبقة واحدة صغيرة تحت الاسم بدل طبقة كاملة فوق البوستر (أخف على التلفاز)
            Box(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE0B0614))))
                    .padding(start = 8.dp, end = 8.dp, top = 18.dp, bottom = 7.dp),
            ) {
                BodyText(item.name, 12, C.text, bold = true, maxLines = 2)
            }
        }
    }
}

@Composable
fun ChannelCard(
    item: Item,
    modifier: Modifier = Modifier,
    w: Dp = 230.dp,
    fav: Boolean = false,
    epg: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Focusable(modifier = modifier.width(w).height(78.dp), shape = shape, focusScale = 1.05f, onLongClick = onLongClick, onClick = onClick) { f ->
        Row(
            Modifier.fillMaxSize().clip(shape).background(if (f) C.surfaceHi else C.surface).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x22FFFFFF)), contentAlignment = Alignment.Center) {
                if (item.image.isBlank()) RText(item.name.take(1), 22, C.primaryLight, bold = true)
                Img(item.image, Modifier.fillMaxSize().padding(4.dp), ContentScale.Fit, 100, 100)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                BodyText(item.name, 14, C.text, bold = f, maxLines = if (epg != null) 1 else 2)
                if (epg != null) BodyText(epg, 11, C.primaryLight, maxLines = 1)
            }
            if (fav) Icon(Icons.Filled.Favorite, null, tint = C.accent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun ChannelRow(
    item: Item,
    playing: Boolean,
    fav: Boolean = false,
    epg: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Focusable(Modifier.fillMaxWidth().height(64.dp), shape, 1.02f, onLongClick = onLongClick, onClick = onClick) { f ->
        Row(
            Modifier.fillMaxSize().clip(shape).background(if (f) C.surfaceHi else C.surface).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x22FFFFFF)), contentAlignment = Alignment.Center) {
                if (item.image.isBlank()) RText(item.name.take(1), 20, C.primaryLight, bold = true)
                Img(item.image, Modifier.fillMaxSize().padding(3.dp), ContentScale.Fit, 100, 100)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                BodyText(item.name, 16, if (playing) C.accent else C.text, bold = f || playing, maxLines = 1)
                if (epg != null) BodyText(epg, 12, C.primaryLight, maxLines = 1)
            }
            if (fav) Icon(Icons.Filled.Favorite, null, tint = C.accent, modifier = Modifier.size(20.dp))
            if (playing) RText("▶", 18, C.accent)
        }
    }
}

@Composable
fun ItemCard(item: Item, onClick: () -> Unit) {
    if (item.kind == Kind.LIVE) ChannelCard(item, onClick = onClick) else PosterCard(item, onClick = onClick)
}

/** يقرأ صفحات من القاعدة عند الحاجة فقط (تمرير سلس حتى لو القائمة بعشرات الآلاف). */
class Pager<T>(
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val pageSize: Int,
    private val loader: suspend (offset: Int, limit: Int) -> List<T>,
) {
    val items = androidx.compose.runtime.mutableStateListOf<T>()
    var loaded by androidx.compose.runtime.mutableStateOf(false)
        private set
    private var end = false
    private var busy = false

    fun loadMore() {
        if (busy || end) return
        busy = true
        scope.launch {
            val page = runCatching { loader(items.size, pageSize) }.getOrDefault(emptyList())
            items.addAll(page)
            if (page.size < pageSize) end = true
            loaded = true
            busy = false
        }
    }
}

@Composable
fun <T> rememberPager(key: Any?, pageSize: Int = 60, loader: suspend (Int, Int) -> List<T>): Pager<T> {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val p = remember(key) { Pager(scope, pageSize, loader) }
    LaunchedEffect(p) { p.loadMore() }
    return p
}

/** صف تصنيف يحمّل عناصره فقط لما يظهر على الشاشة. */
@Composable
fun LazyCategoryRow(title: String, version: Int, loader: suspend () -> List<Item>, onItem: (Item) -> Unit) {
    var items by remember(title, version) { androidx.compose.runtime.mutableStateOf<List<Item>?>(null) }
    LaunchedEffect(title, version) {
        // انتظار قصير: إذا مرّ الصف سريعًا أثناء التمرير يُلغى التحميل بدل ما يثقل القاعدة
        delay(180)
        items = runCatching { loader() }.getOrDefault(emptyList())
    }
    val list = items
    if (list == null) {
        // مساحة بديلة حتى لا تُحمَّل كل الصفوف مرة وحدة
        Box(Modifier.fillMaxWidth().height(if (isCompact()) 240.dp else 264.dp).padding(start = if (isCompact()) 14.dp else 28.dp, top = 8.dp)) {
            RText(title, 24, C.textDim, bold = true)
        }
    } else {
        ContentRow(title, list, onItem)
    }
}

@Composable
fun ContentRow(title: String, items: List<Item>, onItem: (Item) -> Unit) {
    if (items.isEmpty()) return
    val pad = if (isCompact()) 14.dp else 28.dp
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        RText(title, if (isCompact()) 19 else 24, C.text, bold = true, modifier = Modifier.padding(horizontal = pad))
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = pad, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(items, key = { it.key }) { item ->
                ItemCard(item) { onItem(item) }
            }
        }
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Focusable(shape = shape, focusScale = 1.06f, onClick = onClick) { f ->
        Box(
            Modifier
                .clip(shape)
                .background(
                    when {
                        f -> C.surfaceHi
                        selected -> C.primary
                        else -> C.surface
                    },
                )
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            RText(label, 16, if (selected && !f) Color.White else C.text, bold = selected, maxLines = 1)
        }
    }
}

@Composable
fun IconBtn(
    icon: ImageVector,
    size: Dp = 54.dp,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Focusable(modifier = modifier.size(size), shape = CircleShape, focusScale = 1.12f, onClick = onClick) { f ->
        Box(
            Modifier.fillMaxSize().clip(CircleShape)
                .background(
                    when {
                        f -> C.primary
                        active -> C.surfaceHi
                        else -> Color(0x66000000)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
        }
    }
}

@Composable
fun PanelItem(label: String, selected: Boolean = false, sub: String? = null, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Focusable(modifier = modifier.fillMaxWidth(), shape = shape, focusScale = 1.02f, onClick = onClick) { f ->
        Row(
            Modifier.fillMaxWidth().clip(shape).background(if (f) C.surfaceHi else C.surface).padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                RText(label, 18, C.text, bold = selected, maxLines = 2)
                if (sub != null) BodyText(sub, 12, C.textDim, maxLines = 1)
            }
            if (selected) Icon(Icons.Filled.Check, null, tint = C.accent, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun AyhamField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onDone: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        label = { RText(label, 16, C.textDim) },
        leadingIcon = if (icon != null) ({ Icon(icon, null, tint = C.primaryLight, modifier = Modifier.size(24.dp)) }) else null,
        textStyle = TextStyle(fontSize = 18.sp, color = C.text),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }, onSearch = { onDone() }),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = C.text,
            unfocusedTextColor = C.text,
            cursorColor = C.accent,
            focusedBorderColor = if (icon != null) C.accent else C.primary,
            unfocusedBorderColor = Color(0x44FFFFFF),
            focusedContainerColor = Color(0x22000000),
            unfocusedContainerColor = Color(0x11000000),
        ),
    )
}

/** شارة تقييم IMDb. */
@Composable
fun ImdbBadge(rating: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFF5C518)).padding(horizontal = 7.dp, vertical = 2.dp)) {
            RText("IMDb", 13, Color.Black, bold = true)
        }
        RText("★ $rating", 17, Color(0xFFF5C518), bold = true)
    }
}

/** صف فيلم/مسلسل بالتخطيط الطولي: صورة صغيرة + الاسم + التقييم. */
@Composable
fun VodRow(item: Item, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Focusable(Modifier.fillMaxWidth().height(86.dp), shape, 1.02f, onClick = onClick) { f ->
        Row(
            Modifier.fillMaxSize().clip(shape).background(if (f) C.surfaceHi else C.surface).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(48.dp).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(Color(0x22FFFFFF)), contentAlignment = Alignment.Center) {
                if (item.image.isBlank()) RText(item.name.take(1), 20, C.primaryLight, bold = true)
                Img(item.image, Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BodyText(item.name, 17, C.text, bold = f, maxLines = 2)
                if (item.rating.isNotBlank() && item.rating != "0") BodyText("★ ${item.rating}", 13, C.primaryLight, maxLines = 1)
            }
        }
    }
}


/** عنوان البرنامج الحالي للقناة (يُجلب فقط للعناصر الظاهرة، مع كاش). */
@Composable
fun rememberEpgNow(vm: com.ayham.player.AppViewModel, streamId: String): String? {
    var s by remember(streamId) { androidx.compose.runtime.mutableStateOf<String?>(null) }
    LaunchedEffect(streamId) {
        val nowS = System.currentTimeMillis() / 1000
        val cur = vm.epg(streamId).firstOrNull { it.start <= nowS && it.end > nowS }
        s = cur?.title
    }
    return s
}
