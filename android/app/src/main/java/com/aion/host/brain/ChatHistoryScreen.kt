package com.aion.host.brain

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aion.host.memory.ConversationEntity
import com.aion.host.ui.theme.AionColors
import com.aion.host.ui.theme.AionTopBar
import com.aion.host.ui.theme.GlassPanel
import java.text.DateFormat
import java.util.Date

/** Lists real past conversations (T-060's `conversations` table, now actually written to by [ChatScreen]). */
@Composable
fun ChatHistoryScreen(
    chatHistoryStore: ChatHistoryStore,
    resumeSignal: Int,
    onBack: () -> Unit,
    onOpenConversation: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var conversations by remember { mutableStateOf(emptyList<ConversationEntity>()) }
    LaunchedEffect(resumeSignal) { conversations = chatHistoryStore.conversations() }

    Column(modifier = modifier.fillMaxSize()) {
        AionTopBar(title = "Chat History", onBack = onBack)
        if (conversations.isEmpty()) {
            Text(
                "No past chats yet — start one from the Chat screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = AionColors.OnSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        } else {
            GlassPanel(modifier = Modifier.padding(horizontal = 20.dp)) {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                    items(conversations) { conversation ->
                        ConversationRow(conversation, onClick = { onOpenConversation(conversation.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationEntity,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
    ) {
        Text(
            conversation.summary?.takeIf { it.isNotBlank() } ?: "(empty chat)",
            style = MaterialTheme.typography.bodyLarge,
            color = AionColors.OnBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(conversation.startedAt)),
            style = MaterialTheme.typography.bodySmall,
            color = AionColors.OnSurfaceVariant,
        )
    }
    HorizontalDivider(color = AionColors.OutlineVariant)
}
