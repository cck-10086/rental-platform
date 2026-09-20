<template>
  <div class="ai-chat-container">
    <!-- 中间:会话列表(玻璃卡) -->
    <div class="conversation-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" class="new-chat-btn" @click="handleNewChat">
          <el-icon><Plus /></el-icon> 新建对话
        </el-button>
      </div>
      <div class="conversation-list-label">历史会话</div>
      <div class="conversation-list" ref="conversationListRef">
        <template v-for="group in groupedConversations" :key="group.label">
          <div class="conversation-group-label">{{ group.label }}</div>
          <div
            v-for="conv in group.items"
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
        </template>
        <div v-if="conversations.length === 0" class="conv-empty">
          <p>暂无历史对话</p>
          <p class="conv-empty-hint">在右侧输入问题开始咨询</p>
        </div>
      </div>
    </div>

    <!-- 右侧:聊天工作区(玻璃卡) -->
    <div class="chat-main">
      <div class="chat-header">
        <div class="chat-header-icon">
          <el-icon :size="16" color="#2563EB"><Service /></el-icon>
        </div>
        <div class="chat-header-text">
          <h2 class="chat-title">AI 租房法律顾问</h2>
          <p class="chat-subtitle">基于 DeepSeek 大模型 · 专业租房法律咨询</p>
        </div>
        <div class="chat-status">
          <span class="status-dot"></span>在线
        </div>
      </div>

      <div ref="chatBodyRef" class="chat-body">
        <el-scrollbar ref="scrollbarRef" height="100%" @scroll="handleChatScroll">
          <div class="message-list">
            <!-- 空状态:玻璃卡 + 示例问题快捷入口 -->
            <div v-if="messages.length === 0" class="welcome-card">
              <div class="welcome-icon">
                <el-icon :size="26" color="#2563EB"><Service /></el-icon>
              </div>
              <p class="welcome-text">{{ welcomeMessage }}</p>
            </div>

            <template v-else>
              <!-- 日期分隔(shadcn Marker 模式) -->
              <div v-for="(day, di) in messagesByDay" :key="di" class="day-group">
                <div class="day-marker"><span>{{ day.label }}</span></div>
                <div
                  v-for="(msg, index) in day.items"
                  :key="index"
                  class="message-item"
                  :class="msg.role === 'user' ? 'message-right' : 'message-left'"
                >
                  <div class="message-avatar" :class="msg.role === 'user' ? 'avatar-user' : 'avatar-ai'">
                    <el-icon v-if="msg.role === 'user'" :size="15"><User /></el-icon>
                    <span v-else class="avatar-text">AI</span>
                  </div>
                  <div class="message-body">
                    <div class="message-bubble" :class="msg.role === 'user' ? 'bubble-user' : 'bubble-ai'">
                      <template v-if="msg.role === 'user'">{{ msg.content }}</template>
                      <template v-else>
                        <div class="markdown-content" v-html="renderMarkdown(msg.content)"></div>
                        <!-- 流式输出中:最后一条 AI 消息显示打字光标 -->
                        <span v-if="isStreamingTarget(msg, index, day)" class="stream-cursor"></span>
                      </template>
                    </div>
                    <div class="message-time">{{ formatTime(msg.time) }}</div>
                  </div>
                </div>
              </div>
            </template>

            <!-- 等待首段增量时显示打字动画,首字到达后由流式气泡接管 -->
            <div v-if="sending && waitingFirst" class="message-item message-left">
              <div class="message-avatar avatar-ai">
                <span class="avatar-text">AI</span>
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
        <!-- 回到底部悬浮按钮:用户上滚离开底部时出现 -->
        <transition name="fade">
          <button
            v-if="showBackToBottom"
            class="back-to-bottom"
            title="回到底部"
            @click="scrollToBottom()"
          >
            <el-icon :size="14"><ArrowDownBold /></el-icon>
          </button>
        </transition>
      </div>

      <div class="chat-footer">
        <div class="input-area">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="3"
            placeholder="请输入您的问题,按 Enter 发送…"
            resize="none"
            @keydown.enter.exact.prevent="handleSend"
          />
          <el-button
            type="primary"
            class="send-btn"
            :loading="sending"
            :disabled="!inputText.trim()"
            @click="handleSend"
          >
            <el-icon v-if="!sending"><Promotion /></el-icon>
            <span v-if="sending">发送中</span>
          </el-button>
        </div>
        <!-- 快捷问题常驻输入区:随时一键填入 -->
        <div class="quick-questions">
          <button
            v-for="q in quickQuestions"
            :key="q"
            class="quick-chip"
            :class="{ used: inputText === q }"
            @click="inputText = inputText === q ? '' : q"
          >{{ q }}</button>
        </div>
        <p class="input-hint">回答由 AI 生成,仅供参考,重要事项请咨询专业律师</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Promotion, Plus, Delete, ArrowDownBold } from '@element-plus/icons-vue'
import { aiChatStream, getConversations, getConversationDetail, deleteConversation } from '@/api/ai'

const welcomeMessage = '您好!我是您的 AI 租房顾问,您可以向我咨询任何租房相关的法律问题。'

const quickQuestions = [
  '押金纠纷如何处理?',
  '不续租提前多久通知?',
  '房东私闯违法吗?'
]

const messages = ref([])
const inputText = ref('')
const sending = ref(false)
const waitingFirst = ref(false)
const conversationId = ref('')
const conversations = ref([])
const chatBodyRef = ref(null)
const scrollbarRef = ref(null)
const showBackToBottom = ref(false)
const conversationListRef = ref(null)

/* —— 会话按时间分组:今天 / 近 7 天 / 更早 —— */
const groupedConversations = computed(() => {
  const now = new Date()
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const sevenDaysAgo = startOfToday - 6 * 24 * 3600 * 1000
  const groups = [
    { label: '今天', min: startOfToday, items: [] },
    { label: '近 7 天', min: sevenDaysAgo, items: [] },
    { label: '更早', min: -Infinity, items: [] }
  ]
  for (const conv of conversations.value) {
    const t = new Date(conv.lastTime).getTime() || 0
    const bucket = groups.find(g => t >= g.min)
    bucket.items.push(conv)
  }
  return groups.filter(g => g.items.length > 0)
})

/* —— 消息按日期分组(聊天区日期分隔线) —— */
const messagesByDay = computed(() => {
  const dayMap = new Map()
  for (const msg of messages.value) {
    const label = formatDayLabel(msg.time)
    if (!dayMap.has(label)) dayMap.set(label, [])
    dayMap.get(label).push(msg)
  }
  return Array.from(dayMap, ([label, items]) => ({ label, items }))
})

/* 是否为流式输出目标气泡:发送中且是最后一条 AI 消息时显示打字光标 */
const isStreamingTarget = (msg, _index, day) => {
  if (!sending.value || waitingFirst.value) return false
  const all = messages.value
  const last = all[all.length - 1]
  return msg === last && day.items[day.items.length - 1] === msg
}

const formatDayLabel = (time) => {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  const key = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  const today = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
  const yesterday = new Date(now.getTime() - 24 * 3600 * 1000)
  const yKey = `${yesterday.getFullYear()}-${pad(yesterday.getMonth() + 1)}-${pad(yesterday.getDate())}`
  if (key === today) return '今天'
  if (key === yKey) return '昨天'
  return key
}

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

/* 监听滚动:距底部超过 80px 时显示"回到底部"按钮(用户上滚即让位,不打断阅读) */
const handleChatScroll = () => {
  const scrollbar = scrollbarRef.value
  if (!scrollbar?.wrapRef) return
  const { scrollTop, scrollHeight, clientHeight } = scrollbar.wrapRef
  showBackToBottom.value = scrollHeight - scrollTop - clientHeight > 80
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
    await ElMessageBox.confirm('确定删除该对话吗?删除后不可恢复。', '提示', {
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
  waitingFirst.value = true
  let aiMessage = null
  try {
    await aiChatStream(
      {
        question: text,
        conversationId: conversationId.value || undefined
      },
      {
        // 会话开始:记录后端下发的会话 ID,后续追问可延续上下文
        onMeta: (id) => {
          conversationId.value = id || conversationId.value
        },
        // 增量内容:首段到达时创建 AI 消息气泡,其后逐字追加(打字机效果)
        onDelta: (delta) => {
          waitingFirst.value = false
          if (!aiMessage) {
            aiMessage = { role: 'ai', content: '', time: Date.now() }
            messages.value.push(aiMessage)
          }
          aiMessage.content += delta
          scrollToBottom()
        },
        // 回答完成并已落库,刷新左侧会话列表
        onDone: () => {
          loadConversations()
        },
        onError: (message) => {
          ElMessage.error(message || 'AI回复失败,请稍后重试')
        }
      }
    )
    // 流正常结束但没有任何增量(如服务端返回空),给出兜底提示
    if (!aiMessage) {
      messages.value.push({
        role: 'ai',
        content: '抱歉,我暂时无法回答您的问题。',
        time: Date.now()
      })
    }
  } catch {
    ElMessage.error('AI回复失败,请稍后重试')
  } finally {
    sending.value = false
    waitingFirst.value = false
    scrollToBottom()
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
  height: calc(100vh - 60px - 48px);
  gap: 16px;
}

/* —— 中间:会话列表玻璃卡 —— */
.conversation-sidebar {
  width: 280px;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid #E2E8F0;
  border-radius: 14px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: 14px 14px 10px;
}

.new-chat-btn {
  width: 100%;
  border-radius: 10px;
  font-weight: 500;
}

.conversation-list-label {
  display: none;
}

/* 会话分组标签 */
.conversation-group-label {
  font-size: 12px;
  color: #64748B;
  font-weight: 500;
  padding: 10px 8px 6px;
}

.conversation-group-label:first-child {
  padding-top: 2px;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 10px 12px;
}

.conversation-item {
  position: relative;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  margin-bottom: 2px;
  border: 1px solid transparent;
  transition: background 180ms cubic-bezier(0.16, 1, 0.3, 1), border-color 180ms cubic-bezier(0.16, 1, 0.3, 1);
}

.conversation-item:hover {
  background: #F8FAFC;
  border-color: #E2E8F0;
}

.conversation-item.active {
  background: #EFF6FF;
  border-color: rgba(37, 99, 235, 0.15);
  border-left: 3px solid #2563EB;
}

.conv-title {
  font-size: 13px;
  color: #1E293B;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-right: 24px;
}

.conversation-item.active .conv-title {
  color: #1D4ED8;
  font-weight: 500;
}

.conv-meta {
  margin-top: 4px;
  font-size: 11px;
  color: #64748B;
  display: flex;
  justify-content: space-between;
}

.conversation-item.active .conv-meta {
  color: #64748B;
}

.conv-delete {
  position: absolute;
  right: 6px;
  top: 8px;
}

.conv-empty {
  text-align: center;
  padding: 40px 16px;
  color: #64748B;
  font-size: 13px;
}

.conv-empty-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #94A3B8;
}

/* —— 右侧:聊天工作区玻璃卡 —— */
.chat-main {
  flex: 1;
  min-width: 0;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  border-radius: 14px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid #F1F5F9;
}

.chat-header-icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: #EFF6FF;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.chat-title {
  margin: 0;
  font-size: 14px;
  color: #1E293B;
  font-weight: 600;
}

.chat-subtitle {
  margin: 2px 0 0;
  font-size: 12px;
  color: #64748B;
}

/* 在线状态点 */
.chat-status {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #059669;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #059669;
  position: relative;
}

.status-dot::after {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  border: 1px solid rgba(5, 150, 105, 0.35);
  animation: status-pulse 2.4s ease-out infinite;
}

@keyframes status-pulse {
  0% { transform: scale(0.6); opacity: 1; }
  70%, 100% { transform: scale(1.6); opacity: 0; }
}

.chat-body {
  flex: 1;
  overflow: hidden;
  background: #F8FAFC;
}

.message-list {
  padding: 20px;
}

/* 日期分隔(shadcn Marker separator 模式:两侧细线 + 居中标签) */
.day-group {
  display: flex;
  flex-direction: column;
}

.day-marker {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 8px 0 16px;
}

.day-marker::before,
.day-marker::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #E2E8F0;
}

.day-marker span {
  font-size: 11px;
  color: #64748B;
  background: #F1F5F9;
  border-radius: 999px;
  padding: 2px 12px;
}

/* —— 空状态 —— */
.welcome-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 40px 40px;
  text-align: center;
  color: #475569;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid #E2E8F0;
  border-radius: 14px;
  max-width: 560px;
  margin: 24px auto;
  animation: rp-fade-up 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

.welcome-icon {
  width: 52px;
  height: 52px;
  border-radius: 15px;
  background: #EFF6FF;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.welcome-text {
  font-size: 13px;
  line-height: 1.8;
  color: #475569;
  max-width: 400px;
}

/* 快捷问题 chip:常驻输入区 */
.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 9px;
}

.quick-chip {
  border: 1px solid #E2E8F0;
  background: #FFFFFF;
  color: #2563EB;
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 999px;
  cursor: pointer;
  font-family: inherit;
  transition: all 180ms cubic-bezier(0.16, 1, 0.3, 1);
}

.quick-chip:hover {
  background: #EFF6FF;
  border-color: rgba(37, 99, 235, 0.3);
}

.quick-chip.used {
  background: #2563EB;
  border-color: #2563EB;
  color: #fff;
}

.quick-chip:active {
  transform: scale(0.97);
}

/* —— 消息气泡 —— */
.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
  animation: rp-fade-up 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.message-item.message-right {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-user {
  background: #2563EB;
  color: #fff;
}

.avatar-ai {
  background: #EFF6FF;
  color: #2563EB;
  font-size: 11px;
  font-weight: 600;
}

/* 流式打字光标 */
.stream-cursor {
  display: inline-block;
  width: 2px;
  height: 14px;
  background: #2563EB;
  margin-left: 2px;
  vertical-align: -2px;
  animation: cursor-blink 1s steps(2) infinite;
}

@keyframes cursor-blink {
  0% { opacity: 1; }
  50% { opacity: 0; }
}

.message-body {
  max-width: 72%;
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
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.bubble-user {
  background: #2563EB;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.bubble-ai {
  background: #fff;
  color: #334155;
  border: 1px solid #E2E8F0;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.message-time {
  font-size: 11px;
  color: #64748B;
  margin-top: 4px;
}

.markdown-content code {
  background: #F1F5F9;
  padding: 2px 6px;
  border-radius: 5px;
  font-size: 13px;
  color: #1D4ED8;
}

.markdown-content strong {
  color: #1E293B;
}

/* —— 打字指示 —— */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 14px 16px;
}

.typing-indicator .dot {
  width: 6px;
  height: 6px;
  background: #2563EB;
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
    opacity: 0.35;
  }
  30% {
    transform: translateY(-5px);
    opacity: 1;
  }
}

/* 回到底部悬浮按钮 */
.back-to-bottom {
  position: absolute;
  right: 20px;
  bottom: 16px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #FFFFFF;
  border: 1px solid #E2E8F0;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #475569;
  transition: all 180ms cubic-bezier(0.16, 1, 0.3, 1);
  z-index: 5;
}

.back-to-bottom:hover {
  color: #2563EB;
  border-color: rgba(37, 99, 235, 0.35);
  transform: translateY(-1px);
}

.chat-body {
  position: relative;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 180ms cubic-bezier(0.16, 1, 0.3, 1), transform 180ms cubic-bezier(0.16, 1, 0.3, 1);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

/* —— 输入区 —— */
.chat-footer {
  background: #FFFFFF;
  border-top: 1px solid #F1F5F9;
  padding: 14px 18px 12px;
}

.input-area {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.input-area :deep(.el-textarea__inner) {
  border-radius: 12px;
  border-color: #E2E8F0;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
}

.input-area :deep(.el-textarea__inner:focus) {
  border-color: #2563EB;
}

.send-btn {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  flex-shrink: 0;
  font-size: 13px;
  padding: 0;
}

.input-hint {
  margin: 8px 2px 0;
  font-size: 11px;
  color: #94A3B8;
}

/* 窄屏:会话列表收窄 */
@media (max-width: 900px) {
  .conversation-sidebar { width: 200px; }
  .message-body { max-width: 85%; }
}
</style>
