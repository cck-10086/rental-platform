<template>
  <div class="ai-chat-container">
    <!-- 左侧会话栏 -->
    <div class="conversation-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" class="new-chat-btn" @click="handleNewChat">
          <el-icon><Plus /></el-icon> 新建对话
        </el-button>
      </div>
      <div class="conversation-list">
        <div
          v-for="conv in conversations"
          :key="conv.conversationId"
          class="conversation-item"
          :class="{ active: conv.conversationId === conversationId }"
          @click="handleSelectConversation(conv)"
        >
          <div class="conv-title">{{ conv.title || '未命名对话' }}</div>
          <div class="conv-meta">
            <span>{{ conv.messageCount }} 条</span>
            <span>{{ formatTime(conv.lastTime) }}</span>
          </div>
          <el-button
            v-if="conv.conversationId === conversationId"
            class="conv-delete"
            link
            type="danger"
            size="small"
            @click.stop="handleDeleteConversation(conv)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <el-empty v-if="conversations.length === 0" description="暂无历史对话" :image-size="60" />
      </div>
    </div>

    <!-- 右侧聊天区 -->
    <div class="chat-main">
      <div class="chat-header">
        <h2 class="chat-title">AI 租房法律顾问</h2>
        <p class="chat-subtitle">基于DeepSeek大模型，为您提供专业的租房法律咨询</p>
      </div>

      <div ref="chatBodyRef" class="chat-body">
        <el-scrollbar ref="scrollbarRef" height="100%">
          <div class="message-list">
            <div v-if="messages.length === 0" class="welcome-card">
              <el-icon :size="48" color="#409eff"><Service /></el-icon>
              <p>{{ welcomeMessage }}</p>
            </div>

            <div
              v-for="(msg, index) in messages"
              :key="index"
              class="message-item"
              :class="msg.role === 'user' ? 'message-right' : 'message-left'"
            >
              <div class="message-avatar">
                <el-avatar v-if="msg.role === 'user'" :size="36" :icon="User" />
                <el-avatar v-else :size="36" :icon="Service" />
              </div>
              <div class="message-body">
                <div class="message-bubble" :class="msg.role === 'user' ? 'bubble-user' : 'bubble-ai'">
                  <template v-if="msg.role === 'user'">{{ msg.content }}</template>
                  <template v-else>
                    <div class="markdown-content" v-html="renderMarkdown(msg.content)"></div>
                  </template>
                </div>
                <div class="message-time">{{ formatTime(msg.time) }}</div>
              </div>
            </div>

            <div v-if="sending" class="message-item message-left">
              <div class="message-avatar">
                <el-avatar :size="36" :icon="Service" />
              </div>
              <div class="message-body">
                <div class="message-bubble bubble-ai typing-indicator">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                </div>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>

      <div class="chat-footer">
        <div class="input-area">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="3"
            placeholder="请输入您的问题，按Enter发送..."
            resize="none"
            @keydown.enter.exact.prevent="handleSend"
          />
          <el-button type="primary" :loading="sending" :disabled="!inputText.trim()" @click="handleSend">
            <el-icon><Promotion /></el-icon> 发送
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Service, Promotion, Plus, Delete } from '@element-plus/icons-vue'
import { aiChat, getConversations, getConversationDetail, deleteConversation } from '@/api/ai'

const welcomeMessage = '您好！我是您的AI租房顾问，您可以向我咨询任何租房相关的法律问题，比如：押金纠纷如何处理？合同到期不续租需要提前多久通知？房东私自进入房间是否违法？'

const messages = ref([])
const inputText = ref('')
const sending = ref(false)
const conversationId = ref('')
const conversations = ref([])
const chatBodyRef = ref(null)
const scrollbarRef = ref(null)

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const renderMarkdown = (text) => {
  if (!text) return ''
  let html = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br/>')
  return html
}

const scrollToBottom = () => {
  nextTick(() => {
    const scrollbar = scrollbarRef.value
    if (scrollbar) {
      scrollbar.setScrollTop(scrollbar.wrapRef.scrollHeight)
    }
  })
}

const loadConversations = async () => {
  try {
    const res = await getConversations()
    conversations.value = res.data || []
  } catch {
    // 错误已在拦截器中处理
  }
}

const handleNewChat = () => {
  messages.value = []
  conversationId.value = ''
  inputText.value = ''
  scrollToBottom()
}

const handleSelectConversation = async (conv) => {
  conversationId.value = conv.conversationId
  try {
    const res = await getConversationDetail(conv.conversationId)
    messages.value = (res.data || []).map((m) => ({
      role: m.role,
      content: m.content,
      time: m.time
    }))
    scrollToBottom()
  } catch {
    // 错误已在拦截器中处理
  }
}

const handleDeleteConversation = async (conv) => {
  try {
    await ElMessageBox.confirm('确定删除该对话吗？删除后不可恢复。', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await deleteConversation(conv.conversationId)
    ElMessage.success('对话已删除')
    conversations.value = conversations.value.filter(c => c.conversationId !== conv.conversationId)
    if (conversationId.value === conv.conversationId) {
      handleNewChat()
    }
  } catch {
    // 错误已在拦截器中处理
  }
}

const handleSend = async () => {
  const text = inputText.value.trim()
  if (!text || sending.value) return

  messages.value.push({
    role: 'user',
    content: text,
    time: Date.now()
  })
  inputText.value = ''
  scrollToBottom()

  sending.value = true
  try {
    const res = await aiChat({
      question: text,
      conversationId: conversationId.value || undefined
    })
    const data = res.data || res
    conversationId.value = data.conversationId || conversationId.value
    const reply = data.answer || '抱歉，我暂时无法回答您的问题。'
    messages.value.push({
      role: 'ai',
      content: reply,
      time: Date.now()
    })
    scrollToBottom()
    loadConversations()
  } catch {
    ElMessage.error('AI回复失败，请稍后重试')
  } finally {
    sending.value = false
  }
}

onMounted(() => {
  loadConversations()
  scrollToBottom()
})
</script>

<style scoped>
.ai-chat-container {
  display: flex;
  height: calc(100vh - 100px);
  max-width: 1200px;
  margin: 0 auto;
  background: #f5f6fa;
  border-radius: 12px;
  overflow: hidden;
}

/* 左侧会话栏 */
.conversation-sidebar {
  width: 260px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
}

.new-chat-btn {
  width: 100%;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conversation-item {
  position: relative;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
}

.conversation-item:hover {
  background: #f5f7fa;
}

.conversation-item.active {
  background: #ecf5ff;
}

.conv-title {
  font-size: 13px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-right: 24px;
}

.conv-meta {
  margin-top: 4px;
  font-size: 11px;
  color: #909399;
  display: flex;
  justify-content: space-between;
}

.conv-delete {
  position: absolute;
  right: 6px;
  top: 8px;
}

/* 右侧聊天区 */
.chat-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.chat-header {
  text-align: center;
  padding: 20px 24px 12px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.chat-title {
  margin: 0;
  font-size: 20px;
  color: #303133;
  font-weight: 600;
}

.chat-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #909399;
}

.chat-body {
  flex: 1;
  overflow: hidden;
  background: #f5f6fa;
}

.message-list {
  padding: 20px;
}

.welcome-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 40px;
  text-align: center;
  color: #606266;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.welcome-card p {
  margin-top: 16px;
  font-size: 14px;
  line-height: 1.8;
  max-width: 500px;
}

.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.message-item.message-right {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
}

.message-body {
  max-width: 70%;
  display: flex;
  flex-direction: column;
}

.message-right .message-body {
  align-items: flex-end;
}

.message-left .message-body {
  align-items: flex-start;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.bubble-user {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.bubble-ai {
  background: #fff;
  color: #303133;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.message-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}

.markdown-content code {
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  color: #e6a23c;
}

.markdown-content strong {
  color: #303133;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 16px;
}

.typing-indicator .dot {
  width: 8px;
  height: 8px;
  background: #c0c4cc;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator .dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-6px);
    opacity: 1;
  }
}

.chat-footer {
  background: #fff;
  border-top: 1px solid #ebeef5;
  padding: 16px 20px;
}

.input-area {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-area :deep(.el-textarea__inner) {
  border-radius: 8px;
}

.input-area .el-button {
  height: 40px;
  flex-shrink: 0;
  border-radius: 8px;
}
</style>
