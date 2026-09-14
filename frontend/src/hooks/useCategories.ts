import { useQuery } from '@tanstack/react-query'
import { shopApi } from '../api/shop'

/**
 * 店铺分类字典（F2）：全站共享一份缓存。
 * 分类属运行期内不变的静态字典，staleTime Infinity —— 三处使用方（店铺列表/管理列表/管理编辑）
 * 不再各自请求（F2 现状问题 3「重复拉取」的落点）。
 */
export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => shopApi.categories(),
    staleTime: Infinity,
  })
}
