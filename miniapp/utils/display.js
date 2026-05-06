function formatDistance(distanceKm) {
  const value = Number(distanceKm)
  if (!Number.isFinite(value) || value < 0) {
    return ''
  }
  if (value < 1) {
    return `${Math.round(value * 1000)}m`
  }
  if (Number.isInteger(value)) {
    return `${value}km`
  }
  return `${value.toFixed(1)}km`
}

function formatRating(avgScore, ratingCount) {
  const count = Number(ratingCount) || 0
  const score = Number(avgScore)
  if (count <= 0 || !Number.isFinite(score)) {
    return '暂无评价'
  }
  return `${score.toFixed(1)}分 · ${count}条评价`
}

function activitySummary(item) {
  if (item.content) {
    return item.content
  }
  return item.title || ''
}

module.exports = {
  formatDistance,
  formatRating,
  activitySummary
}
