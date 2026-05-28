<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const isVisible = ref(false)
const statsVisible = ref(false)

onMounted(() => {
  setTimeout(() => {
    isVisible.value = true
  }, 100)

  const observer = new IntersectionObserver(
    (entries) => {
      if (entries[0].isIntersecting) {
        statsVisible.value = true
      }
    },
    { threshold: 0.3 }
  )

  const statsSection = document.getElementById('stats-section')
  if (statsSection) observer.observe(statsSection)
})

const navigateToForum = () => {
  router.push('/forum')
}

const navigateToInterview = () => {
  router.push('/interview')
}

const navigateToLogin = () => {
  router.push('/auth/login')
}
</script>

<template>
  <!-- Hero Section -->
  <section class="relative overflow-hidden">
    <!-- Background decorations -->
      <div class="absolute inset-0 overflow-hidden pointer-events-none">
        <div class="absolute -top-40 -right-40 w-96 h-96 bg-amber-100 rounded-full blur-3xl opacity-50"></div>
        <div class="absolute top-60 -left-20 w-72 h-72 bg-orange-100 rounded-full blur-3xl opacity-40"></div>
        <!-- Grid pattern -->
        <div class="absolute inset-0 opacity-[0.015]" style="background-image: linear-gradient(#D97706 1px, transparent 1px), linear-gradient(90deg, #D97706 1px, transparent 1px); background-size: 60px 60px;"></div>
      </div>

      <div class="relative max-w-6xl mx-auto px-6">
        <div class="grid lg:grid-cols-2 gap-16 items-center">
          <!-- Left: Text content -->
          <div :class="['transition-all duration-700', isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8']">
            <div class="inline-flex items-center gap-2 px-3 py-1 bg-amber-50 border border-amber-200 rounded-full mb-6">
              <span class="w-2 h-2 bg-amber-500 rounded-full animate-pulse"></span>
              <span class="text-sm text-amber-700 font-medium">AI 赋能社区</span>
            </div>

            <h1 class="text-5xl lg:text-6xl font-bold text-stone-900 leading-[1.1] mb-6">
              连接思想<br/>
              <span class="text-amber-600">激发对话</span>
            </h1>

            <p class="text-xl text-stone-500 leading-relaxed mb-8 max-w-lg">
              一个充满活力的社区平台，在这里你可以分享见解、提出问题、结识志同道合的伙伴。配合 AI 模拟面试，助你职业成长。
            </p>

            <div class="flex flex-wrap gap-4">
              <button
                @click="navigateToForum"
                class="px-6 py-3 bg-stone-900 hover:bg-stone-800 text-white font-medium rounded-xl transition-all hover:shadow-lg hover:-translate-y-0.5"
              >
                探索论坛
              </button>
              <button
                @click="navigateToInterview"
                class="px-6 py-3 bg-amber-500 hover:bg-amber-600 text-white font-medium rounded-xl transition-all hover:shadow-lg hover:-translate-y-0.5"
              >
                AI 模拟面试
              </button>
            </div>
          </div>

          <!-- Right: Visual card -->
          <div :class="['relative transition-all duration-700 delay-200', isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8']">
            <!-- Main card -->
            <div class="relative bg-white rounded-2xl shadow-xl border border-stone-100 p-6 transform rotate-2 hover:rotate-0 transition-transform duration-500">
              <!-- Card header -->
              <div class="flex items-center gap-3 mb-4">
                <div class="w-10 h-10 bg-amber-500 rounded-xl flex items-center justify-center">
                  <span class="text-xl">💬</span>
                </div>
                <div>
                  <div class="font-semibold text-stone-900">热门讨论</div>
                  <div class="text-sm text-stone-400">实时更新</div>
                </div>
              </div>
              <!-- Fake posts -->
              <div class="space-y-3">
                <div class="p-3 bg-stone-50 rounded-xl">
                  <div class="text-sm font-medium text-stone-900">Tailwind CSS 4.0 带来了哪些新特性？</div>
                  <div class="text-xs text-stone-400 mt-1">32 条回复 · 1 小时前</div>
                </div>
                <div class="p-3 bg-stone-50 rounded-xl">
                  <div class="text-sm font-medium text-stone-900">Vue 3.5 响应式系统升级解析</div>
                  <div class="text-xs text-stone-400 mt-1">28 条回复 · 3 小时前</div>
                </div>
                <div class="p-3 bg-stone-50 rounded-xl">
                  <div class="text-sm font-medium text-stone-900">程序员如何高效学习新技术</div>
                  <div class="text-xs text-stone-400 mt-1">45 条回复 · 5 小时前</div>
                </div>
              </div>
            </div>

            <!-- Floating elements -->
            <div class="absolute -top-6 -left-6 bg-white rounded-xl shadow-lg border border-stone-100 p-4 transform -rotate-6">
              <div class="flex items-center gap-2">
                <div class="w-8 h-8 bg-green-100 rounded-lg flex items-center justify-center">
                  <span class="text-sm">✓</span>
                </div>
                <span class="text-sm font-medium text-stone-700">实时互动</span>
              </div>
            </div>

            <div class="absolute -bottom-4 -right-4 bg-white rounded-xl shadow-lg border border-stone-100 p-4 transform rotate-6">
              <div class="flex items-center gap-2">
                <div class="w-8 h-8 bg-amber-100 rounded-lg flex items-center justify-center">
                  <span class="text-sm">👥</span>
                </div>
                <span class="text-sm font-medium text-stone-700">结识伙伴</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section id="features" class="py-20 bg-white">
      <div class="max-w-6xl mx-auto px-6">
        <div class="text-center mb-16">
          <h2 class="text-4xl font-bold text-stone-900 mb-4">为什么选择我们</h2>
          <p class="text-xl text-stone-500">丰富的功能，全面的支持</p>
        </div>

        <div class="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
          <!-- Feature 1 -->
          <div class="group p-6 bg-stone-50 rounded-2xl hover:bg-amber-50 transition-colors">
            <div class="w-12 h-12 bg-amber-500 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <span class="text-2xl">📝</span>
            </div>
            <h3 class="text-xl font-semibold text-stone-900 mb-2">社区论坛</h3>
            <p class="text-stone-500 leading-relaxed">
              分享知识、提出问题、参与讨论。与志同道合的开发者一起成长。
            </p>
          </div>

          <!-- Feature 2 -->
          <div class="group p-6 bg-stone-50 rounded-2xl hover:bg-amber-50 transition-colors">
            <div class="w-12 h-12 bg-amber-500 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <span class="text-2xl">🤖</span>
            </div>
            <h3 class="text-xl font-semibold text-stone-900 mb-2">AI 模拟面试</h3>
            <p class="text-stone-500 leading-relaxed">
              上传简历，输入 JD，AI 面试官实时评估你的表现，助你斩获心仪 Offer。
            </p>
          </div>

          <!-- Feature 3 -->
          <div class="group p-6 bg-stone-50 rounded-2xl hover:bg-amber-50 transition-colors">
            <div class="w-12 h-12 bg-amber-500 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <span class="text-2xl">💬</span>
            </div>
            <h3 class="text-xl font-semibold text-stone-900 mb-2">私信交流</h3>
            <p class="text-stone-500 leading-relaxed">
              与社区中的其他用户一对一私信交流，拓展人脉，建立连接。
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- Stats Section -->
    <section id="stats" class="py-20 bg-stone-900 text-white">
      <div class="max-w-6xl mx-auto px-6">
        <div class="grid md:grid-cols-3 gap-12 text-center">
          <div>
            <div class="text-5xl font-bold text-amber-500 mb-2" :class="{ 'animate-counter': statsVisible }">
              {{ statsVisible ? '10,000+' : '0' }}
            </div>
            <div class="text-stone-400">社区成员</div>
          </div>
          <div>
            <div class="text-5xl font-bold text-amber-500 mb-2">
              {{ statsVisible ? '5,000+' : '0' }}
            </div>
            <div class="text-stone-400">讨论帖子</div>
          </div>
          <div>
            <div class="text-5xl font-bold text-amber-500 mb-2">
              {{ statsVisible ? '98%' : '0' }}
            </div>
            <div class="text-stone-400">用户满意度</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Interview Feature Highlight -->
    <section class="py-20 overflow-hidden">
      <div class="max-w-6xl mx-auto px-6">
        <div class="grid lg:grid-cols-2 gap-16 items-center">
          <!-- Visual -->
          <div class="order-2 lg:order-1">
            <div class="relative">
              <div class="bg-white rounded-2xl shadow-xl border border-stone-100 p-8">
                <div class="flex items-center gap-3 mb-6">
                  <div class="w-10 h-10 bg-green-500 rounded-xl flex items-center justify-center">
                    <span class="text-xl">✓</span>
                  </div>
                  <div>
                    <div class="font-semibold text-stone-900">面试进行中</div>
                    <div class="text-sm text-green-500">AI 正在评估你的回答</div>
                  </div>
                </div>

                <div class="space-y-4">
                  <div class="flex justify-end">
                    <div class="max-w-[80%] px-4 py-3 bg-amber-500 text-white rounded-2xl rounded-br-md">
                      我有 3 年 Vue 开发经验
                    </div>
                  </div>
                  <div class="flex justify-start">
                    <div class="max-w-[80%] px-4 py-3 bg-stone-100 text-stone-900 rounded-2xl rounded-bl-md">
                      很好，请描述一下你在项目中遇到的最大技术挑战是什么？你是如何解决的？
                    </div>
                  </div>
                  <div class="flex justify-end">
                    <div class="max-w-[80%] px-4 py-3 bg-amber-500 text-white rounded-2xl rounded-br-md">
                      我们遇到了性能优化问题...
                    </div>
                  </div>
                </div>

                <div class="mt-6 pt-4 border-t border-stone-100">
                  <div class="flex items-center gap-2 text-sm text-stone-500">
                    <div class="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                    <span>剩余时间 24:35</span>
                  </div>
                </div>
              </div>

              <!-- Decorative elements -->
              <div class="absolute -z-10 top-8 left-8 w-full h-full bg-gradient-to-br from-amber-100 to-orange-100 rounded-2xl"></div>
            </div>
          </div>

          <!-- Text -->
          <div class="order-1 lg:order-2">
            <div class="inline-flex items-center gap-2 px-3 py-1 bg-green-50 border border-green-200 rounded-full mb-6">
              <span class="text-sm text-green-700 font-medium">新功能</span>
            </div>

            <h2 class="text-4xl font-bold text-stone-900 mb-4">
              AI 模拟面试<br/>
              <span class="text-amber-600">你的私人教练</span>
            </h2>

            <p class="text-xl text-stone-500 leading-relaxed mb-6">
              无论你是应届生还是有经验的开发者，我们的 AI 面试官都能根据你的简历和目标岗位，生成个性化的面试问题。
            </p>

            <ul class="space-y-3 mb-8">
              <li class="flex items-center gap-3 text-stone-700">
                <span class="w-6 h-6 bg-amber-100 rounded-full flex items-center justify-center text-amber-600 text-sm">✓</span>
                支持 PDF、DOC、DOCX 格式简历解析
              </li>
              <li class="flex items-center gap-3 text-stone-700">
                <span class="w-6 h-6 bg-amber-100 rounded-full flex items-center justify-center text-amber-600 text-sm">✓</span>
                自定义 JD，精准匹配岗位需求
              </li>
              <li class="flex items-center gap-3 text-stone-700">
                <span class="w-6 h-6 bg-amber-100 rounded-full flex items-center justify-center text-amber-600 text-sm">✓</span>
                30 分钟模拟面试，实时反馈
              </li>
            </ul>

            <button
              @click="navigateToInterview"
              class="px-6 py-3 bg-amber-500 hover:bg-amber-600 text-white font-medium rounded-xl transition-all hover:shadow-lg hover:-translate-y-0.5"
            >
              立即体验
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section id="cta" class="py-20 bg-gradient-to-br from-amber-500 to-orange-600 text-white">
      <div class="max-w-4xl mx-auto px-6 text-center">
        <h2 class="text-4xl font-bold mb-4">准备好开始了吗？</h2>
        <p class="text-xl text-amber-100 mb-8">
          加入我们的社区，开启你的成长之旅
        </p>
        <div class="flex flex-wrap justify-center gap-4">
          <button
            @click="navigateToForum"
            class="px-6 py-3 bg-white text-amber-600 font-medium rounded-xl transition-all hover:shadow-lg hover:-translate-y-0.5"
          >
            进入论坛
          </button>
          <button
            @click="navigateToLogin"
            class="px-6 py-3 bg-transparent border-2 border-white text-white font-medium rounded-xl transition-all hover:bg-white/10"
          >
            注册账号
          </button>
        </div>
      </div>
    </section>
</template>

<style scoped>
@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-counter {
  animation: fade-in-up 0.6s ease-out;
}
</style>