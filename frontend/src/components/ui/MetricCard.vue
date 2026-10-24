<script setup lang="ts">
import { computed, toRef, type Component } from 'vue'
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue'
import { useCountUp } from '@/composables/useCountUp'
import { formatInteger } from '@/domain/format'

const props = withDefaults(
  defineProps<{
    label: string
    value: number | null
    loading?: boolean
    icon?: Component
    caption?: string
  }>(),
  {
    loading: false,
    icon: undefined,
    caption: undefined,
  },
)

const animated = useCountUp(toRef(props, 'value'))
const text = computed(() => formatInteger(animated.value))
</script>

<template>
  <div class="metric">
    <div class="metric-head">
      <span class="metric-label">{{ label }}</span>
      <component :is="icon" v-if="icon" class="metric-icon" :size="16" stroke-width="2" />
    </div>
    <SkeletonBlock v-if="loading && value === null" width="64px" height="28px" radius="8px" />
    <p v-else class="metric-value tabular">{{ text }}</p>
    <p v-if="caption" class="metric-caption">{{ caption }}</p>
  </div>
</template>

<style scoped>
.metric {
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  transition:
    transform var(--dur-slow) var(--ease),
    border-color var(--dur) var(--ease),
    box-shadow var(--dur-slow) var(--ease);
}

.metric:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--accent) 30%, var(--border));
  box-shadow: 0 8px 20px color-mix(in srgb, var(--accent) 8%, transparent);
}

.metric-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.metric-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.metric-icon {
  color: var(--text-muted);
  transition:
    color var(--dur) var(--ease),
    transform var(--dur-slow) var(--ease-spring);
}

.metric:hover .metric-icon {
  color: var(--accent);
  transform: scale(1.15) rotate(-6deg);
}

.metric-value {
  font-size: 26px;
  font-weight: 600;
  line-height: 28px;
  letter-spacing: -0.02em;
}

.metric-caption {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
}
</style>
