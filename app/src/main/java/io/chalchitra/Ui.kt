package io.chalchitra

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** Small RAM thumbnail cache (RGB_565, 12 MB) on top of the HTTP disk cache. */
object Thumbs {
    private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val gate = Semaphore(4)

    suspend fun get(url: String): Bitmap? {
        cache.get(url)?.let { return it }
        return gate.withPermit {
            cache.get(url) ?: withContext(Dispatchers.IO) {
                try {
                    val b = Net.bytes(url)
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(b, 0, b.size, bounds)
                    var s = 1
                    while (bounds.outWidth / (s * 2) >= 360) s *= 2
                    val o = BitmapFactory.Options().apply {
                        inSampleSize = s
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    BitmapFactory.decodeByteArray(b, 0, b.size, o)?.also { cache.put(url, it) }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
}

@Composable
fun Root() {
    var detail by remember { mutableStateOf<Wall?>(null) }
    var settings by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Home(onOpen = { detail = it }, onSettings = { settings = true })
        if (settings) Settings(onBack = { settings = false })
        detail?.let { w -> Detail(w, onClose = { detail = null }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(onOpen: (Wall) -> Unit, onSettings: () -> Unit) {
    var srcId by rememberSaveable { mutableStateOf(Sources.all[0].id) }
    var sort by rememberSaveable { mutableStateOf(Sort.HOT) }
    val src = Sources.byId(srcId)

    val items = remember { mutableStateListOf<Wall>() }
    var after by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var end by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val gen = remember { IntArray(1) }
    val scope = rememberCoroutineScope()

    fun more() {
        if (loading || end) return
        loading = true
        error = null
        val g = gen[0]
        val s = Sources.byId(srcId)
        val so = sort
        scope.launch {
            try {
                var cursor = after
                var tries = 0
                while (true) {
                    val p = withContext(Dispatchers.IO) { Feed.load(s, so, cursor) }
                    if (g != gen[0]) return@launch
                    items.addAll(p.items.filter { n -> items.none { it.id == n.id } })
                    cursor = p.after
                    after = cursor
                    if (cursor == null) end = true
                    tries++
                    if (items.isNotEmpty() || cursor == null || tries >= 4) break
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (g == gen[0]) error = e.message ?: "The internet has left the chat"
            } finally {
                if (g == gen[0]) loading = false
            }
        }
    }

    LaunchedEffect(srcId, sort) {
        gen[0]++
        items.clear()
        after = null
        end = false
        error = null
        loading = false
        more()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chalchitra") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings, for the control freaks")
                    }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Sources.all, key = { it.id }) { s ->
                    FilterChip(
                        selected = s.id == srcId,
                        onClick = { srcId = s.id },
                        label = { Text(s.label) },
                    )
                }
            }
            if (src.kind == Kind.REDDIT) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(Sort.entries.toList(), key = { it.name }) { s ->
                        FilterChip(
                            selected = s == sort,
                            onClick = { sort = s },
                            label = { Text(s.label) },
                        )
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(items, key = { _, w -> w.id }) { i, w ->
                    if (i >= items.size - 6) LaunchedEffect(items.size) { more() }
                    val press = remember { MutableInteractionSource() }
                    val pressed by press.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        if (pressed) 0.93f else 1f,
                        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                        label = "press",
                    )
                    Box(
                        Modifier
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .aspectRatio(9f / 16f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(interactionSource = press, indication = LocalIndication.current) { onOpen(w) },
                    ) {
                        val bm by produceState<Bitmap?>(null, w.thumb) { value = Thumbs.get(w.thumb) }
                        bm?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = w.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
                if (loading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                error?.let { msg ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(msg, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { more() }) { Text("Try again, optimism is free") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Detail(w: Wall, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val pr = remember { Prefs(ctx) }
    val scope = rememberCoroutineScope()
    var raw by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val thumb by produceState<Bitmap?>(null, w.thumb) { value = Thumbs.get(w.thumb) }

    BackHandler(onBack = onClose)

    LaunchedEffect(w.id) {
        try {
            raw = withContext(Dispatchers.IO) { Engine.render(ctx, w.full) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = e.message ?: "Image didn't load. It's shy."
        }
    }

    fun set(target: Int) {
        val b = raw ?: return
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    Engine.apply(ctx, b, target, pr.dimOn, pr.dimStrength)
                    pr.markSeen(w.id)
                    Engine.clearCache(ctx)
                    true
                } catch (_: Exception) {
                    false
                }
            }
            busy = false
            Toast.makeText(ctx, if (ok) "Done. Your phone looks marginally less sad." else "Android refused. Shocking, I know.", Toast.LENGTH_SHORT).show()
            if (ok) onClose()
        }
    }

    val density = LocalDensity.current.density
    val dimHeight = (Engine.dimLengthPx(ctx) / density).dp

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val shown = raw ?: thumb
        if (shown != null) {
            Image(
                bitmap = remember(shown) { shown.asImageBitmap() },
                contentDescription = w.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        if (pr.dimOn) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(dimHeight)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = pr.dimStrength / 100f), Color.Transparent)
                        )
                    ),
            )
        }
        IconButton(onClick = onClose, modifier = Modifier.statusBarsPadding().padding(4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            Text(w.title, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            when {
                failed != null -> Text(failed!!, color = Color.White)
                busy || raw == null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { set(0) }, modifier = Modifier.weight(1f)) { Text("Home") }
                    FilledTonalButton(onClick = { set(1) }, modifier = Modifier.weight(1f)) { Text("Lock") }
                    Button(onClick = { set(2) }, modifier = Modifier.weight(1f)) { Text("Both") }
                }
            }
        }
    }
}

private val intervals = listOf(
    15 to "15 min", 60 to "1 h", 180 to "3 h", 360 to "6 h", 720 to "12 h", 1440 to "24 h",
)
private val targets = listOf(0 to "Home", 1 to "Lock", 2 to "Both")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Settings(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val pr = remember { Prefs(ctx) }
    val scope = rememberCoroutineScope()
    var auto by remember { mutableStateOf(pr.autoOn) }
    var interval by remember { mutableStateOf(pr.interval) }
    var target by remember { mutableStateOf(pr.autoTarget) }
    var srcs by remember { mutableStateOf(pr.sources) }
    var dim by remember { mutableStateOf(pr.dimOn) }
    var strength by remember { mutableStateOf(pr.dimStrength.toFloat()) }
    var busy by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Surface(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { pad ->
            Column(
                Modifier
                    .padding(pad)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SwitchRow("Auto change", "Because tapping twice a day is exhausting. Runs only with network and battery not low", auto) {
                    auto = it
                    pr.autoOn = it
                    if (it) AutoJob.schedule(ctx, interval) else AutoJob.cancel(ctx)
                }

                Text("Change every (commitment issues welcome)", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    intervals.forEach { (m, label) ->
                        FilterChip(
                            selected = interval == m,
                            onClick = {
                                interval = m
                                pr.interval = m
                                if (auto) AutoJob.schedule(ctx, m)
                            },
                            label = { Text(label) },
                        )
                    }
                }

                Text("Apply to (pick wisely, or don't)", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    targets.forEach { (t, label) ->
                        FilterChip(
                            selected = target == t,
                            onClick = {
                                target = t
                                pr.autoTarget = t
                            },
                            label = { Text(label) },
                        )
                    }
                }

                Text("Where the pretty things come from", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sources.all.forEach { s ->
                        FilterChip(
                            selected = s.id in srcs,
                            onClick = {
                                val n = if (s.id in srcs) srcs - s.id else srcs + s.id
                                if (n.isNotEmpty()) {
                                    srcs = n
                                    pr.sources = n
                                }
                            },
                            label = { Text(s.label) },
                        )
                    }
                }

                SwitchRow("Dim status bar", "So your clock stays readable over that sunset you picked", dim) {
                    dim = it
                    pr.dimOn = it
                }
                if (dim) {
                    Slider(
                        value = strength,
                        onValueChange = { strength = it },
                        onValueChangeFinished = { pr.dimStrength = strength.toInt() },
                        valueRange = 10f..90f,
                    )
                }

                Button(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                try {
                                    Auto.runOnce(ctx)
                                } catch (_: Exception) {
                                    false
                                }
                            }
                            busy = false
                            Toast.makeText(
                                ctx,
                                if (ok) "Fresh wallpaper. Try to act surprised." else "Nothing found. The internet is being dramatic.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (busy) "Judging pixels..." else "Change it now, I guess") }

                Text(
                    "Images come from public Reddit and Bing feeds. Animated subreddits are skipped " +
                        "because this app is tiny, not magic. NSFW posts are always skipped. You're welcome.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
