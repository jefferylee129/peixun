/**
 * 客户端 CSV 导出(带 BOM, Excel 可直接打开)
 */
export function exportCsv(filename, headers, rows) {
  const head = headers.map(h => `"${h.label}"`).join(',')
  const body = rows
    .map(r => headers.map(h => `"${(r[h.prop] ?? '').toString().replace(/"/g, '""')}"`).join(','))
    .join('\n')
  const csv = '\ufeff' + head + '\n' + body
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
  URL.revokeObjectURL(link.href)
}
