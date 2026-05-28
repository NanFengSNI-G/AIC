<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useForumStore } from '@/stores/forum'
import { useAuthStore } from '@/stores/auth'
import notification from '@/utils/notification'

const route = useRoute()
const router = useRouter()
const forumStore = useForumStore()
const authStore = useAuthStore()

const postId = Number(route.params.id)

const title = ref('')
const content = ref('')
const contentTextarea = ref<HTMLTextAreaElement | null>(null)
const sectionId = ref<number | null>(null)
const submitting = ref(false)
const loading = ref(true)

// 图片对话框
const showImageDialog = ref(false)
const imageDialogTab = ref<'upload' | 'url'>('upload')
const imageUrlInput = ref('')
const uploadFile = ref<File | null>(null)
const uploadPreview = ref('')
const uploading = ref(false)

onMounted(async () => {
  try {
    const post = await forumStore.fetchPostById(postId)

    title.value = post.title
    content.value = post.content
    sectionId.value = post.sectionId

    if (forumStore.sections.length === 0) {
      await forumStore.fetchSections()
    }
  } catch (e) {
    console.error('Failed to load post:', e)
    notification.error('加载帖子失败', { title: '加载失败' })
    router.back()
  } finally {
    loading.value = false
  }
})

const handleSubmit = async () => {
  if (!title.value.trim() || !content.value.trim() || !sectionId.value) return

  submitting.value = true
  try {
    await forumStore.updatePost(postId, {
      title: title.value,
      content: content.value,
      images: []
    })
    notification.success('帖子更新成功', { title: '更新成功' })
    router.replace(`/post/${postId}`)
  } catch (e: any) {
    notification.error(e.message || '更新失败，请重试', { title: '更新失败' })
  } finally {
    submitting.value = false
  }
}

// ── 图片插入 ──

const openImageDialog = () => {
  showImageDialog.value = true
  imageDialogTab.value = 'upload'
  imageUrlInput.value = ''
  uploadFile.value = null
  uploadPreview.value = ''
}

const closeImageDialog = () => {
  showImageDialog.value = false
}

const handleFileChange = (e: Event) => {
  const input = e.target as HTMLInputElement
  if (input.files && input.files[0]) {
    uploadFile.value = input.files[0]
    uploadPreview.value = URL.createObjectURL(uploadFile.value)
  }
}

const insertImageToContent = (url: string) => {
  const md = `![image](${url})`
  const textarea = contentTextarea.value
  if (textarea) {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    content.value = content.value.substring(0, start) + md + content.value.substring(end)
    nextTick(() => {
      textarea.selectionStart = textarea.selectionEnd = start + md.length
      textarea.focus()
    })
  } else {
    content.value += '\n' + md
  }
}

const handleUploadFile = async () => {
  if (!uploadFile.value) return

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadFile.value)

    const response = await fetch('/api/images/upload', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      },
      body: formData
    })

    const result = await response.json()
    if (result.code === 200 && result.data?.url) {
      insertImageToContent(result.data.url)
      closeImageDialog()
      notification.success('图片已插入', { title: '上传成功' })
    } else {
      notification.error(result.message || '上传失败', { title: '上传失败' })
    }
  } catch (e: any) {
    notification.error('图片上传失败: ' + e.message, { title: '上传失败' })
  } finally {
    uploading.value = false
  }
}

const confirmUrlImage = () => {
  const url = imageUrlInput.value.trim()
  if (!url) return

  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('/')) {
    insertImageToContent(url)
    closeImageDialog()
    notification.success('图片已插入', { title: '添加成功' })
  } else {
    notification.error('请输入有效的URL（以 http://、https:// 或 / 开头）', { title: '添加失败' })
  }
}

const handleInsertMarkdown = (syntax: string, placeholder: string) => {
  const textarea = contentTextarea.value
  if (textarea) {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const selected = content.value.substring(start, end) || placeholder
    const md = syntax.replace('$1', selected)
    content.value = content.value.substring(0, start) + md + content.value.substring(end)
    nextTick(() => {
      const cursor = start + syntax.indexOf('$1') + (end > start ? selected.length : 0)
      textarea.selectionStart = textarea.selectionEnd = end > start ? start + md.length : cursor
      textarea.focus()
    })
  } else {
    content.value += '\n' + syntax.replace('$1', placeholder)
  }
}
</script>

<template>
  <div class="max-w-2xl mx-auto">
    <!-- Loading -->
    <div v-if="loading" class="space-y-4">
      <div class="h-8 bg-stone-200 rounded animate-pulse"></div>
      <div class="h-10 bg-stone-200 rounded animate-pulse"></div>
      <div class="h-64 bg-stone-200 rounded animate-pulse"></div>
    </div>

    <div v-else>
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-xl font-bold">编辑帖子</h1>
        <button @click="router.back()" class="text-text-secondary hover:text-text-primary">取消</button>
      </div>

      <form @submit.prevent="handleSubmit" class="space-y-4">
        <!-- Section Select -->
        <div>
          <label class="block text-sm font-medium mb-2">选择板块</label>
          <select
            v-model="sectionId"
            class="w-full px-3 py-2 bg-bg-tertiary rounded-lg text-text-primary focus:outline-none focus:ring-1 focus:ring-accent-primary"
          >
            <option :value="null" disabled>请选择板块</option>
            <option v-for="section in forumStore.sections" :key="section.id" :value="section.id">
              {{ section.name }}
            </option>
          </select>
        </div>

        <!-- Title -->
        <div>
          <label class="block text-sm font-medium mb-2">标题</label>
          <input
            v-model="title"
            type="text"
            placeholder="给帖子取个标题"
            class="w-full px-3 py-2 bg-bg-tertiary rounded-lg text-text-primary placeholder-text-tertiary focus:outline-none focus:ring-1 focus:ring-accent-primary"
          />
        </div>

        <!-- Content -->
        <div>
          <div class="flex items-center justify-between mb-2">
            <label class="text-sm font-medium">内容</label>
          </div>

          <!-- Markdown 工具栏 -->
          <div class="flex items-center gap-0.5 px-2 py-1.5 bg-stone-50 rounded-t-lg border border-b-0 border-stone-200">
            <button type="button" @click="handleInsertMarkdown('**$1**', '加粗文字')" class="px-2 py-1 text-xs font-bold text-stone-600 hover:bg-stone-200 rounded transition-colors" title="加粗">B</button>
            <button type="button" @click="handleInsertMarkdown('*$1*', '斜体文字')" class="px-2 py-1 text-xs italic text-stone-600 hover:bg-stone-200 rounded transition-colors" title="斜体">I</button>
            <button type="button" @click="handleInsertMarkdown('`$1`', '代码')" class="px-2 py-1 text-xs font-mono text-stone-600 hover:bg-stone-200 rounded transition-colors" title="行内代码">&lt;/&gt;</button>
            <span class="w-px h-4 bg-stone-300 mx-0.5"></span>
            <button type="button" @click="handleInsertMarkdown('### $1', '小标题')" class="px-2 py-1 text-xs text-stone-600 hover:bg-stone-200 rounded transition-colors" title="标题">H</button>
            <button type="button" @click="handleInsertMarkdown('- $1', '列表项')" class="px-2 py-1 text-xs text-stone-600 hover:bg-stone-200 rounded transition-colors" title="列表">≡</button>
            <button type="button" @click="handleInsertMarkdown('> $1', '引用')" class="px-2 py-1 text-xs text-stone-600 hover:bg-stone-200 rounded transition-colors" title="引用">❝</button>
            <button type="button" @click="handleInsertMarkdown('[链接文字](url)', '链接文字')" class="px-2 py-1 text-xs text-stone-600 hover:bg-stone-200 rounded transition-colors" title="链接">🔗</button>
            <button type="button" @click="handleInsertMarkdown('```\n$1\n```', '代码块')" class="px-2 py-1 text-xs font-mono text-stone-600 hover:bg-stone-200 rounded transition-colors" title="代码块">{ }</button>
            <span class="w-px h-4 bg-stone-300 mx-0.5"></span>
            <button type="button" @click="openImageDialog" class="px-2 py-1 text-xs text-amber-600 hover:bg-amber-50 rounded transition-colors font-medium" title="插入图片">🖼 图片</button>
          </div>

          <textarea
            ref="contentTextarea"
            v-model="content"
            placeholder="分享你的想法...（支持 Markdown 格式）&#10;点击工具栏 🖼图片 按钮上传并插入图片"
            rows="12"
            class="w-full px-3 py-2 bg-bg-tertiary rounded-b-lg rounded-t-none text-text-primary placeholder-text-tertiary resize-none focus:outline-none focus:ring-1 focus:ring-accent-primary"
          />
        </div>

        <!-- Submit -->
        <button
          type="submit"
          :disabled="submitting || !title.trim() || !content.trim() || !sectionId"
          class="w-full py-3 bg-accent-primary hover:bg-accent-hover disabled:opacity-50 disabled:cursor-not-allowed text-white font-medium rounded-lg transition-colors"
        >
          {{ submitting ? '保存中...' : '保存修改' }}
        </button>
      </form>

      <!-- 图片插入对话框 -->
      <div
        v-if="showImageDialog"
        class="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
        @click="closeImageDialog"
      >
        <div
          class="bg-white rounded-xl p-6 w-full max-w-md mx-4 shadow-xl"
          @click.stop
        >
          <h3 class="text-lg font-semibold mb-4">插入图片</h3>

          <!-- 标签切换 -->
          <div class="flex border-b border-stone-200 mb-4">
            <button
              type="button"
              @click="imageDialogTab = 'upload'"
              :class="imageDialogTab === 'upload' ? 'border-amber-500 text-amber-600' : 'border-transparent text-stone-500 hover:text-stone-700'"
              class="px-3 py-2 text-sm font-medium border-b-2 transition-colors"
            >本地上传</button>
            <button
              type="button"
              @click="imageDialogTab = 'url'"
              :class="imageDialogTab === 'url' ? 'border-amber-500 text-amber-600' : 'border-transparent text-stone-500 hover:text-stone-700'"
              class="px-3 py-2 text-sm font-medium border-b-2 transition-colors"
            >输入URL</button>
          </div>

          <!-- 上传文件 -->
          <div v-if="imageDialogTab === 'upload'" class="space-y-3">
            <label
              class="flex flex-col items-center justify-center gap-2 h-40 border-2 border-dashed border-stone-300 rounded-xl cursor-pointer hover:border-amber-400 hover:bg-amber-50/30 transition-colors"
            >
              <template v-if="uploadPreview">
                <img :src="uploadPreview" class="max-h-32 max-w-full object-contain rounded" />
                <span class="text-xs text-stone-400">点击更换图片</span>
              </template>
              <template v-else>
                <span class="text-3xl text-stone-300">🖼</span>
                <span class="text-sm text-stone-400">点击选择图片文件</span>
                <span class="text-xs text-stone-300">支持 JPG、PNG、GIF、WebP</span>
              </template>
              <input type="file" accept="image/*" class="hidden" @change="handleFileChange" />
            </label>

            <div class="flex gap-2 justify-end">
              <button
                type="button"
                @click="closeImageDialog"
                class="px-4 py-2 text-sm text-stone-500 hover:text-stone-700 transition-colors"
              >取消</button>
              <button
                type="button"
                @click="handleUploadFile"
                :disabled="!uploadFile || uploading"
                class="px-4 py-2 bg-amber-500 hover:bg-amber-600 disabled:bg-amber-300 text-white rounded-lg text-sm font-medium transition-colors"
              >
                {{ uploading ? '上传中...' : '上传并插入' }}
              </button>
            </div>
          </div>

          <!-- 输入URL -->
          <div v-if="imageDialogTab === 'url'" class="space-y-3">
            <input
              v-model="imageUrlInput"
              type="text"
              placeholder="粘贴图片URL（http:// 或 https:// 开头）"
              class="w-full px-3 py-2 bg-stone-50 rounded-lg text-sm focus:outline-none focus:ring-1 focus:ring-amber-500"
              @keyup.enter="confirmUrlImage"
            />
            <div class="flex gap-2 justify-end">
              <button
                type="button"
                @click="closeImageDialog"
                class="px-4 py-2 text-sm text-stone-500 hover:text-stone-700 transition-colors"
              >取消</button>
              <button
                type="button"
                @click="confirmUrlImage"
                :disabled="!imageUrlInput.trim()"
                class="px-4 py-2 bg-amber-500 hover:bg-amber-600 disabled:bg-amber-300 text-white rounded-lg text-sm font-medium transition-colors"
              >插入</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
