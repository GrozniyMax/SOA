export const EMPTY = '—'

const integerFormat = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 0 })
const decimalFormat = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 6 })
const moneyFormat = new Intl.NumberFormat('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const dateFormat = new Intl.DateTimeFormat('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric' })
const longDateFormat = new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' })

function parseIsoDate(value: string): Date | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value)
  if (!match) return null
  const [, y, m, d] = match
  return new Date(Number(y), Number(m) - 1, Number(d))
}

export function formatInteger(value: number | null | undefined): string {
  return value === null || value === undefined ? EMPTY : integerFormat.format(value)
}

export function formatDecimal(value: number | null | undefined): string {
  return value === null || value === undefined ? EMPTY : decimalFormat.format(value)
}

export function formatMoney(value: number | null | undefined): string {
  return value === null || value === undefined ? EMPTY : moneyFormat.format(value)
}

export function formatDate(value: string | null | undefined): string {
  if (!value) return EMPTY
  const date = parseIsoDate(value)
  return date ? dateFormat.format(date) : value
}

export function formatLongDate(value: string | null | undefined): string {
  if (!value) return EMPTY
  const date = parseIsoDate(value)
  return date ? longDateFormat.format(date) : value
}

export function formatText(value: string | null | undefined): string {
  return value === null || value === undefined || value === '' ? EMPTY : value
}

export function pluralize(count: number, one: string, few: string, many: string): string {
  const mod10 = count % 10
  const mod100 = count % 100
  if (mod10 === 1 && mod100 !== 11) return one
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few
  return many
}
