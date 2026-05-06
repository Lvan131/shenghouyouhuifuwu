import request from './request'

export function getAdminAnnouncements() {
  return request.get('/admin/announcements')
}

export function createAnnouncement(payload) {
  return request.post('/admin/announcements', payload)
}

export function updateAnnouncement(announcementId, payload) {
  return request.put(`/admin/announcements/${announcementId}`, payload)
}
