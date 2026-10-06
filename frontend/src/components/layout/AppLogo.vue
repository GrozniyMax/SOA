<template>
  <span class="logo" aria-hidden="true">
    <svg viewBox="0 0 24 24" width="16" height="16">
      <rect x="4" y="10.5" width="16" height="9.5" rx="2" fill="currentColor" />
      <g class="stick">
        <rect x="4" y="6" width="16" height="3.4" rx="1" fill="currentColor" />
        <path d="M7.4 6h2.4l-2 3.4H5.4zM12.2 6h2.4l-2 3.4h-2.4zM17 6h2.4l-2 3.4h-2.4z" class="stripe" />
      </g>
    </svg>
  </span>
</template>

<style scoped>
.logo {
  display: inline-grid;
  place-items: center;
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: var(--accent);
  color: var(--on-accent);
}

.stripe {
  fill: var(--accent);
}

.stick {
  transform-box: view-box;
  transform-origin: 4px 9.4px;
  transform: rotate(-16deg);
}
</style>
