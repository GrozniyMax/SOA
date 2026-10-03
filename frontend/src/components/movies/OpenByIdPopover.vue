<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { IconHash } from '@tabler/icons-vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import FormField from '@/components/ui/FormField.vue'
import PopoverPanel from '@/components/ui/PopoverPanel.vue'

const emit = defineEmits<{
  open: [id: number]
}>()

const open = ref(false)
const value = ref('')
const error = ref<string | null>(null)
const inputRef = ref<InstanceType<typeof BaseInput> | null>(null)

watch(open, (isOpen) => {
  if (!isOpen) return
  value.value = ''
  error.value = null
  void nextTick(() => inputRef.value?.focus())
})

watch(value, () => {
  error.value = null
})

function submit() {
  const raw = value.value.trim()
  if (!/^\d+$/.test(raw) || Number(raw) < 1 || Number(raw) > 2147483647) {
    error.value = 'Нужно целое число больше 0'
    return
  }
  emit('open', Number(raw))
  open.value = false
}
</script>

<template>
  <PopoverPanel v-model:open="open" align="end" width="280px">
    <template #trigger>
      <BaseButton variant="secondary" :icon="IconHash" @click="open = !open">Открыть по ID</BaseButton>
    </template>
    <form class="by-id" @submit.prevent="submit">
      <FormField label="ID фильма" for-id="open-by-id" :error="error">
        <BaseInput id="open-by-id" ref="inputRef" v-model="value" placeholder="12" size="sm" :invalid="!!error" />
      </FormField>
      <BaseButton type="submit" size="sm" variant="primary">Открыть</BaseButton>
    </form>
  </PopoverPanel>
</template>

<style scoped>
.by-id {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: end;
}

.by-id :deep(.field-error) {
  position: absolute;
}

.by-id :deep(.field) {
  position: relative;
}
</style>
