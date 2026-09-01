/** 交错入场延迟（ms）：index 超过 cap 后不再递增，避免长列表末尾等太久 */
export function staggerDelay(index: number, step = 55, cap = 10): number {
  return Math.min(index, cap) * step
}
