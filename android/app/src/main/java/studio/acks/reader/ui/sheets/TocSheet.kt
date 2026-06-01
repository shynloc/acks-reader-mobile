package studio.acks.reader.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.acks.reader.ui.*

data class TocHeading(val level: Int, val text: String, val index: Int)

fun parseTocHeadings(markdown: String): List<TocHeading> {
    var idx = 0
    return markdown.lines().mapNotNull { line ->
        val m = Regex("^(#{1,3})\\s+(.+)").find(line) ?: return@mapNotNull null
        TocHeading(
            level = m.groupValues[1].length,
            text  = m.groupValues[2].trim(),
            index = idx++
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TocSheet(
    markdownSource: String,
    activeHead: Int,
    onGoto: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val headings = remember(markdownSource) { parseTocHeadings(markdownSource) }
    val listState = rememberLazyListState()

    // Scroll to active heading on first open
    LaunchedEffect(activeHead) {
        val pos = headings.indexOfFirst { it.index == activeHead }.takeIf { it >= 0 } ?: return@LaunchedEffect
        listState.animateScrollToItem(pos)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AcksSurface,
        dragHandle       = { SheetHandle() }
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text(
                "目录",
                color = AcksFg, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            if (headings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("此文档暂无目录", color = AcksFg3, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.heightIn(max = 480.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    itemsIndexed(headings, key = { _, h -> h.index }) { _, heading ->
                        val isActive = heading.index == activeHead
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActive) AcksAccentSoft else Color.Transparent)
                                .clickable { onGoto(heading.index); onDismiss() }
                                .padding(
                                    start = (8 + (heading.level - 1) * 18).dp,
                                    end = 12.dp, top = 10.dp, bottom = 10.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Level indicator dot
                            Box(
                                modifier = Modifier
                                    .size(if (heading.level == 1) 6.dp else 4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) AcksAccent
                                        else if (heading.level == 1) AcksFg2
                                        else AcksFg3
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                heading.text,
                                color      = if (isActive) AcksAccent else if (heading.level == 1) AcksFg else AcksFg2,
                                fontSize   = if (heading.level == 1) 14.sp else 13.sp,
                                fontWeight = if (heading.level == 1) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines   = 2,
                                overflow   = TextOverflow.Ellipsis,
                                modifier   = Modifier.weight(1f)
                            )
                            if (isActive) {
                                Text(
                                    "当前",
                                    color    = AcksAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(AcksAccentSoft, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (heading.level == 1 && headings.indexOf(heading) < headings.size - 1) {
                            HorizontalDivider(
                                color     = AcksBorder.copy(alpha = 0.4f),
                                thickness = 0.5.dp,
                                modifier  = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}
