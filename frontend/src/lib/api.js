import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
const apiToken = import.meta.env.VITE_API_TOKEN

const api = axios.create({
  baseURL,
})

api.interceptors.request.use((config) => {
  if (apiToken) {
    config.headers['X-API-Token'] = apiToken
  }
  return config
})

export default api
