import request from '@/utils/request'

// 获取我的合同列表
export const getMyContractsApi = () => {
  return request.get('/landlord/contract/my')
}

// 上传我的合同
export const uploadMyContractApi = (formData: FormData) => {
  return request.post('/landlord/contract/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 删除我的合同
export const deleteMyContractApi = (id: string) => {
  return request.delete(`/landlord/contract/${id}`)
}

// 下载我的合同
export const downloadMyContractApi = (id: string) => {
  const token = localStorage.getItem('token_landlord')
  window.open(`http://localhost:8080/api/landlord/contract/download/${id}?token=${token}`, '_blank')
}
