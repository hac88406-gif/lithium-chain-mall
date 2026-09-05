/* ==========================================================================
 * SPU / SKU 聚合工具（所有"商品列表/排行 → 按产品基础名聚合"的场景都调用这里）
 *
 * 用法：
 *   import { spuKeyOf, aggregateSpu, extractSpec } from '@/utils/spu'
 *   const spuList = aggregateSpu(skuArray)
 * ========================================================================== */

/**
 * 取 SPU Key（产品基础名聚合键）：把商品名中的规格信息（电压/容量/电流/尺寸/
 * 串并联写法/型号编码/卖点后缀）全部去掉，相同基础产品会返回相同 key。
 *
 * 例：
 *   「18650锂电池组 48V 【LC-128E1】A品电芯·电动车专用」 → 「18650锂电池组」
 *   「磷酸铁锂 18650 电芯 3200mAh」 → 「磷酸铁锂 18650 电芯」
 *   「户外便携储能电源 2kWh 220V」 → 「户外便携储能电源」
 *   「户用光储一体机 10kWh 堆叠式」 → 「户用光储一体机 堆叠式」
 */
export const spuKeyOf = (p) => {
  let s = (p?.name ?? String(p ?? '')).trim()
  // 1) 去掉【型号编码】包裹
  s = s.replace(/【[^】]*】/g, '')
  // 2) 去掉 ·分隔号后面的"卖点说明"（电动车专用/快充版/长续航等）
  s = s.replace(/\s*·\s*.+$/, '')
  // 3) 去掉电压/容量/电流/尺寸等规格单位（迭代 4 次清理多规格连写）
  const unitPattern =
    /\s*(?:\d+(?:\.\d+)?\s*(?:V|KV|Ah|mAh|kWh|MWh|Wh|A|W|kW|MW|S|P|C|mm|cm|kg|节|只|路|组|°|度|串)?\s*(?:\([^\)]*\))?)/gi
  for (let i = 0; i < 4; i++) {
    const before = s
    s = s.replace(unitPattern, ' ')
    if (s === before) break
  }
  // 4) 去掉 (13S10P) / (12.8V) 这种括号规格（前面没清干净的）
  s = s.replace(/\s*\([^\)]*\d[^\)]*\)/g, '')
  // 5) 去掉"数字×/~/~数字"范围的数字规格 10-100 / 0~60 / 2×3
  s = s.replace(/\s*\d+(?:\.\d+)?(?:\s*[×xX\-~]\s*\d+(?:\.\d+)?)\s*(?:V|A|Ah|Wh|kW|mm)?/g, ' ')
  // 6) 去掉末尾悬空的纯数字串（连续 2+ 位）
  s = s.replace(/\s+\d{2,}$/g, ' ')
  // 压缩空白
  s = s.replace(/\s+/g, ' ').trim()
  return s || (p?.name || '未命名商品')
}

/**
 * 从商品名里把"规格描述字符串"提取出来，用于详情页「型号选择器」显示。
 *   例：18650锂电池组 48V 【LC-128E1】A品电芯·电动车专用 → 「48V · LC-128E1」
 *   例：户外便携储能电源 2kWh 220V → 「2kWh · 220V」
 */
export const extractSpec = (p) => {
  const name = p?.name ?? ''
  // 抓【】里的型号编码
  const codeMatch = name.match(/【([^】]+)】/)
  const code = codeMatch ? codeMatch[1].trim() : ''
  // 抓所有带单位的数字规格（保留顺序、去重）
  const unitRegex = /\d+(?:\.\d+)?\s*(?:V|KV|Ah|mAh|kWh|MWh|Wh|A|W|kW|MW|S|P|C|mm|cm|kg|节|只|路|组|°|度|串)/gi
  const unitMatches = name.match(unitRegex) || []
  const specs = [...new Set(unitMatches.map((s) => s.replace(/\s+/g, '')))]
  if (code) specs.unshift(code)
  // 如果啥都没有，用分类前缀 + id 兜底
  if (!specs.length) {
    const CATEGORY_PREFIX = { 1: 'LC', 2: 'PB', 3: 'ES', 4: 'BM', 5: 'PV', 6: 'CG' }
    const cid = Number(p?.categoryId) || 0
    return `${CATEGORY_PREFIX[cid] || 'GL'}-${String(p?.id ?? 0).padStart(3, '0')}`
  }
  return specs.join(' · ')
}

/**
 * 把 SKU 列表按 SPU 聚合，返回 SPU 数组。
 * 每个 SPU 结构：
 *  {
 *    _spuKey, spuName, categoryId, categoryName,
 *    id,                           // 销量最高 SKU 的 id（点击跳详情用）
 *    image, description,
 *    skuCount,                     // 本 SPU 有多少款规格
 *    minPrice, maxPrice,
 *    totalSales, totalStock,
 *    variants: [{sku, ...}]        // 原始 SKU 列表（已按销量倒序）
 *    // 向后兼容 alias（模板不用改）：
 *    name = spuName, price = minPrice, sales = totalSales, stock = totalStock
 *  }
 */
export const aggregateSpu = (skuList) => {
  const map = new Map()
  for (const sku of skuList || []) {
    const key = `${sku.categoryId || 0}#${spuKeyOf(sku)}`
    if (!map.has(key)) {
      map.set(key, {
        _spuKey: key,
        spuName: spuKeyOf(sku),
        categoryId: sku.categoryId,
        categoryName: sku.categoryName,
        id: sku.id,
        image: sku.image,
        description: sku.description,
        minPrice: Number(sku.price),
        maxPrice: Number(sku.price),
        totalSales: Number(sku.sales || 0),
        totalStock: Number(sku.stock || 0),
        skuCount: 1,
        variants: [sku],
        _bestSkuSales: Number(sku.sales || 0)
      })
    } else {
      const spu = map.get(key)
      spu.variants.push(sku)
      spu.skuCount += 1
      spu.minPrice = Math.min(spu.minPrice, Number(sku.price))
      spu.maxPrice = Math.max(spu.maxPrice, Number(sku.price))
      spu.totalSales += Number(sku.sales || 0)
      spu.totalStock += Number(sku.stock || 0)
      if (Number(sku.sales || 0) > spu._bestSkuSales) {
        spu._bestSkuSales = Number(sku.sales || 0)
        spu.id = sku.id
        spu.image = sku.image
        spu.description = sku.description
      }
    }
  }

  // variants 内部按销量倒序
  const list = Array.from(map.values()).sort(
    (a, b) => b.totalSales - a.totalSales || b.skuCount - a.skuCount
  )
  for (const spu of list) {
    spu.variants.sort((a, b) => Number(b.sales || 0) - Number(a.sales || 0))
    // 兼容 alias（模板可直接用 product.name / product.price / product.sales / product.stock）
    Object.defineProperty(spu, 'name', { get: () => spu.spuName, configurable: true })
    Object.defineProperty(spu, 'price', { get: () => spu.minPrice, configurable: true })
    Object.defineProperty(spu, 'sales', { get: () => spu.totalSales, configurable: true })
    Object.defineProperty(spu, 'stock', { get: () => spu.totalStock, configurable: true })
  }
  return list
}
