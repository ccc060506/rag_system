import request from '@/utils/request'

// ==================== Auth ====================
export const authApi = {
  login: (data) => request.post('/user/center/login', data),
}

// ==================== User ====================
export const userApi = {
  getInfo:    ()     => request.get('/user/center/select'),
  update:     (data) => request.put('/user/center/update', data),
  register:   (data) => request.post('/user/center/register', data),
  getConfig:  ()     => request.get('/user/config/select'),
  verifyKey:  (apiKey) => request.post('/user/config/verify', null, { params: { apiKey } }),
  getOssSts: () => request.get('/user/center/oss/sts'), 
}

// ==================== Knowledge ====================
export const knowledgeApi = {
  upload: (formData) => request.post('/KnowledgeBase/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  getPage: (pageNum, pageSize, text) => {
    if (text) {
      return request.get(`/KnowledgeBase/pageByOffer/${pageNum}/${pageSize}`, { params: { text } })
    }
    return request.get(`/KnowledgeBase/page/${pageNum}/${pageSize}`)
  },
  deleteById: (id) => request.delete(`/KnowledgeBase/delete/${id}`),
}

// ==================== Chat ====================
export const chatApi = {
  // SSE流式接口需特殊处理，用fetch + EventSource
  streamUrl: '/api/ai/chat/stream',
}

// ==================== Log ====================
export const logApi = {
  getPage: (num, size) => request.get(`/log/page/${num}/${size}`),
}

// ==================== Sensitive Word ====================
export const sensitiveApi = {
  add:       (words)   => request.post('/sensitiveWord/add', words),
  update:    (id, dto) => request.put(`/sensitiveWord/update/${id}`, dto),
  deleteById:(id)      => request.delete(`/sensitiveWord/delete/${id}`),
  getById:   (id)      => request.get(`/sensitiveWord/select/${id}`),
  getPage:   (num, size, word) => {
    if (word) {
      return request.get(`/sensitiveWord/pageAllByOffer/${num}/${size}`, { params: { word } })
    }
    return request.get(`/sensitiveWord/pageAll/${num}/${size}`)
  },
  refreshDfa: () => request.post('/sensitiveWord/refresh'),
}