/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_MOVIE_API?: string
  readonly VITE_OSCAR_API?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
