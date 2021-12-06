<script lang="ts" setup="">


</script>

<template>
  <div class="line"></div>
</template>

<style lang="scss" scoped>
.line{
  text-align: center;
  width: 100%;
  height: 3px;
  border-radius:10px;
  margin: 20px;
  background: linear-gradient(30deg, #656565, #3d4149, #192754, #030405);
  background-size: 1400% 300%;
  animation: mymove 2.5s ease-in-out infinite alternate;
}
@keyframes mymove {
  0% {
    background-position: 0 0;
  }
  50% {
    background-position: 50% 100%;
  }
  100% {
    background-position: 100% 0;
  }
}
</style>
