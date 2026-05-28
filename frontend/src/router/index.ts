import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/pages/HomePage.vue')
    },
    {
      path: '/forum',
      name: 'forum',
      component: () => import('@/pages/ForumPage.vue')
    },
    {
      path: '/post/:id',
      name: 'post-detail',
      component: () => import('@/pages/PostDetailPage.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/post/create',
      name: 'create-post',
      component: () => import('@/pages/CreatePostPage.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/post/:id/edit',
      name: 'edit-post',
      component: () => import('@/pages/EditPostPage.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('@/pages/ChatPage.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/chat/:friendId',
      name: 'private-message',
      component: () => import('@/pages/PrivateMessagePage.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/pages/ProfilePage.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/auth/login',
      name: 'login',
      component: () => import('@/pages/LoginPage.vue')
    },
    {
      path: '/auth/register',
      name: 'register',
      component: () => import('@/pages/RegisterPage.vue')
    },
    {
      path: '/auth/forgot-password',
      name: 'forgot-password',
      component: () => import('@/pages/ForgotPasswordPage.vue')
    },
    {
      path: '/interview',
      name: 'interview',
      component: () => import('@/pages/InterviewPage.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/interview/:id',
      name: 'interview-detail',
      component: () => import('@/pages/InterviewDetailPage.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/blog',
      name: 'blog',
      component: () => import('@/pages/BlogPage.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/update_knowledge',
      name: 'update-knowledge',
      component: () => import('@/pages/UpdateKnowledgePage.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next({ name: 'login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
