import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/movies' },
  {
    path: '/movies',
    name: 'movies',
    component: () => import('@/views/MoviesView.vue'),
    meta: { title: 'Фильмы' },
  },
  {
    path: '/analytics',
    name: 'analytics',
    component: () => import('@/views/AnalyticsView.vue'),
    meta: { title: 'Аналитика' },
  },
  {
    path: '/oscar',
    name: 'oscar',
    component: () => import('@/views/OscarView.vue'),
    meta: { title: 'Оскары' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: 'Страница не найдена' },
  },
]

export const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : ''
  document.title = title ? `${title} · SOA Movies` : 'SOA Movies'
})
