<template>
  <div class="backend-layout">
    <el-container class="main-container">
      <Sidebar />
      <el-container>
        <el-header class="backend-header">
          <Navbar />
        </el-header>
        <el-main class="main-content">
          <router-view class="content-container" />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import Sidebar from './Sidebar.vue'
import Navbar from './Navbar.vue'
</script>

<style lang="scss" scoped>
.backend-layout {
  height: 100vh;
  background: #eef2f7;

  .main-container {
    height: 100%;
  }

  .backend-header {
    height: 74px !important;
    padding: 0 !important;
  }

  .main-content {
    padding: 20px;
    background: #eef2f7;
  }

  .content-container {
    min-height: calc(100vh - 114px);
    padding: 20px;
    border-radius: 10px;
    background: #fff;
  }
}
</style>
