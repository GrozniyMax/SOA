<script setup lang="ts">
import { nextTick, onMounted, ref, watch, type Component } from 'vue'
import { useRoute } from 'vue-router'
import { IconAward, IconChartBar, IconMovie } from '@tabler/icons-vue'

interface NavItem {
  to: string
  label: string
  icon: Component
}

const items: NavItem[] = [
  { to: '/movies', label: 'Фильмы', icon: IconMovie },
  { to: '/analytics', label: 'Аналитика', icon: IconChartBar },
  { to: '/oscar', label: 'Оскары', icon: IconAward },
]

const route = useRoute()
const navRef = ref<HTMLElement | null>(null)
const pill = ref({ top: 0, left: 0, width: 0, height: 0, visible: false })

function movePill() {
  const nav = navRef.value
  if (!nav) return
  const active = nav.querySelector<HTMLElement>('.nav-link.active')
  if (!active) {
    pill.value = { ...pill.value, visible: false }
    return
  }
  pill.value = {
    top: active.offsetTop,
    left: active.offsetLeft,
    width: active.offsetWidth,
    height: active.offsetHeight,
    visible: true,
  }
}

onMounted(() => {
  void nextTick(movePill)
  window.addEventListener('resize', movePill)
})

watch(
  () => route.path,
  () => void nextTick(movePill),
)
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <span class="logo" aria-hidden="true" />
      <span class="brand-name">SOA Movies</span>
    </div>
    <nav ref="navRef" class="nav">
      <span
        class="pill"
        :class="{ visible: pill.visible }"
        :style="{
          transform: `translate(${pill.left}px, ${pill.top}px)`,
          width: `${pill.width}px`,
          height: `${pill.height}px`,
        }"
      />
      <RouterLink
        v-for="item in items"
        :key="item.to"
        :to="item.to"
        class="nav-link"
        active-class="active"
      >
        <component :is="item.icon" :size="18" stroke-width="1.75" class="nav-icon" />
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>
  </aside>
</template>

<style scoped>
.sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  padding: 20px 12px;
  border-right: 1px solid var(--border);
  background: var(--surface);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 10px 22px;
  font-weight: 600;
  font-size: 15px;
  letter-spacing: -0.01em;
  white-space: nowrap;
}

.logo {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border-radius: 7px;
  background: var(--accent);
  transition: transform var(--dur-slow) var(--ease-spring);
}

.brand:hover .logo {
  transform: rotate(-12deg) scale(1.06);
}

.nav {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.pill {
  position: absolute;
  top: 0;
  left: 0;
  border-radius: var(--radius);
  background: var(--accent-soft);
  opacity: 0;
  transition:
    transform var(--dur-slow) var(--ease),
    width var(--dur-slow) var(--ease),
    height var(--dur-slow) var(--ease),
    opacity var(--dur) var(--ease);
  pointer-events: none;
}

.pill.visible {
  opacity: 1;
}

.nav-link {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  height: 36px;
  padding: 0 12px;
  border-radius: var(--radius);
  color: var(--text-secondary);
  font-weight: 500;
  transition:
    color var(--dur) var(--ease),
    background var(--dur) var(--ease);
}

.nav-link:hover {
  color: var(--text);
}

.nav-link:not(.active):hover {
  background: var(--surface-muted);
}

.nav-link.active {
  color: var(--accent-text);
}

.nav-icon {
  flex-shrink: 0;
  transition: transform var(--dur) var(--ease);
}

.nav-link:hover .nav-icon {
  transform: translateX(2px) scale(1.06);
}

@media (max-width: 860px) {
  .sidebar {
    position: sticky;
    z-index: 10;
    height: auto;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    border-right: none;
    border-bottom: 1px solid var(--border);
    overflow-x: auto;
    scrollbar-width: none;
  }

  .brand {
    padding: 0 6px;
  }

  .brand-name {
    display: none;
  }

  .nav {
    flex-direction: row;
  }

  .nav-link {
    gap: 6px;
    padding: 0 10px;
    font-size: 13px;
    white-space: nowrap;
  }
}
</style>
