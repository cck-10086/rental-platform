<template>
  <div class="ai-workspace">
    <aside class="conversation-column">
      <ConversationList
        :conversations="conversations"
        :active-id="conversationId"
        :loading="conversationsLoading"
        :error="conversationsError"
        @new="handleNewChat"
        @select="handleSelectConversation"
        @delete="handleDeleteConversation"
        @retry="loadConversations"
      />
    </aside>

    <section class="chat-workspace" aria-label="AI 租房顾问聊天工作区">
      <header class="chat-header">
        <button class="history-trigger" type="button" aria-label="打开历史会话" @click="historyOpen = true">
          <el-icon><PanelLeft /></el-icon>
        </button>
        <div class="chat-header__copy">
          <h2>AI 租房顾问</h2>
          <p>合同风险与租住事务咨询</p>
        </div>
        <div class="chat-status"><span></span>在线</div>
      </header>

      <div class="chat-thread">
        <MessageScroller
          ref="messageScrollerRef"
          :messages="messages"
          :waiting="sending && waitingFirst"
          :streaming="sending && !waitingFirst"
          :error="streamError"
          @retry="handleRetry"
        />
      </div>

      <footer class="chat-composer">
        <div class="quick-questions" aria-label="常用问题">
          <button
            v-for="question in quickQuestions"
            :key="question"
            type="button"
            :class="{ 'is-selected': inputText === question }"
            @click="inputText = inputText === question ? '' : question"
          >{{ question }}</button>
        </div>
        <div class="composer-field">
          <el-input
            v-model="inputText"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 5 }"
            aria-label="咨询问题"
            placeholder="输入你的租房问题…"
            resize="none"
            @keydown.enter.exact.prevent="handleSend"
          />
          <button
            type="button"
            class="send-button"
            :class="{ 'is-disabled': !inputText.trim() || sending }"
            :disabled="!inputText.trim() || sending"
            aria-label="发送问题"
            @click="handleSend"
          >
            <el-icon v-if="!sending"><Send /></el-icon>
            <span v-else class="send-button__spinner" aria-label="发送中"></span>
          </button>
        </div>
        <p class="composer-hint">Enter 发送，Shift + Enter 换行 · AI 回答仅供参考</p>
      </footer>
    </section>

    <el-drawer v-model="historyOpen" title="历史会话" direction="ltr" size="min(88vw, 300px)" class="history-drawer">
      <ConversationList
        :conversations="conversations"
        :active-id="conversationId"
        :loading="conversationsLoading"
        :error="conversationsError"
        @new="handleNewChat"
        @select="handleSelectConversation"
        @delete="handleDeleteConversation"
        @retry="loadConversations"
      />
    </el-drawer>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { PanelLeft, Send } from '@lucide/vue'
import ConversationList from '@/components/chat/ConversationList.vue'
import MessageScroller from '@/components/chat/MessageScroller.vue'
import { aiChatStream, deleteConversation, getConversationDetail, getConversations } from '@/api/ai'

/* 快捷问题:围绕论文核心场景 */
const quickQuestions = [
  '押金纠纷如何处理？',
  '不续租需要提前多久通知？',
  '房东私闯违法吗？',
  '合同中的高风险条款有哪些？',
  '退租时押金如何结算？'
]

const messages = ref([])
const inputText = ref('')
const sending = ref(false)
const waitingFirst = ref(false)
const conversationId = ref('')
const conversations = ref([])
const conversationsLoading = ref(false)
const conversationsError = ref('')
const streamError = ref('')
const lastQuestion = ref('')
const historyOpen = ref(false)
const messageScrollerRef = ref(null)

const loadConversations = async () => {
  conversationsLoading.value = true
  conversationsError.value = ''
  try {
    const response = await getConversations()
    conversations.value = response.data || []
  } catch {
    conversationsError.value = '暂时无法获取历史会话，请检查网络后重试。'
  } finally {
    conversationsLoading.value = false
  }
}

const handleNewChat = () => {
  messages.value = []
  conversationId.value = ''
  inputText.value = ''
  streamError.value = ''
  historyOpen.value = false
  nextTick(() => messageScrollerRef.value?.scrollToBottom(true))
}

const handleSelectConversation = async conversation => {
  conversationId.value = conversation.conversationId
  streamError.value = ''
  historyOpen.value = false
  try {
    const response = await getConversationDetail(conversation.conversationId)
    messages.value = (response.data || []).map((message, index) => ({
      id: message.id || `${conversation.conversationId}-${index}`,
      role: message.role,
      content: message.content,
      time: message.time
    }))
    nextTick(() => messageScrollerRef.value?.scrollToBottom(true))
  } catch {
    streamError.value = '会话内容加载失败，请重新选择或稍后再试。'
  }
}

const handleDeleteConversation = async conversation => {
  try {
    await ElMessageBox.confirm('删除后将无法恢复这段对话。', '删除会话', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
  } catch {
    return
  }

  try {
    await deleteConversation(conversation.conversationId)
    conversations.value = conversations.value.filter(item => item.conversationId !== conversation.conversationId)
    if (conversationId.value === conversation.conversationId) handleNewChat()
    ElMessage.success('会话已删除')
  } catch {
    ElMessage.error('删除失败，请稍后重试')
  }
}

const handleSend = async () => {
  const question = inputText.value.trim()
  if (!question || sending.value) return

  lastQuestion.value = question
  streamError.value = ''
  messages.value.push({ id: `user-${Date.now()}`, role: 'user', content: question, time: Date.now() })
  inputText.value = ''
  sending.value = true
  waitingFirst.value = true
  nextTick(() => messageScrollerRef.value?.scrollToBottom(true))

  let aiMessage = null
  try {
    await aiChatStream(
      { question, conversationId: conversationId.value || undefined },
      {
        onMeta: id => { conversationId.value = id || conversationId.value },
        onDelta: delta => {
          waitingFirst.value = false
          if (!aiMessage) {
            aiMessage = { id: `ai-${Date.now()}`, role: 'ai', content: '', time: Date.now() }
            messages.value.push(aiMessage)
          }
          aiMessage.content += delta
        },
        onDone: loadConversations,
        onError: message => { streamError.value = message || 'AI 回答失败，请稍后重试。' }
      }
    )
    if (!aiMessage && !streamError.value) streamError.value = '本次没有生成有效回答，请重新发送。'
  } catch {
    streamError.value = 'AI 服务暂时不可用，请检查网络后重试。'
  } finally {
    sending.value = false
    waitingFirst.value = false
  }
}

const handleRetry = () => {
  if (!lastQuestion.value || sending.value) return
  inputText.value = lastQuestion.value
  handleSend()
}

onMounted(loadConversations)
</script>

<style scoped>
/* Codex 工作台三栏:全局导航(Layout) | 会话列表 | 聊天工作区 */
.ai-workspace {
  display: grid;
  width: min(100%, 1600px);
  height: 100%;
  min-height: 0;
  margin: 0 auto;
  grid-template-columns: 264px minmax(0, 1fr);
  border: 1px solid var(--rp-border);
  border-radius: var(--rp-radius-card);
  background: var(--rp-surface);
  overflow: hidden;
}
.conversation-column { min-width: 0; min-height: 0; }

.chat-workspace {
  display: grid;
  min-width: 0;
  min-height: 0;
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
  background: var(--rp-surface);
}

/* 顶栏:仅名称 + 说明 + 在线状态 */
.chat-header {
  display: flex;
  min-height: 56px;
  align-items: center;
  gap: 10px;
  padding: 10px 18px;
  border-bottom: 1px solid var(--rp-border);
  background: var(--rp-surface);
}
.chat-header__copy { min-width: 0; }
.chat-header__copy h2 { margin: 0; color: var(--rp-text); font-size: 14px; font-weight: 600; }
.chat-header__copy p { margin: 2px 0 0; overflow: hidden; color: var(--rp-text-muted); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.chat-status { display: flex; align-items: center; gap: 6px; margin-left: auto; color: var(--rp-success); font-size: 11px; }
.chat-status span { width: 6px; height: 6px; border-radius: 50%; background: var(--rp-success); }
.history-trigger { display: none; width: 32px; height: 32px; place-items: center; border: 1px solid var(--rp-border); border-radius: var(--rp-radius-btn); background: var(--rp-surface); color: var(--rp-text-secondary); cursor: pointer; }
.chat-thread { min-height: 0; overflow: hidden; }

/* 输入区:紧凑低高度 */
.chat-composer { padding: 10px 16px calc(10px + env(safe-area-inset-bottom)); border-top: 1px solid var(--rp-border); background: var(--rp-surface); }
.quick-questions { display: flex; gap: 6px; margin-bottom: 8px; overflow-x: auto; scrollbar-width: none; }
.quick-questions::-webkit-scrollbar { display: none; }
.quick-questions button { flex: 0 0 auto; min-height: 26px; padding: 3px 10px; border: 1px solid var(--rp-border); border-radius: 999px; background: var(--rp-surface); color: var(--rp-text-secondary); cursor: pointer; font-size: 11px; transition: background var(--rp-duration), border-color var(--rp-duration), color var(--rp-duration); }
.quick-questions button:hover, .quick-questions button.is-selected { border-color: var(--rp-primary); background: var(--rp-primary-light); color: var(--rp-primary-hover); }
.composer-field { display: flex; align-items: flex-end; gap: 8px; }
.composer-field :deep(.el-textarea__inner) { min-height: 38px !important; padding: 8px 11px; border: 0; background: var(--rp-surface-muted); box-shadow: 0 0 0 1px var(--rp-border) inset; line-height: 1.55; }
.composer-field :deep(.el-textarea__inner:focus) { box-shadow: 0 0 0 1px var(--rp-primary) inset; }
.send-button { display: grid; width: 38px; height: 38px; flex: 0 0 auto; place-items: center; border: 0; border-radius: var(--rp-radius-btn); background: var(--rp-primary); color: #fff; cursor: pointer; font-size: 15px; transition: background var(--rp-duration); }
.send-button:hover { background: var(--rp-primary-hover); }
.send-button.is-disabled { background: #D6D3D1; color: #FAFAF9; cursor: not-allowed; }
.send-button__spinner { width: 13px; height: 13px; border: 2px solid rgba(255,255,255,.35); border-top-color: #fff; border-radius: 50%; animation: rp-spin .7s linear infinite; }
@keyframes rp-spin { to { transform: rotate(360deg); } }
.composer-hint { margin: 6px 2px 0; color: var(--rp-text-muted); font-size: 10px; }

:global(.history-drawer .el-drawer__header) { margin-bottom: 0; padding: 14px 16px; border-bottom: 1px solid var(--rp-divider); color: var(--rp-text); font-size: 14px; font-weight: 600; }
:global(.history-drawer .el-drawer__body) { padding: 0; }

@media (max-width: 1199px) and (min-width: 960px) {
  .ai-workspace { grid-template-columns: 232px minmax(0, 1fr); }
}

@media (max-width: 959px) {
  .ai-workspace { grid-template-columns: minmax(0, 1fr); }
  .conversation-column { display: none; }
  .history-trigger { display: grid; }
}

@media (max-width: 767px) {
  .chat-header { min-height: 52px; padding: 8px 12px; }
  .chat-status { display: none; }
  .chat-composer { padding: 8px 10px calc(8px + env(safe-area-inset-bottom)); }
  .composer-hint { display: none; }
}
</style>
