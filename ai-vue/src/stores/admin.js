import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAdminStore = defineStore('admin',()=>{
    const isAdmin = ref(false)
    const toggleAdminStatus = ()=>{
        isAdmin.value = !isAdmin.value
    }
    return {
        isAdmin,
        toggleAdminStatus
    }
})